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
 * Jar entry represents one 'library=folder list' entry
 * in plugin.jars file.
 */
public interface IBuildEntry extends IWritable {
	/**
	 * A property name for changes to the 'name' field.
	 */
	public static final String P_NAME = "name"; //$NON-NLS-1$
	/**
	 * A property name for changes to the '' field.
	 */
	public static final String JAR_PREFIX = "source."; //$NON-NLS-1$
	/**
	 * A property name for changes to the 'name' field.
	 */	
	public static final String OUTPUT_PREFIX = "output."; //$NON-NLS-1$
	/**
	 * A property name for changes to the 'name' field.
	 */	
	public static final String BIN_INCLUDES = "bin.includes"; //$NON-NLS-1$
	/**
	 * A property name for changes to the 'name' field.
	 */	
	public static final String SRC_INCLUDES = "src.includes"; //$NON-NLS-1$
	/**
	 * A property name for changes to the 'name' field.
	 */	
	public static final String JARS_EXTRA_CLASSPATH = "jars.extra.classpath"; //$NON-NLS-1$
	/**
	 * Adds the token to the list of token for this entry.
	 * This method will throw a CoreException if
	 * the model is not editable.
	 *
	 * @param token a name to be added to the list of tokens
	 */
	void addToken(String token) throws CoreException;
	
	/**
	 * Adds the token to the list of token for this entry.
	 *
	 * @param token a name to be added to the list of tokens
	 */
	void addTokenWithoutNotify(String token);

	/**
	 * Returns a model that owns this entry
	 * @return build.properties model
	 */
	IBuildModel getModel();
	/**
	 * Returns the name of this entry.
	 * @return the entry name
	 */
	String getName();
	/**
	 * Returns an array of tokens for this entry
	 * @return array of tokens
	 */
	String[] getTokens();

	/**
	 * Returns true if the provided token exists in this entry.
	 * @return true if the token exists in the entry
	 */
	boolean contains(String token);
	/**
	 * Removes the token from the list of tokens for this entry.
	 * This method will throw a CoreException if
	 * the model is not editable.
	 *
	 * @param token a name to be removed from the list of tokens
	 */
	void removeToken(String token) throws CoreException;
	
	/**
	 * Removes the token from the list of tokens for this entry.
	 *
	 * @param token a name to be removed from the list of tokens
	 */
	void removeTokenWithoutNotify(String token);
	
	/**
	 * Changes the name of the token without changing its
	 * position in the list. This method will throw
	 * a CoreException if the model is not editable.
	 *
	 * @param oldToken the old token name
	 * @param newToken the new token name
	 */
	void renameToken(String oldToken, String newToken) throws CoreException;
	/**
	 * Sets the name of this build entry. This
	 * method will throw a CoreException if
	 * model is not editable.
	 *
	 * @param name the new name for the entry
	 */
	void setName(String name) throws CoreException;
}
