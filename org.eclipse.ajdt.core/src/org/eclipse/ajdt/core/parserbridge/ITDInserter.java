/*******************************************************************************
 * Copyright (c) 2008 SpringSource and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *      Andrew Eisenberg - Initial implementation
 *******************************************************************************/
package org.eclipse.ajdt.core.parserbridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aspectj.asm.IProgramElement;
import org.eclipse.ajdt.core.AJLog;
import org.eclipse.ajdt.core.model.AJProjectModelFacade;
import org.eclipse.ajdt.core.model.AJProjectModelFactory;
import org.eclipse.ajdt.core.model.AJRelationshipManager;
import org.eclipse.ajdt.core.model.AJWorldFacade;
import org.eclipse.ajdt.core.model.AJWorldFacade.ErasedTypeSignature;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MemberTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;
import org.eclipse.jdt.internal.compiler.parser.TypeConverter;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;

/**
 * Inserts ITD information into a TypeDeclaration so that
 * when reconciling occurs introduced methods, fields, and constructors
 * can be found.
 * 
 * This class works by mocking up the ITDs in the type declaration including 
 * altering the type hierarchy.  
 * 
 * This mocking up occurs after parsing, but before binding.  The mocked up elements
 * are then used as part of the type binding.
 * 
 * The mocked up elements are then removed after the compilation is finished because
 * the compilation results are used elsewhere.
 * 
 * This class is a visitor.  It visits a compilation unit and augments all types
 * with ITDs.
 * 
 * See Bug 255848
 * 
 * @author andrew
 * @created Nov 26, 2008
 */
public class ITDInserter extends ASTVisitor {
    
    private static class OrigContents {
        AbstractMethodDeclaration[] methods;
        FieldDeclaration[] fields;
        TypeReference superClass;
        TypeReference[] superInterfaces;
        TypeDeclaration[] memberTypes;
    }
    
    private static class ITDTypeConverter extends TypeConverter {
        public ITDTypeConverter(ProblemReporter reporter) {
            super(reporter, Signature.C_DOT);
        }
        protected TypeReference createTypeReference(char[] typeName) {
            return super.createTypeReference(typeName, 0, typeName.length);
        }
    }

    private final ICompilationUnit unit;

    private Map<TypeDeclaration, OrigContents> origMap = new HashMap<TypeDeclaration, OrigContents>();

    private final ITDTypeConverter typeConverter;

    private AJProjectModelFacade model;
    
    public ITDInserter(ICompilationUnit unit, ProblemReporter reporter) {
        this.unit = unit;
        typeConverter = new ITDTypeConverter(reporter);
        model = AJProjectModelFactory.getInstance().getModelForJavaElement(unit);
    }
    
    public boolean visit(TypeDeclaration type, BlockScope blockScope) {
        augmentType(type);
        return false; // no local/anonymous type
    }
    public boolean visit(TypeDeclaration type, CompilationUnitScope compilationUnitScope) {
        augmentType(type);
        return true;
    }
    public boolean visit(TypeDeclaration memberType, ClassScope classScope) {
        augmentType(memberType);
        return true;
    }
    
    /**
     * augments a type with ITD info
     */
    private void augmentType(TypeDeclaration type) {
        
        OrigContents orig = new OrigContents();
        orig.methods = type.methods;
        orig.fields = type.fields;
        orig.superClass = type.superclass;
        orig.superInterfaces = type.superInterfaces;
        orig.memberTypes = type.memberTypes;
        
        try {
            List<FieldDeclaration> itdFields = new LinkedList<FieldDeclaration>();
            List<AbstractMethodDeclaration> itdMethods = new LinkedList<AbstractMethodDeclaration>();
            List<TypeDeclaration> itits = new LinkedList<TypeDeclaration>();
            IType handle = getHandle(type);
            

            List<IProgramElement> ipes = getITDs(handle);
            for (IProgramElement elt : ipes) {
                if (elt.getKind() == IProgramElement.Kind.INTER_TYPE_METHOD) {
                    // ignore if type is an interface.
                    // assumption is that this ITD is an implementation of an interface method
                    // adding it here would cause a duplicate method error.
                    // These are added to the first class that instantiates this interface
                    // See bug 257437
                    if (TypeDeclaration.kind(type.modifiers) == TypeDeclaration.CLASS_DECL) {
                        if(elt.getAccessibility() != IProgramElement.Accessibility.PRIVATE) {
                            itdMethods.add(createMethod(elt, type, handle));
                        }
                    }
                    
                } else if (elt.getKind() == IProgramElement.Kind.INTER_TYPE_CONSTRUCTOR) {
                    if(elt.getAccessibility() != IProgramElement.Accessibility.PRIVATE) {
                        itdMethods.add(createConstructor(elt, type));
                    }
                } else if (elt.getKind() == IProgramElement.Kind.INTER_TYPE_FIELD) {
                    // XXX hmmmm..should I also skip this if the type is an interface???
                    if(elt.getAccessibility() != IProgramElement.Accessibility.PRIVATE) {
                        itdFields.add(createField(elt, type));
                    }
                } else if (elt.getKind() == IProgramElement.Kind.DECLARE_PARENTS) {
                    // XXX this is wrong.  Can't tell by looking at the text, must determine if it is class or interface through the model 
                    String details = elt.getDetails();
                    boolean isExtends = details != null && details.startsWith("extends");
                    if (elt.getParentTypes() != null && elt.getParentTypes().size() > 0) {
                        if (isExtends && TypeDeclaration.kind(type.modifiers) == TypeDeclaration.CLASS_DECL) {
                            addSuperClass(elt, type);
                        } else {
                            addSuperInterfaces(elt, type);
                        }
                    }
                } else if (elt.getKind() == IProgramElement.Kind.CLASS) {
                    // this is an ITIT - intertype inner type
//                    IType ititModel = unit.getJavaProject().findType(elt.getFullyQualifiedName(), (IProgressMonitor) null);
                    TypeDeclaration ititAST = createITIT(elt.getName(), type);
                    if (ititAST != null) {
                        itits.add(ititAST);
                    }
                } else if (elt.getKind() == IProgramElement.Kind.ASPECT) {
                    // probably an instantiation of a declare parents relationship from an abstact aspect
                    Map<String, List<String>> parentsMap = elt.getDeclareParentsMap();
                    if (parentsMap != null && type.binding != null && type.binding.compoundName != null) {
                        List<String> parents = parentsMap.get(String.valueOf(CharOperation.concatWith(type.binding.compoundName, '.')));
                        List<String> interfacesToAdd = new LinkedList<String>();
                        for (String parent : parents) {
                            try {
                                IType parentElt = unit.getJavaProject().findType(parent, (IProgressMonitor) null);
                                if (parentElt != null && parentElt.isClass()) {
                                    addSuperClass(parent, type);
                                } else if (parentElt != null && parentElt.isInterface()) {
                                    interfacesToAdd.add(parent);
                                }
                            } catch (JavaModelException e) { }
                        }
                        addSuperInterfaces(interfacesToAdd, type);
                    }
                }
            }
            
            if (ipes.size() > 0) {
                origMap.put(type, orig);
                
                // now add the ITDs into the declaration
                if (itdFields.size() > 0) {
                    int numFields = type.fields == null ? 0 : type.fields.length;
                    FieldDeclaration[] fields = new FieldDeclaration[numFields + itdFields.size()];
                    if (numFields > 0) {
                        System.arraycopy(type.fields, 0, fields, 0, numFields);
                    }
                    for (int i = 0; i < itdFields.size(); i++) {
                        fields[i + numFields] = itdFields.get(i);
                    }
                    type.fields = fields;
                }
                if (itdMethods.size() > 0) {
                    int numMethods = type.methods == null ? 0 : type.methods.length;
                    AbstractMethodDeclaration[] methods = new AbstractMethodDeclaration[numMethods + itdMethods.size()];
                    if (numMethods > 0) {
                        System.arraycopy(type.methods, 0, methods, 0, numMethods);
                    }
                    for (int i = 0; i < itdMethods.size(); i++) {
                        methods[i + numMethods] = itdMethods.get(i);
                    }
                    type.methods = methods;
                }
                if (itits.size() > 0) {
                    int numInners = type.memberTypes == null ? 0 : type.memberTypes.length;
                    TypeDeclaration[] inners = new TypeDeclaration[numInners + itits.size()];
                    if (numInners > 0) {
                        System.arraycopy(type.methods, 0, inners, 0, numInners);
                    }
                    for (int i = 0; i < itits.size(); i++) {
                        inners[i + numInners] = itits.get(i);
                    }
                    type.memberTypes = inners;
                }
            }
        } catch (Exception e) {
            // back out what we have done
            origMap.remove(type);
            revertType(type, orig);
        }
    }
    
    private TypeDeclaration createITIT(String name, TypeDeclaration enclosing) {
        TypeDeclaration decl = new TypeDeclaration(enclosing.compilationResult);
        decl.enclosingType = enclosing;
        decl.name = name.toCharArray();
        ClassScope innerClassScope = new ClassScope(enclosing.scope, decl);
        decl.binding = new MemberTypeBinding(new char[][] { enclosing.name, name.toCharArray()}, innerClassScope, enclosing.binding);
        decl.staticInitializerScope = enclosing.staticInitializerScope;
        decl.initializerScope = enclosing.initializerScope;
        decl.scope = innerClassScope;
        decl.binding.superInterfaces = new ReferenceBinding[0];
        decl.binding.typeVariables = new TypeVariableBinding[0];
        
        return decl;
    }

    private FieldDeclaration createField(IProgramElement field, TypeDeclaration type) {
        FieldDeclaration decl = new FieldDeclaration();
        decl.name = field.getName().split("\\.")[1].toCharArray();
        decl.type = createTypeReference(field.getCorrespondingType(true));
        decl.modifiers = field.getRawModifiers();
        return decl;
    }
    
    private MethodDeclaration createMethod(IProgramElement method, TypeDeclaration type, IType handle) {
        MethodDeclaration decl = new MethodDeclaration(type.compilationResult);
        decl.scope = new MethodScope(type.scope, decl, true);
        
        decl.selector = method.getName().split("\\.")[1].toCharArray();
        decl.modifiers = method.getRawModifiers();
        

        
        decl.returnType = createTypeReference(method.getCorrespondingType(true));
        decl.modifiers = method.getRawModifiers();
        Argument[] args = method.getParameterTypes() != null ? 
                new Argument[method.getParameterTypes().size()] :
                    new Argument[0];
        try {
            AJWorldFacade world = new AJWorldFacade(handle.getJavaProject().getProject());
            ErasedTypeSignature sig = world.getTypeParameters(Signature.createTypeSignature(handle.getFullyQualifiedName(), true), method);
            if (sig == null) {
                String[] params = new String[method.getParameterTypes().size()];
                for (int i = 0; i < params.length; i++) {
                    params[i] = new String(Signature.getTypeErasure((char[]) method.getParameterTypes().get(i)));
                }
                sig = new ErasedTypeSignature(method.getCorrespondingType(true), params);
            }
            
            List<String> pNames = method.getParameterNames();
            // bug 270123... no parameter names if coming in from a jar and
            // not build with debug info...mock it up.
            if (pNames == null || pNames.size() != args.length) {
                pNames = new ArrayList<String>(args.length);
                for (int i = 0; i < args.length; i++) {
                    pNames.add("args" + i);
                }
            }
            for (int i = 0; i < args.length; i++) {
                args[i] = new Argument(((String) pNames.get(i)).toCharArray(),
                        0,
                        createTypeReference(Signature.getElementType(sig.paramTypes[i])),
                        0);
            }
        } catch (Exception e) {
            AJLog.log("Exception occurred in ITDInserter.createMethod().  (Ignoring)");
            AJLog.log("Relevant method: " + method.getParent().getName() + "." + method.getName());
            List<String> pNames = method.getParameterNames();
            // bug 270123... no parameter names if coming in from a jar and
            // not build with debug info...mock it up.
            if (pNames == null || pNames.size() != args.length) {
                pNames = new ArrayList<String>(args.length);
                for (int i = 0; i < args.length; i++) {
                    pNames.add("args" + i);
                }
            }
            for (int i = 0; i < args.length; i++) {
                args[i] = new Argument(((String) pNames.get(i)).toCharArray(),
                        0,
                        createTypeReference(new String((char[]) method.getParameterTypes().get(i))),
                        0);
            }
        }
        decl.arguments = args;
        return decl;
    }
    
    private ConstructorDeclaration createConstructor(IProgramElement constructor, TypeDeclaration type) {
        ConstructorDeclaration decl = new ConstructorDeclaration(type.compilationResult);
        decl.scope = new MethodScope(type.scope, decl, true);
        decl.selector = constructor.getName().split("\\.")[1].toCharArray();
        decl.modifiers = constructor.getRawModifiers();
        Argument[] args = constructor.getParameterTypes() != null ? 
                new Argument[constructor.getParameterTypes().size()] :
                    new Argument[0];

        List<String> pNames = constructor.getParameterNames();
        // bug 270123, bug 334328... no parameter names if coming in from a jar and
        // not build with debug info...mock it up.
        if (pNames == null || pNames.size() != args.length) {
            pNames = new ArrayList<String>(args.length);
            for (int i = 0; i < args.length; i++) {
                pNames.add("args" + i);
            }
        }
         
       for (int i = 0; i < args.length; i++) {
            args[i] = new Argument(pNames.get(i).toCharArray(),
                    0,
                    createTypeReference(new String((char[]) constructor.getParameterTypes().get(i))),
                    0);
        }
        decl.arguments = args;
        return decl;
    }
    
    private void addSuperClass(IProgramElement ipe, TypeDeclaration decl) {
        List<String> types = ipe.getParentTypes();
        if (types == null || types.size() < 1) return;
        
        String typeName = types.get(0);
        addSuperClass(typeName, decl); 
    }

    private void addSuperClass(String newSuper, TypeDeclaration decl) {
        newSuper = newSuper.replaceAll("\\$", "\\.");
        decl.superclass = createTypeReference(newSuper);
    }
    
    private void addSuperInterfaces(IProgramElement ipe, TypeDeclaration decl) {
        List<String> newInterfaces = ipe.getParentTypes();
        addSuperInterfaces(newInterfaces, decl);
    }

    /**
     * @param newInterfaces
     * @param decl
     */
    private void addSuperInterfaces(List<String> newInterfaces,
            TypeDeclaration decl) {
        if (newInterfaces != null) {
            int superInterfacesNum = decl.superInterfaces == null ? 0 : decl.superInterfaces.length;
            TypeReference[] refs = new TypeReference[superInterfacesNum + newInterfaces.size()];
            if (superInterfacesNum > 0) {
                System.arraycopy(decl.superInterfaces, 0, refs, 0, decl.superInterfaces.length);
            }
            for (int i = 0; i < refs.length-superInterfacesNum; i++) {
                refs[i + superInterfacesNum] = createTypeReference(newInterfaces.get(i).replaceAll("\\$", "\\."));
            }
            decl.superInterfaces = refs;
        }
    }
    

    // Do it this way in order to ensure that Aspects are returned as AspectElements
    private IType getHandle(TypeDeclaration decl) {
        String typeName = new String(decl.name);
        try {
            IJavaElement maybeType = unit.getElementAt(decl.sourceStart);
            if (maybeType != null && maybeType.getElementType() == IJavaElement.TYPE) {
                return (IType) maybeType;
            }
        } catch (JavaModelException e) {
        }
        try {
            // try getting by name
            IType type = getHandleFromChild(typeName, unit);
            if (type != null) {
                return type;
            }
        } catch (JavaModelException e) {
        }
        // this type does not exist, but create a mock one anyway
        return unit.getType(typeName);
    }
    
    private IType getHandleFromChild(String typeName, IParent parent) 
            throws JavaModelException {
        IJavaElement[] children = parent.getChildren();
        for (int i = 0; i < children.length; i++) {
            if ((children[i].getElementType() == IJavaElement.TYPE) &&
                    typeName.equals(children[i].getElementName())) {
                return (IType) children[i];
            }
        }
        for (int i = 0; i < children.length; i++) {
            if (children[i].getElementType() == IJavaElement.TYPE ||
                    children[i].getElementType() == IJavaElement.METHOD) {
                IType type = getHandleFromChild(typeName, (IParent) children[i]);
                if (type != null) {
                    return type;
                }
            }
        }
        return null;
    }
    
    private List<IProgramElement> getITDs(IType handle) {
        if (model.hasModel()) {
            if (model.hasProgramElement(handle)) {
                List<IJavaElement> rels = model
                        .getRelationshipsForElement(handle,
                                AJRelationshipManager.ASPECT_DECLARATIONS);
                List<IProgramElement> elts = new ArrayList<IProgramElement>();
                for (IJavaElement je : rels) {
                    IProgramElement declareElt = model
                            .javaElementToProgramElement(je);
                    elts.add(declareElt);
                }
                return elts;
            }
        }
        return Collections.emptyList();
    }
    
    private TypeReference createTypeReference(String origTypeName) {
        // remove any references to #RAW
        if (origTypeName .endsWith("#RAW")) {
            origTypeName = origTypeName.substring(0, origTypeName.length()-4);
        }
        // can't use the binary name
        origTypeName = origTypeName.replace('$', '.');
        
        return typeConverter.createTypeReference(origTypeName.toCharArray());
    }

    
    /**
     * replaces type declarations with their original contents after the compilation is
     * complete
     */
    public void revert() {
        for (Map.Entry<TypeDeclaration, OrigContents> entry : origMap.entrySet()) {
            revertType(entry.getKey(), entry.getValue());
        }
    }

    private void revertType(TypeDeclaration type, OrigContents orig) {
        type.methods = orig.methods;
        type.fields = orig.fields;
        type.superclass = orig.superClass;
        type.superInterfaces = orig.superInterfaces;
        type.memberTypes = orig.memberTypes;
    }
}
