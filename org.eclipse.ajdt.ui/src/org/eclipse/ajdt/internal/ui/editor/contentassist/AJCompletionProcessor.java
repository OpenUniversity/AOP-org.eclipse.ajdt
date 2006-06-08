/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Sian January - initial implementation
 *******************************************************************************/
package org.eclipse.ajdt.internal.ui.editor.contentassist;

import java.util.Iterator;
import java.util.List;

import org.eclipse.ajdt.internal.ui.text.UIMessages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProcessor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.IEditorPart;

/**
 * Code completion processor for the AspectJ editor
 */
public class AJCompletionProcessor extends JavaCompletionProcessor {
	
	protected static final String intertypeMemberTag = "ajc$";  //$NON-NLS-1$
	private int offset;
	
	public AJCompletionProcessor(IEditorPart editor, ContentAssistant assistant, String partition) {
		super(editor, assistant, partition);
	}
	
	// Filter out proposals starting with "ajc$" and add a "limited AspectJ Support" message
	protected List filterAndSortProposals(List proposals, IProgressMonitor monitor, ContentAssistInvocationContext context) {
		List newProposals = super.filterAndSortProposals(proposals, monitor, context);
		for (Iterator iter = newProposals.iterator(); iter.hasNext();) {
			ICompletionProposal proposal = (ICompletionProposal) iter.next();			
			if (proposal.getDisplayString().startsWith(intertypeMemberTag)) {
				iter.remove();
			}
		}
		if (newProposals.size() > 0) {
			// only add limited message if there are any proposals
			newProposals.add(newProposals.size(), 
				new CompletionProposal("", offset, 0, 0, null, UIMessages.codeAssist_limited_title, null, UIMessages.codeAssist_limited_message));//$NON-NLS-1$ )
		}
		return newProposals;
	}
	
	protected ContentAssistInvocationContext createContext(ITextViewer viewer, int offset) {
		this.offset = offset;
		return new AJContentAssistInvocationContext(viewer, offset, fEditor);
	}
	
	
	private class AJContentAssistInvocationContext extends JavaContentAssistInvocationContext {
		
		private IEditorPart fEditor;
		
		/**
		 * Creates a new context.
		 * 
		 * @param viewer the viewer used by the editor
		 * @param offset the invocation offset
		 * @param editor the editor that content assist is invoked in
		 */
		public AJContentAssistInvocationContext(ITextViewer viewer, int offset, IEditorPart editor) {
			super(viewer, offset, editor);
			fEditor= editor;
		}
		
		/**
		 * Returns the compilation unit that content assist is invoked in, <code>null</code> if there
		 * is none.
		 * 
		 * @return the compilation unit that content assist is invoked in, possibly <code>null</code>
		 */
		public ICompilationUnit getCompilationUnit() {
			return JavaUI.getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
		}

	}

}
