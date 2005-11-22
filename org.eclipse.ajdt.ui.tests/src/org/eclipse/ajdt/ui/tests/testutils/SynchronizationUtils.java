/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ajdt.ui.tests.testutils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

// Adapted from org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCase
public class SynchronizationUtils {

		
	public static void joinBackgroudActivities()  {
		// Join Building
		boolean interrupted= true;
		while (interrupted) {
			try {
				Platform.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				interrupted= false;
			} catch (InterruptedException e) {
				interrupted= true;
			}
		}
		
		// Join jobs
		joinJobs(0, 0, 500);
	}


	private static boolean joinJobs(long minTime, long maxTime, long intervalTime) {
		long startTime= System.currentTimeMillis() + minTime;
		runEventQueue();
		while (System.currentTimeMillis() < startTime)
			runEventQueue(intervalTime);
		
		long endTime= maxTime > 0  && maxTime < Long.MAX_VALUE ? System.currentTimeMillis() + maxTime : Long.MAX_VALUE;
		boolean calm= allJobsQuiet();
		while (!calm && System.currentTimeMillis() < endTime) {
			runEventQueue(intervalTime);
			calm= allJobsQuiet();
		}
		return calm;
	}
	
	private static void sleep(int intervalTime) {
		try {
			Thread.sleep(intervalTime);
		} catch (InterruptedException e) {
		}
	}
	
	private static boolean allJobsQuiet() {
		IJobManager jobManager= Platform.getJobManager();
		Job[] jobs= jobManager.find(null);
		for (int i= 0; i < jobs.length; i++) {
			Job job= jobs[i];
			int state= job.getState();
			if (state == Job.RUNNING || state == Job.WAITING)
				return false;
		}
		return true;
	}
	
	private static void runEventQueue() {
		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null)
			runEventQueue(window.getShell());
	}
	
	private static void runEventQueue(Shell shell) {
		try {
			while (shell.getDisplay().readAndDispatch()) {
				// do nothing
			}
		} catch (SWTException e) {
			System.err.println(e);
		}
	}
	
	private static void runEventQueue(long minTime) {
		long nextCheck= System.currentTimeMillis() + minTime;
		while (System.currentTimeMillis() < nextCheck) {
			runEventQueue();
			sleep(1);
		}
	}
}
