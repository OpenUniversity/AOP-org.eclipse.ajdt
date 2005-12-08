/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ajdt.pde.internal.ui.editor;

import java.io.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class SystemFileDocumentProvider extends StreamDocumentProvider {

	public SystemFileDocumentProvider(IDocumentPartitioner partitioner) {
		this(partitioner, null);
	}

	public SystemFileDocumentProvider(
		IDocumentPartitioner partitioner,
		String encoding) {
		super(partitioner, encoding);
	}
	/*
	 * @see AbstractDocumentProvider#createAnnotationModel(Object)
	 */
	protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
		if (element instanceof SystemFileEditorInput) {
			SystemFileEditorInput input= (SystemFileEditorInput) element;
			File file = (File)input.getAdapter(File.class);
			if (file!=null) {
				return new SystemFileMarkerAnnotationModel();
			}
		}
		return super.createAnnotationModel(element);
	}

	protected IDocument createDocument(Object element) throws CoreException {
		if (element instanceof SystemFileEditorInput) {
			IDocument document = createEmptyDocument();
			IDocumentPartitioner part = getPartitioner();
			if (part != null) {
				part.connect(document);
				document.setDocumentPartitioner(part);
			}
			File file =
				(File) ((SystemFileEditorInput) element).getAdapter(File.class);
			setDocumentContent(document, file);
			return document;
		}
		return null;
	}
	protected void doSaveDocument(
		IProgressMonitor monitor,
		Object element,
		IDocument document,
		boolean force)
		throws CoreException {
	}
	protected void setDocumentContent(IDocument document, File file) {
		try {
			InputStream contentStream = new FileInputStream(file);
			setDocumentContent(document, contentStream);
			contentStream.close();
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
	}

}
