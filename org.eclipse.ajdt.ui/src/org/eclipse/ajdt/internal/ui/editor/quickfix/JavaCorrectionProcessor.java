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

package org.eclipse.ajdt.internal.ui.editor.quickfix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerHelpRegistry;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.ChangeCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.ContributedProcessorDescriptor;
import org.eclipse.jdt.internal.ui.text.correction.MarkerResolutionProposal;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * Copied from org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor
 * Any changes marked with // AspectJ Change
 */
public class JavaCorrectionProcessor implements IContentAssistProcessor {

	private static final String QUICKFIX_PROCESSOR_CONTRIBUTION_ID= "quickFixProcessors"; //$NON-NLS-1$
	private static final String QUICKASSIST_PROCESSOR_CONTRIBUTION_ID= "quickAssistProcessors"; //$NON-NLS-1$
		
	private static ContributedProcessorDescriptor[] fContributedAssistProcessors= null;
	private static ContributedProcessorDescriptor[] fContributedCorrectionProcessors= null;
	private static String fErrorMessage;
	
	private static ContributedProcessorDescriptor[] getProcessorDescriptors(String contributionId, boolean testMarkerTypes) {
		IConfigurationElement[] elements= Platform.getExtensionRegistry().getConfigurationElementsFor(JavaUI.ID_PLUGIN, contributionId);
		ArrayList res= new ArrayList(elements.length);
		
		for (int i= 0; i < elements.length; i++) {
			// AspectJ Change Begin
			// skip jdt's processor, as we have registered our own
			if (!elements[i]
					.getAttribute("class") //$NON-NLS-1$
					.equals(
							"org.eclipse.jdt.internal.ui.text.correction.QuickFixProcessor")) { //$NON-NLS-1$
				ContributedProcessorDescriptor desc = new ContributedProcessorDescriptor(
						elements[i], testMarkerTypes);
				IStatus status = desc.checkSyntax();
				if (status.isOK()) {
					res.add(desc);
				} else {
					JavaPlugin.log(status);
				}
			}
			// AspectJ Change End
		}
		return (ContributedProcessorDescriptor[]) res.toArray(new ContributedProcessorDescriptor[res.size()]);		
	}
	
	private static ContributedProcessorDescriptor[] getCorrectionProcessors() {
		if (fContributedCorrectionProcessors == null) {
			fContributedCorrectionProcessors= getProcessorDescriptors(QUICKFIX_PROCESSOR_CONTRIBUTION_ID, true);
		}
		return fContributedCorrectionProcessors;
	}
	
	private static ContributedProcessorDescriptor[] getAssistProcessors() {
		if (fContributedAssistProcessors == null) {
			fContributedAssistProcessors= getProcessorDescriptors(QUICKASSIST_PROCESSOR_CONTRIBUTION_ID, false);
		}
		return fContributedAssistProcessors;
	}
		
	public static boolean hasCorrections(ICompilationUnit cu, int problemId) {
		ContributedProcessorDescriptor[] processors= getCorrectionProcessors();
		for (int i= 0; i < processors.length; i++) {
			try {
				IQuickFixProcessor processor= (IQuickFixProcessor) processors[i].getProcessor(cu);
				if (processor != null && processor.hasCorrections(cu, problemId)) {
					return true;
				}
			} catch (CoreException e) {
			}
		}
		return false;
	}
	
	public static boolean isQuickFixableType(Annotation annotation) {
		return (annotation instanceof IJavaAnnotation || annotation instanceof SimpleMarkerAnnotation) && !annotation.isMarkedDeleted();
	}
	
	
	public static boolean hasCorrections(Annotation annotation) {
		if (annotation instanceof IJavaAnnotation) {
			IJavaAnnotation javaAnnotation= (IJavaAnnotation) annotation;
			int problemId= javaAnnotation.getId();
			if (problemId != -1) {
				ICompilationUnit cu= javaAnnotation.getCompilationUnit();
				if (cu != null) {
					return hasCorrections(cu, problemId);
				}
			}
		}
		if (annotation instanceof SimpleMarkerAnnotation) {
			return hasCorrections(((SimpleMarkerAnnotation) annotation).getMarker());
		}
		return false;
	}
	
	private static boolean hasCorrections(IMarker marker) {
		if (marker == null || !marker.exists())
			return false;
			
		IMarkerHelpRegistry registry= IDE.getMarkerHelpRegistry();
		return registry != null && registry.hasResolutions(marker);
	}
	
	public static boolean hasAssists(IInvocationContext context) {
		ContributedProcessorDescriptor[] processors= getAssistProcessors();
		for (int i= 0; i < processors.length; i++) {
			try {
				IQuickAssistProcessor processor= (IQuickAssistProcessor) processors[i].getProcessor(context.getCompilationUnit());
				if (processor != null && processor.hasAssists(context)) {
					return true;
				}				
			} catch (Exception e) {
			}
		}
		return false;
	}	
	
	private JavaCorrectionAssistant fAssistant;

	/**
	 * Constructor for JavaCorrectionProcessor.
	 */
	public JavaCorrectionProcessor(JavaCorrectionAssistant assistant) {
		fAssistant= assistant;
	}

	/*
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		IEditorPart part= fAssistant.getEditor();
		
		ICompilationUnit cu= JavaUI.getWorkingCopyManager().getWorkingCopy(part.getEditorInput());
		IAnnotationModel model= JavaUI.getDocumentProvider().getAnnotationModel(part.getEditorInput());

		int length= viewer != null ? viewer.getSelectedRange().y : 0;
		AssistContext context= new AssistContext(cu, documentOffset, length);

		fErrorMessage= null;
		ArrayList proposals= new ArrayList();
		if (model != null) {
			processAnnotations(context, model, proposals);
		}
		if (proposals.isEmpty()) {
			proposals.add(new ChangeCorrectionProposal(CorrectionMessages.NoCorrectionProposal_description, null, 0, null));
		}
		
		ICompletionProposal[] res= (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
		Arrays.sort(res, new CompletionProposalComparator());
		return res;
	}
	
	private boolean isAtPosition(int offset, Position pos) {
		return (pos != null) && (offset >= pos.getOffset() && offset <= (pos.getOffset() +  pos.getLength()));
	}
	

	private void processAnnotations(IInvocationContext context, IAnnotationModel model, ArrayList proposals) {
		int offset= context.getSelectionOffset();
		
		ArrayList problems= new ArrayList();
		Iterator iter= model.getAnnotationIterator();
		while (iter.hasNext()) {
			Annotation annotation= (Annotation) iter.next();
			if (isQuickFixableType(annotation)) {
				Position pos= model.getPosition(annotation);
				if (isAtPosition(offset, pos)) {
					processAnnotation(annotation, pos, problems, proposals);
				}
			}
		}	
		IProblemLocation[] problemLocations= (IProblemLocation[]) problems.toArray(new IProblemLocation[problems.size()]);
		collectCorrections(context, problemLocations, proposals);
		if (!fAssistant.isUpdatedOffset()) {
			collectAssists(context, problemLocations, proposals);
		}
	}
	
	private void processAnnotation(Annotation curr, Position pos, List problems, List proposals) {
		if (curr instanceof IJavaAnnotation) {
			IJavaAnnotation javaAnnotation= (IJavaAnnotation) curr;
			int problemId= javaAnnotation.getId();
			if (problemId != -1) {
				problems.add(new ProblemLocation(pos.getOffset(), pos.getLength(), javaAnnotation));
				return; // java problems all handled by the quick assist processors
			}
		}
		if (curr instanceof SimpleMarkerAnnotation) {
			IMarker marker= ((SimpleMarkerAnnotation) curr).getMarker();
			IMarkerResolution[] res= IDE.getMarkerHelpRegistry().getResolutions(marker);
			if (res.length > 0) {
				for (int i= 0; i < res.length; i++) {
					proposals.add(new MarkerResolutionProposal(res[i], marker));
				}
			}
		}
	}

	public static void collectCorrections(IInvocationContext context, IProblemLocation[] locations, ArrayList proposals) {
		ContributedProcessorDescriptor[] processors= getCorrectionProcessors();
		for (int i= 0; i < processors.length; i++) {
			try {
				IQuickFixProcessor curr= (IQuickFixProcessor) processors[i].getProcessor(context.getCompilationUnit());
				if (curr != null) {
					IJavaCompletionProposal[] res= curr.getCorrections(context, locations);
					if (res != null) {
						for (int k= 0; k < res.length; k++) {
							proposals.add(res[k]);
						}
					}
				}
			} catch (NullPointerException e) {
				// bug 118073: avoid showing status line error
			} catch (Exception e) {
				fErrorMessage= CorrectionMessages.JavaCorrectionProcessor_error_quickfix_message;
			}
		}
	}
	
	public static void collectAssists(IInvocationContext context, IProblemLocation[] locations, ArrayList proposals) {
		ContributedProcessorDescriptor[] processors= getAssistProcessors();
		for (int i= 0; i < processors.length; i++) {
			try {
				IQuickAssistProcessor curr= (IQuickAssistProcessor) processors[i].getProcessor(context.getCompilationUnit());
				if (curr != null) {
					IJavaCompletionProposal[] res= curr.getAssists(context, locations);
					if (res != null) {
						for (int k= 0; k < res.length; k++) {
							proposals.add(res[k]);
						}
					}				
				}				
			} catch (Exception e) {
				fErrorMessage= CorrectionMessages.JavaCorrectionProcessor_error_quickassist_message;
			}
		}
	}	

	/*
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fErrorMessage;
	}
}
