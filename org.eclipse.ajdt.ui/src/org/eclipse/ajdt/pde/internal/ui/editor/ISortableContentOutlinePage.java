/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ajdt.pde.internal.ui.editor;

import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * IConentOutlinePage with externally enabled/disabled element sorting
 */
public interface ISortableContentOutlinePage extends IContentOutlinePage {
	/**
	 * Turns sorting on or off
	 * @param sorting - boolean value indicating if sorting should be enabled
	 */
	public void sort(boolean sorting);
}
