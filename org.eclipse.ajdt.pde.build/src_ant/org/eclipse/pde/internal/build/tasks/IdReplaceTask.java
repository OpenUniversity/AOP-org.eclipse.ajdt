/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.io.*;
import java.util.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/** 
 * Internal task.
 * This task aims at replacing the generic ids used into a feature.xml by another value, and also replace the feature version number if necessary.  
 * @since 3.0
 */
public class IdReplaceTask extends Task {
	private static final String UTF_8 = "UTF-8"; //$NON-NLS-1$
	private static final String FEATURE_START_TAG = "<feature";//$NON-NLS-1$
	private static final String ID = "id";//$NON-NLS-1$
	private static final String VERSION = "version";//$NON-NLS-1$
	private static final String COMMA = ","; //$NON-NLS-1$
	private static final String BACKSLASH = "\""; //$NON-NLS-1$
	private static final String EMPTY = ""; //$NON-NLS-1$
	private static final String PLUGIN_START_TAG = "<plugin"; //$NON-NLS-1$
	private static final String INCLUDES_START_TAG = "<includes"; //$NON-NLS-1$
	private static final String COMMENT_START_TAG = "<!--"; //$NON-NLS-1$
	private static final String COMMENT_END_TAG = "-->"; //$NON-NLS-1$
	
	//Path of the file where we are replacing the values
	private String featureFilePath;
	//Map of the plugin ids (key) and their version number (value)  
	private Map pluginIds = new HashMap(10);
	//Map of the feature ids (key) and their version number (value)
	private Map featureIds = new HashMap(4);
	//The new version number for this feature
	private String selfVersion;

	private boolean contentChanged = false;
	
	private final static String GENERIC_VERSION_NUMBER = "0.0.0"; //$NON-NLS-1$
	private final static String DOT_QUALIFIER = ".qualifier"; //$NON-NLS-1$

	/**
	 * The location of a feature.xml file 
	 * @param path
	 */
	public void setFeatureFilePath(String path) {
		featureFilePath = path;
	}

	/** 
	 * The value with which the current version of the feature will be replaced. 
	 * @param version
	 */
	public void setSelfVersion(String version) {
		selfVersion = version;
	}

	/**
	 * Set the values to use when replacing a generic value used in a plugin reference.
	 * Note all the pluginIds that have a generic number into the feature.xml must be
	 * listed in <param>values</param>.
	 * @param values a comma separated list alternating pluginId and versionNumber.
	 * For example: org.eclipse.pde.build,2.1.0,org.eclipse.core.resources,1.2.0
	 */
	public void setPluginIds(String values) {
		pluginIds = new HashMap(10);
		for (StringTokenizer tokens = new StringTokenizer(values, COMMA); tokens.hasMoreTokens();) { 
			String token = tokens.nextToken().trim();
			String id = EMPTY;
			if (!token.equals(EMPTY))
				id = token;

			String version = EMPTY;
			token = tokens.nextToken().trim();
			if (!token.equals(EMPTY))
				version = token;
			pluginIds.put(id, version);
		}
	}

	/**
	 * Set the values to use when replacing a generic value used in a feature reference
	 * Note that all the featureIds that have a generic number into the feature.xml must
	 * be liste in <param>values</param>.
	 * @param values
	 */
	public void setFeatureIds(String values) {
		featureIds = new HashMap(10);
		for (StringTokenizer tokens = new StringTokenizer(values, COMMA); tokens.hasMoreTokens();) { 
			String token = tokens.nextToken().trim();
			String id = EMPTY;
			if (!token.equals(EMPTY))
				id = token;

			String version = EMPTY;
			token = tokens.nextToken().trim();
			if (!token.equals(EMPTY))
				version = token;
			featureIds.put(id, version);
		}
	}

	public void execute() {
		StringBuffer buffer = null;
		try {
			buffer = readFile(new File(featureFilePath));
		} catch (IOException e) {
			throw new BuildException(e);
		}

		//Skip feature declaration because it contains the word "plugin"
		int startComment = scan(buffer, 0, COMMENT_START_TAG);
		int endComment = startComment > -1 ? scan(buffer, startComment, COMMENT_END_TAG) : -1;
		int startFeature = scan(buffer, 0, FEATURE_START_TAG);

		while (startComment != -1 && startFeature > startComment && startFeature < endComment) {
			startFeature = scan(buffer, endComment, FEATURE_START_TAG);
			startComment = scan(buffer, endComment, COMMENT_START_TAG);
			endComment = startComment > -1 ? scan(buffer, startComment, COMMENT_END_TAG) : -1;
		}

		if (startFeature == -1)
			return;

		int endFeature = scan(buffer, startFeature, ">"); //$NON-NLS-1$

		if (selfVersion != null) {
			boolean versionFound = false;
			while (!versionFound) {
				int startVersionWord = scan(buffer, startFeature, VERSION);
				if (startVersionWord == -1 || startVersionWord > endFeature)
					return;
				if (!Character.isWhitespace(buffer.charAt(startVersionWord - 1))) {
					startFeature = startVersionWord + VERSION.length();
					continue;
				}
				//Verify that the word version found is the actual attribute
				int endVersionWord = startVersionWord + VERSION.length();
				while (Character.isWhitespace(buffer.charAt(endVersionWord)) && endVersionWord < endFeature) {
					endVersionWord++;
				}
				if (endVersionWord > endFeature) { //version has not been found
					System.err.println("Could not find the tag 'version' in the feature header, file: " + featureFilePath); //$NON-NLS-1$
					return;
				}

				if (buffer.charAt(endVersionWord) != '=') {
					startFeature = endVersionWord;
					continue;
				}

				int startVersionId = scan(buffer, startVersionWord + 1, BACKSLASH);
				int endVersionId = scan(buffer, startVersionId + 1, BACKSLASH);
				buffer.replace(startVersionId + 1, endVersionId, selfVersion);
				contentChanged = true;
				versionFound = true;
			}
		}

		int startElement = endFeature;
		int startId = 0;
		while (true) {
			int startPlugin = scan(buffer, startElement + 1, PLUGIN_START_TAG);
			int startInclude = scan(buffer, startElement + 1, INCLUDES_START_TAG);

			if (startPlugin == -1 && startInclude == -1)
				break;

			startComment = scan(buffer, startElement + 1, COMMENT_START_TAG);
			endComment = startComment > -1 ? scan(buffer, startComment, COMMENT_END_TAG) : -1;

			int foundElement = -1;
			boolean isPlugin = false;

			//Find which of a plugin or a feature is referenced first 
			if (startPlugin == -1 || startInclude == -1) {
				foundElement = startPlugin != -1 ? startPlugin : startInclude;
				isPlugin = (startPlugin != -1 ? true : false);
			} else {
				if (startPlugin < startInclude) {
					foundElement = startPlugin;
					isPlugin = true;
				} else {
					foundElement = startInclude;
					isPlugin = false;
				}
			}

			if (startComment != -1 && foundElement > startComment && foundElement < endComment) {
				startElement = endComment;
				continue;
			}

			startId = scan(buffer, foundElement, ID);
			if (startId == -1)
				break;

			int startElementId = scan(buffer, startId + 1, BACKSLASH);
			int endElementId = scan(buffer, startElementId + 1, BACKSLASH);
			char[] elementId = new char[endElementId - startElementId - 1];
			buffer.getChars(startElementId + 1, endElementId, elementId, 0);

			int startVersionWord = scan(buffer, endElementId + 1, VERSION);
			int startVersionId = scan(buffer, startVersionWord + 1, BACKSLASH);
			int endVersionId = scan(buffer, startVersionId + 1, BACKSLASH);
			char[] versionId = new char[endVersionId - startVersionId - 1];
			buffer.getChars(startVersionId + 1, endVersionId, versionId, 0);
			if (!new String(versionId).equals(GENERIC_VERSION_NUMBER) && !new String(versionId).endsWith(DOT_QUALIFIER)) {
				startElement = startVersionId;
				continue;
			}

			startVersionId++;
			String replacementVersion = null;
			if (isPlugin) {
				replacementVersion = (String) pluginIds.get(new String(elementId));
			} else {
				replacementVersion = (String) featureIds.get(new String(elementId));
			}
			if (replacementVersion == null) {
				System.err.println("Could not find" + new String(elementId)); //$NON-NLS-1$
			} else {
				buffer.replace(startVersionId, endVersionId, replacementVersion);
				contentChanged = true;
			}

			startElement = startVersionId;
		}

		if (!contentChanged)
			return;
		
		try {	
			OutputStreamWriter w = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(featureFilePath)), UTF_8);
			w.write(buffer.toString());
			w.close();
		} catch (FileNotFoundException e) {
			// ignore
		} catch (IOException e) {
			throw new BuildException(e);
		}
	}

	private int scan(StringBuffer buf, int start, String targetName) {
		return scan(buf, start, new String[] {targetName});
	}

	private int scan(StringBuffer buf, int start, String[] targets) {
		for (int i = start; i < buf.length(); i++) {
			for (int j = 0; j < targets.length; j++) {
				if (i < buf.length() - targets[j].length()) {
					String match = buf.substring(i, i + targets[j].length());
					if (targets[j].equalsIgnoreCase(match))
						return i;
				}
			}
		}
		return -1;
	}

	private StringBuffer readFile(File targetName) throws IOException {
		InputStreamReader reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(targetName)), UTF_8);
		StringBuffer result = new StringBuffer();
		char[] buf = new char[4096];
		int count;
		try {
			count = reader.read(buf, 0, buf.length);
			while (count != -1) {
				result.append(buf, 0, count);
				count = reader.read(buf, 0, buf.length);
			}
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// ignore exceptions here
			}
		}
		return result;
	}
}
