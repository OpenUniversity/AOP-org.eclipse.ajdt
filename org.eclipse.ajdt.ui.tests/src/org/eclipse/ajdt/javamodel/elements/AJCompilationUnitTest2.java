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
package org.eclipse.ajdt.javamodel.elements;

import junit.framework.TestCase;

import org.eclipse.ajdt.core.javaelements.AJCompilationUnitManager;
import org.eclipse.ajdt.test.AllTests;
import org.eclipse.ajdt.test.utils.Utils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IConfirmQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgQueries;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaDeleteProcessor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;

/**
 * 
 */
public class AJCompilationUnitTest2 extends TestCase {

	// bug 74426
	public void testDeletingAJCompilationUnits() throws Exception {
		AllTests.setupAJDTPlugin();
		IProject project = Utils
				.createPredefinedProject("Introduction Example");

		IFile file1 = (IFile) project
				.findMember("src/introduction/CloneablePoint.aj");
		assertNotNull("Couldn't find CloneablePoint.aj in project", file1);
		IFile file2 = (IFile) project
				.findMember("src/introduction/ComparablePoint.aj");
		assertNotNull("Couldn't find ComparablePoint.aj in project", file1);

		final ICompilationUnit unit1 = AJCompilationUnitManager.INSTANCE
				.getAJCompilationUnit(file1);
		assertNotNull("Couldn't obtain compilation unit for file: " + file1,
				unit1);
		ICompilationUnit unit2 = AJCompilationUnitManager.INSTANCE
				.getAJCompilationUnit(file2);
		assertNotNull("Couldn't obtain compilation unit for file: " + file2,
				unit2);

		// now try to delete these units
		Utils.waitForJobsToComplete();

		Object[] elements = new Object[] { unit1, unit2 };
		JavaDeleteProcessor processor = new JavaDeleteProcessor(elements);
		IProgressMonitor pm = new NullProgressMonitor();
		CheckConditionsContext context = new CheckConditionsContext();
		context.add(new ValidateEditChecker(context));
		IReorgQueries q = new IReorgQueries() {
			public IConfirmQuery createYesYesToAllNoNoToAllQuery(
					String queryTitle, boolean allowCancel, int queryID) {
				return null;
			}

			public IConfirmQuery createYesNoQuery(String queryTitle,
					boolean allowCancel, int queryID) {
				return null;
			}

			public IConfirmQuery createSkipQuery(String queryTitle, int queryID) {
				return null;
			}
		};
		processor.setQueries(q);
		processor.checkInitialConditions(pm);
		processor.checkFinalConditions(pm, context);
		Change change = processor.createChange(pm);
		change.perform(pm);

		Utils.waitForJobsToComplete();

		// check the corresponding files have gone
		assertFalse("File should have been deleted: " + file1, file1.exists());
		assertFalse("File should have been deleted: " + file2, file2.exists());

		Utils.deleteProject(project);
	}

}
