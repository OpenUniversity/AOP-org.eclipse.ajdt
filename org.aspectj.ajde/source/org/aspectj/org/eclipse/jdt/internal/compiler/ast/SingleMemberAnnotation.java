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
 *******************************************************************************/
package org.aspectj.org.eclipse.jdt.internal.compiler.ast;

import org.aspectj.org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.*;

/**
 * SingleMemberAnnotation node
 */
public class SingleMemberAnnotation extends Annotation {

	public Expression memberValue;
	private MemberValuePair[] singlePairs; // fake pair set, only value has accurate positions

	public SingleMemberAnnotation(TypeReference type, int sourceStart) {
		this.type = type;
		this.sourceStart = sourceStart;
		this.sourceEnd = type.sourceEnd;
	}

	public ElementValuePair[] computeElementValuePairs() {
		return new ElementValuePair[] {memberValuePairs()[0].compilerElementPair};
	}

	/**
	 * @see org.aspectj.org.eclipse.jdt.internal.compiler.ast.Annotation#memberValuePairs()
	 */
	public MemberValuePair[] memberValuePairs() {
		if (this.singlePairs == null) {
			this.singlePairs =
				new MemberValuePair[]{
					new MemberValuePair(VALUE, this.memberValue.sourceStart, this.memberValue.sourceEnd, this.memberValue)
				};
		}
		return this.singlePairs;
	}

	public StringBuffer printExpression(int indent, StringBuffer output) {
		super.printExpression(indent, output);
		output.append('(');
		this.memberValue.printExpression(indent, output);
		return output.append(')');
	}

	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.type != null) {
				this.type.traverse(visitor, scope);
			}
			if (this.memberValue != null) {
				this.memberValue.traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}

	public void traverse(ASTVisitor visitor, ClassScope scope) {
		if (visitor.visit(this, scope)) {
			if (this.type != null) {
				this.type.traverse(visitor, scope);
			}
			if (this.memberValue != null) {
				this.memberValue.traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}
}
