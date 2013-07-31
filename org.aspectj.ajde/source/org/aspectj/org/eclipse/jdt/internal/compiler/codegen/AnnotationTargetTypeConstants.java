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

public interface AnnotationTargetTypeConstants {

	// Targets for type parameter declarations
	int CLASS_TYPE_PARAMETER                 = 0x00;
	int METHOD_TYPE_PARAMETER                = 0x01;

	// Targets that may be externally visible in classes and members
	int CLASS_EXTENDS                        = 0x10;
	int CLASS_TYPE_PARAMETER_BOUND           = 0x11;
	int METHOD_TYPE_PARAMETER_BOUND          = 0x12;
	int FIELD                                = 0x13;
	int METHOD_RETURN                        = 0x14;
	int METHOD_RECEIVER                      = 0x15;
	int METHOD_FORMAL_PARAMETER              = 0x16;
	int THROWS                               = 0x17;

	// Targets for type uses that occur only within code blocks
	int LOCAL_VARIABLE                       = 0x40;
	int RESOURCE_VARIABLE                    = 0x41;
	int EXCEPTION_PARAMETER                  = 0x42;
	int INSTANCEOF                           = 0x43;
	int NEW                                  = 0x44;
	int CONSTRUCTOR_REFERENCE                = 0x45;
	int METHOD_REFERENCE                     = 0x46;
	int CAST                                 = 0x47;
	int CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT = 0x48;
	int METHOD_INVOCATION_TYPE_ARGUMENT      = 0x49;
	int CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT  = 0x4A;
	int METHOD_REFERENCE_TYPE_ARGUMENT       = 0x4B;

}
