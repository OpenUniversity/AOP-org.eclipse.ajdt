/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matt Chapman - initial version
 *******************************************************************************/
package org.eclipse.ajdt.core.builder;

import java.util.List;

import org.eclipse.ajdt.core.AJLog;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * 
 */
public class CompilerMonitor implements IAJCompilerMonitor {

	private IProgressMonitor monitor = null;

	/**
	 * Is this CompilerMonitor instance currently 'in use' ?
	 */
	private boolean compilationInProgress = false;

	public boolean finished() {
		return !compilationInProgress;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aspectj.ajde.BuildProgressMonitor#start(java.lang.String)
	 */
	public void start(String configFile) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aspectj.ajde.BuildProgressMonitor#setProgressText(java.lang.String)
	 */
	public void setProgressText(String text) {
		AJLog.log("AJC: "+text);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aspectj.ajde.BuildProgressMonitor#setProgressBarVal(int)
	 */
	public void setProgressBarVal(int newVal) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aspectj.ajde.BuildProgressMonitor#incrementProgressBarVal()
	 */
	public void incrementProgressBarVal() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aspectj.ajde.BuildProgressMonitor#setProgressBarMax(int)
	 */
	public void setProgressBarMax(int maxVal) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aspectj.ajde.BuildProgressMonitor#getProgressBarMax()
	 */
	public int getProgressBarMax() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aspectj.ajde.BuildProgressMonitor#finish()
	 */
	public void finish() {
		compilationInProgress = false;
		AJLog.log("AJC: Build finished");
	}

	/**
	 * Called from the Builder to set up the compiler for a new build.
	 */
	public void prepare(IProject project, List buildList,
			IProgressMonitor eclipseMonitor) {
		monitor = eclipseMonitor;
		if (monitor != null) {
			// monitor.beginTask(AspectJUIPlugin.getResourceString("ajCompilation"),
			// AspectJUIPlugin.PROGRESS_MONITOR_MAX);
			monitor.beginTask("AJC build", 100);
		}

		AJLog.log("AJC: Starting new build for project " + project.getName());
		compilationInProgress = true;
	}

	public static void clearOtherProjectMarkers(IProject p) {

	}

	public static void showOutstandingProblems() {

	}
}
