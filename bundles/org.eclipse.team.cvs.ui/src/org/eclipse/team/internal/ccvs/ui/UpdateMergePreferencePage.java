package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.jface.preference.RadioGroupFieldEditor;

public class UpdateMergePreferencePage extends CVSFieldEditorPreferencePage {

	private RadioGroupFieldEditor updateStyle;
	private RadioGroupFieldEditor updatePreviewStyle;

	protected String getPageHelpContextId() {
		// TODO Auto-generated method stub
		return null;
	}

	protected String getPageDescription() {
		return null;
	}

	protected void createFieldEditors() {
		
		updateStyle = new RadioGroupFieldEditor(
						ICVSUIConstants.PREF_UPDATE_HANDLING,
						CVSUIMessages.WorkInProgressPage_0,
						1,
						new String[][] {
							new String[] {CVSUIMessages.WorkInProgressPage_1, ICVSUIConstants.PREF_UPDATE_HANDLING_PREVIEW},
							new String[] {CVSUIMessages.WorkInProgressPage_2, ICVSUIConstants.PREF_UPDATE_HANDLING_PERFORM},
							new String[] {CVSUIMessages.UpdateMergePreferencePage_0, ICVSUIConstants.PREF_UPDATE_HANDLING_TRADITIONAL}
						},
						getFieldEditorParent(),
						true);
		addField(updateStyle);
		
		updatePreviewStyle = new RadioGroupFieldEditor(
				ICVSUIConstants.PREF_UPDATE_PREVIEW,
				CVSUIMessages.UpdateMergePreferencePage_1,
				1,
				new String[][] {
					new String[] {CVSUIMessages.UpdateMergePreferencePage_2,  ICVSUIConstants.PREF_UPDATE_PREVIEW_IN_DIALOG},
					new String[] {CVSUIMessages.UpdateMergePreferencePage_3, ICVSUIConstants.PREF_UPDATE_PREVIEW_IN_SYNCVIEW}
				},
				getFieldEditorParent(),
				true);
		addField(updatePreviewStyle);
		
		// TODO: Add option for sync and compare to use old or new (add to sync compare page)
		
	}

}