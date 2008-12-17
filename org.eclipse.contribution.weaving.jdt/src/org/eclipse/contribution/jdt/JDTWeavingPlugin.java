/*******************************************************************************
 * Copyright (c) 2008 SpringSource and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *      SpringSource
 *      Andrew Eisenberg (initial implementation)
 *******************************************************************************/
package org.eclipse.contribution.jdt;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

public class JDTWeavingPlugin extends Plugin {
    
    private static JDTWeavingPlugin INSTANCE;
    
    public static String ID = "org.eclipse.contribution.jdt"; //$NON-NLS-1$
    
    public JDTWeavingPlugin() {
        super();
        INSTANCE = this;
    }

    
    public static void logException(Throwable t) {
        INSTANCE.getLog().log(new Status(IStatus.ERROR, ID, t.getMessage(), t));
    }
    
    
    public static JDTWeavingPlugin getInstance() {
        return INSTANCE;
    }
    
    public void start(BundleContext context) throws Exception {
        super.start(context);
    }
}