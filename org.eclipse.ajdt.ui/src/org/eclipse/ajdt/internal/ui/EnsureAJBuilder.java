/*******************************************************************************
 * Copyright (c) 2009 SpringSource and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Andrew Eisenberg - initial API and implementation
 *******************************************************************************/

package org.eclipse.ajdt.internal.ui;

import org.eclipse.ajdt.core.AspectJPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

/**
 * @author Andrew Eisenberg
 * @created Mar 30, 2009
 *
 * Ensures that AJ Projects have an AJBuilder and 
 * no JavaBuilder.
 * 
 * This class should be in core, but it references {@link AspectJProjectNature}. 
 * That class should be in core as well, but for historical reasons, it isn't.
 * Can we refactor?
 */
public class EnsureAJBuilder implements IResourceChangeListener {

    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.PRE_BUILD) {
            IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
                public boolean visit(IResourceDelta delta) throws CoreException {
                    if (delta.getResource().getType() == IResource.PROJECT && delta.getKind() == IResourceDelta.ADDED) {
                        IProject project = (IProject) delta.getResource();
                        if (AspectJPlugin.isAJProject(project)) {
                            ensureNoJavaBuilder(project);
                            ensureAJBuilder(project);
                        }
                        return false;
                    }
                    return true;
                }
            };
            try {
                event.getDelta().accept(visitor);
            } catch (CoreException e) {
            }
        }
    }

//    public void handleEvent(LifecycleEvent event) throws CoreException {
//        if (event.kind == LifecycleEvent.PRE_PROJECT_OPEN ||
//                event.kind == LifecycleEvent.PRE_PROJECT_CREATE) {
//            if (event.resource instanceof IProject && 
//                    AspectJPlugin.isAJProject((IProject) event.resource)) {
//                try {
//                    Job.getJobManager().beginRule(ResourcesPlugin.getWorkspace().getRoot(), null);
//                    ensureNoJavaBuilder((IProject) event.resource);
//                    ensureAJBuilder((IProject) event.resource);
//                } finally {
//                    Job.getJobManager().endRule(ResourcesPlugin.getWorkspace().getRoot());
//                }
//            }
//        }
//
//    }

    private void ensureNoJavaBuilder(IProject project) throws CoreException {
        AspectJProjectNature.removeJavaBuilder(project);
    }

    private void ensureAJBuilder(IProject project) throws CoreException {
        AspectJProjectNature.addNewBuilder(project);
    }
    
    

}
