/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import java.util.*;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.ResourceSaveableCompareModel.ResourceDiffCompareInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * A abstract implementation of {@link ISynchronizationCompareAdapter}. Most of the methods
 * are no-ops except for the {@link #asCompareInput(ISynchronizationContext, Object) }
 * which will convert file objects to an appropriate compare input.
 * <p>
 * Clients may subclass this class.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * 
 * @since 3.2
 */
public class SynchronizationCompareAdapter implements ISynchronizationCompareAdapter {

	/**
	 * Default implementaton that is capable of returning a compare input for objects
	 * that adapt to {@link IFile}. Subclasses shoudl override if compare inputs are
	 * availabel for other types of model elements.
	 * @see ISynchronizationCompareAdapter#asCompareInput(ISynchronizationContext, Object)
	 */
	public ICompareInput asCompareInput(ISynchronizationContext context, Object o) {
		IResource resource = Utils.getResource(o);
		if (resource != null) {
			if (resource.getType() == IResource.FILE) {
				IDiff node = context.getDiffTree().getDiff(resource);
				if (node != null)
					return new ResourceDiffCompareInput(node);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ICompareAdapter#hasCompareInput(org.eclipse.team.core.mapping.ISynchronizationContext, java.lang.Object)
	 */
	public boolean hasCompareInput(ISynchronizationContext context, Object object) {
		return asCompareInput(context, object) != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IResourceMappingPersistenceAdapter#getName(org.eclipse.core.resources.mapping.ResourceMapping)
	 */
	public String getName(ResourceMapping mapping) {
		Object object = mapping.getModelObject();
		IWorkbenchAdapter adapter = (IWorkbenchAdapter) Utils.getAdapter(
				object, IWorkbenchAdapter.class);
		if (adapter != null) {
			String label = adapter.getLabel(object);
			if (label != null)
				return label;
		}
		IResource resource = Utils.getResource(object);
		if (resource != null)
			return resource.getName();
		if (object instanceof ModelProvider) {
			ModelProvider provider = (ModelProvider) object;
			return provider.getDescriptor().getLabel();
		}
		return ""; //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IResourceMappingPersistenceAdapter#getFullPath(org.eclipse.core.resources.mapping.ResourceMapping)
	 */
	public String getPathString(ResourceMapping mapping) {
		Object object = mapping.getModelObject();
		IWorkbenchAdapter adapter = (IWorkbenchAdapter) Utils.getAdapter(
				object, IWorkbenchAdapter.class);
		if (adapter != null) {
			List segments = new ArrayList();
			Object parent = object;
			do {
				String segment = adapter.getLabel(parent);
				if (segment != null && segment.length() > 0)
					segments.add(0, segment);
				parent = adapter.getParent(parent);
			} while (parent != null);
			if (!segments.isEmpty()) {
				IPath path = Path.EMPTY;
				for (Iterator iter = segments.iterator(); iter.hasNext();) {
					String segment = (String) iter.next();
					path = path.append(segment);
				}
				return path.toString();
			}
		}
		return getName(mapping);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter#save(org.eclipse.core.resources.mapping.ResourceMapping[], org.eclipse.ui.IMemento)
	 */
	public void save(ResourceMapping[] mappings, IMemento memento) {
		// Do nothing by default
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter#restore(org.eclipse.ui.IMemento)
	 */
	public ResourceMapping[] restore(IMemento memento) {
		return new ResourceMapping[0];
	}
}