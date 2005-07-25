/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ajdt.internal.ui.editor.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationMessages;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportReferencesCollector;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.corext.util.TypeInfoRequestor;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;
import org.eclipse.jdt.internal.ui.text.correction.SimilarElementsRequestor;
import org.eclipse.jface.text.Region;

/**
 * @author Sian
 * Mostly copied from OrganizeImportsOperation - changes marked with // AspectJ Change
 */
public class AJOrganizeImportsOperation implements IWorkspaceRunnable {

	public static interface IChooseImportQuery {
		/**
		 * Selects imports from a list of choices.
		 * @param openChoices From each array, a type ref has to be selected
		 * @param ranges For each choice the range of the corresponding  type reference.
		 * @return Returns <code>null</code> to cancel the operation, or the
		 *         selected imports.
		 */
		TypeInfo[] chooseImports(TypeInfo[][] openChoices, ISourceRange[] ranges);
	}
	
	
	private static class TypeReferenceProcessor {
		
		private static class UnresolvedTypeData {
			final SimpleName ref;
			final int typeKinds;
			final List foundInfos;

			public UnresolvedTypeData(SimpleName ref) {
				this.ref= ref;
				this.typeKinds= ASTResolving.getPossibleTypeKinds(ref, true);
				this.foundInfos= new ArrayList(3);
			}
		}
		
		private Set fOldSingleImports;
		private Set fOldDemandImports;
		
		private Set fImplicitImports;
		
		private ImportsStructure fImpStructure;
		
		private boolean fDoIgnoreLowerCaseNames;
		
		private IPackageFragment fCurrPackage;
		
		private ScopeAnalyzer fAnalyzer;
		private boolean fAllowDefaultPackageImports;
		
		private Map fUnresolvedTypes;
		private Set fImportsAdded;
		private TypeInfo[][] fOpenChoices;
		private SourceRange[] fSourceRanges;
		
		
		public TypeReferenceProcessor(Set oldSingleImports, Set oldDemandImports, CompilationUnit root, ImportsStructure impStructure, boolean ignoreLowerCaseNames) {
			fOldSingleImports= oldSingleImports;
			fOldDemandImports= oldDemandImports;
			fImpStructure= impStructure;
			fDoIgnoreLowerCaseNames= ignoreLowerCaseNames;
			
			ICompilationUnit cu= impStructure.getCompilationUnit();
			
			fImplicitImports= new HashSet(3);
			fImplicitImports.add(""); //$NON-NLS-1$
			fImplicitImports.add("java.lang"); //$NON-NLS-1$
			fImplicitImports.add(cu.getParent().getElementName());	
			
			fAnalyzer= new ScopeAnalyzer(root);
			
			fCurrPackage= (IPackageFragment) cu.getParent();
			
			fAllowDefaultPackageImports= cu.getJavaProject().getOption(JavaCore.COMPILER_COMPLIANCE, true).equals(JavaCore.VERSION_1_3);
			
			fImportsAdded= new HashSet();
			fUnresolvedTypes= new HashMap();
		}
		
		private boolean needsImport(ITypeBinding typeBinding, SimpleName ref) {
			if (!typeBinding.isTopLevel() && !typeBinding.isMember()) {
				return false; // no imports for anonymous, local, primitive types or parameters types
			}
			int modifiers= typeBinding.getModifiers();
			if (Modifier.isPrivate(modifiers)) {
				return false; // imports for privates are not required
			}
			ITypeBinding currTypeBinding= Bindings.getBindingOfParentType(ref);
			if (currTypeBinding == null) {
				return false; // not in a type
			}
			if (!Modifier.isPublic(modifiers)) {
				if (!currTypeBinding.getPackage().getName().equals(typeBinding.getPackage().getName())) {
					return false; // not visible
				}
			}
			
			ASTNode parent= ref.getParent();
			if (parent instanceof Type) {
				parent= parent.getParent();
			}
			if (parent instanceof AbstractTypeDeclaration && parent.getParent() instanceof CompilationUnit) {
				return true;
			}
			
			if (typeBinding.isMember()) {
				IBinding[] visibleTypes= fAnalyzer.getDeclarationsInScope(ref, ScopeAnalyzer.TYPES);
				for (int i= 0; i < visibleTypes.length; i++) {
					ITypeBinding curr= ((ITypeBinding) visibleTypes[i]).getTypeDeclaration();
					if (curr == typeBinding) {
						return false;
					}
				}
			}
			return true;				
		}
		
		
		/**
		 * Tries to find the given type name and add it to the import structure.
		 */
		public void add(SimpleName ref) throws CoreException {
			String typeName= ref.getIdentifier();
			
			if (fImportsAdded.contains(typeName)) {
				return;
			}
			
			IBinding binding= ref.resolveBinding();
			if (binding != null) {
				if (binding.getKind() == IBinding.TYPE) {
					ITypeBinding typeBinding= (ITypeBinding) binding;
					if (typeBinding.isArray()) {
						typeBinding= typeBinding.getElementType();
					}
					typeBinding= typeBinding.getTypeDeclaration();
					
					if (needsImport(typeBinding, ref)) {
						fImpStructure.addImport(typeBinding);
						fImportsAdded.add(typeName);
					}
				}	
				return;
			}
			
			if (fDoIgnoreLowerCaseNames && typeName.length() > 0) {
				char ch= typeName.charAt(0);
				if (Strings.isLowerCase(ch) && Character.isLetter(ch)) {
					return;
				}
			}
			fImportsAdded.add(typeName);			
			fUnresolvedTypes.put(typeName, new UnresolvedTypeData(ref));
		}
			
		public boolean process(IProgressMonitor monitor) throws JavaModelException {
			try {
				int nUnresolved= fUnresolvedTypes.size();
				if (nUnresolved == 0) {
					return false;
				}
				char[][] allTypes= new char[nUnresolved][];
				int i= 0;
				for (Iterator iter= fUnresolvedTypes.keySet().iterator(); iter.hasNext();) {
					allTypes[i++]= ((String) iter.next()).toCharArray();
				}
				ArrayList typesFound= new ArrayList();
				IJavaProject project= fCurrPackage.getJavaProject();
				IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { project });
				TypeInfoRequestor requestor= new TypeInfoRequestor(typesFound);
				new SearchEngine().searchAllTypeNames(null, allTypes, scope, requestor, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, monitor);

				boolean is50OrHigher= 	JavaModelUtil.is50OrHigher(project);
				
				for (i= 0; i < typesFound.size(); i++) {
					TypeInfo curr= (TypeInfo) typesFound.get(i);
					UnresolvedTypeData data= (UnresolvedTypeData) fUnresolvedTypes.get(curr.getTypeName());
					if (data != null && isVisible(curr) && isOfKind(curr, data.typeKinds, is50OrHigher)) {
						if (fAllowDefaultPackageImports || curr.getPackageName().length() > 0) {
							data.foundInfos.add(curr);
						}
					}
				}
				// TODO: Sian - implement this
//				// AspectJ Change Begin
//				IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {fImpStructure.getCompilationUnit().getJavaProject()}, 
//						IJavaSearchScope.APPLICATION_LIBRARIES |
//						IJavaSearchScope.REFERENCED_PROJECTS |
//						IJavaSearchScope.SOURCES); 
////					JavaSearchScopeFactory.getInstance().createJavaProjectSearchScope(fImpStructure.getCompilationUnit().getJavaProject(), false);
//				List ajTypes = getAspectJTypes(scope);
//				for (Iterator iter = ajTypes.iterator(); iter.hasNext();) {
//					AJCUTypeInfo curr = (AJCUTypeInfo) iter.next();
//					if(curr.getTypeName().equals(simpleTypeName)) {
//						IType type= curr.resolveType(fSearchScope);
//						UnresolvedTypeData data= (UnresolvedTypeData) fUnresolvedTypes.get(curr.getTypeName());
//						if (type != null && JavaModelUtil.isVisible(type, fCurrPackage)) {
//							data.foundInfos.add(curr);
//						}
//					}
//				}	
//				// AspectJ Change End				
				ArrayList openChoices= new ArrayList(nUnresolved);
				ArrayList sourceRanges= new ArrayList(nUnresolved);
				for (Iterator iter= fUnresolvedTypes.values().iterator(); iter.hasNext();) {
					UnresolvedTypeData data= (UnresolvedTypeData) iter.next();
					TypeInfo[] openChoice= processTypeInfo(data.foundInfos);
					if (openChoice != null) {
						openChoices.add(openChoice);
						sourceRanges.add(new SourceRange(data.ref.getStartPosition(), data.ref.getLength()));
					}
				}
				if (openChoices.isEmpty()) {
					return false;
				}
				fOpenChoices= (TypeInfo[][]) openChoices.toArray(new TypeInfo[openChoices.size()][]);
				fSourceRanges= (SourceRange[]) sourceRanges.toArray(new SourceRange[sourceRanges.size()]);
				return true;
			} finally {
				monitor.done();
			}
		}
		
		private TypeInfo[] processTypeInfo(List typeRefsFound) {
			int nFound= typeRefsFound.size();
			if (nFound == 0) {
				// nothing found
				return null;
			} else if (nFound == 1) {
				TypeInfo typeRef= (TypeInfo) typeRefsFound.get(0);
				fImpStructure.addImport(typeRef.getFullyQualifiedName());
				return null;
			} else {
				String typeToImport= null;
				boolean ambiguousImports= false;
				
				// multiple found, use old imports to find an entry
				for (int i= 0; i < nFound; i++) {
					TypeInfo typeRef= (TypeInfo) typeRefsFound.get(i);
					String fullName= typeRef.getFullyQualifiedName();
					String containerName= typeRef.getTypeContainerName();
					if (fOldSingleImports.contains(fullName)) {
						// was single-imported
						fImpStructure.addImport(fullName);
						return null;
					} else if (fOldDemandImports.contains(containerName) || fImplicitImports.contains(containerName)) {
						if (typeToImport == null) {
							typeToImport= fullName;
						} else {  // more than one import-on-demand
							ambiguousImports= true;
						}
					}
				}
				
				if (typeToImport != null && !ambiguousImports) {
					fImpStructure.addImport(typeToImport);
					return null;
				}
				// return the open choices
				return (TypeInfo[]) typeRefsFound.toArray(new TypeInfo[nFound]);
			}
		}
		
		private boolean isOfKind(TypeInfo curr, int typeKinds, boolean is50OrHigher) {
			int flags= curr.getModifiers();
			if (Flags.isAnnotation(flags)) {
				return is50OrHigher && ((typeKinds & SimilarElementsRequestor.ANNOTATIONS) != 0);
			}
			if (Flags.isEnum(flags)) {
				return is50OrHigher && ((typeKinds & SimilarElementsRequestor.ENUMS) != 0);
			}
			if (Flags.isInterface(flags)) {
				return (typeKinds & SimilarElementsRequestor.INTERFACES) != 0;
			}
			return (typeKinds & SimilarElementsRequestor.CLASSES) != 0;
		}

		private boolean isVisible(TypeInfo curr) {
			int flags= curr.getModifiers();
			if (Flags.isPrivate(flags)) {
				return false;
			}
			if (Flags.isPublic(flags) || Flags.isProtected(flags)) {
				return true;
			}
			return curr.getPackageName().equals(fCurrPackage.getElementName());
		}

		public TypeInfo[][] getChoices() {
			return fOpenChoices;
		}

		public ISourceRange[] getChoicesSourceRanges() {
			return fSourceRanges;
		}
	}	


	private Region fRange;
	private ImportsStructure fImportsStructure;	
	private boolean fDoSave;
	
	private boolean fIgnoreLowerCaseNames;
	
	private IChooseImportQuery fChooseImportQuery;
	
	private int fNumberOfImportsAdded;
	private int fNumberOfImportsRemoved;

	private IProblem fParsingError;
	private CompilationUnit fASTRoot;

	public AJOrganizeImportsOperation(ImportsStructure impStructure, Region range, boolean ignoreLowerCaseNames, boolean save, IChooseImportQuery chooseImportQuery) {
		super();
		fImportsStructure= impStructure;
		fRange= range;
		fDoSave= save;
		fIgnoreLowerCaseNames= ignoreLowerCaseNames;
		fChooseImportQuery= chooseImportQuery;

		fNumberOfImportsAdded= 0;
		fNumberOfImportsRemoved= 0;
		
		fParsingError= null;
		ASTParser parser= ASTParser.newParser(ASTProvider.AST_LEVEL);
		parser.setSource(impStructure.getCompilationUnit());
		parser.setResolveBindings(true);
		fASTRoot= (CompilationUnit) parser.createAST(null);
	}
	
	public AJOrganizeImportsOperation(ICompilationUnit cu, String[] importOrder, int importThreshold, boolean ignoreLowerCaseNames, boolean save, boolean doResolve, IChooseImportQuery chooseImportQuery) throws CoreException {
		this(new ImportsStructure(cu, importOrder, importThreshold, false), null, ignoreLowerCaseNames, save, chooseImportQuery);
	}
	
	/**
	 * Runs the operation.
	 * @throws OperationCanceledException Runtime error thrown when operation is cancelled.
	 */	
	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {
			ICompilationUnit cu= fImportsStructure.getCompilationUnit();
			fNumberOfImportsAdded= 0;
			fNumberOfImportsRemoved= 0;
			
			monitor.beginTask(Messages.format(CodeGenerationMessages.OrganizeImportsOperation_description, cu.getElementName()), 5); 

			Set/*<String>*/ oldSingleImports= new HashSet();
			Set/*<String>*/  oldDemandImports= new HashSet();
			List/*<SimpleName>*/ typeReferences= new ArrayList();
			List/*<SimpleName>*/ staticReferences= new ArrayList();
			
			boolean res= collectReferences(typeReferences, staticReferences, oldSingleImports, oldDemandImports);
			if (!res) {
				return;
			}
						
			monitor.worked(1);
		
			TypeReferenceProcessor processor= new TypeReferenceProcessor(oldSingleImports, oldDemandImports, fASTRoot, fImportsStructure, fIgnoreLowerCaseNames);
			
			Iterator refIterator= typeReferences.iterator();
			while (refIterator.hasNext()) {
				SimpleName typeRef= (SimpleName) refIterator.next();
				processor.add(typeRef);
			}
			
			boolean hasOpenChoices= processor.process(new SubProgressMonitor(monitor, 3));
			addStaticImports(staticReferences, fImportsStructure);
			
			if (hasOpenChoices && fChooseImportQuery != null) {
				TypeInfo[][] choices= processor.getChoices();
				ISourceRange[] ranges= processor.getChoicesSourceRanges();
				TypeInfo[] chosen= fChooseImportQuery.chooseImports(choices, ranges);
				if (chosen == null) {
					// cancel pressed by the user
					throw new OperationCanceledException();
				}
				for (int i= 0; i < chosen.length; i++) {
					TypeInfo typeInfo= chosen[i];
					fImportsStructure.addImport(typeInfo.getFullyQualifiedName());
				}				
			}
			fImportsStructure.create(fDoSave, new SubProgressMonitor(monitor, 1));
			
			determineImportDifferences(fImportsStructure, oldSingleImports, oldDemandImports);
			processor= null;
		} finally {
			monitor.done();
		}
	}
	
	private void determineImportDifferences(ImportsStructure importsStructure, Set oldSingleImports, Set oldDemandImports) {
  		ArrayList importsAdded= new ArrayList();
  		importsAdded.addAll(Arrays.asList(importsStructure.getCreatedImports()));
  		importsAdded.addAll(Arrays.asList(importsStructure.getCreatedStaticImports()));
		
		Object[] content= oldSingleImports.toArray();
	    for (int i= 0; i < content.length; i++) {
	        String importName= (String) content[i];
	        if (importsAdded.remove(importName))
	            oldSingleImports.remove(importName);
	    }
	    content= oldDemandImports.toArray();
	    for (int i= 0; i < content.length; i++) {
	        String importName= (String) content[i]; 
	        if (importsAdded.remove(importName + ".*")) //$NON-NLS-1$
	            oldDemandImports.remove(importName);
	    }
	    fNumberOfImportsAdded= importsAdded.size();
	    fNumberOfImportsRemoved= oldSingleImports.size() + oldDemandImports.size();	    
	}
	
	
	private void addStaticImports(List/*<SimpleName>*/ staticReferences, ImportsStructure importsStructure) {
		for (int i= 0; i < staticReferences.size(); i++) {
			Name name= (Name) staticReferences.get(i);
			IBinding binding= name.resolveBinding();
			if (binding != null) { // paranoidal check
				importsStructure.addStaticImport(binding);
			}
		}
	}

	private boolean isAffected(IProblem problem) {
		return fRange == null || (fRange.getOffset() <= problem.getSourceEnd() && (fRange.getOffset() + fRange.getLength()) > problem.getSourceStart());
	}

	
	// find type references in a compilation unit
	private boolean collectReferences(List typeReferences, List staticReferences, Set oldSingleImports, Set oldDemandImports) {
		IProblem[] problems= fASTRoot.getProblems();
		for (int i= 0; i < problems.length; i++) {
			IProblem curr= problems[i];
			if (curr.isError() && (curr.getID() & IProblem.Syntax) != 0 && isAffected(curr)) {
				fParsingError= problems[i];
				return false;
			}
		}
		List imports= fASTRoot.imports();
		for (int i= 0; i < imports.size(); i++) {
			ImportDeclaration curr= (ImportDeclaration) imports.get(i);
			String id= ASTResolving.getFullName(curr.getName());
			if (curr.isOnDemand()) {
				oldDemandImports.add(id);
			} else {
				oldSingleImports.add(id);
			}
		}
		
		IJavaProject project= fImportsStructure.getCompilationUnit().getJavaProject();
		ImportReferencesCollector visitor = new ImportReferencesCollector(project, fRange, typeReferences, staticReferences);
		fASTRoot.accept(visitor);

		return true;
	}	
	
	/**
	 * After executing the operation, returns <code>null</code> if the operation has been executed successfully or
	 * the range where parsing failed. 
	 */
	public IProblem getParseError() {
		return fParsingError;
	}
	
	public int getNumberOfImportsAdded() {
		return fNumberOfImportsAdded;
	}
	
	public int getNumberOfImportsRemoved() {
		return fNumberOfImportsRemoved;
	}

	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
	
}

