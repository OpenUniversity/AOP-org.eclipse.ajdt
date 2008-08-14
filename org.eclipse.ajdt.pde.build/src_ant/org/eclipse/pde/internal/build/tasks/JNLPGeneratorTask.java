/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class JNLPGeneratorTask extends Task {

	private String feature;
	private String jnlp = null;
	private String codebase = null;
	private String j2se;

	/**
	 * The URL location of a feature.xml file.  This can be either a jar: URL to the feature.xml,
	 * or a file: url to the feature.
	 * @param value
	 */
	public void setFeature(String value) {
		feature = value;
	}

	/**
	 * The location the output jnlp file.  The value may be null (or simply not set).  If it is
	 * null then the output file will be placed beside the feature dir or jar (as appropriate)
	 * and its name will be derived from the feature id and version.  If this is set
	 * it must be a file path.  If it ends in a "/" or "\" then the output file is places in this 
	 * directory with the file name derived as described.  Finally, if the value is a file name, 
	 * the output is placed in that file.
	 * @param value the location to write the output or null if the default 
	 * computed location is to be used.
	 */
	public void setJNLP(String value) {
		jnlp = value;
	}

	/**
	 * The codebase of the output.  This shoudl be a URL that will be used as the
	 * root of all relative URLs in the output.  Typically this is the root of the application 
	 * deployment website.
	 * @param value the URL root of for the application content.
	 */
	public void setCodebase(String value) {
		codebase = value;
	}

	/**
	 * The j2se spec of the output
	 * @param value the JNLP j2se spec to use in the output.
	 */
	public void setJ2SE(String value) {
		j2se = value;
	}

	public void execute() throws BuildException {
		JNLPGenerator generator = new JNLPGenerator(feature, jnlp, codebase, j2se);
		generator.process();
	}
}
