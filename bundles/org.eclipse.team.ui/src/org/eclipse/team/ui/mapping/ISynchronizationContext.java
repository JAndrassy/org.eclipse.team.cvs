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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;

/**
 * Allows a model provider to build a view of their model that includes
 * synchronization information with a remote location (usually a repository).
 * <p>
 * The scope of the context is defined when the context is created. The creator
 * of the scope may affect changes on the scope which will result in property
 * change events from the scope and may result in sync-info change events from
 * the sync-info tree. Clients should note that it is possible that a change in
 * the scope will result in new out-of-sync resources being covered by the scope
 * but not result in a sync-info change event from the sync-info tree. This can
 * occur because the set may already have contained the out-of-sync resource
 * with the understanding that the client would have ignored it. Consequently,
 * clients should listen to both sources in order to guarantee that they update
 * any dependent state appropriately.
 * <p>
 * This interface is not intended to be implemented by clients.
 * 
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * 
 * @since 3.2
 */
public interface ISynchronizationContext extends ITeamViewerContext {

	/**
	 * Synchronization type constant that indicates that
	 * context is a two-way synchronization.
	 */
	public final static String TWO_WAY = "two-way"; //$NON-NLS-1$
	
	/**
	 * Synchronization type constant that indicates that
	 * context is a three-way synchronization.
	 */
	public final static String THREE_WAY = "three-way"; //$NON-NLS-1$

	/**
	 * Return the scope of this synchronization context. The scope determines
	 * the set of resources to which the context applies. Changes in the scope
	 * may result in changes to the sync-info available in the tree of this
	 * context.
	 * 
	 * @return the set of mappings for which this context applies.
	 */
	public ISynchronizeScope getScope();

	/**
	 * Return a tree that contains <code>SyncInfo</code> nodes for resources
	 * that are out-of-sync. The tree will contain sync-info for any out-of-sync
	 * resources that are within the scope of this context. The tree
	 * may include additional out-of-sync resources, which should be ignored by
	 * the client. Clients can test for inclusion using the method 
	 * {@link ISynchronizeScope#contains(IResource)}.
	 * 
	 * @return a tree that contains a <code>SyncInfo</code> node for any
	 *         resources that are out-of-sync.
	 */
	public SyncInfoTree getSyncInfoTree();
	
	/**
	 * Returns synchronization info for the given resource, or <code>null</code>
	 * if there is no synchronization info because the resource is not a
	 * candidate for synchronization.
	 * <p>
	 * Note that sync info may be returned for non-existing or for resources
	 * which have no corresponding remote resource.
	 * </p>
	 * <p>
	 * This method will be quick. If synchronization calculation requires content from
	 * the server it must be cached when the context is created or refreshed. A client should
	 * call refresh before calling this method to ensure that the latest information
	 * is available for computing the sync state.
	 * </p>
	 * @param resource the resource of interest
	 * @return sync info
	 * @throws CoreException 
	 */
	public SyncInfo getSyncInfo(IResource resource) throws CoreException;

	/**
	 * Return the synchronization type. A type of <code>TWO_WAY</code>
	 * indicates that the synchronization information (i.e.
	 * <code>SyncInfo</code>) associated with the context will also be
	 * two-way (i.e. there is only a remote but no base involved in the
	 * comparison used to determine the synchronization state of resources. A
	 * type of <code>THREE_WAY</code> indicates that the synchronization
	 * information will be three-way and include the local, base (or ancestor)
	 * and remote.
	 * 
	 * @return the type of merge to take place
	 * 
	 * @see org.eclipse.team.core.synchronize.SyncInfo
	 */
	public String getType();
	
	/**
	 * Dispose of the synchronization context. This method should be
	 * invoked by clients when the context is no longer needed.
	 */
	public void dispose();
	
    /**
	 * Refresh the context in order to update the sync-info to include the
	 * latest remote state. any changes will be reported through the change
	 * listeners registered with the sync-info tree of this context. Changes to
	 * the set may be triggered by a call to this method or by a refresh
	 * triggered by some other source.
	 * 
	 * @see SyncInfoSet#addSyncSetChangedListener(ISyncInfoSetChangeListener)
	 * @see org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent
	 * 
	 * @param traversals the resource traversals which indicate which resources
	 *            are to be refreshed
	 * @param flags additional refresh behavior. For instance, if
	 *            <code>RemoteResourceMappingContext.FILE_CONTENTS_REQUIRED</code>
	 *            is one of the flags, this indicates that the client will be
	 *            accessing the contents of the files covered by the traversals.
	 *            <code>NONE</code> should be used when no additional behavior
	 *            is required
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *            reporting is not desired
	 * @throws CoreException if the refresh fails. Reasons include:
	 *             <ul>
	 *             <li>The server could not be contacted for some reason (e.g.
	 *             the context in which the operation is being called must be
	 *             short running). The status code will be
	 *             SERVER_CONTACT_PROHIBITED. </li>
	 *             </ul>
	 */
    public void refresh(ResourceTraversal[] traversals, int flags, IProgressMonitor monitor) throws CoreException;
	
}