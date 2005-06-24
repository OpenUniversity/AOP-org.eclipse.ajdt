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
package org.eclipse.ajdt.pde.internal.ui.editor;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * @version 	1.0
 * @author
 */
public abstract class StructuredViewerSection extends PDESection {
	protected StructuredViewerPart viewerPart;
	/**
	 * Constructor for StructuredViewerSection.
	 * @param formPage
	 */
	public StructuredViewerSection(PDEFormPage formPage, Composite parent, int style, String [] buttonLabels) {
		super(formPage, parent, style);
		viewerPart = createViewerPart(buttonLabels);
		viewerPart.setMinimumSize(50, 50);
		FormToolkit toolkit = formPage.getManagedForm().getToolkit();
		createClient(getSection(), toolkit);
	}

	protected void createViewerPartControl(Composite parent, int style, int span, FormToolkit toolkit) {
		viewerPart.createControl(parent, style, span, toolkit);
		MenuManager popupMenuManager = new MenuManager();
		popupMenuManager.setRemoveAllWhenShown(true);
		Control control = viewerPart.getControl();
		Menu menu = popupMenuManager.createContextMenu(control);
		control.setMenu(menu);
	}
	
	protected Composite createClientContainer(Composite parent, int span, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 2;
		layout.numColumns = span;
		container.setLayout(layout);
		return container;
	}
	
	protected abstract StructuredViewerPart createViewerPart(String [] buttonLabels);
	

	protected void doPaste() {
	}

	public boolean canPaste(Clipboard clipboard) {
		return false;
	}
	protected ISelection getViewerSelection() {
		return viewerPart.getViewer().getSelection();
	}

	public void setFocus() {
		viewerPart.getControl().setFocus();
	}
}
