/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.ant;

/**
 * Represents an Ant fileset.
 */
public class FileSet {

	protected String dir; // true
	protected String defaultexcludes;
	protected String includes;
	protected String includesfile;
	protected String excludes;
	protected String excludesfile;
	protected String casesensitive;

	/**
	 * Constructor for the file set.
	 * 
	 * @param dir
	 * @param defaultexcludes
	 * @param includes
	 * @param includesfile
	 * @param excludes
	 * @param excludesfile
	 * @param casesensitive
	 */
	public FileSet(String dir, String defaultexcludes, String includes, String includesfile, String excludes, String excludesfile, String casesensitive) {
		this.dir = dir;
		this.defaultexcludes = defaultexcludes;
		this.includes = includes;
		this.includesfile = includesfile;
		this.excludes = excludes;
		this.excludesfile = excludesfile;
		this.casesensitive = casesensitive;
	}

	/**
	 * Print this fileset to the given Ant script.
	 * 
	 * @param script the script to output to
	 */
	protected void print(AntScript script) {
		script.printTab();
		script.print("<fileset"); //$NON-NLS-1$
		script.printAttribute("dir", dir, true); //$NON-NLS-1$
		script.printAttribute("defaultexcludes", defaultexcludes, false); //$NON-NLS-1$
		script.printAttribute("includes", includes, false); //$NON-NLS-1$
		script.printAttribute("includesfile", includesfile, false); //$NON-NLS-1$
		script.printAttribute("excludes", excludes, false); //$NON-NLS-1$
		script.printAttribute("excludesfile", excludesfile, false); //$NON-NLS-1$
		script.printAttribute("casesensitive", casesensitive, false); //$NON-NLS-1$
		script.println("/>"); //$NON-NLS-1$
	}
}
