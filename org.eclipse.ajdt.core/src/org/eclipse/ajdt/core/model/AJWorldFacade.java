/*******************************************************************************
 * Copyright (c) 2009 SpringSource and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Andrew Eisenberg - initial API and implementation
 *******************************************************************************/

package org.eclipse.ajdt.core.model;

import java.util.Iterator;
import java.util.List;

import org.aspectj.ajde.core.AjCompiler;
import org.aspectj.ajdt.internal.core.builder.AjBuildManager;
import org.aspectj.ajdt.internal.core.builder.AjState;
import org.aspectj.ajdt.internal.core.builder.IncrementalStateManager;
import org.aspectj.asm.IProgramElement;
import org.aspectj.weaver.ConcreteTypeMunger;
import org.aspectj.weaver.Member;
import org.aspectj.weaver.ResolvedMember;
import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.UnresolvedType;
import org.aspectj.weaver.World;
import org.eclipse.ajdt.core.AspectJPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.Signature;

/**
 * @author Andrew Eisenberg
 * @created Jan 13, 2009
 * 
 * This class provides AJDT access to the {@link World} object.
 * Clients must not hold a reference to any world object 
 * for longer than necessary
 */
public final class AJWorldFacade {
    private final AjBuildManager manager;
    private final World world;
    
    public AJWorldFacade(IProject project) {
        AjCompiler compiler = AspectJPlugin.getDefault().getCompilerFactory().getCompilerForProject(project);
        AjState state = IncrementalStateManager.retrieveStateFor(compiler.getId());
        if (state != null) {
            manager = state.getAjBuildManager();
            world = manager.getWorld();
        } else {
            manager = null;
            world = null;
        }
    }
    
    public ErasedTypeSignature getTypeParameters(String typeSignature, IProgramElement elt) {
        ResolvedType type = world.getCoreType(UnresolvedType.forSignature(typeSignature));
        if (type == null || type.isMissing()) {
            return null;
        }
        List itds = type.getInterTypeMungersIncludingSupers();
        ConcreteTypeMunger myMunger = null;
        for (Iterator iterator = itds.iterator(); iterator.hasNext();) {
            ConcreteTypeMunger munger = (ConcreteTypeMunger) iterator.next();
            if (equalSignatures(elt, munger)) {
                myMunger = munger;
                break;
            }
        }
        if (myMunger == null) {
            return null;
        }
        
        String returnType = myMunger.getSignature().getReturnType().getErasureSignature();
        returnType = returnType.replaceAll("/", "\\.");
        returnType = Signature.toString(returnType);
        UnresolvedType[] parameterTypes = myMunger.getSignature().getParameterTypes();
        String[] parameterTypesStr = new String[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypesStr[i] = parameterTypes[i].getErasureSignature();
            parameterTypesStr[i] = parameterTypesStr[i].replaceAll("/", "\\.");
            parameterTypesStr[i] = Signature.toString(parameterTypesStr[i]);
        }
        return new ErasedTypeSignature(returnType, parameterTypesStr);
    }

    private boolean equalSignatures(IProgramElement elt, ConcreteTypeMunger munger) {
        try {
            return equalNames(elt, munger) && equalParams(elt, munger);
        } catch (NullPointerException e) {
            // lots of things can be null
            return false;
        }
    }

    private boolean equalNames(IProgramElement elt, ConcreteTypeMunger munger) {
        ResolvedMember signature = munger.getSignature();
        return signature != null && (signature.getDeclaringType().getBaseName() +"."+signature.getName()).equals(
                elt.getPackageName() + "." + elt.getName());
    }

    private boolean equalParams(IProgramElement elt, ConcreteTypeMunger munger) {
        UnresolvedType[] unresolvedTypes = munger.getSignature().getParameterTypes();
        List eltTypes = elt.getParameterTypes();
        int unresolvedTypesLength = unresolvedTypes == null ? 0 : unresolvedTypes.length;
        int eltTypesLength = eltTypes == null ? 0 : eltTypes.size();
        if (unresolvedTypesLength != eltTypesLength) {
            return false;
        }
        for (int i = 0; i < unresolvedTypesLength; i++) {
            String eltParamType = new String( (char[]) eltTypes.get(i));
            int genericStart = eltParamType.indexOf('<');
            if (genericStart > -1) {
                eltParamType = eltParamType.substring(0, genericStart);
            }
            String unresolvedTypeName = unresolvedTypes[i].getName();
            genericStart = unresolvedTypeName.indexOf('<');
            if (genericStart > -1) {
                unresolvedTypeName = unresolvedTypeName.substring(0, genericStart);
            }
            if (! unresolvedTypeName.equals(eltParamType)) {
                return false;
            }
        }
        return true;
    }
    
    
    public static class ErasedTypeSignature {
        public ErasedTypeSignature(String returnType, String[] paramTypes) {
            super();
            this.returnType = returnType;
            this.paramTypes = paramTypes;
        }
        public final String returnType;
        public final String[] paramTypes;
        
    }
}
