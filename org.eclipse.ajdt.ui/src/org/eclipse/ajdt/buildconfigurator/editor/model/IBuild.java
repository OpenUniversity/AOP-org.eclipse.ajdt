/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ajdt.buildconfigurator.editor.model;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
/**
 * The top-level model object of the model that is created from
 * "build.properties" file.
 *  
 */
public interface IBuild extends IWritable {
	/**
	 * Adds a new build entry. This method can throw a CoreException if the
	 * model is not editable.
	 * 
	 * @param entry
	 *            an entry to be added
	 */
	void add(IBuildEntry entry) throws CoreException;
	
	/**
	 * Adds a new build entry.
	 * 
	 * @param entry
	 *            an entry to be added
	 */
	void addWithoutNotify(IBuildEntry entry);
	
	/**
	 * Returns all the build entries in this object.
	 * 
	 * @return an array of build entries
	 */
	IBuildEntry[] getBuildEntries();
	/**
	 * Returns the build entry with the specified name.
	 * 
	 * @param the
	 *            name of the desired entry
	 * @return the entry object with the specified name, or <samp>null </samp>
	 *         if not found.
	 */
	IBuildEntry getEntry(String name);
	/**
	 * Removes a build entry. This method can throw a CoreException if the model
	 * is not editable.
	 * 
	 * @param entry
	 *            an entry to be removed
	 */
	void remove(IBuildEntry entry) throws CoreException;
}