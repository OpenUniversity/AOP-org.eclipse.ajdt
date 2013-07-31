/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/
package org.aspectj.org.eclipse.jdt.internal.compiler.codegen;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.org.eclipse.jdt.internal.compiler.ClassFile;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.aspectj.org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class TypeAnnotationCodeStream extends StackMapFrameCodeStream {
	public List allTypeAnnotationContexts;

	public TypeAnnotationCodeStream(ClassFile givenClassFile) {
		super(givenClassFile);
		this.generateAttributes |= ClassFileConstants.ATTR_TYPE_ANNOTATION;
		this.allTypeAnnotationContexts = new ArrayList();
	}
	
	private void addAnnotationContext(TypeReference typeReference, int info, int targetType, Annotation[][] annotationsOnDimensions, int dimensions) {
		typeReference.getAllAnnotationContexts(targetType, info, this.allTypeAnnotationContexts, annotationsOnDimensions, dimensions);
	}
	
	private void addAnnotationContext(TypeReference typeReference, int info, int targetType) {
		typeReference.getAllAnnotationContexts(targetType, info, this.allTypeAnnotationContexts);
	}

	private void addAnnotationContext(TypeReference typeReference, int info, int typeIndex, int targetType) {
		typeReference.getAllAnnotationContexts(targetType, info, typeIndex, this.allTypeAnnotationContexts);
	}
	
	public void instance_of(TypeReference typeReference, TypeBinding typeBinding) {
		if (typeReference != null && (typeReference.bits & ASTNode.HasTypeAnnotations) != 0) {
			addAnnotationContext(typeReference, this.position, AnnotationTargetTypeConstants.INSTANCEOF);
		}
		super.instance_of(typeReference, typeBinding);
	}
	
	public void multianewarray(
			TypeReference typeReference,
			TypeBinding typeBinding,
			int dimensions,
			Annotation [][] annotationsOnDimensions) {
		if (typeReference != null && (typeReference.bits & ASTNode.HasTypeAnnotations) != 0) {
			addAnnotationContext(typeReference, this.position, AnnotationTargetTypeConstants.NEW, annotationsOnDimensions, dimensions);
		}
		super.multianewarray(typeReference, typeBinding, dimensions, annotationsOnDimensions);
	}

	public void new_(TypeReference typeReference, TypeBinding typeBinding) {
		if (typeReference != null && (typeReference.bits & ASTNode.HasTypeAnnotations) != 0) {
			addAnnotationContext(typeReference, this.position, AnnotationTargetTypeConstants.NEW);
		}
		super.new_(typeReference, typeBinding);
	}
	
	public void newArray(TypeReference typeReference, Annotation[][] annotationsOnDimensions, ArrayBinding arrayBinding) {
		if (typeReference != null && (typeReference.bits & ASTNode.HasTypeAnnotations) != 0) {
			addAnnotationContext(typeReference, this.position, AnnotationTargetTypeConstants.NEW, annotationsOnDimensions, 1);
		}
		super.newArray(typeReference, annotationsOnDimensions, arrayBinding);
	}
	
	public void checkcast(TypeReference typeReference, TypeBinding typeBinding) {
		if (typeReference != null && (typeReference.bits & ASTNode.HasTypeAnnotations) != 0) {
			addAnnotationContext(typeReference, this.position, AnnotationTargetTypeConstants.CAST);
		}
		super.checkcast(typeReference, typeBinding);
	}
	
	public void invoke(byte opcode, MethodBinding methodBinding, TypeBinding declaringClass, TypeReference[] typeArguments) {
		if (typeArguments != null) {
			int targetType = methodBinding.isConstructor()
					? AnnotationTargetTypeConstants.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
					: AnnotationTargetTypeConstants.METHOD_INVOCATION_TYPE_ARGUMENT;
			for (int i = 0, max = typeArguments.length; i < max; i++) {
				TypeReference typeArgument = typeArguments[i];
				if ((typeArgument.bits & ASTNode.HasTypeAnnotations) != 0) { // TODO can check this at a higher level?
					addAnnotationContext(typeArgument, this.position, i, targetType);
				}
			}
		}
		super.invoke(opcode, methodBinding, declaringClass, typeArguments);
	}
	
	public void invokeDynamic(int bootStrapIndex, int argsSize, int returnTypeSize, char[] selector, char[] signature, 
			boolean isConstructorReference, TypeReference lhsTypeReference, TypeReference [] typeArguments) {
		if (lhsTypeReference != null && (lhsTypeReference.bits & ASTNode.HasTypeAnnotations) != 0) {
			if (isConstructorReference) {
				addAnnotationContext(lhsTypeReference, this.position, 0, AnnotationTargetTypeConstants.CONSTRUCTOR_REFERENCE);
			} else {
				addAnnotationContext(lhsTypeReference, this.position, 0, AnnotationTargetTypeConstants.METHOD_REFERENCE);
			}
		}
		if (typeArguments != null) {
			int targetType = 
					isConstructorReference
					? AnnotationTargetTypeConstants.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
					: AnnotationTargetTypeConstants.METHOD_REFERENCE_TYPE_ARGUMENT;
			for (int i = 0, max = typeArguments.length; i < max; i++) {
				TypeReference typeArgument = typeArguments[i];
				if ((typeArgument.bits & ASTNode.HasTypeAnnotations) != 0) {
					addAnnotationContext(typeArgument, this.position, i, targetType);
				}
			}
		}
		super.invokeDynamic(bootStrapIndex, argsSize, returnTypeSize, selector, signature, isConstructorReference, lhsTypeReference, typeArguments);
	}

	public void reset(ClassFile givenClassFile) {
		super.reset(givenClassFile);
		this.allTypeAnnotationContexts = new ArrayList();
	}
	
	public void init(ClassFile targetClassFile) {
		super.init(targetClassFile);
		this.allTypeAnnotationContexts = new ArrayList();
	}
}
