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
import java.util.*;

import org.eclipse.jface.action.IAction;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.forms.editor.IFormPage;

/**
 * @version 	1.0
 * @author
 */
public abstract class ModelUndoManager
	implements IModelUndoManager, IModelChangedListener {
	private static final String KEY_NO_UNDO = "UpdateManager.noUndo"; //$NON-NLS-1$
	private static final String KEY_NO_REDO = "UpdateManager.noRedo"; //$NON-NLS-1$
	private static final String KEY_UNDO = "UpdateManager.undo"; //$NON-NLS-1$
	private static final String KEY_REDO = "UpdateManager.redo"; //$NON-NLS-1$
	private static final String KEY_OP_ADD = "UpdateManager.op.add"; //$NON-NLS-1$
	private static final String KEY_OP_REMOVE = "UpdateManager.op.remove"; //$NON-NLS-1$
	private static final String KEY_OP_CHANGE = "UpdateManager.op.change"; //$NON-NLS-1$
	private boolean ignoreChanges;
	private List operations;
	private int undoLevelLimit = 10;
	private int cursor = -1;
	private IAction undoAction;
	private IAction redoAction;
	private PDEFormEditor editor;
	
	public ModelUndoManager(PDEFormEditor editor) {
		this.editor = editor;
		operations = new Vector();
	}

	/*
	 * @see IModelUndoManager#connect(IModelChangeProvider)
	 */
	public void connect(IModelChangeProvider provider) {
		provider.addModelChangedListener(this);
		if (operations==null) initialize();
	}

	/*
	 * @see IModelUndoManager#disconnect(IModelChangeProvider)
	 */
	public void disconnect(IModelChangeProvider provider) {
		provider.removeModelChangedListener(this);
	}

	private void initialize() {
		operations = new Vector();
		cursor = -1;
		updateActions();
	}

	/*
	 * @see IModelUndoManager#isUndoable()
	 */
	public boolean isUndoable() {
		return cursor>=0;
	}

	/*
	 * @see IModelUndoManager#isRedoable()
	 */
	public boolean isRedoable() {
		if (operations == null) initialize();
		return (cursor+1)<operations.size();
	}

	/*
	 * @see IModelUndoManager#undo()
	 */
	public void undo() {
		IModelChangedEvent op = getCurrentOperation();
		if (op==null) return;
		ignoreChanges = true;
		openRelatedPage(op);
		execute(op, true);
		cursor --;
		updateActions();
		ignoreChanges = false;
	}

	/*
	 * @see IModelUndoManager#redo()
	 */
	public void redo() {
		cursor ++;
		IModelChangedEvent op = getCurrentOperation();
		if (op==null) return;
		ignoreChanges = true;
		openRelatedPage(op);
		execute(op, false);
		ignoreChanges = false;
		updateActions();
	}
	
	protected abstract String getPageId(Object object);
	
	protected abstract void execute(IModelChangedEvent op, boolean undo);
	
	private void openRelatedPage(IModelChangedEvent op) {
		Object obj = op.getChangedObjects()[0];
		String pageId = getPageId(obj);
		if (pageId!=null) {
			IFormPage cpage = editor.getActivePageInstance();
			IFormPage newPage = editor.findPage(pageId);
			if (cpage != newPage)
				editor.setActivePage(newPage.getId());
		}
	}

	/*
	 * @see IModelChangedListener#modelChanged(IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		if (ignoreChanges)
			return;
			
		if (event.getChangeType()==IModelChangedEvent.WORLD_CHANGED) {
			initialize();
			return;
		}
		addOperation(event);
	}
	
	private IModelChangedEvent getCurrentOperation() {
		if (cursor == -1 || cursor == operations.size()) return null;
		return (IModelChangedEvent)operations.get(cursor);
	}
	
	private IModelChangedEvent getNextOperation() {
		int peekCursor = cursor+1;
		if (peekCursor >= operations.size()) return null;
		return (IModelChangedEvent)operations.get(peekCursor);
	}
	
	private void addOperation(IModelChangedEvent operation) {
		operations.add(operation);
		int size = operations.size();
		if (size > undoLevelLimit) {
			int extra = size-undoLevelLimit;
			// trim
			for (int i=0; i<extra; i++) {
				operations.remove(i);
			}
		}
		cursor = operations.size() -1;
		updateActions();
	}
	
	public void setActions(IAction undoAction, IAction redoAction) {
		this.undoAction = undoAction;
		this.redoAction = redoAction;
		updateActions();
	}
	
	private void updateActions() {
		if (undoAction!=null && redoAction!=null) {
			undoAction.setEnabled(isUndoable());
			undoAction.setText(getUndoText());
			redoAction.setEnabled(isRedoable());
			redoAction.setText(getRedoText());
		}
	}
	
	private String getUndoText() {
		IModelChangedEvent op = getCurrentOperation();
		if (op == null) {
			return PDEPlugin.getResourceString(KEY_NO_UNDO);
		}
		return PDEPlugin.getFormattedMessage(KEY_UNDO, getOperationText(op));	
	}

	private String getRedoText() {
		IModelChangedEvent op = getNextOperation();
		if (op == null) {
			return PDEPlugin.getResourceString(KEY_NO_REDO);
		}
		return PDEPlugin.getFormattedMessage(KEY_REDO, getOperationText(op));
	}
		
	private String getOperationText(IModelChangedEvent op) {
		String opText=""; //$NON-NLS-1$
		switch (op.getChangeType()) {
			case IModelChangedEvent.INSERT:
			opText = PDEPlugin.getResourceString(KEY_OP_ADD);
			break;
			case IModelChangedEvent.REMOVE:
			opText = PDEPlugin.getResourceString(KEY_OP_REMOVE);
			break;
			case IModelChangedEvent.CHANGE:
			opText = PDEPlugin.getResourceString(KEY_OP_CHANGE);
			break;
		}
		return opText;
	}
	
	public void setUndoLevelLimit(int limit) {
		this.undoLevelLimit = limit;
	}

	public void setIgnoreChanges(boolean ignore) {
		this.ignoreChanges = ignore;
	}
}
