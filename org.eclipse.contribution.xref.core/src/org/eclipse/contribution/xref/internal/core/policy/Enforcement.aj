/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Helen Hawkins   - iniital version
 *******************************************************************************/
package org.eclipse.contribution.xref.internal.core.policy;


/**
 * An enforcement aspect to ensure there are no printlns and 
 * printStackTraces.
 */
public aspect Enforcement { 

	pointcut printToTheScreen() : (get(* System.out) || get(* System.err));
    
	declare warning : (printToTheScreen() && !within(TracingPolicy)) : "There should be no printlns";
	
	declare warning : call(* Exception.printStackTrace(..)) : 
	    "There should be no calls to printStackTrace";

    
}
