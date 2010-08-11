/*
 * Copyright 2003-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.ajdt.internal.core.search;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.internal.core.hierarchy.HierarchyResolver;
import org.eclipse.jdt.internal.core.search.matching.PackageReferencePattern;
import org.eclipse.jdt.internal.core.search.matching.PossibleMatch;

/**
 * Looks for package names in aspect-specific locations
 * @author Andrew Eisenberg
 * @created Aug 6, 2010
 */
public class ExtraPackageReferenceFinder implements IExtraMatchFinder<PackageReferencePattern> {

    public List<SearchMatch> findExtraMatches(PossibleMatch match,
            PackageReferencePattern pattern, HierarchyResolver resolver)
            throws JavaModelException {
        return Collections.emptyList();
    }

}
