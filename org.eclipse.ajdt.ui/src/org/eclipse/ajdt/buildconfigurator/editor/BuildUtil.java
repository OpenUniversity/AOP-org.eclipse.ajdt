/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ajdt.buildconfigurator.editor;

import java.util.Vector;

import org.eclipse.ajdt.buildconfigurator.editor.model.IBuildEntry;

public class BuildUtil {

	public static IBuildEntry[] getBuildLibraries(IBuildEntry[] entries) {
		Vector temp = new Vector();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].getName().startsWith(IBuildEntry.JAR_PREFIX))
				temp.add(entries[i]);
		}
		IBuildEntry[] result = new IBuildEntry[temp.size()];
		temp.copyInto(result);
		return result;
	}

}
