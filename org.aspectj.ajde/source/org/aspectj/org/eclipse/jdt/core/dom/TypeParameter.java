/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

/**
 * Type parameter node (added in JLS3 API).
 * 
 * <pre>
 * TypeParameter:
 *    { Annotation } TypeVariable [ <b>extends</b> Type { <b>&</b> Type } ]
 * </pre>
 *
 * @since 3.1
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class TypeParameter extends ASTNode {

	/**
	 * The "annotations" structural property of this node type (element type: {@link Annotation}) (added in JLS8 API).
	 * @since 3.9 BETA_JAVA8
	 */
	public static final ChildListPropertyDescriptor ANNOTATIONS_PROPERTY =
			new ChildListPropertyDescriptor(TypeParameter.class, "annotations", Annotation.class, CYCLE_RISK); //$NON-NLS-1$
	
	/**
	 * The "name" structural property of this node type (child type: {@link SimpleName}).
	 */
	public static final ChildPropertyDescriptor NAME_PROPERTY =
		new ChildPropertyDescriptor(TypeParameter.class, "name", SimpleName.class, MANDATORY, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * The "typeBounds" structural property of this node type (element type: {@link Type}).
	 */
	public static final ChildListPropertyDescriptor TYPE_BOUNDS_PROPERTY =
		new ChildListPropertyDescriptor(TypeParameter.class, "typeBounds", Type.class, NO_CYCLE_RISK); //$NON-NLS-1$

	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 */
	private static final List PROPERTY_DESCRIPTORS;
	/**
	 * A list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor}),
	 * or null if uninitialized.
	 * @since 3.9 BETA_JAVA8
	 */
	private static final List PROPERTY_DESCRIPTORS_8_0;

	static {
		List propertyList = new ArrayList(3);
		createPropertyList(TypeParameter.class, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
		addProperty(TYPE_BOUNDS_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS = reapPropertyList(propertyList);
		
		propertyList = new ArrayList(4);
		createPropertyList(TypeParameter.class, propertyList);
		addProperty(ANNOTATIONS_PROPERTY, propertyList);
		addProperty(NAME_PROPERTY, propertyList);
		addProperty(TYPE_BOUNDS_PROPERTY, propertyList);
		PROPERTY_DESCRIPTORS_8_0 = reapPropertyList(propertyList);
	}

	/**
	 * Returns a list of structural property descriptors for this node type.
	 * Clients must not modify the result.
	 *
	 * @param apiLevel the API level; one of the
	 * <code>AST.JLS*</code> constants

	 * @return a list of property descriptors (element type:
	 * {@link StructuralPropertyDescriptor})
	 */
	public static List propertyDescriptors(int apiLevel) {
		switch (apiLevel) {
			case AST.JLS2_INTERNAL :
			case AST.JLS3_INTERNAL :
			case AST.JLS4_INTERNAL:
				return PROPERTY_DESCRIPTORS;
			default :
				return PROPERTY_DESCRIPTORS_8_0;
		}
	}

	/**
	 * The type variable node; lazily initialized; defaults to an unspecified,
	 * but legal, name.
	 */
	private SimpleName typeVariableName = null;

	/**
	 * The type bounds (element type: {@link Type}).
	 * Defaults to an empty list.
	 */
	private ASTNode.NodeList typeBounds =
		new ASTNode.NodeList(TYPE_BOUNDS_PROPERTY);

	/**
	 * The type annotations (element type: {@link Annotation}).
	 * Null in JLS < 8. Added in JLS8; defaults to an empty list
	 * (see constructor).
	 */
	private ASTNode.NodeList annotations = null;
	
	/**
	 * Creates a new unparented node for a parameterized type owned by the
	 * given AST. By default, an unspecified, but legal, type variable name,
	 * and no type bounds.
	 * <p>
	 * N.B. This constructor is package-private.
	 * </p>
	 *
	 * @param ast the AST that is to own this node
	 */
	TypeParameter(AST ast) {
		super(ast);
	    unsupportedIn2();
	    if (ast.apiLevel >= AST.JLS8) {
			this.annotations = new ASTNode.NodeList(ANNOTATIONS_PROPERTY);
		}
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final List internalStructuralPropertiesForType(int apiLevel) {
		return propertyDescriptors(apiLevel);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final ASTNode internalGetSetChildProperty(ChildPropertyDescriptor property, boolean get, ASTNode child) {
		if (property == NAME_PROPERTY) {
			if (get) {
				return getName();
			} else {
				setName((SimpleName) child);
				return null;
			}
		}
		// allow default implementation to flag the error
		return super.internalGetSetChildProperty(property, get, child);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final List internalGetChildListProperty(ChildListPropertyDescriptor property) {
		if (property == ANNOTATIONS_PROPERTY) {
			return annotations();
		}
		if (property == TYPE_BOUNDS_PROPERTY) {
			return typeBounds();
		}
		// allow default implementation to flag the error
		return super.internalGetChildListProperty(property);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final int getNodeType0() {
		return TYPE_PARAMETER;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	ASTNode clone0(AST target) {
		TypeParameter result = new TypeParameter(target);
		result.setSourceRange(getStartPosition(), getLength());
		if (this.ast.apiLevel >= AST.JLS8) {
			result.annotations().addAll(
					ASTNode.copySubtrees(target, annotations()));
		}
		result.setName((SimpleName) ((ASTNode) getName()).clone(target));
		result.typeBounds().addAll(
			ASTNode.copySubtrees(target, typeBounds()));
		return result;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	final boolean subtreeMatch0(ASTMatcher matcher, Object other) {
		// dispatch to correct overloaded match method
		return matcher.match(this, other);
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	void accept0(ASTVisitor visitor) {
		boolean visitChildren = visitor.visit(this);
		if (visitChildren) {
			// visit children in normal left to right reading order
			if (this.ast.apiLevel >= AST.JLS8) {
				acceptChildren(visitor, this.annotations);
			}
			acceptChild(visitor, getName());
			acceptChildren(visitor, this.typeBounds);
		}
		visitor.endVisit(this);
	}

	/**
	 * Returns the name of the type variable declared in this type parameter.
	 *
	 * @return the name of the type variable
	 */
	public SimpleName getName() {
		if (this.typeVariableName == null) {
			// lazy init must be thread-safe for readers
			synchronized (this) {
				if (this.typeVariableName == null) {
					preLazyInit();
					this.typeVariableName = new SimpleName(this.ast);
					postLazyInit(this.typeVariableName, NAME_PROPERTY);
				}
			}
		}
		return this.typeVariableName;
	}

	/**
	 * Resolves and returns the binding for this type parameter.
	 * <p>
	 * Note that bindings are generally unavailable unless requested when the
	 * AST is being built.
	 * </p>
	 *
	 * @return the binding, or <code>null</code> if the binding cannot be
	 *    resolved
	 */
	public final ITypeBinding resolveBinding() {
		return this.ast.getBindingResolver().resolveTypeParameter(this);
	}

	/**
	 * Sets the name of the type variable of this type parameter to the given
	 * name.
	 *
	 * @param typeName the new name of this type parameter
	 * @exception IllegalArgumentException if:
	 * <ul>
	 * <li>the node belongs to a different AST</li>
	 * <li>the node already has a parent</li>
	 * </ul>
	 */
	public void setName(SimpleName typeName) {
		if (typeName == null) {
			throw new IllegalArgumentException();
		}
		ASTNode oldChild = this.typeVariableName;
		preReplaceChild(oldChild, typeName, NAME_PROPERTY);
		this.typeVariableName = typeName;
		postReplaceChild(oldChild, typeName, NAME_PROPERTY);
	}

	/**
	 * Returns the live ordered list of type bounds of this type parameter.
	 * For the type parameter to be plausible, there can be at most one
	 * class in the list, and it must be first, and the remaining ones must be
	 * interfaces; the list should not contain primitive types (but array types
	 * and parameterized types are allowed).
	 *
	 * @return the live list of type bounds
	 *    (element type: {@link Type})
	 */
	public List typeBounds() {
		return this.typeBounds;
	}
	
	/**
	 * Returns the live ordered list of annotations for this TypeParameter node (added in JLS8 API).
	 *
	 * @return the live list of annotations (element type: {@link Annotation})
	 * @exception UnsupportedOperationException if this operation is used
	 *            in a JLS2, JLS3 or JLS4 AST
	 * @since 3.9 BETA_JAVA8
	 */
	public List annotations() {
		// more efficient than just calling unsupportedIn2_3_4() to check
		if (this.annotations == null) {
			unsupportedIn2_3_4();
		}
		return this.annotations;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	int memSize() {
		// treat Code as free
		return BASE_NODE_SIZE + 3 * 4;
	}

	/* (omit javadoc for this method)
	 * Method declared on ASTNode.
	 */
	int treeSize() {
		return
			memSize()
			+ (this.annotations == null ? 0 : this.annotations.listSize())
			+ (this.typeVariableName == null ? 0 : getName().treeSize())
			+ this.typeBounds.listSize();
	}
}

