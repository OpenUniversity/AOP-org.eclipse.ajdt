/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ajdt.internal.core.exports;

import java.io.File;

import org.eclipse.pde.internal.core.exports.FeatureExportInfo;


public class PluginExportOperation extends FeatureBasedExportOperation {

	public PluginExportOperation(FeatureExportInfo info) {
		super(info);
	}
	
	protected void createPostProcessingFiles() {
		createPostProcessingFile(new File(fFeatureLocation, PLUGIN_POST_PROCESSING));		
	}

}
