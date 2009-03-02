/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Helen Hawkins - initial version
 *******************************************************************************/
package org.eclipse.ajdt.ui.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.ajdt.ui.tests.testutils.SynchronizationUtils;
import org.eclipse.ajdt.ui.tests.visualiser.AJDTContentProviderTest;
import org.eclipse.contribution.xref.ui.views.XReferenceView;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.intro.IIntroPart;

public class AllUITests {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllUITests.class.getName());
		//$JUnit-BEGIN$
		
		// AJDT UI Tests
		suite.addTest(AllAJDTUITests.suite());
		
		// visualiser tests
		suite.addTest(org.eclipse.contribution.visualiser.tests.AllTests.suite());

		// AJDT visualiser content provider tests
		suite.addTest(new TestSuite(AJDTContentProviderTest.class));
		
		
		//$JUnit-END$
		return suite;
	}
		
	/**
	 * Prevents AJDTPrefWizard from popping up during tests and simulates normal
	 * usage by closing the welcome page, and opening the java perspective
	 */
	public static synchronized void setupAJDTPlugin() {
		if (setupDone) {
			return;
		}

		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();

		// close welcome page
		IIntroPart intro = PlatformUI.getWorkbench().getIntroManager()
				.getIntro();
		if (intro != null) {
			try {
				PlatformUI.getWorkbench().getIntroManager().setIntroStandby(intro, true);
			} catch (NullPointerException npe) {
				// don't care about this
			}
		}

		// open Java perspective
		try {
			PlatformUI.getWorkbench().showPerspective(JavaUI.ID_PERSPECTIVE,
					window);
		} catch (WorkbenchException e) {
		}

		// open Cross Ref view
		try {
			window.getActivePage().showView(XReferenceView.ID);
		} catch (PartInitException e1) {
		}

		// open Console view
		try {
			window.getActivePage().showView("org.eclipse.ui.console.ConsoleView"); //$NON-NLS-1$
		} catch (PartInitException e1) {
		}

		waitForJobsToComplete();
		setupDone = true;
	}
	
	private static void waitForJobsToComplete() {
		SynchronizationUtils.joinBackgroudActivities();
	}

	private static boolean setupDone = false;
}
