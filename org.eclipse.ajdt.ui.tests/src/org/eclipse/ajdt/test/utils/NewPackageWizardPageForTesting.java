/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Helen Hawkins   - iniital version
 *******************************************************************************/
package org.eclipse.ajdt.test.utils;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.ui.wizards.NewContainerWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * This class is the same as NewPackageWizardPage except for the
 * code marked with 'Testing Extension'. This is required so that we
 * can use a BlockingProgressMonitor to help with the timings of the tests
 * 
 * @author hawkinsh
 */
public class NewPackageWizardPageForTesting extends NewContainerWizardPage {

	private static final String PAGE_NAME= "NewPackageWizardPage"; //$NON-NLS-1$
	
	private static final String PACKAGE= "NewPackageWizardPage.package"; //$NON-NLS-1$
	
	private StringDialogField fPackageDialogField;
	
	/*
	 * Status of last validation of the package field
	 */
	private IStatus fPackageStatus;
	
	private IPackageFragment fCreatedPackageFragment;

	// Testing Extension - to do with timings
	private BlockingProgressMonitor blockingMonitor;
	
	// Testing Extension - to do with timings
	public void setBlockingProgressMonitor(BlockingProgressMonitor monitor) {
		blockingMonitor = monitor;
	}

	
	/**
	 * Creates a new <code>NewPackageWizardPage</code>
	 */
	public NewPackageWizardPageForTesting() {
		super(PAGE_NAME);
		
		setTitle(NewWizardMessages.getString("NewPackageWizardPage.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("NewPackageWizardPage.description"));		 //$NON-NLS-1$
		
		fCreatedPackageFragment= null;

		PackageFieldAdapter adapter= new PackageFieldAdapter();
		
		fPackageDialogField= new StringDialogField();
		fPackageDialogField.setDialogFieldListener(adapter);
		fPackageDialogField.setLabelText(NewWizardMessages.getString("NewPackageWizardPage.package.label")); //$NON-NLS-1$
		
		fPackageStatus= new StatusInfo();
	}

	// -------- Initialization ---------

	/**
	 * The wizard owning this page is responsible for calling this method with the
	 * current selection. The selection is used to initialize the fields of the wizard 
	 * page.
	 * 
	 * @param selection used to initialize the fields
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement jelem= getInitialJavaElement(selection);	
		
		initContainerPage(jelem);
		String pName= ""; //$NON-NLS-1$
		if (jelem != null && jelem.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
			IPackageFragment pf= (IPackageFragment)jelem;
			if (!pf.isDefaultPackage())
				pName= jelem.getElementName();
		}
		setPackageText(pName, true); //$NON-NLS-1$
		updateStatus(new IStatus[] { fContainerStatus, fPackageStatus });		
	}
	
	// -------- UI Creation ---------

	/*
	 * @see WizardPage#createControl
	 */	
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
			
		Composite composite= new Composite(parent, SWT.NONE);
		
		int nColumns= 3;
		
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		layout.numColumns= 3;		
		composite.setLayout(layout);
		
		Label label= new Label(composite, SWT.WRAP);
		label.setText(NewWizardMessages.getString("NewPackageWizardPage.info")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.widthHint= convertWidthInCharsToPixels(80);
		gd.horizontalSpan= 3;
		label.setLayoutData(gd);
		
		createContainerControls(composite, nColumns);
		createPackageControls(composite, nColumns);
		
		setControl(composite);
		Dialog.applyDialogFont(composite);
		//WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.NEW_PACKAGE_WIZARD_PAGE);
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setFocus();
		}
	}
	
	/**
	 * Sets the focus to the package name input field.
	 */		
	protected void setFocus() {
		fPackageDialogField.setFocus();
	}
	

	private void createPackageControls(Composite composite, int nColumns) {
		fPackageDialogField.doFillIntoGrid(composite, nColumns - 1);
		LayoutUtil.setWidthHint(fPackageDialogField.getTextControl(null), getMaxFieldWidth());
		LayoutUtil.setHorizontalGrabbing(fPackageDialogField.getTextControl(null));
		DialogField.createEmptySpace(composite);		
	}
				
	// -------- PackageFieldAdapter --------

	private class PackageFieldAdapter implements IDialogFieldListener {
		
		// --------- IDialogFieldListener
		
		public void dialogFieldChanged(DialogField field) {
			fPackageStatus= packageChanged();
			// tell all others
			handleFieldChanged(PACKAGE);
		}
	}
		
	// -------- update message ----------------		

	/*
	 * @see org.eclipse.jdt.ui.wizards.NewContainerWizardPage#handleFieldChanged(String)
	 */
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (fieldName == CONTAINER) {
			fPackageStatus= packageChanged();
		}
		// do status line update
		updateStatus(new IStatus[] { fContainerStatus, fPackageStatus });		
	}	
			
	// ----------- validation ----------
			
	/**
	 * Verifies the input for the package field.
	 */
	private IStatus packageChanged() {
		StatusInfo status= new StatusInfo();
		String packName= getPackageText();
		if (packName.length() > 0) {
			IStatus val= JavaConventions.validatePackageName(packName);
			if (val.getSeverity() == IStatus.ERROR) {
				status.setError(NewWizardMessages.getFormattedString("NewPackageWizardPage.error.InvalidPackageName", val.getMessage())); //$NON-NLS-1$
				return status;
			} else if (val.getSeverity() == IStatus.WARNING) {
				status.setWarning(NewWizardMessages.getFormattedString("NewPackageWizardPage.warning.DiscouragedPackageName", val.getMessage())); //$NON-NLS-1$
			}
		} else {
			status.setError(NewWizardMessages.getString("NewPackageWizardPage.error.EnterName")); //$NON-NLS-1$
			return status;
		}			

		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root != null && root.getJavaProject().exists()) {
			IPackageFragment pack= root.getPackageFragment(packName);
			try {
				IPath rootPath= root.getPath();
				IPath outputPath= root.getJavaProject().getOutputLocation();
				if (rootPath.isPrefixOf(outputPath) && !rootPath.equals(outputPath)) {
					// if the bin folder is inside of our root, don't allow to name a package
					// like the bin folder
					IPath packagePath= pack.getPath();
					if (outputPath.isPrefixOf(packagePath)) {
						status.setError(NewWizardMessages.getString("NewPackageWizardPage.error.IsOutputFolder")); //$NON-NLS-1$
						return status;
					}
				}		
				if (pack.exists()) {
					if (pack.containsJavaResources() || !pack.hasSubpackages()) {
						status.setError(NewWizardMessages.getString("NewPackageWizardPage.error.PackageExists")); //$NON-NLS-1$
					} else {
						status.setError(NewWizardMessages.getString("NewPackageWizardPage.warning.PackageNotShown"));  //$NON-NLS-1$
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			
		}
		return status;
	}

	/**
	 * Returns the content of the package input field.
	 * 
	 * @return the content of the package input field
	 */
	public String getPackageText() {
		return fPackageDialogField.getText();
	}

	/**
	 * Sets the content of the package input field to the given value.
	 * 
	 * @param str the new package input field text
	 * @param canBeModified if <code>true</code> the package input
	 * field can be modified; otherwise it is read-only.
	 */	
	public void setPackageText(String str, boolean canBeModified) {
		fPackageDialogField.setText(str);
		
		fPackageDialogField.setEnabled(canBeModified);
	}
	
	/**
	 * Returns the resource handle that corresponds to the element to was created or
	 * will be created.
	 * @return A resource or null if the page contains illegal values.
	 * @since 3.0
	 */
	public IResource getModifiedResource() {
		IPackageFragmentRoot root= getPackageFragmentRoot();
		if (root != null) {
			return root.getPackageFragment(getPackageText()).getResource();
		}
		return null;
	}
	
		
	// ---- creation ----------------

	/**
	 * Returns a runnable that creates a package using the current settings.
	 * 
	 * @return the runnable that creates the new package
	 */	
	public IRunnableWithProgress getRunnable() {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					createPackage(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} 
			}
		};
	}

	/**
	 * Returns the created package fragment. This method only returns a valid value
	 * after <code>getRunnable</code> or <code>createPackage</code> have been 
	 * executed.
	 * 
	 * @return the created package fragment
	 */	
	public IPackageFragment getNewPackageFragment() {
		return fCreatedPackageFragment;
	}
	
	/**
	 * Creates the new package using the entered field values.
	 * 
	 * @param monitor a progress monitor to report progress. The progress
	 * monitor must not be <code>null</code>
	 * @throws CoreException Thrown if creating the package failed.
	 * @throws InterruptedException Thrown when the operation has been cancelled.
	 * @since 2.1
	 */
	public void createPackage(IProgressMonitor monitor) throws CoreException, InterruptedException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}

		// Testing extension - using BlockingProgressMonitor
		IPackageFragmentRoot root= getPackageFragmentRoot();
		String packName= getPackageText();
		blockingMonitor.reset();
		fCreatedPackageFragment= root.createPackageFragment(packName, true, blockingMonitor);
		blockingMonitor.waitForCompletion();
		if (monitor.isCanceled()) {
			throw new InterruptedException();
		}
	}	

}
