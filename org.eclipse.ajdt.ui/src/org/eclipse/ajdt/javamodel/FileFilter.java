/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Luzius Meisser - initial implementation
 *******************************************************************************/
package org.eclipse.ajdt.javamodel;

import org.eclipse.ajdt.core.javaelements.AJCompilationUnit;
import org.eclipse.ajdt.internal.core.AJDTUtils;
import org.eclipse.ajdt.ui.AspectJUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Display;

/**
 * 
 * To prevent the .aj files to be displayed twice (as IFile and as ICompilationUnit),
 * we need to filter the IFile if an according ICompilationUnit exists.
 * 
 * Side effect of filter: prevents AJCompilationUnits from disappearing when jdt
 * refreshes the javamodel. See comment in select method. 
 * 
 * @author Luzius Meisser
 * 
 */
public class FileFilter extends ViewerFilter {
	
	public static final String ID = "org.eclipse.ajdt.javamodel.FileFilter"; //$NON-NLS-1$
	public static final String FILTER_DIALOG_ID = "DontInformUserAboutFileFilter"; //$NON-NLS-1$
	
	public FileFilter(){
		super();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IFile){
			AJCompilationUnit unit = AJCompilationUnitManager.INSTANCE.getAJCompilationUnitFromCache((IFile)element);
			if (unit != null){
				try {
					//ensure the unit is part of the model, and if it was not, refresh view
					if (!AJCompilationUnitManager.INSTANCE.ensureUnitIsInModel(unit)){
						AJDTUtils.refreshPackageExplorer();
					}
					return false;
				} catch (JavaModelException e) {
					//something has gone wrong, do better not filter IFile and return true
				}
			}
		}
		return true;
	}
	
	//checks if this FileFilter is enabled and if not, it tells user to do so
	//(It seems to be difficult to enable it programmatically since the package explorer loads
	//its preferences before we get called)
	//this whole check might not be necessary any more if 73991 (a jdt bug) gets fixed
	public static void checkIfFileFilterEnabled() {

		IPreferenceStore javaStore = JavaPlugin.getDefault().getPreferenceStore();

		//checks if the javaStore has been initialized
		if (isRelevant(javaStore)){
		
		if (!javaStore.contains(FileFilter.ID) || !javaStore.getBoolean(FileFilter.ID)) {
			
			final IPreferenceStore store = AspectJUIPlugin.getDefault().getPreferenceStore();
			if (!store.contains(FILTER_DIALOG_ID) || !store.getBoolean(FILTER_DIALOG_ID)){

			//wrap into low priority job so it does not get displayed before package explorer is loaded
			Job job = new Job(AspectJUIPlugin.getResourceString("FileFilterDialog.JobTitle")) { //$NON-NLS-1$

				public IStatus run(IProgressMonitor m) {
					final Display display = AspectJUIPlugin.getDefault().getDisplay();
					
					Runnable myRun = new Runnable() {
						public void run() {
							MessageDialogWithToggle md = MessageDialogWithToggle
									.openInformation(
											display.getActiveShell(),
											AspectJUIPlugin.getResourceString("FileFilterDialog.Title"), //$NON-NLS-1$
											AspectJUIPlugin.getResourceString("FileFilterDialog.Message"), //$NON-NLS-1$
											AspectJUIPlugin.getResourceString("FileFilterDialog.CheckboxCaption"), //$NON-NLS-1$
											true, null,	null);
							store.setValue(FILTER_DIALOG_ID, md.getToggleState());
						}
					};
					display.asyncExec(myRun);
					return Status.OK_STATUS;
				}
			};
			job.setPriority(Job.DECORATE);
			job.setRule(null);
			job.schedule();
			}
		}
		}
	}
	
	private static boolean isRelevant(IPreferenceStore javaStore) {
		//XXX: likely to be different in post 3.0 releases of eclipse!!
		return javaStore.contains("CustomFiltersActionGroup." + //$NON-NLS-1$
				PackageExplorerPart.VIEW_ID + ".TAG_DUMMY_TO_TEST_EXISTENCE"); //$NON-NLS-1$
	}

}
