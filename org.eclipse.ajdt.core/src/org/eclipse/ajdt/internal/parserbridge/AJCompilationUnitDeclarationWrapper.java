/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: Sian January - initial version
 ******************************************************************************/
package org.eclipse.ajdt.internal.parserbridge;

import org.aspectj.org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.ajdt.core.AJLog;
import org.eclipse.ajdt.core.javaelements.AJCompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilationUnit;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;

/**
 * Wrapper class that extends org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration 
 * and wraps an org.aspectj.org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration
 */
public class AJCompilationUnitDeclarationWrapper extends
		CompilationUnitDeclaration {

	
	private org.aspectj.org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration delegate;
	private AJCompilationUnit cUnit;

	/**
	 * @param problemReporter
	 * @param compilationResult
	 * @param sourceLength
	 */
	public AJCompilationUnitDeclarationWrapper(org.aspectj.org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration delegate, AJCompilationUnit cUnit) {
		super(null, null, 0);
		this.delegate = delegate;
		this.cUnit = cUnit;
	}

	/*
	 *	We cause the compilation task to abort to a given extent.
	 */
	public void abort(int abortLevel, IProblem problem) {
		delegate.abort(abortLevel, new org.aspectj.org.eclipse.jdt.internal.compiler.problem.DefaultProblem(
				problem.getOriginatingFileName(),
				problem.getMessage(),
				problem.getID(),
				problem.getArguments(),
				problem.isWarning()? ProblemSeverities.Error : ProblemSeverities.Warning,
				problem.getSourceStart(),
				problem.getSourceEnd(),
				problem.getSourceLineNumber()));
	}

	/*
	 * Dispatch code analysis AND request saturation of inner emulation
	 */
	public void analyseCode() {
		delegate.analyseCode();
	}

	/*
	 * When unit result is about to be accepted, removed back pointers
	 * to compiler structures.
	 */
	public void cleanUp() {
		delegate.cleanUp();
	}


	public void checkUnusedImports(){
		delegate.checkUnusedImports();
		
	}
	
	public CompilationResult compilationResult() {
		CompilationResult cr = new CompilationResult(cUnit,
				delegate.compilationResult.unitIndex,
				delegate.compilationResult.totalUnitsKnown,
				500);
		cr.lineSeparatorPositions = delegate.compilationResult.lineSeparatorPositions;
		cr.problemCount = delegate.compilationResult.problemCount;
		cr.compiledTypes = delegate.compilationResult.compiledTypes;
		cr.hasBeenAccepted = delegate.compilationResult.hasBeenAccepted;
		cr.qualifiedReferences = delegate.compilationResult.qualifiedReferences;
		cr.simpleNameReferences = delegate.compilationResult.simpleNameReferences;
		if(delegate.compilationResult.problems != null) {
			cr.problems = new IProblem[delegate.compilationResult.problems.length];
			for (int i = 0; i < delegate.compilationResult.problems.length; i++) {
				org.aspectj.org.eclipse.jdt.core.compiler.IProblem ajprob = delegate.compilationResult.problems[i];
				if(ajprob != null) {
					cr.problems[i] = new DefaultProblem(
							ajprob.getOriginatingFileName(),
							ajprob.getMessage(),
							ajprob.getID(),
							ajprob.getArguments(),
							ajprob.isWarning()? ProblemSeverities.Error : ProblemSeverities.Warning,
							ajprob.getSourceStart(),
							ajprob.getSourceEnd(),
							ajprob.getSourceLineNumber());
				} 
			}
		} else { 
			cr.problems = new IProblem[0];
		}
		cr.taskCount = delegate.compilationResult.taskCount;
//		cr.tasks
		return cr;
	}
	
	/*
	 * Finds the matching type amoung this compilation unit types.
	 * Returns null if no type with this name is found.
	 * The type name is a compound name
	 * eg. if we're looking for X.A.B then a type name would be {X, A, B}
	 */
	public TypeDeclaration declarationOfType(char[][] typeName) {
		return new TypeDeclaration(compilationResult());
	}

	/**
	 * Bytecode generation
	 */
	public void generateCode() {
delegate.generateCode();
	}

	public char[] getFileName() {
return delegate.getFileName();
	}

	public char[] getMainTypeName() {
return delegate.getMainTypeName();
	}

	public boolean isEmpty() {

		return delegate.isEmpty();
	}

	public boolean hasErrors() {
		return delegate.hasErrors();
	}

	public StringBuffer print(int indent, StringBuffer output) {
		return delegate.print(indent, output);
	}
	
	/*
	 * Force inner local types to update their innerclass emulation
	 */
	public void propagateInnerEmulationForAllLocalTypes() {
delegate.propagateInnerEmulationForAllLocalTypes();
	}

	/*
	 * Keep track of all local types, so as to update their innerclass
	 * emulation later on.
	 */
	public void record(LocalTypeBinding localType) {
//		delegate.record(new org.aspectj.org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding()localType);
	}

	public void resolve() {
		delegate.resolve();
	}

	public void tagAsHavingErrors() {
		delegate.tagAsHavingErrors();
	}

	public void traverse(
		ASTVisitor visitor,
		CompilationUnitScope unitScope) {
		
		if (delegate.ignoreFurtherInvestigation)
			return;
		try {
//			JavaProject project = (JavaProject)cUnit.getJavaProject();
//			INameEnvironment nameEnv = new NameEnvironment(project);
//			CompilerOptions options = new CompilerOptions(project.getOptions(true));
//			AJCompilationUnitProblemFinder probFinder = new AJCompilationUnitProblemFinder(nameEnv,
//					null,
//					project.getOptions(true),
//					null,
//					null,
//					cUnit);
//			LookupEnvironment env = new LookupEnvironment(probFinder, options, new ProblemReporter(), nameEnv);
//			CompilationUnitScope scope = new CompilationUnitScope(this, env);
			if (visitor.visit(this, unitScope)) {
				if (currentPackage != null) {
					currentPackage.traverse(visitor, unitScope);
				}
				if (delegate.imports != null) {
					int importLength = delegate.imports.length;
					for (int i = 0; i < importLength; i++) {
						AJLog.log("AJCompilationUnitDeclarationWrapper - Not traversing import: " + delegate.imports[i]);
//						System.err.println("Not traversing import: " + delegate.imports[i]);
//						delegate.imports[i].traverse(visitor, unitScope);
					}
				}
				if (delegate.types != null) {
					int typesLength = delegate.types.length;
					for (int i = 0; i < typesLength; i++) {
						AJLog.log("AJCompilationUnitDeclarationWrapper - Not traversing type: " + delegate.types[i]);
//						System.err.println("Not traversing type: " + delegate.types[i]);
//						delegate.types[i].traverse(visitor, scope);
					}
				}
			}
			visitor.endVisit(this, scope);
		} catch (AbortCompilationUnit e) {
			// ignore
		}
		
//
//		delegate.traverse(new org.aspectj.org.eclipse.jdt.internal.compiler.ASTVisitor(){
//			}, 
//			new org.aspectj.org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope(delegate, delegate.scope.environment));
	}

	/**
	 * 
	 */
	public void reconcileVars() {
		this.compilationResult = compilationResult();
	}
	
}
