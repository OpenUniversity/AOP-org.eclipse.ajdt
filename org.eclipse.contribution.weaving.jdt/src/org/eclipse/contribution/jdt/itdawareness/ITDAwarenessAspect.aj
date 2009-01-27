package org.eclipse.contribution.jdt.itdawareness;

import java.util.HashMap;

import org.eclipse.contribution.jdt.JDTWeavingPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.internal.compiler.SourceElementParser;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.ISourceType;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.Openable;
import org.eclipse.jdt.internal.core.SearchableEnvironment;
import org.eclipse.jdt.internal.core.CompilationUnitProblemFinder;
import org.eclipse.jdt.internal.core.hierarchy.TypeHierarchy;
import org.eclipse.jdt.internal.core.hierarchy.HierarchyBuilder;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.hierarchy.HierarchyResolver;

/**
 * Aspect to add ITD awareness to various kinds of searches in the IDE
 * This aspect swaps out a SearchableEnvironment with an ITDAwareNameEnvironment
 * 
 * See bug 256312 for an explanation of this aspect
 * 
 * @author andrew
 * @created Nov 22, 2008
 *
 */
public aspect ITDAwarenessAspect {
    
    /**
     * set by the AspectJPlugin on startup
     */
    public static INameEnvironmentProvider provider;
    
    
    /********************************************
     * This section deals with ensuring the focus type has its 
     * super types properly created
     */
    
    /**
     * This pointcut grabs all calls to the getting of the element info
     * for source types that occur within the HierarchyResolver.
     * 
     * Need to convert all SourceTypeInfos into ITDAwareSourceTypeInfos
     * when they occur in the HierarchyResolver
     */
    pointcut typeHierachySourceTypeInfoCreation() :
        call(public Object JavaElement+.getElementInfo()) &&
        within(HierarchyResolver);
    
    /** 
     * Capture all creations of source type element infos
     * and convert them into ITD aware source type element infos
     */
    Object around() : typeHierachySourceTypeInfoCreation() {
        Object info = proceed();
        if (info instanceof ISourceType &&
                provider != null) {
            info = provider.transformSourceTypeInfo((ISourceType) info);
        }
        return info;
    }

    /********************************************
     * This section deals with ensuring that all other types
     * have their super types properly created
     * 
     * Note that *sub* types are not properly found.
     * 
     * The reason is that sub types are found through the indexer.
     * The indexer does not index declare parents relationships
     * 
     * Don't do subtypes for now.
     */

    /**
     * Converts a name environment into an ITD Aware Name Environment
     * when we are finding type hierarchies
     */
    SearchableEnvironment around(JavaProject project,
            ICompilationUnit[] workingCopies) : 
                interestingSearchableEnvironmentCreation(project, workingCopies) {
        if (provider != null) {
            try {
                SearchableEnvironment newEnvironment = provider.getNameEnvironment(project, workingCopies);
                if (newEnvironment != null) {
                    return newEnvironment;
                }
            } catch (RuntimeException e) {
                JDTWeavingPlugin.logException(e);
            }
        }
        return proceed(project, workingCopies);
    }
    
    /**
     * The creation of a SearchableEnvironment
     */
    pointcut searchableEnvironmentCreation(JavaProject project,
            ICompilationUnit[] workingCopies) : 
                call(SearchableEnvironment.new(JavaProject,
                        ICompilationUnit[])) && args(project, workingCopies); 
                        
    
    /**
     * Only certain SearchableEnvironment creations are interesting
     * This pointcut determines which ones they are.
     */
    pointcut interestingSearchableEnvironmentCreation(JavaProject project,
            ICompilationUnit[] workingCopies) : 
                searchableEnvironmentCreation(JavaProject, ICompilationUnit[]) &&
                (
                        cflow(typeHierarchyCreation()) || // creation of type hierarchies
                        cflow(typeHierarchyComputing())  // computing the type hierarchy (do we need both?)
                ) && args(project, workingCopies);
    /**
     * The creation of a type hierarchy
     */
    pointcut typeHierarchyCreation() : execution(public HierarchyBuilder.new(TypeHierarchy));
    
    /**
     * the computation of a type hierarchy
     */
    pointcut typeHierarchyComputing() : execution(protected void TypeHierarchy.compute());

    
    SearchableEnvironment around(JavaProject project,
            WorkingCopyOwner owner) : interestingSearchableEnvironmentCreation2(project, owner) {
        if (provider != null) {
            try {
                SearchableEnvironment newEnvironment = provider.getNameEnvironment(project, 
                        owner == null ? null : JavaModelManager.getJavaModelManager().getWorkingCopies(owner, true/*add primary WCs*/));
                if (newEnvironment != null) {
                    return newEnvironment;
                }
            } catch (RuntimeException e) {
                JDTWeavingPlugin.logException(e);
            }
        }            
        return proceed(project, owner);
    }

    
    /**
     * Only certain SearchableEnvironment creations are interesting
     * This pointcut determines which ones they are.
     */
    pointcut interestingSearchableEnvironmentCreation2(JavaProject project,
            WorkingCopyOwner workingCopyOwner) : 
                searchableEnvironmentCreation2(JavaProject, WorkingCopyOwner) &&
                (
                        cflow(codeSelect())  // open type action
                ) && args(project, workingCopyOwner);
            
    // alternate creation of searchble environment
    pointcut searchableEnvironmentCreation2(JavaProject project,
            WorkingCopyOwner workingCopyOwner) : 
                call(SearchableEnvironment.new(JavaProject,
                        WorkingCopyOwner)) && args(project, workingCopyOwner); 
    
    /**
     * for determining hyperlinks and open action
     */
    pointcut codeSelect() : execution(protected IJavaElement[] Openable.codeSelect(org.eclipse.jdt.internal.compiler.env.ICompilationUnit,int,int,WorkingCopyOwner) throws JavaModelException);
    
    /********************************************
     * This section handles reconciling of java CompilationUnits. 
     * Ensure that the Java compilation unit is reconciled with an AJReconcileWorkingCopyOperation
     * so that ITDs are properly ignored.
     */

    @SuppressWarnings("unchecked")
    pointcut findProblemsInJava(
            CompilationUnit unitElement,
            SourceElementParser parser,
            WorkingCopyOwner workingCopyOwner,
            HashMap problems,
            boolean creatingAST,
            int reconcileFlags,
            IProgressMonitor monitor) : execution(public static CompilationUnitDeclaration CompilationUnitProblemFinder.process(CompilationUnit, SourceElementParser, WorkingCopyOwner, HashMap, boolean, int, IProgressMonitor)) &&
            args(unitElement, parser, workingCopyOwner, problems, creatingAST, reconcileFlags, monitor);

    
    @SuppressWarnings("unchecked")
    CompilationUnitDeclaration around(
            CompilationUnit unitElement, 
            SourceElementParser parser,
            WorkingCopyOwner workingCopyOwner,
            HashMap problems,
            boolean creatingAST,
            int reconcileFlags,
            IProgressMonitor monitor) throws JavaModelException : findProblemsInJava(unitElement, parser, workingCopyOwner, problems, creatingAST, reconcileFlags, monitor) {
        if (provider != null && provider.shouldFindProblems(unitElement)) {
            try {
                return provider.problemFind(unitElement, parser, workingCopyOwner, problems, creatingAST, reconcileFlags, monitor);
            } catch (Exception e) {
                if (! (e instanceof OperationCanceledException)) {
                    JDTWeavingPlugin.logException(e);
                } else {
                    // rethrow the cancel
                    throw (OperationCanceledException) e;
                }
            }
        }
        return proceed(unitElement, parser, workingCopyOwner, problems, creatingAST, reconcileFlags, monitor);
    }
            
            
    /*********************************
     * This section handles ITD aware content assist in Java files
     * 
     * Hmmmm...maybe want to promote this one to its own package because other plugins may
     * want to add their own way of doing completions for Java files
     */
    public static IJavaContentAssistProvider contentAssistProvider;
            
    pointcut codeCompleteInJavaFile(org.eclipse.jdt.internal.compiler.env.ICompilationUnit cu,
            org.eclipse.jdt.internal.compiler.env.ICompilationUnit unitToSkip,
            int position, CompletionRequestor requestor,
            WorkingCopyOwner owner,
            ITypeRoot typeRoot, Openable target) : 
        execution(protected void Openable.codeComplete(
                org.eclipse.jdt.internal.compiler.env.ICompilationUnit,
                org.eclipse.jdt.internal.compiler.env.ICompilationUnit,
                int, CompletionRequestor,
                WorkingCopyOwner,
                ITypeRoot)) && within(Openable) && this(target) && args(cu, unitToSkip, position, requestor, owner, typeRoot);
    
    void around(org.eclipse.jdt.internal.compiler.env.ICompilationUnit cu,
            org.eclipse.jdt.internal.compiler.env.ICompilationUnit unitToSkip,
            int position, CompletionRequestor requestor,
            WorkingCopyOwner owner,
            ITypeRoot typeRoot, Openable target) : 
                codeCompleteInJavaFile(cu, unitToSkip, position, requestor, owner, typeRoot, target) && 
                if (contentAssistProvider != null) {
        boolean result;
        try {
            result = contentAssistProvider.doContentAssist(cu, unitToSkip, position, requestor, owner, typeRoot, target);
        } catch (Exception e) {
            JDTWeavingPlugin.logException(e);
            result = false;
        }
            
        if (!result) {
            proceed(cu, unitToSkip, position, requestor, owner, typeRoot, target);
        }
    }
}
