#!/usr/bin/env python3
"""Build the Eclipse CVS Client p2 repository archive."""
import argparse
import os
import re
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

SITE = Path(__file__).resolve().parent
ROOT = SITE.parents[1]


def build_properties(project):
    text = (project / "build.properties").read_text().replace("\\\n", "")
    properties = {}
    for line in text.splitlines():
        if "=" in line and not line.startswith("#"):
            key, values = line.split("=", 1)
            properties[key.strip()] = [value.strip() for value in values.split(",")]
    return properties


def add_files(archive, root, path, exclude=()):
    paths = sorted(path.rglob("*")) if path.is_dir() else [path]
    for entry in paths:
        if entry.is_file() and entry not in exclude:
            archive.write(entry, entry.relative_to(root))


def build(sdk, qualifier, output, work):
    plugins = sdk / "plugins"
    compiler, = plugins.glob("org.eclipse.jdt.core.compiler.batch_*.jar")
    launcher, = plugins.glob("org.eclipse.equinox.launcher_*.jar")
    java = str(Path(os.environ["JAVA_HOME"]) / "bin/java") if "JAVA_HOME" in os.environ else "java"
    classpath = [p for p in sorted(plugins.glob("*.jar")) if ".source_" not in p.name]
    source = work / "input"
    (source / "plugins").mkdir(parents=True)
    (source / "features").mkdir()
    feature_dir = ROOT / "features/org.eclipse.cvs-feature"
    feature = ET.parse(feature_dir / "feature.xml")
    for plugin in feature.findall("plugin"):
        bundle_id = plugin.attrib["id"]
        project = ROOT / "bundles" / bundle_id
        manifest = project / "META-INF/MANIFEST.MF"
        text = manifest.read_text()
        version = re.search(r"^Bundle-Version: (\S+)\.qualifier$", text, re.M)[1] + "." + qualifier
        plugin.set("version", version)
        properties = build_properties(project)
        with ZipFile(source / "plugins" / f"{bundle_id}_{version}.jar", "w", ZIP_DEFLATED) as jar:
            jar.writestr("META-INF/MANIFEST.MF", text.replace(".qualifier", "." + qualifier).replace("\n", "\r\n"))
            for entry in properties["bin.includes"]:
                if entry != ".":
                    add_files(jar, project, project / entry, (manifest,))
                    continue
                classes = work / bundle_id
                classes.mkdir()
                roots = [project / path for path in properties["source.."]]
                subprocess.run([java, "-jar", compiler, "-17", "-encoding", "UTF-8", "-nowarn",
                                "-proc:none", "-classpath", os.pathsep.join(map(str, classpath)),
                                "-d", classes, *roots], check=True)
                classpath.insert(0, classes)
                add_files(jar, classes, classes)
                for root in roots:
                    for resource in sorted(root.rglob("*")):
                        if resource.is_file() and resource.suffix != ".java":
                            jar.write(resource, resource.relative_to(root))

    feature_id = feature.getroot().attrib["id"]
    version = feature.getroot().attrib["version"].replace(".qualifier", "." + qualifier)
    feature.getroot().set("version", version)
    feature_jar = f"{feature_id}_{version}.jar"
    with ZipFile(source / "features" / feature_jar, "w", ZIP_DEFLATED) as jar:
        jar.writestr("feature.xml", ET.tostring(feature.getroot(), encoding="utf-8", xml_declaration=True))
        for entry in build_properties(feature_dir)["bin.includes"]:
            if entry != "feature.xml":
                add_files(jar, feature_dir, feature_dir / entry)

    category = ET.parse(SITE / "site.xml")
    reference = category.find(f"feature[@id='{feature_id}']")
    reference.set("url", "features/" + feature_jar)
    reference.set("version", version)
    category_file = work / "category.xml"
    category.write(category_file, encoding="utf-8", xml_declaration=True)
    repository = work / "repository"
    command = [java, "-Duser.home=" + str(work), "-Dosgi.configuration.cascaded=true",
               "-Dosgi.sharedConfiguration.area=" + (sdk / "configuration").as_uri(),
               "-jar", launcher, "-nosplash", "-configuration", work / "configuration",
               "-data", work / "workspace", "-consoleLog", "-application"]
    publisher = "org.eclipse.equinox.p2.publisher."
    subprocess.run(command + [publisher + "FeaturesAndBundlesPublisher", "-source", source,
                   "-metadataRepository", repository.as_uri(), "-artifactRepository", repository.as_uri(),
                   "-publishArtifacts", "-compress"], check=True)
    subprocess.run(command + [publisher + "CategoryPublisher", "-metadataRepository", repository.as_uri(),
                   "-categoryDefinition", category_file.as_uri(), "-compress"], check=True)
    with ZipFile(work / "site.zip", "w", ZIP_DEFLATED) as archive:
        add_files(archive, repository, repository)
    shutil.copyfile(work / "site.zip", output)
    print(output)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("sdk", type=Path, help="unpacked Eclipse SDK 4.42 directory")
    parser.add_argument("qualifier", nargs="?", default=datetime.now(timezone.utc).strftime("%Y%m%d%H%M"),
                        help="UTC timestamp in YYYYMMDDHHMM format (default: current UTC minute)")
    parser.add_argument("output", type=Path, help="destination ZIP")
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9]{12}", args.qualifier):
        parser.error("qualifier must be a 12-digit UTC timestamp")
    with tempfile.TemporaryDirectory(prefix="eclipse-cvs-") as temp:
        build(args.sdk.resolve(), args.qualifier, args.output, Path(temp))
