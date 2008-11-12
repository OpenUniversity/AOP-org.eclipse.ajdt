/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Luzius Meisser - initial implementation
 *******************************************************************************/
package org.eclipse.ajdt.core.parserbridge;

import java.util.ArrayList;
import java.util.Map;

import org.aspectj.ajdt.internal.compiler.ast.AdviceDeclaration;
import org.aspectj.ajdt.internal.compiler.ast.DeclareDeclaration;
import org.aspectj.ajdt.internal.compiler.ast.InterTypeConstructorDeclaration;
import org.aspectj.ajdt.internal.compiler.ast.InterTypeDeclaration;
import org.aspectj.ajdt.internal.compiler.ast.InterTypeFieldDeclaration;
import org.aspectj.ajdt.internal.compiler.ast.InterTypeMethodDeclaration;
import org.aspectj.ajdt.internal.compiler.ast.PointcutDeclaration;
import org.aspectj.asm.IProgramElement;
import org.aspectj.org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.aspectj.org.eclipse.jdt.core.compiler.IProblem;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.aspectj.org.eclipse.jdt.internal.compiler.parser.Parser;
import org.aspectj.weaver.patterns.DeclareAnnotation;
import org.aspectj.weaver.patterns.DeclareErrorOrWarning;
import org.aspectj.weaver.patterns.DeclareParents;
import org.aspectj.weaver.patterns.DeclarePrecedence;
import org.eclipse.ajdt.core.javaelements.AJCompilationUnitInfo;
import org.eclipse.ajdt.core.javaelements.AdviceElement;
import org.eclipse.ajdt.core.javaelements.AdviceElementInfo;
import org.eclipse.ajdt.core.javaelements.AspectElement;
import org.eclipse.ajdt.core.javaelements.AspectElementInfo;
import org.eclipse.ajdt.core.javaelements.CompilationUnitTools;
import org.eclipse.ajdt.core.javaelements.DeclareElement;
import org.eclipse.ajdt.core.javaelements.DeclareElementInfo;
import org.eclipse.ajdt.core.javaelements.IntertypeElement;
import org.eclipse.ajdt.core.javaelements.IntertypeElementInfo;
import org.eclipse.ajdt.core.javaelements.PointcutElement;
import org.eclipse.ajdt.core.javaelements.PointcutElementInfo;
import org.eclipse.ajdt.internal.core.parserbridge.IAspectSourceElementRequestor;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.core.AnnotatableInfo;
import org.eclipse.jdt.internal.core.CompilationUnitStructureRequestor;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.JavaElementInfo;
import org.eclipse.jdt.internal.core.PackageDeclaration;
import org.eclipse.jdt.internal.core.SourceTypeElementInfo;

/**
 * @author Luzius Meisser 
 */
public class AJCompilationUnitStructureRequestor extends
		CompilationUnitStructureRequestor implements IAspectSourceElementRequestor {


	public AJCompilationUnitStructureRequestor(ICompilationUnit unit, AJCompilationUnitInfo unitInfo, Map newElements) {
		super(unit, unitInfo, newElements);
	} 
	
	public void setParser(Parser parser){
		CompilerOptions options = new CompilerOptions();
		ProblemReporter probrep = new ProblemReporter(null, options, null);
		org.eclipse.jdt.internal.compiler.parser.Parser otherParser = 
		    new org.eclipse.jdt.internal.compiler.parser.Parser(probrep, false);
		setJDTParser(otherParser);
	}
	
	private void setJDTParser(org.eclipse.jdt.internal.compiler.parser.Parser parser) {
	    this.parser = parser;
	}
	
	public void setSource(char[] source) {
	    this.parser.scanner.source = source;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ajdt.javamodel.parser2.extra.IAspectSourceElementRequestor#enterClass(int, int, char[], int, int, char[], char[][], int)
	 */
	public void enterClass(int declarationStart, int modifiers, char[] name, int nameSourceStart, int nameSourceEnd, char[] superclass, char[][] superinterfaces, boolean isAspect) {
		enterType(declarationStart, modifiers, name, nameSourceStart, nameSourceEnd, superclass, superinterfaces, isAspect);
		
	}

	public void enterMethod(
			int declarationStart,
			int modifiers,
			char[] returnType,
			char[] name,
			int nameSourceStart,
			int nameSourceEnd,
			char[][] parameterTypes,
			char[][] parameterNames,
			char[][] exceptionTypes,
			AbstractMethodDeclaration methodDeclaration) {

		if (methodDeclaration instanceof AdviceDeclaration){
			enterAdvice(declarationStart, modifiers, returnType, name, nameSourceStart, nameSourceEnd, parameterTypes, parameterNames, exceptionTypes, (AdviceDeclaration)methodDeclaration);
			return;
		}
		
		if (methodDeclaration instanceof PointcutDeclaration){
			enterPointcut(declarationStart, modifiers, returnType, name, nameSourceStart, nameSourceEnd, parameterTypes, parameterNames, exceptionTypes, (PointcutDeclaration)methodDeclaration);
			return;
		}
		
		if (methodDeclaration instanceof DeclareDeclaration){
			enterDeclare(declarationStart, modifiers, returnType, name, nameSourceStart, nameSourceEnd, parameterTypes, parameterNames, exceptionTypes, (DeclareDeclaration)methodDeclaration);
			return;
		}
		
		if (methodDeclaration instanceof InterTypeDeclaration){
			enterInterTypeDeclaration(declarationStart, modifiers, returnType, name, nameSourceStart, nameSourceEnd, parameterTypes, parameterNames, exceptionTypes, (InterTypeDeclaration)methodDeclaration);
			return;
		}
		
		org.eclipse.jdt.internal.compiler.ISourceElementRequestor.MethodInfo mi = 
			new org.eclipse.jdt.internal.compiler.ISourceElementRequestor.MethodInfo();
		mi.declarationStart = declarationStart;
		mi.modifiers = modifiers;
		mi.name = name;
		mi.nameSourceStart = nameSourceStart;
		mi.nameSourceEnd = nameSourceEnd;
		mi.parameterNames = parameterNames;
		mi.parameterTypes = parameterTypes;
		mi.exceptionTypes = exceptionTypes;
		
		super.enterMethod(mi);
	}

	public void enterMethod(org.eclipse.jdt.internal.compiler.ISourceElementRequestor.MethodInfo mi) {
		enterMethod(mi.declarationStart,mi.modifiers,mi.returnType,mi.name,mi.nameSourceStart,mi.nameSourceEnd,mi.parameterTypes,mi.parameterNames,mi.exceptionTypes,null);
	}
	
	public void enterMethod(org.aspectj.org.eclipse.jdt.internal.compiler.ISourceElementRequestor.MethodInfo mi) {
		enterMethod(mi.declarationStart,mi.modifiers,mi.returnType,mi.name,mi.nameSourceStart,mi.nameSourceEnd,mi.parameterTypes,mi.parameterNames,mi.exceptionTypes,null);
	}
	
	public void enterMethod(org.eclipse.jdt.internal.compiler.ISourceElementRequestor.MethodInfo mi,AbstractMethodDeclaration mdecl) {
		enterMethod(mi.declarationStart,mi.modifiers,mi.returnType,mi.name,mi.nameSourceStart,mi.nameSourceEnd,mi.parameterTypes,mi.parameterNames,mi.exceptionTypes,mdecl);
	}
	
	public void enterMethod(org.aspectj.org.eclipse.jdt.internal.compiler.ISourceElementRequestor.MethodInfo mi,AbstractMethodDeclaration mdecl) {
		enterMethod(mi.declarationStart,mi.modifiers,mi.returnType,mi.name,mi.nameSourceStart,mi.nameSourceEnd,mi.parameterTypes,mi.parameterNames,mi.exceptionTypes,mdecl);
	}
	/**
	 * Common processing for classes and interfaces.
	 */
	protected void enterType(
		int declarationStart,
		int modifiers,
		char[] name,
		int nameSourceStart,
		int nameSourceEnd,
		char[] superclass,
		char[][] superinterfaces,
		boolean isAspect) {
		
		if (!isAspect) {
			org.eclipse.jdt.internal.compiler.ISourceElementRequestor.TypeInfo ti = 
				new org.eclipse.jdt.internal.compiler.ISourceElementRequestor.TypeInfo();
			ti.declarationStart = declarationStart;
			ti.modifiers = modifiers;
			ti.name = name;
			ti.nameSourceStart = nameSourceStart;
			ti.nameSourceEnd = nameSourceEnd;
			ti.superclass = superclass;
			ti.superinterfaces = superinterfaces;
			super.enterType(ti);
		} else {
		
		Object parentInfo = this.infoStack.peek();
		JavaElement parentHandle= (JavaElement) this.handleStack.peek();
		String nameString= new String(name);
		AspectElement handle = new AspectElement(parentHandle, nameString);
		
		resolveDuplicates(handle);
		
		AspectElementInfo info = new AspectElementInfo();
		
		info.setAJKind(IProgramElement.Kind.ASPECT);
		info.setAJAccessibility(CompilationUnitTools.getAccessibilityFromModifierCode(modifiers));
		info.setAJModifiers(CompilationUnitTools.getModifiersFromModifierCode(modifiers));
		
		info.setHandle(handle);
		info.setSourceRangeStart(declarationStart);
		info.setFlags(modifiers);
		// odd !! info.setName(name);
		info.setNameSourceStart(nameSourceStart);
		info.setNameSourceEnd(nameSourceEnd);
		info.setSuperclassName(superclass);
		info.setSuperInterfaceNames(superinterfaces);
//		info.setSourceFileName(this.sourceFileName);
//		info.setPackageName(this.packageName);
		
		addToChildren(parentInfo, handle);	
		
		this.newElements.put(handle, info);

		this.infoStack.push(info);
		this.handleStack.push(handle);
		}

	}
	
	/* default */ static String[] convertTypeNamesToSigsCopy(char[][] typeNames) {
		if (typeNames == null)
			return CharOperation.NO_STRINGS;
		int n = typeNames.length;
		if (n == 0)
			return CharOperation.NO_STRINGS;
		String[] typeSigs = new String[n];
		for (int i = 0; i < n; ++i) {
			typeSigs[i] = Signature.createTypeSignature(typeNames[i], false);
		}
		return typeSigs;
	}
	
	public void enterAdvice(
			int declarationStart,
			int modifiers,
			char[] returnType,
			char[] name,
			int nameSourceStart,
			int nameSourceEnd,
			char[][] parameterTypes,
			char[][] parameterNames,
			char[][] exceptionTypes,
			AdviceDeclaration decl) {
		
			
				SourceTypeElementInfo parentInfo = (SourceTypeElementInfo) this.infoStack.peek();
				JavaElement parentHandle= (JavaElement) this.handleStack.peek();
				AdviceElement handle = null;

				// translate nulls to empty arrays
				if (parameterTypes == null) {
					parameterTypes= CharOperation.NO_CHAR_CHAR;
				}
				if (parameterNames == null) {
					parameterNames= CharOperation.NO_CHAR_CHAR;
				}
				if (exceptionTypes == null) {
					exceptionTypes= CharOperation.NO_CHAR_CHAR;
				}
				
				String nameString = decl.kind.getName();
				String[] parameterTypeSigs = convertTypeNamesToSigsCopy(parameterTypes);
				handle = new AdviceElement(parentHandle, new String(nameString), parameterTypeSigs);
			
				resolveDuplicates(handle);
				
				AdviceElementInfo info = new AdviceElementInfo();
				info.setAJKind(IProgramElement.Kind.ADVICE);
				IProgramElement.ExtraInformation extraInfo = new IProgramElement.ExtraInformation();
				info.setAJExtraInfo(extraInfo);
				extraInfo.setExtraAdviceInformation(decl.kind.getName());
				
				info.setSourceRangeStart(declarationStart);
				int flags = modifiers;
				info.setName(nameString.toCharArray());
				info.setNameSourceStart(nameSourceStart);
				info.setNameSourceEnd(nameSourceEnd);
				info.setFlags(flags);
				info.setArgumentNames(parameterNames);
				//info.setArgumentTypeNames(parameterTypes);
				info.setReturnType(returnType == null ? new char[]{'v', 'o','i', 'd'} : returnType);
				info.setExceptionTypeNames(exceptionTypes);

				addToChildren(parentInfo, handle);
				this.newElements.put(handle, info);
				this.infoStack.push(info);
				this.handleStack.push(handle);	
		}
	
	public void enterInterTypeDeclaration(
			int declarationStart,
			int modifiers,
			char[] returnType,
			char[] name,
			int nameSourceStart,
			int nameSourceEnd,
			char[][] parameterTypes,
			char[][] parameterNames,
			char[][] exceptionTypes,
			InterTypeDeclaration decl) {
		
				nameSourceEnd = nameSourceStart + decl.getDeclaredSelector().length - 1; 
		
				SourceTypeElementInfo parentInfo = (SourceTypeElementInfo) this.infoStack.peek();
				JavaElement parentHandle= (JavaElement) this.handleStack.peek();
				IntertypeElement handle = null;

				// translate nulls to empty arrays
				if (parameterTypes == null) {
					parameterTypes= CharOperation.NO_CHAR_CHAR;
				}
				if (parameterNames == null) {
					parameterNames= CharOperation.NO_CHAR_CHAR;
				}
				if (exceptionTypes == null) {
					exceptionTypes= CharOperation.NO_CHAR_CHAR;
				}
				
				String nameString = new String(decl.getOnType().getTypeName()[0]) + "." + new String(decl.getDeclaredSelector()); //$NON-NLS-1$
				String[] parameterTypeSigs = convertTypeNamesToSigsCopy(parameterTypes);
				handle = new IntertypeElement(parentHandle, nameString, parameterTypeSigs);
				
				resolveDuplicates(handle);
				
				IntertypeElementInfo info = new IntertypeElementInfo();
				
				if (decl instanceof InterTypeFieldDeclaration)
					info.setAJKind(IProgramElement.Kind.INTER_TYPE_FIELD);
				else if (decl instanceof InterTypeMethodDeclaration)
					info.setAJKind(IProgramElement.Kind.INTER_TYPE_METHOD);
				else if (decl instanceof InterTypeConstructorDeclaration)
					info.setAJKind(IProgramElement.Kind.INTER_TYPE_CONSTRUCTOR);
				else
					info.setAJKind(IProgramElement.Kind.INTER_TYPE_PARENT);
				
				// Fix for 116846 - incorrect icons for itds - use declaredModifiers instead
				info.setAJAccessibility(CompilationUnitTools.getAccessibilityFromModifierCode(decl.declaredModifiers));
				info.setAJModifiers(CompilationUnitTools.getModifiersFromModifierCode(decl.declaredModifiers));
				
				info.setSourceRangeStart(declarationStart);
				int flags = modifiers;
				info.setName(nameString.toCharArray());
				info.setNameSourceStart(nameSourceStart);
				info.setNameSourceEnd(nameSourceEnd);
				info.setTargetType(decl.getOnType().getTypeName()[0]);
				info.setFlags(flags);
				info.setArgumentNames(parameterNames);
				//info.setArgumentTypeNames(parameterTypes);
				info.setReturnType(returnType == null ? new char[]{'v', 'o','i', 'd'} : returnType);
				info.setExceptionTypeNames(exceptionTypes);

				addToChildren(parentInfo, handle);
				this.newElements.put(handle, info);
				this.infoStack.push(info);
				this.handleStack.push(handle);	
		}
	
	public void enterDeclare(
			int declarationStart,
			int modifiers,
			char[] returnType,
			char[] name,
			int nameSourceStart,
			int nameSourceEnd,
			char[][] parameterTypes,
			char[][] parameterNames,
			char[][] exceptionTypes,
			DeclareDeclaration decl) {
		
				nameSourceStart += 8;
				nameSourceEnd = nameSourceStart;
				SourceTypeElementInfo parentInfo = (SourceTypeElementInfo) this.infoStack.peek();
				JavaElement parentHandle= (JavaElement) this.handleStack.peek();
				DeclareElement handle = null;

				DeclareElementInfo info = new DeclareElementInfo();
				
				String msg = ""; //$NON-NLS-1$
				if (decl.declareDecl instanceof DeclareErrorOrWarning){
					msg = ": \"" + ((DeclareErrorOrWarning)decl.declareDecl).getMessage() + "\"";  //$NON-NLS-1$//$NON-NLS-2$
					if (((DeclareErrorOrWarning)decl.declareDecl).isError()){
						info.setAJKind(IProgramElement.Kind.DECLARE_ERROR);
						nameSourceEnd += 4;
					}else{
						info.setAJKind(IProgramElement.Kind.DECLARE_WARNING);
						nameSourceEnd += 6;
					}
				} else if (decl.declareDecl instanceof DeclareParents){
					info.setAJKind(IProgramElement.Kind.DECLARE_PARENTS);
					nameSourceEnd += 6;
				} else if (decl.declareDecl instanceof DeclarePrecedence){
					info.setAJKind(IProgramElement.Kind.DECLARE_PRECEDENCE);
					nameSourceEnd += 9;
				} else if (decl.declareDecl instanceof DeclareAnnotation){
					DeclareAnnotation anno = (DeclareAnnotation)decl.declareDecl;
					if (anno.isDeclareAtConstuctor()) {
						info.setAJKind(IProgramElement.Kind.DECLARE_ANNOTATION_AT_CONSTRUCTOR);
						nameSourceEnd += "@constructor".length()-1; //$NON-NLS-1$
					} else if (anno.isDeclareAtField()) {
						info.setAJKind(IProgramElement.Kind.DECLARE_ANNOTATION_AT_FIELD);
						nameSourceEnd += "@field".length()-1; //$NON-NLS-1$
					} else if (anno.isDeclareAtMethod()) {
						info.setAJKind(IProgramElement.Kind.DECLARE_ANNOTATION_AT_METHOD);
						nameSourceEnd += "@method".length()-1; //$NON-NLS-1$
					} else if (anno.isDeclareAtType()) {
						info.setAJKind(IProgramElement.Kind.DECLARE_ANNOTATION_AT_TYPE);
						nameSourceEnd += "@type".length()-1; //$NON-NLS-1$
					}
				} else {
					//assume declare soft
					info.setAJKind(IProgramElement.Kind.DECLARE_SOFT);
					nameSourceEnd += 3;
				}
				String nameString = info.getAJKind().toString() + msg;
				
				
				// translate nulls to empty arrays
				if (parameterTypes == null) {
					parameterTypes= CharOperation.NO_CHAR_CHAR;
				}
				if (parameterNames == null) {
					parameterNames= CharOperation.NO_CHAR_CHAR;
				}
				if (exceptionTypes == null) {
					exceptionTypes= CharOperation.NO_CHAR_CHAR;
				}
				
				String[] parameterTypeSigs = convertTypeNamesToSigsCopy(parameterTypes);
				
				handle = new DeclareElement(parentHandle, nameString, parameterTypeSigs);
				
				
				resolveDuplicates(handle);
				

				
				info.setSourceRangeStart(declarationStart);
				int flags = modifiers;
				info.setName(nameString.toCharArray());
				info.setNameSourceStart(nameSourceStart);
				info.setNameSourceEnd(nameSourceEnd);
				info.setFlags(flags);
				info.setArgumentNames(parameterNames);
				//info.setArgumentTypeNames(parameterTypes);
				info.setReturnType(returnType == null ? new char[]{'v', 'o','i', 'd'} : returnType);
				info.setExceptionTypeNames(exceptionTypes);

				addToChildren(parentInfo, handle);
				this.newElements.put(handle, info);
				this.infoStack.push(info);
				this.handleStack.push(handle);	
		}
	
	public void enterPointcut(
			int declarationStart,
			int modifiers,
			char[] returnType,
			char[] name,
			int nameSourceStart,
			int nameSourceEnd,
			char[][] parameterTypes,
			char[][] parameterNames,
			char[][] exceptionTypes,
			PointcutDeclaration decl) {
		
		

				SourceTypeElementInfo parentInfo = (SourceTypeElementInfo) this.infoStack.peek();
				JavaElement parentHandle= (JavaElement) this.handleStack.peek();
				PointcutElement handle = null;

				// translate nulls to empty arrays
				if (parameterTypes == null) {
					parameterTypes= CharOperation.NO_CHAR_CHAR;
				}
				if (parameterNames == null) {
					parameterNames= CharOperation.NO_CHAR_CHAR;
				}
				if (exceptionTypes == null) {
					exceptionTypes= CharOperation.NO_CHAR_CHAR;
				}
				
				String[] parameterTypeSigs = convertTypeNamesToSigsCopy(parameterTypes);
				handle = new PointcutElement(parentHandle, new String(name), parameterTypeSigs);

				resolveDuplicates(handle);
				
				PointcutElementInfo info = new PointcutElementInfo();
				
				info.setAJKind(IProgramElement.Kind.POINTCUT);
				info.setAJAccessibility(CompilationUnitTools.getAccessibilityFromModifierCode(decl.modifiers));
				info.setAJModifiers(CompilationUnitTools.getModifiersFromModifierCode(decl.modifiers));
				
				info.setSourceRangeStart(declarationStart);
				info.setSourceRangeEnd(decl.sourceEnd+1);
				int flags = modifiers;
				info.setName(name);
				info.setNameSourceStart(nameSourceStart);
				info.setNameSourceEnd(nameSourceEnd);
				info.setFlags(flags);
				info.setArgumentNames(parameterNames);
				//info.setArgumentTypeNames(parameterTypes);
				info.setReturnType(returnType == null ? new char[]{'v', 'o','i', 'd'} : returnType);
				info.setExceptionTypeNames(exceptionTypes);

				addToChildren(parentInfo, handle);
				this.newElements.put(handle, info);
				this.infoStack.push(info);
				this.handleStack.push(handle);	
		}


	public void acceptProblem(CategorizedProblem problem) {
		if ((problem.getID() & IProblem.Syntax) != 0){
			this.hasSyntaxErrors = true;
		}		
	}

	private void addToChildren(Object parentInfo, JavaElement handle) {
		ArrayList childrenList = (ArrayList) this.children.get(parentInfo);
		if (childrenList == null)
			this.children.put(parentInfo, childrenList = new ArrayList());
		childrenList.add(handle);
	}
	
	public void enterType(org.aspectj.org.eclipse.jdt.internal.compiler.ISourceElementRequestor.TypeInfo typeInfo, boolean isAspect) {
		enterType(typeInfo.declarationStart,typeInfo.modifiers,typeInfo.name,typeInfo.nameSourceStart,typeInfo.nameSourceEnd,typeInfo.superclass,typeInfo.superinterfaces,isAspect);
	}

	public void enterConstructor(org.aspectj.org.eclipse.jdt.internal.compiler.ISourceElementRequestor.MethodInfo methodInfo) {
		org.eclipse.jdt.internal.compiler.ISourceElementRequestor.MethodInfo mi = 
			new org.eclipse.jdt.internal.compiler.ISourceElementRequestor.MethodInfo();
		mi.declarationStart = methodInfo.declarationStart;
		mi.modifiers = methodInfo.modifiers;
		mi.name = methodInfo.name;
		mi.nameSourceStart = methodInfo.nameSourceStart;
		mi.nameSourceEnd = methodInfo.nameSourceEnd;
		mi.parameterNames = methodInfo.parameterNames;
		mi.parameterTypes = methodInfo.parameterTypes;
		mi.exceptionTypes = methodInfo.exceptionTypes;
		mi.isConstructor = true;
		enterConstructor(mi);
	}

	public void enterField(org.aspectj.org.eclipse.jdt.internal.compiler.ISourceElementRequestor.FieldInfo fieldInfo) {
		org.eclipse.jdt.internal.compiler.ISourceElementRequestor.FieldInfo fi = 
			new org.eclipse.jdt.internal.compiler.ISourceElementRequestor.FieldInfo();
		fi.declarationStart = fieldInfo.declarationStart;
		fi.modifiers = fieldInfo.modifiers;
		fi.type = fieldInfo.type;
		fi.name = fieldInfo.name;
		fi.nameSourceStart = fieldInfo.nameSourceStart;
		fi.nameSourceEnd = fieldInfo.nameSourceEnd;
		enterField(fi);
	}

	public void enterType(org.aspectj.org.eclipse.jdt.internal.compiler.ISourceElementRequestor.TypeInfo typeInfo) {
		enterType(typeInfo.declarationStart,typeInfo.modifiers,typeInfo.name,typeInfo.nameSourceStart,typeInfo.nameSourceEnd,typeInfo.superclass,typeInfo.superinterfaces,false);
	}
	
	public void enterType(org.eclipse.jdt.internal.compiler.ISourceElementRequestor.TypeInfo typeInfo, boolean isAspect) {
		enterType(typeInfo.declarationStart,typeInfo.modifiers,typeInfo.name,typeInfo.nameSourceStart,typeInfo.nameSourceEnd,typeInfo.superclass,typeInfo.superinterfaces,isAspect);
	}



	public void enterType(org.eclipse.jdt.internal.compiler.ISourceElementRequestor.TypeInfo typeInfo) {
		enterType(typeInfo.declarationStart,typeInfo.modifiers,typeInfo.name,typeInfo.nameSourceStart,typeInfo.nameSourceEnd,typeInfo.superclass,typeInfo.superinterfaces,false);
	}

	public void acceptImport(int declarationStart, int declarationEnd, char[] name, boolean onDemand, int modifiers) {
		super.acceptImport(declarationStart, declarationEnd, CharOperation.splitOn('.', name), onDemand, modifiers);
	}

	/**
	 * use {@link #acceptPackage(ImportReference)} instead
	 * @deprecated
	 */
	public void acceptPackage(int declarationStart, int declarationEnd,
			char[] name) {
		
		// AJDT 1.6 ADE---copied from CompilationUnitStructureProvider.acceptPackage(ImportReference)
		JavaElementInfo parentInfo = (JavaElementInfo) this.infoStack.peek();
		JavaElement parentHandle= (JavaElement) this.handleStack.peek();
		PackageDeclaration handle = null;
		
		if (parentHandle.getElementType() == IJavaElement.COMPILATION_UNIT) {
			handle = createPackageDeclaration(parentHandle, new String(name));
		}
		else {
			Assert.isTrue(false); // Should not happen
		}
		resolveDuplicates(handle);
		
		
		// make the two methods accessible here
		class AJAnnotatableInfo extends AnnotatableInfo {
			protected void setSourceRangeStart(int start) {
				super.setSourceRangeStart(start);
			}
			protected void setSourceRangeEnd(int end) {
				super.setSourceRangeEnd(end);
			}
		}
		AJAnnotatableInfo info = new AJAnnotatableInfo() ;
		info.setSourceRangeStart(declarationStart);
		info.setSourceRangeEnd(declarationEnd);

		addToChildren(parentInfo, handle);
		this.newElements.put(handle, info);

		// We do not deal with annotations here
//		if (importReference.annotations != null) {
//			for (int i = 0, length = importReference.annotations.length; i < length; i++) {
//				org.eclipse.jdt.internal.compiler.ast.Annotation annotation = importReference.annotations[i];
//				enterAnnotation(annotation, info, handle);
//				exitMember(annotation.declarationSourceEnd);
//			}
//		}	

		
	}


	public void exitMethod(int declarationEnd, int defaultValueStart,
			int defaultValueEnd) {
		super.exitMethod(declarationEnd, null);
	}
	
	/**
	 * @since 1.6
	 */
	public void acceptPackage(
			org.aspectj.org.eclipse.jdt.internal.compiler.ast.ImportReference ir) {
		ImportReference dup = new ImportReference(ir.tokens,ir.sourcePositions,(ir.bits & org.aspectj.org.eclipse.jdt.internal.compiler.ast.ASTNode.OnDemand)!=0,ir.modifiers);
		dup.declarationSourceStart = ir.declarationSourceStart;
		dup.declarationSourceEnd = ir.declarationSourceEnd;
		super.acceptPackage(dup);
	}
}