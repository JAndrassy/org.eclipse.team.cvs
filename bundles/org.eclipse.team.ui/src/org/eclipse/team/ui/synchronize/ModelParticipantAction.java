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
package org.eclipse.team.ui.synchronize;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.ui.mapping.ISaveableCompareModel;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

/**
 * Model provider actions for use with a {@link ModelSynchronizeParticipant}.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * 
 * @since 3.2
 */
public abstract class ModelParticipantAction extends BaseSelectionListenerAction {

	private final ISynchronizePageConfiguration configuration;

	/**
	 * Create the model participant action.
	 * @param text the label of the action or <code>null</code>
	 * @param configuration the configuration for the page that is surfacing the action
	 */
	public ModelParticipantAction(String text, ISynchronizePageConfiguration configuration) {
		super(text);
		this.configuration = configuration;
		initialize(configuration);
	}

	private void initialize(ISynchronizePageConfiguration configuration) {
		configuration.getSite().getSelectionProvider().addSelectionChangedListener(this);
		configuration.getPage().getViewer().getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				getConfiguration().getSite().getSelectionProvider().removeSelectionChangedListener(ModelParticipantAction.this);
			}
		});
	}

	/**
	 * Return the page configuration.
	 * @return the page configuration
	 */
	protected ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}
	
	/**
	 * Set the selection of this action to the given selection
	 * 
	 * @param selection the selection
	 */
	public void selectionChanged(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			super.selectionChanged((IStructuredSelection)selection);
		} else {
			super.selectionChanged(StructuredSelection.EMPTY);
		}
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.BaseSelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected boolean updateSelection(IStructuredSelection selection) {
		super.updateSelection(selection);
		return isEnabledForSelection(selection);
	}
	
	/**
	 * Return whether the action is enabled for the given selection
	 * @param selection the selection
	 * @return whether the action is enabled for the given selection
	 */
	protected abstract boolean isEnabledForSelection(IStructuredSelection selection);

	/**
	 * Return the synchronization context associated with this action.
	 * @return the synchronization context associated with this action
	 */
	protected ISynchronizationContext getSynchronizationContext() {
		return (ISynchronizationContext)getConfiguration().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT);
	}

	/**
	 * Return whether the given node is visible in the page based
	 * on the mode in the configuration.
	 * @param node a diff node
	 * @return whether the given node is visible in the page
	 */
	protected boolean isVisible(IDiff node) {
		ISynchronizePageConfiguration configuration = getConfiguration();
		if (configuration.getComparisonType() == ISynchronizePageConfiguration.THREE_WAY 
				&& node instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) node;
			int mode = configuration.getMode();
			switch (mode) {
			case ISynchronizePageConfiguration.INCOMING_MODE:
				if (twd.getDirection() == IThreeWayDiff.CONFLICTING || twd.getDirection() == IThreeWayDiff.INCOMING) {
					return true;
				}
				break;
			case ISynchronizePageConfiguration.OUTGOING_MODE:
				if (twd.getDirection() == IThreeWayDiff.CONFLICTING || twd.getDirection() == IThreeWayDiff.OUTGOING) {
					return true;
				}
				break;
			case ISynchronizePageConfiguration.CONFLICTING_MODE:
				if (twd.getDirection() == IThreeWayDiff.CONFLICTING) {
					return true;
				}
				break;
			case ISynchronizePageConfiguration.BOTH_MODE:
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Check to see if the target saveable mode; differs from the currently 
	 * active model. If it does, prompt to save changes in the
	 * active model if it is dirty.
	 * @throws InterruptedException 
	 * @throws InvocationTargetException 
	 */
	protected void handleModelChange() throws InvocationTargetException, InterruptedException {
		final ISaveableCompareModel targetBuffer = getTargetModel();
		final ISaveableCompareModel  currentBuffer = getActiveModel();
		if (currentBuffer != null && currentBuffer.isDirty()) {
			PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {	
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try {
						handleBufferChange(configuration.getSite().getShell(), targetBuffer, currentBuffer, true, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		}
		setActiveModel(targetBuffer);
	}

	/**
	 * Convenience method that prompts if the currently active model is dirty
	 * and eitehr saves or reverts the model depending on the users input.
	 * @param shell a parent shell
	 * @param targetModel the new model
	 * @param activeModel the current model
	 * @param allowCancel whether cancelling the action is an option
	 * @param monitor a progress monitor
	 * @throws CoreException
	 * @throws InterruptedException
	 */
	public static void handleBufferChange(Shell shell, ISaveableCompareModel targetModel, ISaveableCompareModel activeModel, boolean allowCancel, IProgressMonitor monitor) throws CoreException, InterruptedException {
		if (activeModel != null && targetModel != activeModel) {
			if (activeModel.isDirty()) {
				if (promptToSaveChanges(shell, activeModel, allowCancel)) {
					activeModel.doSave(monitor);
				} else {
					activeModel.doRevert(monitor);
				}
			}
		}
	}

	/**
	 * Convenience method that prompts to save changes in the given dirty model.
	 * @param shell a shell
	 * @param saveableModel a dirty saveable model
	 * @param allowCancel whether cancelling the action is an option
	 * @return whether the usr choose to save (<code>true</code>) or revert (<code>false</code>() the model
	 * @throws InterruptedException thrown if the user choose to cancel
	 */
	public static boolean promptToSaveChanges(final Shell shell, final ISaveableCompareModel saveableModel, final boolean allowCancel) throws InterruptedException {
		final int[] result = new int[] { 0 };
		Runnable runnable = new Runnable() {
			public void run() {
				String[] options;
				if (allowCancel) {
					options = new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL};
				} else {
					options = new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL};
				}
				MessageDialog dialog = new MessageDialog(
						shell, 
						"Save Changes", null, 
						NLS.bind("{0} has unsaved changes. Do you want to save them?", saveableModel.getName()),
						MessageDialog.QUESTION,
						options,
						result[0]);
				result[0] = dialog.open();
			}
		};
		shell.getDisplay().syncExec(runnable);
		if (result[0] == 2)
			throw new InterruptedException();
		return result[0] == 0;
	}

	/**
	 * Return the currently active buffer. By default,
	 * the active buffer is obtained from the synchronization
	 * page configuration.
	 * @return the currently active buffer (or <code>null</code> if
	 * no buffer is active).
	 */
	protected ISaveableCompareModel getActiveModel() {
		return ((ModelSynchronizeParticipant)configuration.getParticipant()).getActiveModel();
	}

	/**
	 * Set the active buffer. By default to active buffer is stored with the
	 * snchronize page configuration.
	 * @param buffer the buffer that is now active (or <code>null</code> if
	 * no buffer is active).
	 */
	protected void setActiveModel(ISaveableCompareModel buffer) {
		((ModelSynchronizeParticipant)configuration.getParticipant()).setActiveModel(buffer);
	}
	
	/**
	 * Return the buffer that is the target of this operation.
	 * By default, <code>null</code> is returned.
	 * @return the buffer that is the target of this operation
	 */
	protected ISaveableCompareModel getTargetModel() {
		return null;
	}
	
}