/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     	IBM Corporation - initial API and implementation
 * 		Matthew Webster - initial version
 *******************************************************************************/
package org.eclipse.ajdt.ras;

import org.eclipse.ajdt.core.AspectJPlugin;
import org.eclipse.ajdt.test.AspectJTestPlugin;
import org.eclipse.ajdt.ui.AspectJUIPlugin;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;

import junit.framework.TestCase;

/**
 * Test the FFDC aspect and its usage in AJDT
 */
public class PluginFFDCTest extends TestCase {
	
	public void testFFDC () {
		LogListener listener = new LogListener(getPlugin());
		String message = "testFFDC";
		
		try {
			throw new Exception(message);
		}
		catch (Exception ex) {
		}
		
		assertMessage(listener,message);
	}
	
	public void testRogueFFDC () {
		LogListener listener = new LogListener(getPlugin());
		String message = "testRogueFFDC";
		
		try {
			throw new Exception(message);
		}
		catch (Exception ex) {
		}
		
		assertMessage(listener,message);
	}
	
	public void testCoreFFDC () {
		LogListener listener = new LogListener(AspectJPlugin.getDefault());
		String key = "bogus.bogus";

		String result = AspectJPlugin.getResourceString(key);

		assertEquals("Resource should not be found",key,result);
		assertMessage(listener,key);
	}
	
	public void testUIFFDC () {
		LogListener listener = new LogListener(AspectJUIPlugin.getDefault());
		String key = "bogus.bogus";

		String result = AspectJUIPlugin.getResourceString(key);

		assertEquals("Resource should not be found",key,result);
		assertMessage(listener,key);
	}

	public static class LogListener implements ILogListener {

		private IStatus status;

		public LogListener (Plugin plugin) {
			plugin.getLog().addLogListener(this);
		}
		
		public boolean hasMessage (String message) {
			return (status != null && status.getMessage().indexOf(message) != -1);
		}
		
		public void logging(IStatus status, String plugin) {
			this.status = status;
//			System.err.println(status);
		}

	}
	
	private static aspect TestFFDCAspect extends PluginFFDC {

		protected pointcut ffdcScope () :
			within(PluginFFDCTest) &&
			cflow(execution(void testFFDC()));
		
		
	    protected String getPluginId () {
	    	return AspectJTestPlugin.getPluginId();
	    }

	    protected void log (IStatus status) {
	    	AspectJTestPlugin.getDefault().getLog().log(status);
	    }

	}
	
	private static aspect RogueFFDCAspect extends PluginFFDC {

		protected pointcut ffdcScope () :
			within(PluginFFDCTest) &&
			cflow(execution(void testRogueFFDC()));
		
		
	    protected String getPluginId () {
	    	return null;
	    }

	    protected void log (IStatus status) {
	    	AspectJTestPlugin.getDefault().getLog().log(status);
	    }

	}
	
	public void assertMessage (LogListener listener, String expected) {
		if (!listener.hasMessage(expected)) {
			fail("The log did not contain the following message\n" + expected);
		}
	}
	
	private static Plugin getPlugin () {
    	return AspectJTestPlugin.getDefault();
	}
}
