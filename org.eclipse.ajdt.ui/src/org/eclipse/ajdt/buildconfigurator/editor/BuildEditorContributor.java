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
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.dnd.Clipboard;

public class BuildEditorContributor extends PDEFormEditorContributor {

	public BuildEditorContributor() {
		super("&Build"); //$NON-NLS-1$
	}
	protected boolean hasKnownTypes(Clipboard clipboard) {
		return true;
	}
}