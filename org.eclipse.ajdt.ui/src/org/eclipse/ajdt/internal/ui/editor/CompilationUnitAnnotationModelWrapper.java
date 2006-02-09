/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: Sian January - initial version
 ******************************************************************************/
package org.eclipse.ajdt.internal.ui.editor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.aspectj.org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.aspectj.org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.aspectj.org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.ajdt.core.javaelements.AJCompilationUnitInfo;
import org.eclipse.ajdt.parserbridge.AJCompilationUnitStructureRequestor;
import org.eclipse.ajdt.parserbridge.AJSourceElementParser;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.ui.text.java.IProblemRequestorExtension;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.IAnnotationModelListenerExtension;
import org.eclipse.jface.util.ListenerList;

/**
 * Wrapper for a CompilationUnitAnnotationModel.  Uses AspectJ's eager parser
 * to report errors and ignores errors from the JDT.
 */
public class CompilationUnitAnnotationModelWrapper implements IAnnotationModel, IProblemRequestor, IProblemRequestorExtension  {

	
	protected static class GlobalAnnotationModelListener implements IAnnotationModelListener, IAnnotationModelListenerExtension {
		
		private ListenerList fListenerList;
		
		public GlobalAnnotationModelListener() {
			fListenerList= new ListenerList();
		}
		
		/**
		 * @see IAnnotationModelListener#modelChanged(IAnnotationModel)
		 */
		public void modelChanged(IAnnotationModel model) {
			Object[] listeners= fListenerList.getListeners();
			for (int i= 0; i < listeners.length; i++) {
				((IAnnotationModelListener) listeners[i]).modelChanged(model);
			}
		}
	
		/**
		 * @see IAnnotationModelListenerExtension#modelChanged(AnnotationModelEvent)
		 */
		public void modelChanged(AnnotationModelEvent event) {
			Object[] listeners= fListenerList.getListeners();
			for (int i= 0; i < listeners.length; i++) {
				Object curr= listeners[i];
				if (curr instanceof IAnnotationModelListenerExtension) {
					((IAnnotationModelListenerExtension) curr).modelChanged(event);
				}
			}
		}
		
		public void addListener(IAnnotationModelListener listener) {
			fListenerList.add(listener);
		}
		
		public void removeListener(IAnnotationModelListener listener) {
			fListenerList.remove(listener);
		}			
	}

	private IAnnotationModel delegate;
	private final ICompilationUnit unit;
	
	public CompilationUnitAnnotationModelWrapper(final ICompilationUnit unit) {
		this.unit = unit;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IAnnotationModel#addAnnotationModelListener(org.eclipse.jface.text.source.IAnnotationModelListener)
	 */
	public void addAnnotationModelListener(IAnnotationModelListener listener) {
		if(delegate != null) {
			delegate.addAnnotationModelListener(listener);
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IAnnotationModel#removeAnnotationModelListener(org.eclipse.jface.text.source.IAnnotationModelListener)
	 */
	public void removeAnnotationModelListener(IAnnotationModelListener listener) {
		if(delegate != null) {
			delegate.removeAnnotationModelListener(listener);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IAnnotationModel#connect(org.eclipse.jface.text.IDocument)
	 */
	public void connect(IDocument document) {
		if(delegate != null) {
			delegate.connect(document);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IAnnotationModel#disconnect(org.eclipse.jface.text.IDocument)
	 */
	public void disconnect(IDocument document) {
		if(delegate != null) {
			delegate.disconnect(document);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IAnnotationModel#addAnnotation(org.eclipse.jface.text.source.Annotation, org.eclipse.jface.text.Position)
	 */
	public void addAnnotation(Annotation annotation, Position position) {
		if(delegate != null) {
			delegate.addAnnotation(annotation, position);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IAnnotationModel#removeAnnotation(org.eclipse.jface.text.source.Annotation)
	 */
	public void removeAnnotation(Annotation annotation) {
		if(delegate != null) {
			delegate.removeAnnotation(annotation);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IAnnotationModel#getAnnotationIterator()
	 */
	public Iterator getAnnotationIterator() {
		if(delegate != null) {
			return delegate.getAnnotationIterator();
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.IAnnotationModel#getPosition(org.eclipse.jface.text.source.Annotation)
	 */
	public Position getPosition(Annotation annotation) {
		if(delegate != null) {
			return delegate.getPosition(annotation);
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IProblemRequestor#acceptProblem(org.eclipse.jdt.core.compiler.IProblem)
	 */
	public void acceptProblem(IProblem problem) {
		// do nothing with problems from the JDT..
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IProblemRequestor#beginReporting()
	 */
	public void beginReporting() {
		if(delegate != null) {
			((IProblemRequestor)delegate).beginReporting();
			
			IJavaProject project = unit.getJavaProject();
	
			AJCompilationUnitStructureRequestor requestor = new AJCompilationUnitStructureRequestor(unit, new AJCompilationUnitInfo(), new HashMap());
			JavaModelManager.PerWorkingCopyInfo perWorkingCopyInfo = ((CompilationUnit)unit).getPerWorkingCopyInfo();
			boolean computeProblems = JavaProject.hasJavaNature(project.getProject()) && perWorkingCopyInfo != null && perWorkingCopyInfo.isActive();
			IProblemFactory problemFactory = new DefaultProblemFactory();
			Map options = project.getOptions(true);
			IBuffer buffer;
			try {
				buffer = unit.getBuffer();
			
				final char[] contents = buffer == null ? null : buffer.getCharacters();
		
				AJSourceElementParser parser = new AJSourceElementParser(
						requestor, 
						problemFactory, 
						new CompilerOptions(options),
						true/*report local declarations*/);
				parser.reportOnlyOneSyntaxError = !computeProblems;
				
				parser.scanner.source = contents;
				requestor.setParser(parser);
				
				CompilationUnitDeclaration unitDec = parser.parseCompilationUnit(new org.aspectj.org.eclipse.jdt.internal.compiler.env.ICompilationUnit() {
						public char[] getContents() {
							return contents;
						}
						public char[] getMainTypeName() {
							return ((CompilationUnit)unit).getMainTypeName();
						}
						public char[][] getPackageName() {
							return ((CompilationUnit)unit).getPackageName();
						}
						public char[] getFileName() {
							return ((CompilationUnit)unit).getFileName();
						}
					}, true /*full parse to find local elements*/);
				org.aspectj.org.eclipse.jdt.core.compiler.IProblem[] problems = unitDec.compilationResult.problems;
				if (problems != null){
					for (int i = 0; i < problems.length; i++) {
						org.aspectj.org.eclipse.jdt.core.compiler.IProblem problem = problems[i];
						if (problem == null)
							continue;
						((IProblemRequestor)delegate).acceptProblem(new DefaultProblem(
						problem.getOriginatingFileName(),
						problem.getMessage(),
						problem.getID(),
						problem.getArguments(),
						problem.isError()?ProblemSeverities.Error:ProblemSeverities.Warning,
						problem.getSourceStart(),
						problem.getSourceEnd(),
						problem.getSourceLineNumber()));
					}
				}
			} catch (JavaModelException e) {
			}			
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IProblemRequestor#endReporting()
	 */
	public void endReporting() {
		((IProblemRequestor)delegate).endReporting();
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IProblemRequestor#isActive()
	 */
	public boolean isActive() {
		if(delegate != null) {
			return ((IProblemRequestor)delegate).isActive();
		} else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.java.IProblemRequestorExtension#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
		if(delegate != null) {
			((IProblemRequestorExtension)delegate).setProgressMonitor(monitor);
		}
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.java.IProblemRequestorExtension#setIsActive(boolean)
	 */
	public void setIsActive(boolean isActive) {
		if(delegate != null) {
			((IProblemRequestorExtension)delegate).setIsActive(isActive);
		}
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.java.IProblemRequestorExtension#beginReportingSequence()
	 */
	public void beginReportingSequence() {
		if(delegate != null) {
			((IProblemRequestorExtension)delegate).beginReportingSequence();
		}
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.java.IProblemRequestorExtension#endReportingSequence()
	 */
	public void endReportingSequence() {
		if(delegate != null) {
			((IProblemRequestorExtension)delegate).endReportingSequence();
		}
	}

	/**
	 * @param annotationModel
	 */
	public void setDelegate(IAnnotationModel annotationModel) {
		delegate = annotationModel;		
	}

	public void setIsHandlingTemporaryProblems(boolean enable) {
		if(delegate != null) {
			((IProblemRequestorExtension)delegate).setIsHandlingTemporaryProblems(enable);
		}
	}

}
