/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Luzius Meisser - adjusted for ajdoc 
 *******************************************************************************/
package org.eclipse.ajdt.ajdocexport;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.ajdt.internal.ajdocexport.AJdocOptionsManager;
import org.eclipse.ajdt.internal.ajdocexport.AJdocProjectContentProvider;
import org.eclipse.ajdt.internal.ajdocexport.AJdocWizardPage;
import org.eclipse.ajdt.ui.AspectJUIPlugin;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.jarpackager.CheckboxTreeAndListGroup;
import org.eclipse.jdt.internal.ui.javadocexport.JavadocExportMessages;
import org.eclipse.jdt.internal.ui.javadocexport.JavadocMemberContentProvider;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Copied from org.eclipse.jdt.internal.ui.javadocexport.JavadocTreeWizardPage
 * Changes marked with // AspectJ Extension
 */
public class AJdocTreeWizardPage extends AJdocWizardPage {

	private CheckboxTreeAndListGroup fInputGroup;

	private Text fDestinationText;
	private Combo fAJdocCommandText;
//	 AspectJ Extension - commenting out unused code
//	private Text fDocletText;
//	private Text fDocletTypeText;
	private Button fStandardButton;
	private Button fDestinationBrowserButton;
//	private Button fCustomButton;
	private Button fPrivateVisibility;
	private Button fProtectedVisibility;
	private Button fPackageVisibility;
	private Button fPublicVisibility;
//	private Label fDocletLabel;
//	private Label fDocletTypeLabel;
	private Label fDestinationLabel;
	private CLabel fDescriptionLabel;
	
	private String fVisibilitySelection;

//	 AspectJ Extension
	private AJdocOptionsManager fStore;

	private StatusInfo fJavadocStatus;
	private StatusInfo fDestinationStatus;
	private StatusInfo fDocletStatus;
	private StatusInfo fTreeStatus;
	private StatusInfo fPreferenceStatus;
	private StatusInfo fWizardStatus;

	private final int PREFERENCESTATUS= 0;
	private final int CUSTOMSTATUS= 1;
	private final int STANDARDSTATUS= 2;
	private final int TREESTATUS= 3;
	private final int JAVADOCSTATUS= 4;

	/**
	 * Constructor for AJdocTreeWizardPage.
	 * @param pageName
	 */
	protected AJdocTreeWizardPage(String pageName, AJdocOptionsManager store) {
		super(pageName);
//		 AspectJ Extension - message
		setDescription(AspectJUIPlugin.getResourceString("ajdocTreeWizardPage.javadoctreewizardpage.description")); //$NON-NLS-1$
		fStore= store;

		// Status variables
		fJavadocStatus= new StatusInfo();
		fDestinationStatus= new StatusInfo();
		fDocletStatus= new StatusInfo();
		fTreeStatus= new StatusInfo();
		fPreferenceStatus= new StatusInfo();
		fWizardStatus= store.getWizardStatus();
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		final Composite composite= new Composite(parent, SWT.NONE);
		final GridLayout layout= new GridLayout();
		layout.numColumns= 6;
		composite.setLayout(layout);

		createAJdocCommandSet(composite);
		createInputGroup(composite);
		createVisibilitySet(composite);
		createOptionsSet(composite);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.JAVADOC_TREE_PAGE);
	}
	
	
	protected void createAJdocCommandSet(Composite composite) {
		
		final int numColumns= 2;
		
		GridLayout layout= createGridLayout(numColumns);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		Composite group = new Composite(composite, SWT.NONE);
		group.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		group.setLayout(layout);

//		 AspectJ Extension - message
		createLabel(group, SWT.NONE, AspectJUIPlugin.getResourceString("ajdocTreeWizardPage.ajdoccommand.label"), createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, numColumns, 0)); //$NON-NLS-1$
		fAJdocCommandText= createCombo(group, SWT.NONE, null, createGridData(GridData.FILL_HORIZONTAL, numColumns - 1, 0));

		fAJdocCommandText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(JAVADOCSTATUS);
			}
		});

		final Button javadocCommandBrowserButton= createButton(group, SWT.PUSH, JavadocExportMessages.getString("JavadocTreeWizardPage.javadoccommand.button.label"), createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, 0)); //$NON-NLS-1$
		SWTUtil.setButtonDimensionHint(javadocCommandBrowserButton);

		javadocCommandBrowserButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				browseForAJdocCommand();
			}
		});
	}

	
	
	protected void createInputGroup(Composite composite) {
//		 AspectJ Extension - message
		createLabel(composite, SWT.NONE, AspectJUIPlugin.getResourceString("ajdoc.info.projectselection"), createGridData(6)); //$NON-NLS-1$
		Composite c= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.makeColumnsEqualWidth= true;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		c.setLayout(layout);
		c.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		
		ITreeContentProvider treeContentProvider= new AJdocProjectContentProvider();
		ITreeContentProvider listContentProvider= new JavadocMemberContentProvider();
		fInputGroup= new CheckboxTreeAndListGroup(c, this, treeContentProvider, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT), listContentProvider, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT), SWT.NONE, convertWidthInCharsToPixels(60), convertHeightInCharsToPixels(7));

		fInputGroup.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent e) {
				doValidation(TREESTATUS);
			}
		});
		fInputGroup.setTreeSorter(new JavaElementSorter());
		
		IJavaElement[] elements= fStore.getInitialElements();
		setTreeChecked(elements);
		if (elements.length > 0) {
			fInputGroup.setTreeSelection(new StructuredSelection(elements[0].getJavaProject()));
		}

		fInputGroup.aboutToOpen();
	}

	private void createVisibilitySet(Composite composite) {

		GridLayout visibilityLayout= createGridLayout(4);
		visibilityLayout.marginHeight= 0;
		visibilityLayout.marginWidth= 0;
		Composite visibilityGroup= new Composite(composite, SWT.NONE);
		visibilityGroup.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		visibilityGroup.setLayout(visibilityLayout);
		
//		 AspectJ Extension - message
		createLabel(visibilityGroup, SWT.NONE, AspectJUIPlugin.getResourceString("ajdocTreeWizardPage.visibilitygroup.label"), createGridData(GridData.FILL_HORIZONTAL, 4, 0)); //$NON-NLS-1$
		fPrivateVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.privatebutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$
		fPackageVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.packagebutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$
		fProtectedVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.protectedbutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$
		fPublicVisibility= createButton(visibilityGroup, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.publicbutton.label"), createGridData(GridData.FILL_HORIZONTAL, 1, 0)); //$NON-NLS-1$

		fDescriptionLabel= new CLabel(visibilityGroup, SWT.LEFT);
		fDescriptionLabel.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 4, convertWidthInCharsToPixels(3) -  3)); // INDENT of CLabel

		fPrivateVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PRIVATE;
					fDescriptionLabel.setText(AspectJUIPlugin.getResourceString("ajdocTreeWizardPage.privatevisibilitydescription.label")); //$NON-NLS-1$
				}
			}
		});
		fPackageVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PACKAGE;
					fDescriptionLabel.setText(AspectJUIPlugin.getResourceString("ajdocTreeWizardPage.packagevisibledescription.label")); //$NON-NLS-1$
				}
			}
		});
		fProtectedVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PROTECTED;
					fDescriptionLabel.setText(AspectJUIPlugin.getResourceString("ajdocTreeWizardPage.protectedvisibilitydescription.label")); //$NON-NLS-1$
				}
			}
		});

		fPublicVisibility.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					fVisibilitySelection= fStore.PUBLIC;
					fDescriptionLabel.setText(AspectJUIPlugin.getResourceString("ajdocTreeWizardPage.publicvisibilitydescription.label")); //$NON-NLS-1$
				}
			}
		});

		setVisibilitySettings();

	}

	protected void setVisibilitySettings() {
//		 AspectJ Extension - message
		fVisibilitySelection= fStore.getAccess();
		fPrivateVisibility.setSelection(fVisibilitySelection.equals(fStore.PRIVATE));
		if (fPrivateVisibility.getSelection())
			fDescriptionLabel.setText(AspectJUIPlugin.getResourceString("ajdocTreeWizardPage.privatevisibilitydescription.label")); //$NON-NLS-1$
		//$NON-NLS-1$
		fProtectedVisibility.setSelection(fVisibilitySelection.equals(fStore.PROTECTED));
		if (fProtectedVisibility.getSelection())
			fDescriptionLabel.setText(AspectJUIPlugin.getResourceString("ajdocTreeWizardPage.protectedvisibilitydescription.label")); //$NON-NLS-1$
		//$NON-NLS-1$
		fPackageVisibility.setSelection(fVisibilitySelection.equals(fStore.PACKAGE));
		if (fPackageVisibility.getSelection())
			fDescriptionLabel.setText(AspectJUIPlugin.getResourceString("ajdocTreeWizardPage.packagevisibledescription.label")); //$NON-NLS-1$
		//$NON-NLS-1$
		fPublicVisibility.setSelection(fVisibilitySelection.equals(fStore.PUBLIC));
		if (fPublicVisibility.getSelection())
			fDescriptionLabel.setText(AspectJUIPlugin.getResourceString("ajdocTreeWizardPage.publicvisibilitydescription.label")); //$NON-NLS-1$
		//$NON-NLS-1$
	}

	private void createOptionsSet(Composite composite) {
		
		final int numColumns= 4;

		final GridLayout layout= createGridLayout(numColumns);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		Composite group= new Composite(composite, SWT.NONE);
		group.setLayoutData(createGridData(GridData.FILL_HORIZONTAL, 6, 0));
		group.setLayout(layout);

		fStandardButton= createButton(group, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.standarddocletbutton.label"), createGridData(GridData.HORIZONTAL_ALIGN_FILL, numColumns, 0)); //$NON-NLS-1$

		fDestinationLabel= createLabel(group, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.destinationfield.label"), createGridData(GridData.HORIZONTAL_ALIGN_FILL, 1, convertWidthInCharsToPixels(3))); //$NON-NLS-1$?
		fDestinationText= createText(group, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.FILL_HORIZONTAL, numColumns - 2, 0));
		((GridData) fDestinationText.getLayoutData()).widthHint= 0;
		fDestinationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				doValidation(STANDARDSTATUS);
			}
		});

		fDestinationBrowserButton= createButton(group, SWT.PUSH, JavadocExportMessages.getString("JavadocTreeWizardPage.destinationbrowse.label"), createGridData(GridData.HORIZONTAL_ALIGN_END, 1, 0)); //$NON-NLS-1$
		SWTUtil.setButtonDimensionHint(fDestinationBrowserButton);

//		 AspectJ Extension - commenting out unused code
		//Option to use custom doclet
		//fCustomButton= createButton(group, SWT.RADIO, JavadocExportMessages.getString("JavadocTreeWizardPage.customdocletbutton.label"), createGridData(GridData.HORIZONTAL_ALIGN_FILL, numColumns, 0)); //$NON-NLS-1$
		
		//For Entering location of custom doclet
//		fDocletTypeLabel= createLabel(group, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.docletnamefield.label"), createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 1, convertWidthInCharsToPixels(3))); //$NON-NLS-1$
//		fDocletTypeText= createText(group, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.HORIZONTAL_ALIGN_FILL, numColumns - 1, 0));
//		((GridData) fDocletTypeText.getLayoutData()).widthHint= 0;
		
		
//		fDocletTypeText.addModifyListener(new ModifyListener() {
//			public void modifyText(ModifyEvent e) {
//				doValidation(CUSTOMSTATUS);
//			}
//
//		});
//
//		fDocletLabel= createLabel(group, SWT.NONE, JavadocExportMessages.getString("JavadocTreeWizardPage.docletpathfield.label"), createGridData(GridData.HORIZONTAL_ALIGN_BEGINNING, 1, convertWidthInCharsToPixels(3))); //$NON-NLS-1$
//		fDocletText= createText(group, SWT.SINGLE | SWT.BORDER, null, createGridData(GridData.HORIZONTAL_ALIGN_FILL, numColumns - 1, 0));
//		((GridData) fDocletText.getLayoutData()).widthHint= 0;
//		
//		fDocletText.addModifyListener(new ModifyListener() {
//			public void modifyText(ModifyEvent e) {
//				doValidation(CUSTOMSTATUS);
//			}
//
//		});

		//Add Listeners
//		fCustomButton.addSelectionListener(new EnableSelectionAdapter(new Control[] { fDocletLabel, fDocletText, fDocletTypeLabel, fDocletTypeText }, new Control[] { fDestinationLabel, fDestinationText, fDestinationBrowserButton }));
//		fCustomButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				doValidation(CUSTOMSTATUS);
//			}
//		});
//		fStandardButton.addSelectionListener(new EnableSelectionAdapter(new Control[] { fDestinationLabel, fDestinationText, fDestinationBrowserButton }, new Control[] { fDocletLabel, fDocletText, fDocletTypeLabel, fDocletTypeText }));
//		fStandardButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				doValidation(STANDARDSTATUS);
//			}
//		});
		fDestinationBrowserButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
										String text= handleFolderBrowseButtonPressed(fDestinationText.getText(), fDestinationText.getShell(), JavadocExportMessages.getString("JavadocTreeWizardPage.destinationbrowsedialog.title"), //$NON-NLS-1$
						    		JavadocExportMessages.getString("JavadocTreeWizardPage.destinationbrowsedialog.label")); //$NON-NLS-1$
				fDestinationText.setText(text);
			}
		});

		setOptionSetSettings();
	}

//	 AspectJ Extension - commenting out unused code
//	public boolean getCustom() {
//		return fCustomButton.getSelection();
//	}

	private void setOptionSetSettings() {

		if (!fStore.isFromStandard()) {
//			 AspectJ Extension - commenting out unused code
//			fCustomButton.setSelection(true);
//			fDocletText.setText(fStore.getDocletPath());
//			fDocletTypeText.setText(fStore.getDocletName());
			fDestinationText.setText(fStore.getDestination());
			fDestinationText.setEnabled(false);
			fDestinationBrowserButton.setEnabled(false);
			fDestinationLabel.setEnabled(false);
			
		} else {
			fStandardButton.setSelection(true);
			fDestinationText.setText(fStore.getDestination());
//			 AspectJ Extension - commenting out unused code
//			fDocletText.setText(fStore.getDocletPath());
//			fDocletTypeText.setText(fStore.getDocletName());
//			fDocletText.setEnabled(false);
//			fDocletLabel.setEnabled(false);
//			fDocletTypeText.setEnabled(false);
//			fDocletTypeLabel.setEnabled(false);
		}
		
		fAJdocCommandText.setItems(fStore.getJavadocCommandHistory());
		fAJdocCommandText.select(0);
	}

	/**
	 * Receives of list of elements selected by the user and passes them
	 * to the CheckedTree. List can contain multiple projects and elements from
	 * different projects. If the list of seletected elements is empty a default
	 * project is selected.
	 */
	private void setTreeChecked(IJavaElement[] sourceElements) {
		for (int i= 0; i < sourceElements.length; i++) {
			IJavaElement curr= sourceElements[i];
			if (curr instanceof ICompilationUnit) {
				fInputGroup.initialCheckListItem(curr);
			} else if (curr instanceof IPackageFragment) {
				fInputGroup.initialCheckTreeItem(curr);
			} else if (curr instanceof IJavaProject) {
				fInputGroup.initialCheckTreeItem(curr);
			} else if (curr instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) curr;
				if (!root.isArchive())
					fInputGroup.initialCheckTreeItem(curr);
			}
		}
	}

	private IPath[] getSourcePath(IJavaProject[] projects) {
		HashSet res= new HashSet();
		//loops through all projects and gets a list if of thier sourpaths
		for (int k= 0; k < projects.length; k++) {
			IJavaProject iJavaProject= projects[k];

			try {
				IPackageFragmentRoot[] roots= iJavaProject.getPackageFragmentRoots();
				for (int i= 0; i < roots.length; i++) {
					IPackageFragmentRoot curr= roots[i];
					if (curr.getKind() == IPackageFragmentRoot.K_SOURCE) {
						IResource resource= curr.getResource();
						if (resource != null) {
							IPath p= resource.getLocation();
							if (p != null) {
								res.add(p);
							}
						}
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return (IPath[]) res.toArray(new IPath[res.size()]);
	}

	private IPath[] getClassPath(IJavaProject[] javaProjects) {
		HashSet res= new HashSet();

		for (int j= 0; j < javaProjects.length; j++) {
			IJavaProject curr= javaProjects[j];

			try {
				IPath p= curr.getProject().getLocation();
				if (p == null)
					continue;

				IPath outputLocation= p.append(curr.getOutputLocation());
				String[] classPath= JavaRuntime.computeDefaultRuntimeClassPath(curr);

				for (int i= 0; i < classPath.length; i++) {
					IPath path= new Path(classPath[i]);
					if (!outputLocation.equals(path)) {
						res.add(path);
					}
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
		return (IPath[]) res.toArray(new IPath[res.size()]);
	}

	/**
	 * Gets a list of elements to generated javadoc for from each project. 
	 * Javadoc can be generated for either a IPackageFragmentRoot or a ICompilationUnit.
	 */
	private IJavaElement[] getSourceElements(IJavaProject[] projects) {
		ArrayList res= new ArrayList();
		try {
			Set allChecked= fInputGroup.getAllCheckedTreeItems();

			Set incompletePackages= new HashSet();
			for (int h= 0; h < projects.length; h++) {
				IJavaProject iJavaProject= projects[h];

				IPackageFragmentRoot[] roots= iJavaProject.getPackageFragmentRoots();
				for (int i= 0; i < roots.length; i++) {
					IPackageFragmentRoot root= roots[i];
					if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
						IJavaElement[] packs= root.getChildren();
						for (int k= 0; k < packs.length; k++) {
							IJavaElement curr= packs[k];
							if (curr.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
								// default packages are always incomplete
								if (curr.getElementName().length() == 0 || !allChecked.contains(curr) || fInputGroup.isTreeItemGreyChecked(curr)) {
									incompletePackages.add(curr.getElementName());
								}
							}
						}
					}
				}
			}

			Iterator checkedElements= fInputGroup.getAllCheckedListItems();
			while (checkedElements.hasNext()) {
				Object element= checkedElements.next();
				if (element instanceof ICompilationUnit) {
					ICompilationUnit unit= (ICompilationUnit) element;
					if (incompletePackages.contains(unit.getParent().getElementName())) {
						res.add(unit);
					}
				}
			}

			Set addedPackages= new HashSet();

			checkedElements= allChecked.iterator();
			while (checkedElements.hasNext()) {
				Object element= checkedElements.next();
				if (element instanceof IPackageFragment) {
					IPackageFragment fragment= (IPackageFragment) element;
					String name= fragment.getElementName();
					if (!incompletePackages.contains(name) && !addedPackages.contains(name)) {
						res.add(fragment);
						addedPackages.add(name);
					}
				}
			}

		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return (IJavaElement[]) res.toArray(new IJavaElement[res.size()]);
	}

	protected void updateStore(IJavaProject[] checkedProjects) {
//		 AspectJ Extension - commenting out unused code
//		if (fCustomButton.getSelection()) {
//			fStore.setDocletName(fDocletTypeText.getText());
//			fStore.setDocletPath(fDocletText.getText());
//			fStore.setFromStandard(false);
//		}
		if (fStandardButton.getSelection()) {
			fStore.setFromStandard(true);
			//the destination used in javadoc generation
			fStore.setDestination(fDestinationText.getText());
		}

		fStore.setSourcepath(getSourcePath(checkedProjects));
		fStore.setClasspath(getClassPath(checkedProjects));
		fStore.setAccess(fVisibilitySelection);
		fStore.setCheckedProjects(checkedProjects);
		
		ArrayList commands= new ArrayList();
		commands.add(fAJdocCommandText.getText()); // must be first
		String[] items= fAJdocCommandText.getItems();
		for (int i= 0; i < items.length; i++) {
			String curr= items[i];
			if (!commands.contains(curr)) {
				commands.add(curr);
			}
		}
		fStore.setJavadocCommandHistory((String[]) commands.toArray(new String[commands.size()]));
	}

	public IJavaProject[] getCheckedProjects() {
		ArrayList res= new ArrayList();
		TreeItem[] treeItems= fInputGroup.getTree().getItems();
		for (int i= 0; i < treeItems.length; i++) {
			if (treeItems[i].getChecked()) {
				Object curr= treeItems[i].getData();
				if (curr instanceof IJavaProject) {
					res.add(curr);
				}
			}
		}
		return (IJavaProject[]) res.toArray(new IJavaProject[res.size()]);
	}
	
	protected void doValidation(int validate) {

		
		switch (validate) {
			case PREFERENCESTATUS :
				fPreferenceStatus= new StatusInfo();
				fDocletStatus= new StatusInfo();
				updateStatus(findMostSevereStatus());
				break;
			case CUSTOMSTATUS :
//				 AspectJ Extension - comemnting out unused code
//				if (fCustomButton.getSelection()) {
//					fDestinationStatus= new StatusInfo();
//					fDocletStatus= new StatusInfo();
//					String doclet= fDocletTypeText.getText();
//					String docletPath= fDocletText.getText();
//					if (doclet.length() == 0) {
//						fDocletStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.nodocletname.error")); //$NON-NLS-1$
//
//					} else if (JavaConventions.validateJavaTypeName(doclet).matches(IStatus.ERROR)) {
//						fDocletStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.invaliddocletname.error")); //$NON-NLS-1$
//					} else if ((docletPath.length() == 0) || !validDocletPath(docletPath)) {
//						fDocletStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.invaliddocletpath.error")); //$NON-NLS-1$
//					}
//					updateStatus(findMostSevereStatus());
//				}
				break;

			case STANDARDSTATUS :
				if (fStandardButton.getSelection()) {
					fDestinationStatus= new StatusInfo();
					fDocletStatus= new StatusInfo();
					IPath path= new Path(fDestinationText.getText());
					if (Path.ROOT.equals(path) || Path.EMPTY.equals(path)) {
						fDestinationStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.nodestination.error")); //$NON-NLS-1$
					}
					File file= new File(path.toOSString());
					if (!path.isValidPath(path.toOSString()) || file.isFile()) {
						fDestinationStatus.setError(JavadocExportMessages.getString("JavadocTreeWizardPage.invaliddestination.error")); //$NON-NLS-1$
					}
//					 AspectJ Extension - message
					if ((path.append("package-list").toFile().exists()) || (path.append("index.html").toFile().exists())) //$NON-NLS-1$//$NON-NLS-2$
						fDestinationStatus.setWarning(AspectJUIPlugin.getResourceString("ajdocTreeWizardPage.warning.mayoverwritefiles")); //$NON-NLS-1$
					updateStatus(findMostSevereStatus());
				}
				break;

			case TREESTATUS :

				fTreeStatus= new StatusInfo();
//				 AspectJ Extension - message
				if (fInputGroup.getCheckedElementCount() == 0){
					fTreeStatus.setError(AspectJUIPlugin.getResourceString("ajdoc.error.noProjectSelected")); //$NON-NLS-1$
				}
				updateStatus(findMostSevereStatus());

				break;
				
			case JAVADOCSTATUS:
				fJavadocStatus= new StatusInfo();
				String text= fAJdocCommandText.getText();
//				 AspectJ Extension - message
				if (text.length() == 0) {
					fJavadocStatus.setError(AspectJUIPlugin.getResourceString("ajdocTreeWizardPage.ajdoccmd.error.enterpath"));  //$NON-NLS-1$
				} else {
					File file= new File(text);
					if (!file.isFile()) {
						fJavadocStatus.setError(AspectJUIPlugin.getResourceString("ajdocTreeWizardPage.ajdoccmd.error.notexists"));  //$NON-NLS-1$
					}
				}
				updateStatus(findMostSevereStatus());
				break;
		} //end switch
		
		
	}
	
	// AspectJ Extension - changed name to "browserForAJdocCommand"
	protected void browseForAJdocCommand() {
		FileDialog dialog= new FileDialog(getShell());
//		 AspectJ Extension - message
		dialog.setText(AspectJUIPlugin.getResourceString("AJdocTreeWizardPage.ajdoccmd.dialog.title")); //$NON-NLS-1$
		String dirName= fAJdocCommandText.getText();
		dialog.setFileName(dirName);
		String selectedDirectory= dialog.open();
		if (selectedDirectory != null) {
			ArrayList newItems= new ArrayList();
			String[] items= fAJdocCommandText.getItems();
			newItems.add(selectedDirectory);
			for (int i= 0; i < items.length && newItems.size() < 5; i++) { // only keep the last 5 entries
				String curr= items[i];
				if (!newItems.contains(curr)) {
					newItems.add(curr);
				}
			}
			fAJdocCommandText.setItems((String[]) newItems.toArray(new String[newItems.size()]));
			fAJdocCommandText.select(0);
		}
	}
	

	private boolean validDocletPath(String docletPath) {
		StringTokenizer tokens= new StringTokenizer(docletPath, ";"); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			File file= new File(tokens.nextToken());
			if (!file.exists())
				return false;
		}
		return true;
	}

	/**
	 * Finds the most severe error (if there is one)
	 */
	private IStatus findMostSevereStatus() {
		return StatusUtil.getMostSevere(new IStatus[] { fTreeStatus, fPreferenceStatus, fDestinationStatus, fDocletStatus, fJavadocStatus, fWizardStatus });
	}

	public void init() {
		updateStatus(new StatusInfo());
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			doValidation(JAVADOCSTATUS);
			doValidation(STANDARDSTATUS);
			doValidation(CUSTOMSTATUS);
			doValidation(TREESTATUS);
			doValidation(PREFERENCESTATUS);
		}
	}

}
