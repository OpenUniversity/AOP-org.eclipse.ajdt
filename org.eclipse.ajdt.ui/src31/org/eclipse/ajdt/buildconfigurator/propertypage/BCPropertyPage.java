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
package org.eclipse.ajdt.buildconfigurator.propertypage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.preferences.PreferencesMessages;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Copied from jdt Original name:
 * org.eclipse.jdt.internal.ui.preferences.BuildPathsPropertyPage
 * only change: use BuildPathsBlock of this package
 *  
 */
public class BCPropertyPage extends PropertyPage implements IStatusChangeListener {
	
	
private static final String PAGE_SETTINGS= "BuildPathsPropertyPage"; //$NON-NLS-1$
private static final String INDEX= "pageIndex"; //$NON-NLS-1$
	
private BuildPathsBlock fBuildPathsBlock;

/*
 * @see PreferencePage#createControl(Composite)
 */
protected Control createContents(Composite parent) {
	// ensure the page has no special buttons
	noDefaultAndApplyButton();		
	
	IProject project= getProject();
	Control result;
	if (project == null || !isJavaProject(project)) {
		result= createWithoutJava(parent);
	} else if (!project.isOpen()) {
		result= createForClosedProject(parent);
	} else {
		result= createWithJava(parent, project);
	}
	Dialog.applyDialogFont(result);
	return result;
}

/*
 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
 */
public void createControl(Composite parent) {
	super.createControl(parent);
	WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.BUILD_PATH_PROPERTY_PAGE);
}	

private IDialogSettings getSettings() {
	IDialogSettings javaSettings= JavaPlugin.getDefault().getDialogSettings();
	IDialogSettings pageSettings= javaSettings.getSection(PAGE_SETTINGS);
	if (pageSettings == null) {
		pageSettings= javaSettings.addNewSection(PAGE_SETTINGS);
		pageSettings.put(INDEX, 3);
	}
	return pageSettings;
}

/* (non-Javadoc)
 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
 */
public void setVisible(boolean visible) {
	if (fBuildPathsBlock != null) {
		if (!visible) {
			if (fBuildPathsBlock.hasChangesInDialog()) {
				String title= PreferencesMessages.BuildPathsPropertyPage_unsavedchanges_title; 
				String message= PreferencesMessages.BuildPathsPropertyPage_unsavedchanges_message; 
				String[] buttonLabels= new String[] {
						PreferencesMessages.BuildPathsPropertyPage_unsavedchanges_button_save, 
						PreferencesMessages.BuildPathsPropertyPage_unsavedchanges_button_discard, 
						PreferencesMessages.BuildPathsPropertyPage_unsavedchanges_button_ignore
				};
				MessageDialog dialog= new MessageDialog(getShell(), title, null, message, MessageDialog.QUESTION, buttonLabels, 0);
				int res= dialog.open();
				if (res == 0) {
					performOk();
				} else if (res == 1) {
					fBuildPathsBlock.init(JavaCore.create(getProject()), null, null);
				} else {
					fBuildPathsBlock.initializeTimeStamps();
				}
			}
		} else {
			if (!fBuildPathsBlock.hasChangesInDialog() && fBuildPathsBlock.hasChangesInClasspathFile()) {
				fBuildPathsBlock.init(JavaCore.create(getProject()), null, null);
			}
		}
	}
	super.setVisible(visible);
}


/*
 * Content for valid projects.
 */
private Control createWithJava(Composite parent, IProject project) {
	fBuildPathsBlock= new BuildPathsBlock(this, getSettings().getInt(INDEX));
	fBuildPathsBlock.init(JavaCore.create(project), null, null);
	return fBuildPathsBlock.createControl(parent);
}

/*
 * Content for non-Java projects.
 */	
private Control createWithoutJava(Composite parent) {
	Label label= new Label(parent, SWT.LEFT);
	label.setText(PreferencesMessages.BuildPathsPropertyPage_no_java_project_message); 
	
	fBuildPathsBlock= null;
	setValid(true);
	return label;
}

/*
 * Content for closed projects.
 */		
private Control createForClosedProject(Composite parent) {
	Label label= new Label(parent, SWT.LEFT);
	label.setText(PreferencesMessages.BuildPathsPropertyPage_closed_project_message); 
	
	fBuildPathsBlock= null;
	setValid(true);
	return label;
}

private IProject getProject() {
	IAdaptable adaptable= getElement();
	if (adaptable != null) {
		IJavaElement elem= (IJavaElement) adaptable.getAdapter(IJavaElement.class);
		if (elem instanceof IJavaProject) {
			return ((IJavaProject) elem).getProject();
		}
	}
	return null;
}

private boolean isJavaProject(IProject proj) {
	try {
		return proj.hasNature(JavaCore.NATURE_ID);
	} catch (CoreException e) {
		JavaPlugin.log(e);
	}
	return false;
}

/*
 * @see IPreferencePage#performOk
 */
public boolean performOk() {
	if (fBuildPathsBlock != null) {
		getSettings().put(INDEX, fBuildPathsBlock.getPageIndex());
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor)	throws CoreException, OperationCanceledException {
				fBuildPathsBlock.configureJavaProject(monitor);
			}
		};
		WorkbenchRunnableAdapter op= new WorkbenchRunnableAdapter(runnable);
		op.runAsUserJob(PreferencesMessages.BuildPathsPropertyPage_job_title, null);  
	}
	return true;
}

/* (non-Javadoc)
 * @see IStatusChangeListener#statusChanged
 */
public void statusChanged(IStatus status) {
	setValid(!status.matches(IStatus.ERROR));
	StatusUtil.applyToStatusLine(this, status);
}

/* (non-Javadoc)
 * @see org.eclipse.jface.preference.IPreferencePage#performCancel()
 */
public boolean performCancel() {
	if (fBuildPathsBlock != null) {
		getSettings().put(INDEX, fBuildPathsBlock.getPageIndex());
	}
	return super.performCancel();
}

}