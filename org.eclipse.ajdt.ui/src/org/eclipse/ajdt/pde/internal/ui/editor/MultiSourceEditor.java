/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.ajdt.pde.internal.ui.editor;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ajdt.pde.internal.ui.editor.context.*;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.IFormPage;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public abstract class MultiSourceEditor extends PDEFormEditor {
	protected void addSourcePage(String contextId) {
		InputContext context = inputContextManager.findContext(contextId);
		if (context == null)
			return;
		PDESourcePage sourcePage;
		// Don't duplicate
		if (findPage(contextId)!=null) return;
		sourcePage = createSourcePage(this, contextId, context.getInput().getName(), context.getId());
		sourcePage.setInputContext(context);
		try {
			addPage(sourcePage, context.getInput());
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}
	
	protected void removePage(String pageId) {
		IFormPage page = findPage(pageId);
		if (page == null)
			return;
		if (page.isDirty()) {
			// need to ask the user about this
		} else {
			removePage(page.getIndex());
			if (!page.isEditor())
				page.dispose();
		}
	}
	
	protected PDESourcePage createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		return new GenericSourcePage(editor, title);
	}
}