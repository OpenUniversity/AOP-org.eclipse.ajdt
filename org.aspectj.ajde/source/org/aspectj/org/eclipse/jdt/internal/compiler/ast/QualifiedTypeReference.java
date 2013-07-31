/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Herrmann - Contribution for
 *								bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *******************************************************************************/
package org.aspectj.org.eclipse.jdt.internal.compiler.ast;

import org.aspectj.org.eclipse.jdt.core.compiler.CharOperation;
import org.aspectj.org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.*;
import org.aspectj.org.eclipse.jdt.internal.compiler.problem.AbortCompilation;

public class QualifiedTypeReference extends TypeReference {

	public char[][] tokens;
	public long[] sourcePositions;

	public QualifiedTypeReference(char[][] sources , long[] poss) {

		this.tokens = sources ;
		this.sourcePositions = poss ;
		this.sourceStart = (int) (this.sourcePositions[0]>>>32) ;
		this.sourceEnd = (int)(this.sourcePositions[this.sourcePositions.length-1] & 0x00000000FFFFFFFFL ) ;
	}

	public TypeReference copyDims(int dim){
		//return a type reference copy of me with some dimensions
		//warning : the new type ref has a null binding
		return new ArrayQualifiedTypeReference(this.tokens, dim, this.sourcePositions);
	}
	
	public TypeReference copyDims(int dim, Annotation[][] annotationsOnDimensions) {
		//return a type reference copy of me with some dimensions
		//warning : the new type ref has a null binding
		ArrayQualifiedTypeReference arrayQualifiedTypeReference = new ArrayQualifiedTypeReference(this.tokens, dim, annotationsOnDimensions, this.sourcePositions);
		arrayQualifiedTypeReference.bits |= (this.bits & ASTNode.HasTypeAnnotations);
		if (annotationsOnDimensions != null) {
			arrayQualifiedTypeReference.bits |= ASTNode.HasTypeAnnotations;
		}
		return arrayQualifiedTypeReference;
	}

	protected TypeBinding findNextTypeBinding(int tokenIndex, Scope scope, PackageBinding packageBinding) {
		LookupEnvironment env = scope.environment();
		try {
			env.missingClassFileLocation = this;
			ReferenceBinding previousType = null;
			if (this.resolvedType == null) {
				this.resolvedType = scope.getType(this.tokens[tokenIndex], packageBinding);
			} else {
				if (this.resolvedType instanceof ReferenceBinding)
					previousType = (ReferenceBinding) this.resolvedType;
				this.resolvedType = scope.getMemberType(this.tokens[tokenIndex], (ReferenceBinding) this.resolvedType);
				if (!this.resolvedType.isValidBinding()) {
					this.resolvedType = new ProblemReferenceBinding(
						CharOperation.subarray(this.tokens, 0, tokenIndex + 1),
						(ReferenceBinding)this.resolvedType.closestMatch(),
						this.resolvedType.problemId());
				}
			}
			if (this.annotations != null && this.annotations[tokenIndex] != null) {
				this.resolvedType = captureTypeAnnotations(scope, previousType, this.resolvedType, this.annotations[tokenIndex]);
			}
			return this.resolvedType;
		} catch (AbortCompilation e) {
			e.updateContext(this, scope.referenceCompilationUnit().compilationResult);
			throw e;
		} finally {
			env.missingClassFileLocation = null;
		}
	}

	public char[] getLastToken() {
		return this.tokens[this.tokens.length-1];
	}

	protected void rejectAnnotationsOnPackageQualifiers(Scope scope, PackageBinding packageBinding) {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=390882
		if (packageBinding == null || this.annotations == null) return;

		int i = packageBinding.compoundName.length;
		for (int j = 0; j < i; j++) {
			Annotation[] qualifierAnnot = this.annotations[j];
			if (qualifierAnnot != null && qualifierAnnot.length > 0) {
				scope.problemReporter().misplacedTypeAnnotations(qualifierAnnot[0], qualifierAnnot[qualifierAnnot.length - 1]);
				this.annotations[j] = null;
			}
		}
	}

	protected void rejectAnnotationsOnStaticMemberQualififer(Scope scope, ReferenceBinding currentType, int tokenIndex) {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=385137
		if (this.annotations != null && currentType.isMemberType() && currentType.isStatic()) {
			Annotation[] qualifierAnnot = this.annotations[tokenIndex - 1];
			if (qualifierAnnot != null) {
				scope.problemReporter().illegalTypeAnnotationsInStaticMemberAccess(qualifierAnnot[0],
						qualifierAnnot[qualifierAnnot.length - 1]);
			}
		}
	}

	protected TypeBinding getTypeBinding(Scope scope) {

		if (this.resolvedType != null) {
			return this.resolvedType;
		}
		Binding binding = scope.getPackage(this.tokens);
		if (binding != null && !binding.isValidBinding()) {
			if (binding instanceof ProblemReferenceBinding && binding.problemId() == ProblemReasons.NotFound) {
				ProblemReferenceBinding problemBinding = (ProblemReferenceBinding) binding;
				Binding pkg = scope.getTypeOrPackage(this.tokens);
				return new ProblemReferenceBinding(problemBinding.compoundName, pkg instanceof PackageBinding ? null : scope.environment().createMissingType(null, this.tokens), ProblemReasons.NotFound);
			}
			return (ReferenceBinding) binding; // not found
		}
	    PackageBinding packageBinding = binding == null ? null : (PackageBinding) binding;
	    rejectAnnotationsOnPackageQualifiers(scope, packageBinding);

	    boolean isClassScope = scope.kind == Scope.CLASS_SCOPE;
	    ReferenceBinding qualifiedType = null;
		for (int i = packageBinding == null ? 0 : packageBinding.compoundName.length, max = this.tokens.length, last = max-1; i < max; i++) {
			findNextTypeBinding(i, scope, packageBinding);
			if (!this.resolvedType.isValidBinding())
				return this.resolvedType;
			if (i == 0 && this.resolvedType.isTypeVariable() && ((TypeVariableBinding) this.resolvedType).firstBound == null) { // cannot select from a type variable
				scope.problemReporter().illegalAccessFromTypeVariable((TypeVariableBinding) this.resolvedType, this);
				return null;
			}
			if (i <= last && isTypeUseDeprecated(this.resolvedType, scope)) {
				reportDeprecatedType(this.resolvedType, scope, i);
			}
			if (isClassScope)
				if (((ClassScope) scope).detectHierarchyCycle(this.resolvedType, this)) // must connect hierarchy to find inherited member types
					return null;
			ReferenceBinding currentType = (ReferenceBinding) this.resolvedType;
			if (qualifiedType != null) {
				rejectAnnotationsOnStaticMemberQualififer(scope, currentType, i);
				ReferenceBinding enclosingType = currentType.enclosingType();
				if (enclosingType != null && enclosingType.erasure() != qualifiedType.erasure()) {
					qualifiedType = enclosingType; // inherited member type, leave it associated with its enclosing rather than subtype
				}
				boolean rawQualified;
				if (currentType.isGenericType()) {
					qualifiedType = scope.environment().createRawType(currentType, qualifiedType);
				} else if ((rawQualified = qualifiedType.isRawType()) && !currentType.isStatic()) {
					qualifiedType = scope.environment().createRawType((ReferenceBinding)currentType.erasure(), qualifiedType);
				} else if ((rawQualified || qualifiedType.isParameterizedType()) && qualifiedType.erasure() == currentType.enclosingType().erasure()) {
					qualifiedType = scope.environment().createParameterizedType((ReferenceBinding)currentType.erasure(), null, qualifiedType);
				} else {
					qualifiedType = currentType;
				}
			} else {
				qualifiedType = currentType.isGenericType() ? (ReferenceBinding)scope.environment().convertToRawType(currentType, false /*do not force conversion of enclosing types*/) : currentType;
			}
			recordResolution(scope.environment(), qualifiedType);
		}
		this.resolvedType = qualifiedType;
		return this.resolvedType;
	}

	void recordResolution(LookupEnvironment env, TypeBinding typeFound) {
		if (typeFound != null && typeFound.isValidBinding())
			for (int i = 0; i < env.resolutionListeners.length; i++) {
				env.resolutionListeners[i].recordResolution(this, typeFound);
			}
	}

	public char[][] getTypeName(){

		return this.tokens;
	}

	public StringBuffer printExpression(int indent, StringBuffer output) {
		for (int i = 0; i < this.tokens.length; i++) {
			if (i > 0) output.append('.');
			if (this.annotations != null && this.annotations[i] != null) {
				printAnnotations(this.annotations[i], output);
				output.append(' ');
			}
			output.append(this.tokens[i]);
		}
		return output;
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.annotations != null) {
				int annotationsLevels = this.annotations.length;
				for (int i = 0; i < annotationsLevels; i++) {
					int annotationsLength = this.annotations[i] == null ? 0 : this.annotations[i].length;
					for (int j = 0; j < annotationsLength; j++)
						this.annotations[i][j].traverse(visitor, scope);
				}
			}
		}
		visitor.endVisit(this, scope);
	}

	public void traverse(ASTVisitor visitor, ClassScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.annotations != null) {
				int annotationsLevels = this.annotations.length;
				for (int i = 0; i < annotationsLevels; i++) {
					int annotationsLength = this.annotations[i] == null ? 0 : this.annotations[i].length;
					for (int j = 0; j < annotationsLength; j++)
						this.annotations[i][j].traverse(visitor, scope);
				}
			}
		}
		visitor.endVisit(this, scope);
	}
	public int getAnnotatableLevels() {
		return this.tokens.length;
	}
}
