/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation 
 * 				 Helen Hawkins   - iniital version
 ******************************************************************************/
package org.eclipse.ajdt.internal.ui.refactoring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.ajdt.core.AJProperties;
import org.eclipse.ajdt.internal.ui.ajde.ErrorHandler;
import org.eclipse.ajdt.internal.ui.text.UIMessages;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameResourceChange;

public class RenamingUtils {

	/**
	 * Utility method - Rename a single file's extension. Add the old and new
	 * names to the map supplied.
	 * 
	 * @param newExtensionIsAJ
	 * @param file
	 * @param monitor
	 * @param oldToNewNames -
	 *            Map of old to new names augmented by this method
	 */
	public static void renameFile(boolean newExtensionIsAJ, IResource file,
			IProgressMonitor monitor, Map oldToNewNames) {
		String oldName = file.getName();
		String nameWithoutExtension = oldName
				.substring(0, oldName.indexOf('.')); //$NON-NLS-1$
		String newExtension = newExtensionIsAJ ? ".aj" : ".java"; //$NON-NLS-1$ //$NON-NLS-2$
		RenameResourceChange change = new RenameResourceChange(file,
				nameWithoutExtension + newExtension, null);
		try {
			change.perform(monitor);
			oldToNewNames.put(oldName, nameWithoutExtension + newExtension);
		} catch (CoreException e) {
			ErrorHandler.handleAJDTError(UIMessages.Refactoring_ErrorRenamingResource, e);
		}
	}

	public static void updateBuildConfigurations(Map oldNamesToNewNames,
			IProject project, IProgressMonitor monitor) {
		List buildConfigs = AJProperties.getAJPropertiesFiles(project);
		for (Iterator iter = buildConfigs.iterator(); iter.hasNext();) {
			IFile buildConfig = (IFile) iter.next();
			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(buildConfig
						.getContents()));
			} catch (CoreException e) {
				continue;
			}
			StringBuffer sb = new StringBuffer();
			try {
				String line = br.readLine();
				while (line != null) {
					for (Iterator iter2 = oldNamesToNewNames.keySet()
							.iterator(); iter2.hasNext();) {
						String oldName = (String) iter2.next();
						String newName = (String) oldNamesToNewNames
								.get(oldName);
						line = line.replaceAll(oldName, newName);
					}
					sb.append(line);
					sb.append(System.getProperty("line.separator")); //$NON-NLS-1$
					line = br.readLine();
				}
				StringReader reader = new StringReader(sb.toString());
				buildConfig.setContents(new ReaderInputStream(reader), true,
						true, monitor);
			} catch (IOException ioe) {
			} catch (CoreException e) {
			} finally {
				try {
					br.close();
				} catch (IOException ioe) {
				}
			}
		}
	}
	
}
