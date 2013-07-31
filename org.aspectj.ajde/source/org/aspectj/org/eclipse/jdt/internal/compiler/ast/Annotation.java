/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Herrmann - Contributions for
 *								bug 186342 - [compiler][null] Using annotations for null checking
 *								bug 365662 - [compiler][null] warn on contradictory and redundant null annotations
 *								bug 331649 - [compiler][null] consider null annotations for fields
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/
package org.aspectj.org.eclipse.jdt.internal.compiler.ast;

import java.util.Stack;

import org.aspectj.org.eclipse.jdt.core.compiler.CharOperation;
import org.aspectj.org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.aspectj.org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.aspectj.org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.aspectj.org.eclipse.jdt.internal.compiler.impl.Constant;
import org.aspectj.org.eclipse.jdt.internal.compiler.impl.IrritantSet;
import org.aspectj.org.eclipse.jdt.internal.compiler.lookup.*;

/**
 * Annotation
 */
public abstract class Annotation extends Expression {

	/**
	 * Return the location for the corresponding annotation inside the type reference, <code>null</code> if none.
	 */
	public static int[] getLocations(
			final TypeReference reference,
			final Annotation[] primaryAnnotation,
			final Annotation annotation,
			final Annotation[][] annotationsOnDimensionsOnExpression,
			final int dimensions) {
	
		class LocationCollector extends ASTVisitor {
			Stack typePathEntries;
			Annotation currentAnnotation;
			boolean search = true;
			
			public LocationCollector(Annotation currentAnnotation) {
				this.typePathEntries = new Stack();
				this.currentAnnotation = currentAnnotation;
			}
			
			public boolean visit(ParameterizedSingleTypeReference typeReference, BlockScope scope) {
				if (!this.search) return false;
								
				Annotation[][] annotationsOnDimensions = typeReference.annotationsOnDimensions;
				if (annotationsOnDimensions != null) {
					for (int i = 0, max = annotationsOnDimensions.length; i < max; i++) {
						Annotation[] annotations = annotationsOnDimensions[i];
						if (annotations != null) {
							for (int j = 0, max2 = annotations.length; j < max2; j++) {
								Annotation current = annotations[j];
								if (current == this.currentAnnotation) {
									// found it, push any relevant type path entries
									for (int k = 0; k < i; k++) {
										this.typePathEntries.push(TYPE_PATH_ELEMENT_ARRAY);
									}
									this.search = false;
									return false;
								}
							}
						}
						
					}
				}
				
				// Example cases handled here: @B(1) List<String>[]
				Annotation[][] annotations = typeReference.annotations;
				if (annotations == null) {
					annotations = new Annotation[][] { primaryAnnotation };
				}
				int annotationsLevels = annotations.length;
				for (int i = 0; i < annotationsLevels; i++) {
					Annotation [] current = annotations[i];
					int annotationsLength = current == null ? 0 : current.length;
					for (int j = 0; j < annotationsLength; j++) {
						if (current[j] == this.currentAnnotation) {
							this.search = false;
							// Found it, insert any necessary type path elements
							for (int k = 0; k < typeReference.dimensions; k++) {
								this.typePathEntries.push(TYPE_PATH_ELEMENT_ARRAY);
							}
							return false;
						}
					}
				}
				
				// If a type argument is annotated it is necessary jump past the array elements
				if (typeReference.dimensions != 0) {
					for (int k = 0, maxk = typeReference.dimensions; k < maxk; k++) {
						this.typePathEntries.push(TYPE_PATH_ELEMENT_ARRAY);
					}
				}
				TypeReference[] typeReferences = typeReference.typeArguments;
				for (int i = 0, max = typeReferences.length; i < max; i++) {
					this.typePathEntries.add(new int[]{3,i});
					typeReferences[i].traverse(this, scope);
					if (!this.search) {
						return false;
					} else {
						this.typePathEntries.pop();
					}
				}
				if (typeReference.dimensions != 0) {
					for (int k = 0, maxk = typeReference.dimensions; k < maxk; k++) {
						this.typePathEntries.pop();
					}					
				}
				return true;
			}

			public boolean visit(SingleTypeReference typeReference, BlockScope scope) {
				if (!this.search) return false;

				// depth allows for the syntax "outerInstance.new @A InnerType();"
				int depth = 0;
				if (typeReference.resolvedType instanceof ReferenceBinding) {
					depth = getInnerDepth((ReferenceBinding)typeReference.resolvedType);
				}
				
				if (dimensions != 0) {
					for (int k = 0; k < dimensions; k++) {
						this.typePathEntries.push(TYPE_PATH_ELEMENT_ARRAY);
					}
				}
				Annotation[][] annotations = typeReference.annotations;
				int annotationsLevels = annotations == null ? 0 : annotations.length;
				for (int i = 0; i < annotationsLevels; i++) {
					Annotation [] current = annotations[i];
					int annotationsLength = current == null ? 0 : current.length;
					for (int j = 0; j < annotationsLength; j++) {
						if (current[j] == this.currentAnnotation) {
							// Found
							this.search = false;
							if (depth != 0) {
								for (int k = 0; k<depth; k++) {
									this.typePathEntries.add(TYPE_PATH_INNER_TYPE);
								}
							}
							return false;
						}
					}
				}
				if (dimensions != 0) {
					for (int k = 0; k < dimensions; k++) {
						this.typePathEntries.pop();
					}
				}
				return false;
			}

			public boolean visit(ArrayTypeReference typeReference, BlockScope scope) {
				if (!this.search) return false;
				
				Annotation[][] annotationsOnDimensions = typeReference.annotationsOnDimensions;
				if (annotationsOnDimensions != null) {
					for (int i = 0, max = annotationsOnDimensions.length; i < max; i++) {
						Annotation[] annotations = annotationsOnDimensions[i];
						if (annotations != null) {
							for (int j = 0, max2 = annotations.length; j < max2; j++) {
								Annotation current = annotations[j];
								if (current == this.currentAnnotation) {
									for (int k = 0; k < i; k++) {
										this.typePathEntries.push(TYPE_PATH_ELEMENT_ARRAY);
									}
									this.search = false;
									return false;
								}
							}
						}
						
					}
				}
				Annotation[][] annotations = typeReference.annotations;
				if (annotations == null) {
					annotations = new Annotation[][] { primaryAnnotation };
				}
				int annotationsLevels = annotations.length;
				for (int i = 0; i < annotationsLevels; i++) {
					Annotation [] current = annotations[i];
					int annotationsLength = current == null ? 0 : current.length;
					for (int j = 0; j < annotationsLength; j++) {
						if (current[j] == this.currentAnnotation) {
							for (int k = 0, maxk=typeReference.dimensions; k < maxk; k++) {
								this.typePathEntries.push(TYPE_PATH_ELEMENT_ARRAY);
							}
							this.search = false;
							return false;
						}
					}
				}
				return true;
			}
			
			public boolean visit(ArrayQualifiedTypeReference typeReference, BlockScope scope) {
				if (!this.search) return false;
				Annotation[][] annotationsOnDimensions = typeReference.annotationsOnDimensions;
				if (annotationsOnDimensions != null) {
					for (int i = 0, max = annotationsOnDimensions.length; i < max; i++) {
						Annotation[] annotations = annotationsOnDimensions[i];
						if (annotations != null) {
							for (int j = 0, max2 = annotations.length; j < max2; j++) {
								Annotation current = annotations[j];
								if (current == this.currentAnnotation) {
									this.search = false;
									// Found it, insert relevant type path elements
									for (int k = 0, maxk = i; k < maxk; k++) {
										this.typePathEntries.push(TYPE_PATH_ELEMENT_ARRAY);
									}
									return false;
								}
							}
						}
					}
				}
				Annotation[][] annotations = typeReference.annotations;
				if (annotations == null) {
					annotations = new Annotation[][] { primaryAnnotation };
				}
				int annotationsLevels = annotations.length;
				for (int i = 0; i < annotationsLevels; i++) {
					Annotation [] current = annotations[i];
					int annotationsLength = current == null ? 0 : current.length;
					for (int j = 0; j < annotationsLength; j++) {
						if (current[j] == this.currentAnnotation) {
							this.search = false;
							for (int k = 0, maxk=typeReference.dimensions; k < maxk; k++) {
								this.typePathEntries.push(TYPE_PATH_ELEMENT_ARRAY);
							}
							return false;
						}
					}
				}
				return true;
			}
			
			public boolean visit(ParameterizedQualifiedTypeReference typeReference, BlockScope scope) {
				if (!this.search) return false;
				
				// Example case handled by this block: java.util.List<String>[]@A[]
				Annotation[][] annotationsOnDimensions = typeReference.annotationsOnDimensions;
				if (annotationsOnDimensions != null) {
					for (int i = 0, max = annotationsOnDimensions.length; i < max; i++) {
						Annotation[] annotations = annotationsOnDimensions[i];
						if (annotations != null) {
							for (int j = 0, max2 = annotations.length; j < max2; j++) {
								Annotation current = annotations[j];
								if (current == this.currentAnnotation) {
									this.search = false;
									// Found it, insert relevant type path elements
									for (int k = 0, maxk = i; k < maxk; k++) {
										this.typePathEntries.push(TYPE_PATH_ELEMENT_ARRAY);
									}
									return false;
								}
							}
						}
					}
				}

				boolean[] needsInnerEntryInfo = computeInnerEntryInfo(typeReference);

				// Example cases handled by this block:
				// java.util.@A List<String>[][], com.demo.@A Outer.@B Inner<String>, java.util.Map.@A Entry<String,String>
				Annotation[][] annotations = typeReference.annotations;
				if (annotations == null) {
					annotations = new Annotation[][] { primaryAnnotation };
				}
				int annotationsLevels = annotations.length;
				for (int i = 0; i < annotationsLevels; i++) {
					Annotation [] current = annotations[i];
					int annotationsLength = current == null ? 0 : current.length;
					for (int j = 0; j < annotationsLength; j++) {
						if (current[j] == this.currentAnnotation) {
							this.search = false;
							// Found, insert any relevant type path elements
							for (int k = 0, maxk = typeReference.dimensions; k < maxk; k++) {
								this.typePathEntries.push(TYPE_PATH_ELEMENT_ARRAY);
							}
							// Found, insert any relevant type path elements
							if (needsInnerEntryInfo != null) {
								for (int k = 0; k <= i; k++) {
									if (needsInnerEntryInfo[k]) {
										this.typePathEntries.push(TYPE_PATH_INNER_TYPE);
									}
								}
							}
							return false;
						}
					}
				}
				
				// Example cases handled by this block:
				// java.util.List<@A String>
				if (typeReference.dimensions != 0) {
					for (int k = 0, maxk = typeReference.dimensions; k < maxk; k++) {
						this.typePathEntries.push(TYPE_PATH_ELEMENT_ARRAY);
					}
				}
				int toPop = 0;
				for (int i = 0, max = typeReference.typeArguments.length; i < max; i++) {
					TypeReference[] typeArgumentsForComponent = typeReference.typeArguments[i];
					if (needsInnerEntryInfo != null && needsInnerEntryInfo[i]) { 
						this.typePathEntries.push(TYPE_PATH_INNER_TYPE);
						toPop++;
					}
					if (typeArgumentsForComponent != null) {
						for (int j = 0, max2 = typeArgumentsForComponent.length; j < max2; j++) {
							this.typePathEntries.push(new int[]{3,j});
							typeArgumentsForComponent[j].traverse(this,scope);
							if (!this.search) return false;
							this.typePathEntries.pop();
						}
					}
				}
				toPop += typeReference.dimensions;
				for (int k = 0, maxk = toPop; k < maxk; k++) {
					this.typePathEntries.pop();
				}
				return true;
			}
			
			public boolean visit(Wildcard typeReference, BlockScope scope) {
				if (!this.search) return false;
				TypeReference bound = typeReference.bound;
				this.typePathEntries.push(TYPE_PATH_ANNOTATION_ON_WILDCARD_BOUND);
				bound.traverse(this, scope);
				if (!this.search) {
					return false;
				}
				this.typePathEntries.pop();
				return true;
			}
			
			private boolean[] computeInnerEntryInfo(QualifiedTypeReference typeReference) {
				ReferenceBinding resolvedType = (ReferenceBinding) 
						(typeReference.resolvedType instanceof ArrayBinding ? typeReference.resolvedType.leafComponentType() : typeReference.resolvedType);
				boolean[] needsInnerEntryInfo = null;
				if (resolvedType != null && resolvedType.isNestedType()) {
					// Work backwards computing whether a INNER_TYPE entry is required for each level
					needsInnerEntryInfo = new boolean[typeReference.tokens.length];
					int counter = needsInnerEntryInfo.length - 1;
					ReferenceBinding type = resolvedType;//resolvedType.enclosingType();
					while (type != null) {
						needsInnerEntryInfo[counter--] = !type.isStatic();
						type = type.enclosingType();
					}
				}
				return needsInnerEntryInfo;
			}
			
			private int getInnerDepth(ReferenceBinding resolvedType) {
				int depth = 0;
				ReferenceBinding type = resolvedType;
				while (type != null) {
					depth += (type.isStatic())?0:1;
					type = type.enclosingType();
				}
				return depth;
			}
			
			public boolean visit(QualifiedTypeReference typeReference, BlockScope scope) {
				if (!this.search) return false;
				boolean[] needsInnerEntryInfo = computeInnerEntryInfo(typeReference);
				
				// Example cases handled by this block:
				// java.util.@A List, com.demo.@A Outer.@B Inner, java.util.Map.@A Entry
				Annotation[][] annotations = typeReference.annotations;
				if (annotations == null) {
					annotations = new Annotation[][] { primaryAnnotation };
				}
				int annotationsLevels = annotations.length;
				for (int i = 0; i < annotationsLevels; i++) {
					Annotation [] current = annotations[i];
					int annotationsLength = current == null ? 0 : current.length;
					for (int j = 0; j < annotationsLength; j++) {
						if (current[j] == this.currentAnnotation) {
							this.search = false;
							// Found, insert any relevant type path elements
							if (needsInnerEntryInfo != null) {
								for (int k = 0; k <= i; k++) {
									if (needsInnerEntryInfo[k]) {
										this.typePathEntries.push(TYPE_PATH_INNER_TYPE);
									}
								}
							}
							return false;
						}
					}
				}
				return true;
			}
			
			public String toString() {
				StringBuffer buffer = new StringBuffer();
				buffer
					.append("search location for ") //$NON-NLS-1$
					.append(this.currentAnnotation)
					.append("\ncurrent type_path entries : "); //$NON-NLS-1$
				for (int i = 0, maxi = this.typePathEntries.size(); i < maxi; i++) {
					int[] typePathEntry = (int[]) this.typePathEntries.get(i);
					buffer
						.append('(')
						.append(typePathEntry[0])
						.append(',')
						.append(typePathEntry[1])
						.append(')');
				}
				return String.valueOf(buffer);
			}
		}
		if (reference == null) return null;
		LocationCollector collector = new LocationCollector(annotation);
		reference.traverse(collector, (BlockScope) null);
		if (collector.typePathEntries.isEmpty()) {
			return null;
		}
		int size = collector.typePathEntries.size();
		int[] result = new int[size*2];
		int offset=0;
		for (int i = 0; i < size; i++) {
			int[] pathElement = (int[])collector.typePathEntries.get(i);
			result[offset++] = pathElement[0];
			result[offset++] = pathElement[1];
		}
		return result;
	}
		
    // jsr 308
	public static class TypeUseBinding extends ReferenceBinding {
		private int kind;
		public TypeUseBinding(int kind) {
			this.tagBits = 0L;
			this.kind = kind;
		}
		public int kind() {
			return this.kind;
		}
		public boolean hasTypeBit(int bit) {
			return false;
		}
	}

	final static MemberValuePair[] NoValuePairs = new MemberValuePair[0];
	private static final long TAGBITS_NULLABLE_OR_NONNULL = TagBits.AnnotationNullable|TagBits.AnnotationNonNull;

	static final int[] TYPE_PATH_ELEMENT_ARRAY = new int[]{0,0};
	static final int[] TYPE_PATH_INNER_TYPE = new int[]{1,0};
	static final int[] TYPE_PATH_ANNOTATION_ON_WILDCARD_BOUND = new int[]{2,0};
	
	public int declarationSourceEnd;
	public Binding recipient;

	public TypeReference type;
	/**
	 *  The representation of this annotation in the type system.
	 */
	private AnnotationBinding compilerAnnotation = null;

	public static long getRetentionPolicy(char[] policyName) {
		if (policyName == null || policyName.length == 0)
			return 0;
		switch(policyName[0]) {
			case 'C' :
				if (CharOperation.equals(policyName, TypeConstants.UPPER_CLASS))
					return TagBits.AnnotationClassRetention;
				break;
			case 'S' :
				if (CharOperation.equals(policyName, TypeConstants.UPPER_SOURCE))
					return TagBits.AnnotationSourceRetention;
				break;
			case 'R' :
				if (CharOperation.equals(policyName, TypeConstants.UPPER_RUNTIME))
					return TagBits.AnnotationRuntimeRetention;
				break;
		}
		return 0; // unknown
	}

	public static long getTargetElementType(char[] elementName) {
		if (elementName == null || elementName.length == 0)
			return 0;
		switch(elementName[0]) {
			case 'A' :
				if (CharOperation.equals(elementName, TypeConstants.UPPER_ANNOTATION_TYPE))
					return TagBits.AnnotationForAnnotationType;
				break;
			case 'C' :
				if (CharOperation.equals(elementName, TypeConstants.UPPER_CONSTRUCTOR))
					return TagBits.AnnotationForConstructor;
				break;
			case 'F' :
				if (CharOperation.equals(elementName, TypeConstants.UPPER_FIELD))
					return TagBits.AnnotationForField;
				break;
			case 'L' :
				if (CharOperation.equals(elementName, TypeConstants.UPPER_LOCAL_VARIABLE))
					return TagBits.AnnotationForLocalVariable;
				break;
			case 'M' :
				if (CharOperation.equals(elementName, TypeConstants.UPPER_METHOD))
					return TagBits.AnnotationForMethod;
				break;
			case 'P' :
				if (CharOperation.equals(elementName, TypeConstants.UPPER_PARAMETER))
					return TagBits.AnnotationForParameter;
				else if (CharOperation.equals(elementName, TypeConstants.UPPER_PACKAGE))
					return TagBits.AnnotationForPackage;
				break;
			case 'T' :
				if (CharOperation.equals(elementName, TypeConstants.TYPE))
					return TagBits.AnnotationForType;
				if (CharOperation.equals(elementName, TypeConstants.TYPE_USE_TARGET))
					return TagBits.AnnotationForTypeUse;
				if (CharOperation.equals(elementName, TypeConstants.TYPE_PARAMETER_TARGET))
					return TagBits.AnnotationForTypeParameter;
				break;
		}
		return 0; // unknown
	}

	public ElementValuePair[] computeElementValuePairs() {
		return Binding.NO_ELEMENT_VALUE_PAIRS;
	}

	/**
	 * Compute the bit pattern for recognized standard annotations the compiler may need to act upon
	 */
	private long detectStandardAnnotation(Scope scope, ReferenceBinding annotationType, MemberValuePair valueAttribute) {
		long tagBits = 0;
		switch (annotationType.id) {
			// retention annotation
			case TypeIds.T_JavaLangAnnotationRetention :
				if (valueAttribute != null) {
					Expression expr = valueAttribute.value;
					if ((expr.bits & Binding.VARIABLE) == Binding.FIELD) {
						if (expr instanceof Reference) { // New AspectJ Extension (pr148537)
						FieldBinding field = ((Reference)expr).fieldBinding();
						if (field != null && field.declaringClass.id == T_JavaLangAnnotationRetentionPolicy) {
							tagBits |= getRetentionPolicy(field.name);
						}
						} // New AspectJ Extension - end of if()
					}
				}
				break;
			// target annotation
			case TypeIds.T_JavaLangAnnotationTarget :
				tagBits |= TagBits.AnnotationTarget; // target specified (could be empty)
				if (valueAttribute != null) {
					Expression expr = valueAttribute.value;
					if (expr instanceof ArrayInitializer) {
						ArrayInitializer initializer = (ArrayInitializer) expr;
						final Expression[] expressions = initializer.expressions;
						if (expressions != null) {
							for (int i = 0, length = expressions.length; i < length; i++) {
								Expression initExpr = expressions[i];
								if ((initExpr.bits & Binding.VARIABLE) == Binding.FIELD) {
									FieldBinding field = ((Reference) initExpr).fieldBinding();
									if (field != null && field.declaringClass.id == T_JavaLangAnnotationElementType) {
										long element = getTargetElementType(field.name);
										if ((tagBits & element) != 0) {
											scope.problemReporter().duplicateTargetInTargetAnnotation(annotationType, (NameReference)initExpr);
										} else {
											tagBits |= element;
										}
									}
								}
							}
						}
					} else if ((expr.bits & Binding.VARIABLE) == Binding.FIELD) {
						FieldBinding field = ((Reference) expr).fieldBinding();
						if (field != null && field.declaringClass.id == T_JavaLangAnnotationElementType) {
							tagBits |= getTargetElementType(field.name);
						}
					}
				}
				break;
			// marker annotations
			case TypeIds.T_JavaLangDeprecated :
				tagBits |= TagBits.AnnotationDeprecated;
				break;
			case TypeIds.T_JavaLangAnnotationDocumented :
				tagBits |= TagBits.AnnotationDocumented;
				break;
			case TypeIds.T_JavaLangAnnotationInherited :
				tagBits |= TagBits.AnnotationInherited;
				break;
			case TypeIds.T_JavaLangOverride :
				tagBits |= TagBits.AnnotationOverride;
				break;
			case TypeIds.T_JavaLangFunctionalInterface :
				tagBits |= TagBits.AnnotationFunctionalInterface;
				break;
			case TypeIds.T_JavaLangSuppressWarnings :
				tagBits |= TagBits.AnnotationSuppressWarnings;
				break;
			case TypeIds.T_JavaLangSafeVarargs :
				tagBits |= TagBits.AnnotationSafeVarargs;
				break;
			case TypeIds.T_JavaLangInvokeMethodHandlePolymorphicSignature :
				tagBits |= TagBits.AnnotationPolymorphicSignature;
				break;
			case TypeIds.T_ConfiguredAnnotationNullable :
				tagBits |= TagBits.AnnotationNullable;
				break;
			case TypeIds.T_ConfiguredAnnotationNonNull :
				tagBits |= TagBits.AnnotationNonNull;
				break;
			case TypeIds.T_ConfiguredAnnotationNonNullByDefault :
				if (valueAttribute != null 
					&& valueAttribute.value instanceof FalseLiteral) 
				{
					// parameter 'false' means: this annotation cancels any defaults
					tagBits |= TagBits.AnnotationNullUnspecifiedByDefault;
					break;
				}
				tagBits |= TagBits.AnnotationNonNullByDefault;
				break;
		}
		return tagBits;
	}

	public AnnotationBinding getCompilerAnnotation() {
		return this.compilerAnnotation;
	}

	public boolean isRuntimeInvisible() {
		final TypeBinding annotationBinding = this.resolvedType;
		if (annotationBinding == null) {
			return false;
		}
		long metaTagBits = annotationBinding.getAnnotationTagBits(); // could be forward reference

		// we need to filter out only "pure" type use and type parameter annotations, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=392119
		if ((metaTagBits & (TagBits.AnnotationForTypeParameter | TagBits.AnnotationForTypeUse)) != 0) {
			if ((metaTagBits & TagBits.SE7AnnotationTargetMASK) == 0) {  // not a hybrid target. 
				return false;
			}
		}

		if ((metaTagBits & TagBits.AnnotationRetentionMASK) == 0)
			return true; // by default the retention is CLASS

		return (metaTagBits & TagBits.AnnotationRetentionMASK) == TagBits.AnnotationClassRetention;
	}

	public boolean isRuntimeTypeInvisible() {
		final TypeBinding annotationBinding = this.resolvedType;
		if (annotationBinding == null) {
			return false;
		}
		long metaTagBits = annotationBinding.getAnnotationTagBits(); // could be forward reference

		if ((metaTagBits & (TagBits.AnnotationTargetMASK)) == 0) { // explicit target required for JSR308 style annotations.
			return false;
		}
		if ((metaTagBits & (TagBits.AnnotationForTypeParameter | TagBits.AnnotationForTypeUse)) == 0) {
			return false;
		}

		if ((metaTagBits & TagBits.AnnotationRetentionMASK) == 0)
			return true; // by default the retention is CLASS

		return (metaTagBits & TagBits.AnnotationRetentionMASK) == TagBits.AnnotationClassRetention;
	}

	public boolean isRuntimeTypeVisible() {
		final TypeBinding annotationBinding = this.resolvedType;
		if (annotationBinding == null) {
			return false;
		}
		long metaTagBits = annotationBinding.getAnnotationTagBits();

		if ((metaTagBits & (TagBits.AnnotationTargetMASK)) == 0) { // explicit target required for JSR308 style annotations.
			return false;
		}
		if ((metaTagBits & (TagBits.AnnotationForTypeParameter | TagBits.AnnotationForTypeUse)) == 0) {
			return false;
		}
		if ((metaTagBits & TagBits.AnnotationRetentionMASK) == 0)
			return false; // by default the retention is CLASS

		return (metaTagBits & TagBits.AnnotationRetentionMASK) == TagBits.AnnotationRuntimeRetention;
	}

	public boolean isRuntimeVisible() {
		final TypeBinding annotationBinding = this.resolvedType;
		if (annotationBinding == null) {
			return false;
		}
		long metaTagBits = annotationBinding.getAnnotationTagBits();
		// we need to filter out only "pure" type use and type parameter annotations, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=392119
		if ((metaTagBits & (TagBits.AnnotationForTypeParameter | TagBits.AnnotationForTypeUse)) != 0) {
			if ((metaTagBits & TagBits.SE7AnnotationTargetMASK) == 0) { // not a hybrid target.
				return false;
			}
		}
		if ((metaTagBits & TagBits.AnnotationRetentionMASK) == 0)
			return false; // by default the retention is CLASS

		return (metaTagBits & TagBits.AnnotationRetentionMASK) == TagBits.AnnotationRuntimeRetention;
	}

	public abstract MemberValuePair[] memberValuePairs();

	public StringBuffer printExpression(int indent, StringBuffer output) {
		output.append('@');
		this.type.printExpression(0, output);
		return output;
	}

	public void recordSuppressWarnings(Scope scope, int startSuppresss, int endSuppress, boolean isSuppressingWarnings) {
		IrritantSet suppressWarningIrritants = null;
		MemberValuePair[] pairs = memberValuePairs();
		pairLoop: for (int i = 0, length = pairs.length; i < length; i++) {
			MemberValuePair pair = pairs[i];
			if (CharOperation.equals(pair.name, TypeConstants.VALUE)) {
				Expression value = pair.value;
				if (value instanceof ArrayInitializer) {
					ArrayInitializer initializer = (ArrayInitializer) value;
					Expression[] inits = initializer.expressions;
					if (inits != null) {
						for (int j = 0, initsLength = inits.length; j < initsLength; j++) {
							Constant cst = inits[j].constant;
							if (cst != Constant.NotAConstant && cst.typeID() == T_JavaLangString) {
								IrritantSet irritants = CompilerOptions.warningTokenToIrritants(cst.stringValue());
								if (irritants != null) {
									if (suppressWarningIrritants == null) {
										suppressWarningIrritants = new IrritantSet(irritants);
									} else if (suppressWarningIrritants.set(irritants) == null) {
											scope.problemReporter().unusedWarningToken(inits[j]);
									}
								} else {
									scope.problemReporter().unhandledWarningToken(inits[j]);
								}
							}
						}
					}
				} else {
					Constant cst = value.constant;
					if (cst != Constant.NotAConstant && cst.typeID() == T_JavaLangString) {
						IrritantSet irritants = CompilerOptions.warningTokenToIrritants(cst.stringValue());
						if (irritants != null) {
							suppressWarningIrritants = new IrritantSet(irritants);
							// TODO: should check for unused warning token against enclosing annotation as well ?
						} else {
							scope.problemReporter().unhandledWarningToken(value);
						}
					}
				}
				break pairLoop;
			}
		}
		if (isSuppressingWarnings && suppressWarningIrritants != null) {
			scope.referenceCompilationUnit().recordSuppressWarnings(suppressWarningIrritants, this, startSuppresss, endSuppress);
		}
	}

	public TypeBinding resolveType(BlockScope scope) {

		if (this.compilerAnnotation != null)
			return this.resolvedType;
		this.constant = Constant.NotAConstant;

		TypeBinding typeBinding = this.type.resolveType(scope);
		if (typeBinding == null) {
			return null;
		}
		this.resolvedType = typeBinding;
		// ensure type refers to an annotation type
		if (!typeBinding.isAnnotationType() && typeBinding.isValidBinding()) {
			scope.problemReporter().typeMismatchError(typeBinding, scope.getJavaLangAnnotationAnnotation(), this.type, null);
			return null;
		}

		ReferenceBinding annotationType = (ReferenceBinding) this.resolvedType;
		MethodBinding[] methods = annotationType.methods();
		// clone valuePairs to keep track of unused ones
		MemberValuePair[] originalValuePairs = memberValuePairs();
		MemberValuePair valueAttribute = null; // remember the first 'value' pair
		MemberValuePair[] pairs;
		int pairsLength = originalValuePairs.length;
		if (pairsLength > 0) {
			System.arraycopy(originalValuePairs, 0, pairs = new MemberValuePair[pairsLength], 0, pairsLength);
		} else {
			pairs = originalValuePairs;
		}

		nextMember: for (int i = 0, requiredLength = methods.length; i < requiredLength; i++) {
			MethodBinding method = methods[i];
			char[] selector = method.selector;
			boolean foundValue = false;
			nextPair: for (int j = 0; j < pairsLength; j++) {
				MemberValuePair pair = pairs[j];
				if (pair == null) continue nextPair;
				char[] name = pair.name;
				if (CharOperation.equals(name, selector)) {
					if (valueAttribute == null && CharOperation.equals(name, TypeConstants.VALUE)) {
						valueAttribute = pair;
					}
					pair.binding = method;
					pair.resolveTypeExpecting(scope, method.returnType);
					pairs[j] = null; // consumed
					foundValue = true;

					// check duplicates
					boolean foundDuplicate = false;
					for (int k = j+1; k < pairsLength; k++) {
						MemberValuePair otherPair = pairs[k];
						if (otherPair == null) continue;
						if (CharOperation.equals(otherPair.name, selector)) {
							foundDuplicate = true;
							scope.problemReporter().duplicateAnnotationValue(annotationType, otherPair);
							otherPair.binding = method;
							otherPair.resolveTypeExpecting(scope, method.returnType);
							pairs[k] = null;
						}
					}
					if (foundDuplicate) {
						scope.problemReporter().duplicateAnnotationValue(annotationType, pair);
						continue nextMember;
					}
				}
			}
			if (!foundValue
					&& (method.modifiers & ClassFileConstants.AccAnnotationDefault) == 0
					&& (this.bits & IsRecovered) == 0
					&& annotationType.isValidBinding()) {
				scope.problemReporter().missingValueForAnnotationMember(this, selector);
			}
		}
		// check unused pairs
		for (int i = 0; i < pairsLength; i++) {
			if (pairs[i] != null) {
				if (annotationType.isValidBinding()) {
					scope.problemReporter().undefinedAnnotationValue(annotationType, pairs[i]);
				}
				pairs[i].resolveTypeExpecting(scope, null); // resilient
			}
		}
//		if (scope.compilerOptions().storeAnnotations)
		this.compilerAnnotation = scope.environment().createAnnotation((ReferenceBinding) this.resolvedType, computeElementValuePairs());
		// recognize standard annotations ?
		long tagBits = detectStandardAnnotation(scope, annotationType, valueAttribute);

		// record annotation positions in the compilation result
		scope.referenceCompilationUnit().recordSuppressWarnings(IrritantSet.NLS, null, this.sourceStart, this.declarationSourceEnd);
		if (this.recipient != null) {
			int kind = this.recipient.kind();
			if (tagBits != 0) {
				// tag bits onto recipient
				switch (kind) {
					case Binding.PACKAGE :
						((PackageBinding)this.recipient).tagBits |= tagBits;
						break;
					case Binding.TYPE_PARAMETER:
					case Binding.TYPE_USE:
						ReferenceBinding typeAnnotationRecipient = (ReferenceBinding) this.recipient;
						typeAnnotationRecipient.tagBits |= tagBits;
						break;
					case Binding.TYPE :
					case Binding.GENERIC_TYPE :
						SourceTypeBinding sourceType = (SourceTypeBinding) this.recipient;
						sourceType.tagBits |= tagBits;
						if ((tagBits & TagBits.AnnotationSuppressWarnings) != 0) {
							TypeDeclaration typeDeclaration =  sourceType.scope.referenceContext;
							int start;
							if (scope.referenceCompilationUnit().types[0] == typeDeclaration) {
								start = 0;
							} else {
								start = typeDeclaration.declarationSourceStart;
							}
							recordSuppressWarnings(scope, start, typeDeclaration.declarationSourceEnd, scope.compilerOptions().suppressWarnings);
						}
						break;
					case Binding.METHOD :
						MethodBinding sourceMethod = (MethodBinding) this.recipient;
						sourceMethod.tagBits |= tagBits;
						if ((tagBits & TagBits.AnnotationSuppressWarnings) != 0) {
							sourceType = (SourceTypeBinding) sourceMethod.declaringClass;
							AbstractMethodDeclaration methodDeclaration = sourceType.scope.referenceContext.declarationOf(sourceMethod);
							recordSuppressWarnings(scope, methodDeclaration.declarationSourceStart, methodDeclaration.declarationSourceEnd, scope.compilerOptions().suppressWarnings);
						}
						if ((sourceMethod.tagBits & TAGBITS_NULLABLE_OR_NONNULL) == TAGBITS_NULLABLE_OR_NONNULL) {
							scope.problemReporter().contradictoryNullAnnotations(this);
							sourceMethod.tagBits &= ~TAGBITS_NULLABLE_OR_NONNULL; // avoid secondary problems
						}
						break;
					case Binding.FIELD :
						FieldBinding sourceField = (FieldBinding) this.recipient;
						sourceField.tagBits |= tagBits;
						if ((tagBits & TagBits.AnnotationSuppressWarnings) != 0) {
							sourceType = (SourceTypeBinding) sourceField.declaringClass;
							FieldDeclaration fieldDeclaration = sourceType.scope.referenceContext.declarationOf(sourceField);
							recordSuppressWarnings(scope, fieldDeclaration.declarationSourceStart, fieldDeclaration.declarationSourceEnd, scope.compilerOptions().suppressWarnings);
						}
						if ((sourceField.tagBits & TAGBITS_NULLABLE_OR_NONNULL) == TAGBITS_NULLABLE_OR_NONNULL) {
							scope.problemReporter().contradictoryNullAnnotations(this);
							sourceField.tagBits &= ~TAGBITS_NULLABLE_OR_NONNULL; // avoid secondary problems
						}
						break;
					case Binding.LOCAL :
						LocalVariableBinding variable = (LocalVariableBinding) this.recipient;
						variable.tagBits |= tagBits;
						if ((tagBits & TagBits.AnnotationSuppressWarnings) != 0) {
							 LocalDeclaration localDeclaration = variable.declaration;
							recordSuppressWarnings(scope, localDeclaration.declarationSourceStart, localDeclaration.declarationSourceEnd, scope.compilerOptions().suppressWarnings);
						}
						if ((variable.tagBits & TAGBITS_NULLABLE_OR_NONNULL) == TAGBITS_NULLABLE_OR_NONNULL) {
							scope.problemReporter().contradictoryNullAnnotations(this);
							variable.tagBits &= ~TAGBITS_NULLABLE_OR_NONNULL; // avoid secondary problems
						}
						break;
				}
			}
			// check (meta)target compatibility
			checkTargetCompatibility: {
				if (!annotationType.isValidBinding()) {
					// no need to check annotation usage if missing
					break checkTargetCompatibility;
				}

				long metaTagBits = annotationType.getAnnotationTagBits(); // could be forward reference
				if ((metaTagBits & TagBits.AnnotationTargetMASK) == 0) {
					// does not specify any target restriction - all locations supported in Java 7 and before are possible
					if (kind == Binding.TYPE_PARAMETER || kind == Binding.TYPE_USE) {
						scope.problemReporter().explitAnnotationTargetRequired(this);
					}
					break checkTargetCompatibility;
				}

				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=391201
				if ((metaTagBits & TagBits.SE7AnnotationTargetMASK) == 0
						&& (metaTagBits & (TagBits.AnnotationForTypeUse | TagBits.AnnotationForTypeParameter)) != 0) {
					if (scope.compilerOptions().sourceLevel < ClassFileConstants.JDK1_8) {
						switch (kind) {
							case Binding.PACKAGE :
							case Binding.TYPE :
							case Binding.GENERIC_TYPE :
							case Binding.METHOD :
							case Binding.FIELD :
							case Binding.LOCAL :
								scope.problemReporter().invalidUsageOfTypeAnnotations(this);
						}
					}
				}
				switch (kind) {
					case Binding.PACKAGE :
						if ((metaTagBits & TagBits.AnnotationForPackage) != 0)
							break checkTargetCompatibility;
						break;
					case Binding.TYPE_USE :
						if ((metaTagBits & TagBits.AnnotationForTypeUse) != 0) {
							// jsr 308
							break checkTargetCompatibility;
						}
						break;
					case Binding.TYPE :
					case Binding.GENERIC_TYPE :
						if (((ReferenceBinding)this.recipient).isAnnotationType()) {
							if ((metaTagBits & (TagBits.AnnotationForAnnotationType | TagBits.AnnotationForType)) != 0)
							break checkTargetCompatibility;
						} else if ((metaTagBits & (TagBits.AnnotationForType | TagBits.AnnotationForTypeUse)) != 0) {
							break checkTargetCompatibility;
						} else if ((metaTagBits & TagBits.AnnotationForPackage) != 0) {
							if (CharOperation.equals(((ReferenceBinding)this.recipient).sourceName, TypeConstants.PACKAGE_INFO_NAME))
								break checkTargetCompatibility;
						}
						break;
					case Binding.METHOD :
						MethodBinding methodBinding = (MethodBinding) this.recipient;
						if (methodBinding.isConstructor()) {
							if ((metaTagBits & (TagBits.AnnotationForConstructor | TagBits.AnnotationForTypeUse)) != 0)
								break checkTargetCompatibility;
						} else if ((metaTagBits & TagBits.AnnotationForMethod) != 0) {
							break checkTargetCompatibility;
						} else if ((metaTagBits & TagBits.AnnotationForTypeUse) != 0) {
							SourceTypeBinding sourceType = (SourceTypeBinding) methodBinding.declaringClass;
							MethodDeclaration methodDecl = (MethodDeclaration) sourceType.scope.referenceContext.declarationOf(methodBinding);
							if (isTypeUseCompatible(methodDecl.returnType, scope)) {
								break checkTargetCompatibility;
							}
						}
						break;
					case Binding.FIELD :
						if ((metaTagBits & TagBits.AnnotationForField) != 0) {
							break checkTargetCompatibility;
						} else if ((metaTagBits & TagBits.AnnotationForTypeUse) != 0) {
							FieldBinding sourceField = (FieldBinding) this.recipient;
							SourceTypeBinding sourceType = (SourceTypeBinding) sourceField.declaringClass;
							FieldDeclaration fieldDeclaration = sourceType.scope.referenceContext.declarationOf(sourceField);
							if (isTypeUseCompatible(fieldDeclaration.type, scope)) {
								break checkTargetCompatibility;
							}
						}
						break;
					case Binding.LOCAL :
						LocalVariableBinding localVariableBinding = (LocalVariableBinding)this.recipient;
						if ((localVariableBinding.tagBits & TagBits.IsArgument) != 0) {
							if ((metaTagBits & TagBits.AnnotationForParameter) != 0) {
								break checkTargetCompatibility;
							} else if ((metaTagBits & TagBits.AnnotationForTypeUse) != 0) {
								if (isTypeUseCompatible(localVariableBinding.declaration.type, scope)) {
									break checkTargetCompatibility;
								}
							}
						} else if ((annotationType.tagBits & TagBits.AnnotationForLocalVariable) != 0) {
							break checkTargetCompatibility;
						} else if ((metaTagBits & TagBits.AnnotationForTypeUse) != 0) {
							if (isTypeUseCompatible(localVariableBinding.declaration.type, scope)) {
								break checkTargetCompatibility;
							}
						}
						break;
					case Binding.TYPE_PARAMETER : // jsr308
						// https://bugs.eclipse.org/bugs/show_bug.cgi?id=391196
						if ((metaTagBits & (TagBits.AnnotationForTypeParameter | TagBits.AnnotationForTypeUse)) != 0) {
							break checkTargetCompatibility;
						}
					}
				scope.problemReporter().disallowedTargetForAnnotation(this);
			}
		}
		return this.resolvedType;
	}
	private boolean isTypeUseCompatible(TypeReference reference, Scope scope) {
		if (!(reference instanceof SingleTypeReference)) {
			Binding binding = scope.getPackage(reference.getTypeName());
			// In case of ProblemReferenceBinding, don't report additional error
			if (binding instanceof PackageBinding) {
				return false;
			}
		}
		return true;
	}

	public abstract void traverse(ASTVisitor visitor, BlockScope scope);

	public abstract void traverse(ASTVisitor visitor, ClassScope scope);
}
