/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Helen Hawkins   - iniital version
 *******************************************************************************/
package org.eclipse.ajdt.ui.tests.ajde;

import org.eclipse.ajdt.core.AspectJPlugin;
import org.eclipse.ajdt.internal.ui.ajde.AJDTErrorHandler;
import org.eclipse.ajdt.ui.AspectJUIPlugin;
import org.eclipse.ajdt.ui.tests.UITestCase;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Tests the AJDTErrorHandler class
 */
public class AJDTErrorHandlerTest extends UITestCase {

	public void testHandleAJDTErrorWithMessage() throws Exception {
		IProject project = createPredefinedProject("Bean Example"); //$NON-NLS-1$
		AspectJPlugin.getDefault().setCurrentProject(project);
		AJDTErrorHandler.setShowErrorDialogs(false);
		String message = ""; //$NON-NLS-1$
		try {
			IStatus error = new Status(IStatus.ERROR, AspectJUIPlugin.PLUGIN_ID,"fake CoreException"); //$NON-NLS-1$
			AJDTErrorHandler.handleAJDTError("fake ajdt error", new CoreException(error)); //$NON-NLS-1$
		} catch (RuntimeException re) {
			message = re.getMessage();
		}
		assertTrue("expected a runtime error with message 'fake error' when " + //$NON-NLS-1$
				" testing error handling but didn't find one",  //$NON-NLS-1$
				message.equals("org.eclipse.core.runtime.CoreException: fake CoreException")); //$NON-NLS-1$
	}
	
	public void testHandleAJDTErrorWithMessageAndTitle() throws Exception {
		IProject project = createPredefinedProject("Bean Example"); //$NON-NLS-1$
		AspectJPlugin.getDefault().setCurrentProject(project);
		AJDTErrorHandler.setShowErrorDialogs(false);
		String message = ""; //$NON-NLS-1$
		try {
			IStatus error = new Status(IStatus.ERROR, AspectJUIPlugin.PLUGIN_ID,"fake CoreException"); //$NON-NLS-1$
			AJDTErrorHandler.handleAJDTError("my error dialog", "fake ajdt error", new CoreException(error)); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (RuntimeException re) {
			message = re.getMessage();
		}
		assertTrue("expected a runtime error with message 'fake error' when " + //$NON-NLS-1$
				" testing error handling but didn't find one",  //$NON-NLS-1$
				message.equals("org.eclipse.core.runtime.CoreException: fake CoreException")); //$NON-NLS-1$
	}
	
}