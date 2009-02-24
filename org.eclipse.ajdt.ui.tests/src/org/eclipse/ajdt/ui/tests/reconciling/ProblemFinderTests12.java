/*******************************************************************************
 * Copyright (c) 2009 SpringSource and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *      Andrew Eisenberg - Initial implementation
 *******************************************************************************/
package org.eclipse.ajdt.ui.tests.reconciling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ajdt.core.AspectJCore;
import org.eclipse.ajdt.core.javaelements.AJCompilationUnit;
import org.eclipse.ajdt.core.parserbridge.AJCompilationUnitProblemFinder;
import org.eclipse.ajdt.internal.core.AJWorkingCopyOwner;
import org.eclipse.ajdt.ui.tests.UITestCase;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.CompilationUnitProblemFinder;
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner;

/**
 * Tests AJCompilationUnitProblemFinder and ITDAwareness
 * 
 * Specifically tests that return types on ITD methods do not need to 
 * be fully qualified.  See bug 265986
 * 
 * @author andrew
 *
 */
public class ProblemFinderTests12 extends UITestCase {
    List/*ICompilationUnit*/ allCUnits = new ArrayList();
    IProject proj;
    protected void setUp() throws Exception {
        super.setUp();
        proj = createPredefinedProject("Bug265986-ITDMissingType"); //$NON-NLS-1$
        waitForJobsToComplete();
        
        IFolder src = proj.getFolder("src");
        
        IResourceVisitor visitor = new IResourceVisitor() {
            public boolean visit(IResource resource) throws CoreException {
                if (resource.getType() == IResource.FILE && 
                        (resource.getName().endsWith("java") ||
                                resource.getName().endsWith("aj"))) {
                    allCUnits.add(createUnit((IFile) resource));
                }
                return true;
            }
        };
        src.accept(visitor);
        
        waitForJobsToComplete();
        setAutobuilding(false);
        
    }
    
    private ICompilationUnit createUnit(IFile file) {
        return (ICompilationUnit) AspectJCore.create(file);
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        setAutobuilding(true);
    }

    public void testProblemFindingAll() throws Exception {
        StringBuffer sb = new StringBuffer();
        for (Iterator cunitIter = allCUnits.iterator(); cunitIter.hasNext();) {
            sb.append(problemFind((ICompilationUnit) cunitIter.next()));
        }
        if (sb.length() > 0) {
            fail(sb.toString());
        }
    }
    
    private String problemFind(ICompilationUnit unit) throws Exception {
        HashMap problems = doFind(unit);
        MockProblemRequestor.filterAllWarningProblems(problems);
        if (MockProblemRequestor.countProblems(problems) > 0) {
            return "Should not have any problems in " + unit + " but found:\n" + MockProblemRequestor.printProblems(problems) + "\n"; //$NON-NLS-1$
        } else {
            return "";
        }
    }
    private HashMap doFind(ICompilationUnit unit)
            throws JavaModelException {
        HashMap problems = new HashMap();
        if (unit instanceof AJCompilationUnit) {
            AJCompilationUnitProblemFinder.processAJ((AJCompilationUnit) unit, 
                    AJWorkingCopyOwner.INSTANCE, problems, true, 
                    ICompilationUnit.ENABLE_BINDINGS_RECOVERY | ICompilationUnit.ENABLE_STATEMENTS_RECOVERY | ICompilationUnit.FORCE_PROBLEM_DETECTION, null);
        } else {
            // Requires JDT Weaving
            CompilationUnitProblemFinder.process((CompilationUnit) unit, null,
                    DefaultWorkingCopyOwner.PRIMARY, problems, true, 
                    ICompilationUnit.ENABLE_BINDINGS_RECOVERY | ICompilationUnit.ENABLE_STATEMENTS_RECOVERY | ICompilationUnit.FORCE_PROBLEM_DETECTION, null);
        }
        return problems;
    }

    
}