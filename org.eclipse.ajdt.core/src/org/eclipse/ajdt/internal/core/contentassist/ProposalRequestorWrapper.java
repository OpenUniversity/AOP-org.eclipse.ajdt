/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Luzius Meisser - initial implementation
 *     Sian January - changed for Eclipse 3.2
 *******************************************************************************/
package org.eclipse.ajdt.internal.core.contentassist;

import java.util.ArrayList;

import org.aspectj.asm.IProgramElement.Accessibility;
import org.eclipse.ajdt.core.codeconversion.AspectsConvertingParser;
import org.eclipse.ajdt.core.codeconversion.JavaCompatibleBuffer;
import org.eclipse.ajdt.core.model.AJWorldFacade;
import org.eclipse.ajdt.core.model.AJWorldFacade.ITDInfo;
import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.compiler.IProblem;

/**
 * Translates code positions from fakeBuffer into realBuffer before
 * passing them on to the wrapped ICompletionRequestor.
 * 
 * A description of how code completion works in AJDT can be found in bug 74419.
 * 
 * @author Luzius Meisser
 */
public class ProposalRequestorWrapper extends CompletionRequestor {

	CompletionRequestor wrapped;
	ArrayList insertionTable;
	
	private AJWorldFacade world;
	private ICompilationUnit unit;

	/**
	 * @param wrapped
	 * @param buffer
	 */
    public ProposalRequestorWrapper(CompletionRequestor wrapped,
            ICompilationUnit unit,
            JavaCompatibleBuffer buffer) {
        super();
        this.wrapped = wrapped;
        this.unit = unit;
        this.insertionTable = buffer.getInsertionTable();
    }
    public ProposalRequestorWrapper(CompletionRequestor wrapped,
            ICompilationUnit unit,
            ArrayList insertionTable) {
        super();
        this.wrapped = wrapped;
        this.unit = unit;
        this.insertionTable = insertionTable;
    }
	
	public void accept(CompletionProposal proposal) {
	    
	    if (!shouldAccept(proposal)) {
	        return;
	    }
	    
		int s = proposal.getReplaceStart();
		int e = proposal.getReplaceEnd();
		proposal.setReplaceRange(trans(s), trans(e));
		wrapped.accept(proposal);
	}
	
	protected boolean shouldAccept(CompletionProposal proposal) {
	    if (proposal.getKind() == CompletionProposal.FIELD_REF ||
	            proposal.getKind() == CompletionProposal.METHOD_REF) {
	     
	        if (world == null) {
	            world = new AJWorldFacade(unit.getJavaProject().getProject());
	        }
	        
	        ITDInfo info = world.findITDInfoIfExists(proposal.getDeclarationSignature(), proposal.getName());
	        if (info != null) {
    	        if (info.accessibility == Accessibility.PUBLIC) {
    	            // accessible everywhere
    	            return true;
    	        } else if (info.accessibility == Accessibility.PACKAGE) {
    	            // accessible only in package of declaring aspect
    	            if (((IPackageFragment) unit.getParent()).getElementName().equals(info.packageDeclaredIn)) {
    	                int oldFlags = proposal.getFlags();
    	                oldFlags |= Flags.AccDefault;
    	                oldFlags &= ~Flags.AccPublic;
    	                proposal.setFlags(oldFlags);
    	                return true;
    	            }
    	        } else if (info.accessibility == Accessibility.PRIVATE) {
    	            // accessible only in declaring aspect's compilation unit
    	            if (unit.getElementName().startsWith(info.topLevelAspectName + ".") &&
    	               ((IPackageFragment) unit.getParent()).getElementName().equals(info.packageDeclaredIn)) {

    	                int oldFlags = proposal.getFlags();
                        oldFlags |= Flags.AccPrivate;
                        oldFlags &= ~Flags.AccPublic;
                        proposal.setFlags(oldFlags);
    	                    return true;
    	            }
    
    	        }
    	        return false;
	        } else {
	            // not an ITD
	            return true;
	        }
	    } else {
	        return true;
	    }
    }
	
	
    private int trans(int pos){
		return AspectsConvertingParser.translatePositionToBeforeChanges(pos, insertionTable);
	}
	
	public void acceptContext(CompletionContext context) {
		wrapped.acceptContext(context);
	}
	
	public void endReporting() {
		wrapped.endReporting();
	}
	
	public void beginReporting() {
		wrapped.beginReporting();
	}
	
	public void completionFailure(IProblem problem) {
		wrapped.completionFailure(problem);
	}

	public boolean equals(Object obj) {
		return wrapped.equals(obj);
	}
	public int hashCode() {
		return wrapped.hashCode();
	}
	public String toString() {
		return wrapped.toString();
	}
	
	

}
