/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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

import java.util.List;

import org.aspectj.org.eclipse.jdt.internal.compiler.ast.UnionTypeReference;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class MultiCatchExceptionLabel extends ExceptionLabel {

	ExceptionLabel[] exceptionLabels;

	public MultiCatchExceptionLabel(CodeStream codeStream, TypeBinding exceptionType) {
		super(codeStream, exceptionType);
	}
	
	public void initialize(UnionTypeReference typeReference) {
		TypeReference[] typeReferences = typeReference.typeReferences;
		int length = typeReferences.length;
		this.exceptionLabels = new ExceptionLabel[length];
		for (int i = 0; i < length; i++) {
			this.exceptionLabels[i] = new ExceptionLabel(this.codeStream, typeReferences[i].resolvedType, typeReferences[i]);
		}
	}
	public void place() {
		for (int i = 0, max = this.exceptionLabels.length; i < max; i++) {
			this.exceptionLabels[i].place();
		}
	}
	public void placeEnd() {
		for (int i = 0, max = this.exceptionLabels.length; i < max; i++) {
			this.exceptionLabels[i].placeEnd();
		}
	}
	public void placeStart() {
		for (int i = 0, max = this.exceptionLabels.length; i < max; i++) {
			this.exceptionLabels[i].placeStart();
		}
	}
	public int getCount() {
		int temp = 0;
		for (int i = 0, max = this.exceptionLabels.length; i < max; i++) {
			temp += this.exceptionLabels[i].getCount();
		}
		return temp;
	}

	public int getAllAnnotationContexts(int tableIndex, List allTypeAnnotationContexts) {
		int localCount = 0;
		for (int i = 0, max = this.exceptionLabels.length; i < max; i++) {
			ExceptionLabel exceptionLabel = this.exceptionLabels[i];
			if (exceptionLabel.exceptionTypeReference != null) { // ignore those which cannot be annotated
				exceptionLabel.exceptionTypeReference.getAllAnnotationContexts(AnnotationTargetTypeConstants.EXCEPTION_PARAMETER, tableIndex + localCount, allTypeAnnotationContexts);
			}
			tableIndex++;
		}
		return localCount;
	}
}
