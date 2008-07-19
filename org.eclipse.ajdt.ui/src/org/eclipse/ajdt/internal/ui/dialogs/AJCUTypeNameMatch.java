/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matt Chapman - initial version
 *******************************************************************************/
 package org.eclipse.ajdt.internal.ui.dialogs;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.core.search.JavaSearchTypeNameMatch;

public class AJCUTypeNameMatch extends JavaSearchTypeNameMatch {

	public AJCUTypeNameMatch(IType type, int modifiers) {
		super(type, modifiers);
	}

}
