/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Matthew Ford - initial API and implementation
 *******************************************************************************/
package org.eclipse.ajdt.internal.buildpath;

import org.eclipse.ajdt.core.AspectJCorePreferences;
import org.eclipse.ajdt.internal.utils.AJDTUtils;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;

public class RemoveFromInpathAction extends AJBuildPathAction implements IObjectActionDelegate {

	public void run(IAction action) {
		if (cpEntry == null) {
			return;
		}
		if (project == null) {
			return;
		}
		AspectJCorePreferences.removeFromInPath(project,cpEntry);
		AJDTUtils.refreshPackageExplorer();
	}

	public void selectionChanged(IAction action, ISelection sel) {
		boolean enable = false;
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) sel;
			Object element = selection.getFirstElement();
			try {
				if (element instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot root = (IPackageFragmentRoot) element;
					project = root.getJavaProject().getProject();
					cpEntry = root.getRawClasspathEntry();
					enable = AspectJCorePreferences.isOnInpath(cpEntry);
				}
			} catch (JavaModelException e) {
			}
			action.setEnabled(enable);
		}
	}

}
