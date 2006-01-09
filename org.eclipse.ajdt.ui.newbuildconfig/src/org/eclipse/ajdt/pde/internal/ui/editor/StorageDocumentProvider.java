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

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IStorageEditorInput;

public class StorageDocumentProvider extends StreamDocumentProvider {

	public StorageDocumentProvider(IDocumentPartitioner partitioner) {
		this(partitioner, null);
	}

	public StorageDocumentProvider(
		IDocumentPartitioner partitioner,
		String encoding) {
		super(partitioner, encoding);
	}

	protected IDocument createDocument(Object element) throws CoreException {
		if (element instanceof IStorageEditorInput) {
			IDocument document = createEmptyDocument();
			IDocumentPartitioner part = getPartitioner();
			if (part != null) {
				part.connect(document);
				document.setDocumentPartitioner(part);
			}
			IStorage storage = ((IStorageEditorInput)element).getStorage();
			setDocumentContent(document, storage);
			return document;
		}
		return null;
	}
	protected void setDocumentContent(IDocument document, IStorage storage) {
		try {
			InputStream contentStream = storage.getContents();
			setDocumentContent(document, contentStream);
			contentStream.close();
		} catch (Exception e) {
			PDEPlugin.logException(e);
		}
	}

}
