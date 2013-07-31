/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.aspectj.org.eclipse.jdt.core.dom;

import java.util.List;

/**
 * Abstract base class of AST nodes that represent an annotatable type (added in JLS8 API).
 * <p>
 * Introduced in JLS8, type references that can be annotated are represented by 
 * AnnotatableType. For the list of types extending AnnotatableType, see {@link Type}.</p>
 *
 * @since 3.9 BETA_JAVA8
 */
public abstract class AnnotatableType extends Type {
	
	/**
	 * The annotations (element type: {@link Annotation}).
	 * Null in JLS < 8. Added in JLS8; defaults to an empty list
	 * (see constructor).
	 */
	ASTNode.NodeList annotations = null;

	/**
	 * Creates and returns a structural property descriptor for the
	 * "annotations" property declared on the given concrete node type (element type: {@link Annotation}) (added in JLS8 API).
	 *
	 * @return the property descriptor
	 */
	static final ChildListPropertyDescriptor internalAnnotationsPropertyFactory(Class nodeClass) {
		return 	new ChildListPropertyDescriptor(nodeClass, "annotations", Annotation.class, CYCLE_RISK); //$NON-NLS-1$
	}

	/**
	 * Returns the structural property descriptor for the "annotations" property
	 * of this node (element type: {@link Annotation}) (added in JLS8 API).
	 *
	 * @return the property descriptor
	 */
	abstract ChildListPropertyDescriptor internalAnnotationsProperty();

	/**
	 * Returns the structural property descriptor for the "annotations" property
	 * of this node (element type: {@link Annotation}) (added in JLS8 API).
	 *
	 * @return the property descriptor
	 */
	public final ChildListPropertyDescriptor getAnnotationsProperty() {
		return internalAnnotationsProperty();
	}

	/**
	 * Creates a new unparented node for an annotatable type owned by the given AST.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	AnnotatableType(AST ast) {
		super(ast);
		if (ast.apiLevel >= AST.JLS8) {
			this.annotations = new ASTNode.NodeList(getAnnotationsProperty());
		}
	}

	/**
	 * Returns the live ordered list of annotations for this Type node (added in JLS8 API).
	 *
	 * @return the live list of annotations (element type: {@link Annotation})
	 * @exception UnsupportedOperationException if this operation is used below JLS8
	 */
	public List annotations() {
		// more efficient than just calling unsupportedIn2_3_4() to check
		if (this.annotations == null) {
			unsupportedIn2_3_4();
		}
		return this.annotations;
	}
}
