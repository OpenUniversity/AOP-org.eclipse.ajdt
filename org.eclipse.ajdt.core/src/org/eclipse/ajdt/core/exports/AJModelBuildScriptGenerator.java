/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *     Andre Eisenberg - Adapted for AJDT 1.6
 *******************************************************************************/
package org.eclipse.ajdt.core.exports;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.build.Messages;
import org.eclipse.pde.internal.build.Utils;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.ant.FileSet;
import org.eclipse.pde.internal.build.ant.JavacTask;
import org.eclipse.pde.internal.build.builder.ClasspathComputer2_1;
import org.eclipse.pde.internal.build.builder.ClasspathComputer3_0;
import org.eclipse.pde.internal.build.builder.IClasspathComputer;
import org.eclipse.pde.internal.build.builder.ModelBuildScriptGenerator;
import org.eclipse.pde.internal.build.builder.ClasspathComputer3_0.ClasspathElement;
import org.eclipse.pde.internal.build.site.compatibility.FeatureEntry;

/**
 * Generic class for generating scripts for plug-ins and fragments.
 */
/*
 * Copied from org.eclipse.pde.internal.build.builder.ModelBuildScriptGenerator
 * to enable AspectJ plugins to be correctly exported.
 * 
 * Changes marked with // AspectJ Change 
 */
public class AJModelBuildScriptGenerator extends ModelBuildScriptGenerator { // AspectJ change
	public static final String SRC_ZIP = "src.zip"; //$NON-NLS-1$
	public static final String EXPANDED_DOT = "@dot"; //$NON-NLS-1$
	public static final String DOT = "."; //$NON-NLS-1$
	
	// AspectJ Change Begin - aspectpath and inpath support
	public static final String PROPERTY_INPATH = "inpath"; //$NON-NLS-1$
	public static final String PROPERTY_ASPECTPATH = "aspectpath"; //$NON-NLS-1$
	// AspectJ Change end


	/**
	 * Represents a entry that must be compiled and which is listed in the build.properties file.
	 */
	static protected class CompiledEntry extends ModelBuildScriptGenerator.CompiledEntry { // AspectJ change
		static final byte JAR = 0;
		static final byte FOLDER = 1;
		private String name;
		private String resolvedName;
		private String[] source;
		private String[] output;
		private String[] extraClasspath;
		private String excludedFromJar;
		byte type;

		protected CompiledEntry(String entryName, String[] entrySource, String[] entryOutput, String[] entryExtraClasspath, String excludedFromJar, byte entryType) {
			// AspectJ Change Begin
			super(entryName, entrySource, entryOutput, entryExtraClasspath, excludedFromJar, entryType);
			// AspectJ Change End
			this.name = entryName;
			this.source = entrySource;
			this.output = entryOutput;
			this.extraClasspath = entryExtraClasspath;
			this.type = entryType;
			this.excludedFromJar = excludedFromJar;
		}

		protected String getName(boolean resolved) {
			if (!resolved)
				return name;

			if (resolvedName == null)
				resolvedName = replaceVariables(name, true);

			return resolvedName;
		}

		protected String[] getSource() {
			return source;
		}

		public String[] getOutput() {
			return output;
		}

		public String[] getExtraClasspath() {
			return extraClasspath;
		}

		public byte getType() {
			return type;
		}

		public String getExcludedFromJar() {
			return excludedFromJar;
		}
	}

	// AspectJ Change Begin - aspectpath and inpath support
	protected List aspectpath;
	protected List inpath;
    // AspectJ Change End
	
	/**
	 * Bundle for which we are generating the script.
	 */
	protected BundleDescription model;
	/**
	 * PluginEntry corresponding to the bundle
	 */
	private FeatureEntry associatedEntry;

	protected String fullName;
	protected String pluginZipDestination;
	protected String pluginUpdateJarDestination;

	private AJBuildDirector featureGenerator; // AspectJ Change

	/** constants */
	protected final String PLUGIN_DESTINATION = Utils.getPropertyFormat(PROPERTY_PLUGIN_DESTINATION);

	private Properties permissionProperties;

	private String propertiesFileName = PROPERTIES_FILE;
	private String buildScriptFileName = DEFAULT_BUILD_SCRIPT_FILENAME;
	private String customBuildCallbacks = null;
	private String customCallbacksBuildpath = null;
	private String customCallbacksFailOnError = null;
	private String customCallbacksInheritAll = null;
	//This list is initialized by the generateBuildJarsTarget
	private ArrayList compiledJarNames;
	private boolean dotOnTheClasspath = false;
	private boolean binaryPlugin = false;
	private boolean signJars = false;

	// added bundles required for AspectJ
	private List addedBundles; // AspectJ Change

	
	// AspectJ Change Begin - override this method to use an AJAntScript	
	/**
	 * @see AbstractScriptGenerator#openScript()
	 */
	public void openScript(String scriptLocation, String scriptName) throws CoreException {
		if (script != null)
			return;

		try {
			OutputStream scriptStream = new BufferedOutputStream(new FileOutputStream(scriptLocation + '/' + scriptName)); //$NON-NLS-1$
			try {
				script = new AJAntScript(scriptStream);
			} catch (IOException e) {
				try {
					scriptStream.close();
					String message = NLS.bind(Messages.exception_writingFile, scriptLocation + '/' + scriptName);
					throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
				} catch (IOException e1) {
					// Ignored		
				}
			}
		} catch (FileNotFoundException e) {
			String message = NLS.bind(Messages.exception_writingFile, scriptLocation + '/' + scriptName);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		}
	}
	
	private String buildConfig;
	
	public void setBuildConfig(String config) {
		buildConfig = config;
	}
//	 AspectJ Change End

	
	
	/**
	 * @see AbstractScriptGenerator#generate()
	 */
	public void generate() throws CoreException {
		//If it is a binary plugin, then we don't generate scripts
		if (binaryPlugin)
			return;

		if (model == null) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ELEMENT_MISSING, Messages.error_missingElement, null));
		}

		// If the the plugin we want to generate is a source plugin, and the feature that required the generation of this plugin is not being asked to build the source
		// we want to leave. This is particularly usefull for the case of the pde.source building (at least for now since the source of pde is not in a feature)
//		if (featureGenerator != null && featureGenerator.getBuildProperties().containsKey(GENERATION_SOURCE_PLUGIN_PREFIX + model.getSymbolicName()))
//			return;

		if (!AbstractScriptGenerator.isBuildingOSGi())
			checkBootAndRuntime();

		initializeVariables();
		if (BundleHelper.getDefault().isDebugging())
			System.out.println("Generating plugin " + model.getSymbolicName()); //$NON-NLS-1$

		String custom = (String) getBuildProperties().get(PROPERTY_CUSTOM);
		if (custom != null && custom.equalsIgnoreCase("true")) { //$NON-NLS-1$
			updateExistingScript();
			return;
		}

		openScript(getLocation(model), buildScriptFileName);
		try {
			generateBuildScript();
		} finally {
			closeScript();
		}
	}

	/**
	 * Check that boot and runtime are available, otherwise throws an exception because the build will fail.
	 */
	private void checkBootAndRuntime() throws CoreException {
		if (getSite(false).getRegistry().getResolvedBundle(PI_BOOT) == null) {
			IStatus status = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, NLS.bind(Messages.exception_missingPlugin, PI_BOOT), null);
			throw new CoreException(status);
		}
		if (getSite(false).getRegistry().getResolvedBundle(PI_RUNTIME) == null) {
			IStatus status = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, NLS.bind(Messages.exception_missingPlugin, PI_RUNTIME), null);
			throw new CoreException(status);
		}
	}

	public static String getNormalizedName(BundleDescription bundle) {
		return bundle.getSymbolicName() + '_' + bundle.getVersion();
	}
 
	private void initializeVariables() throws CoreException {
		fullName = getNormalizedName(model);
		pluginZipDestination = PLUGIN_DESTINATION + '/' + fullName + ".zip"; //$NON-NLS-1$
		pluginUpdateJarDestination = PLUGIN_DESTINATION + '/' + fullName + ".jar"; //$NON-NLS-1$
		String[] classpathInfo = getClasspathEntries(model);
		dotOnTheClasspath = specialDotProcessing(getBuildProperties(), classpathInfo);

		//Persist this information for use in the assemble script generation
		Properties bundleProperties = (Properties) model.getUserObject();
		bundleProperties.put(WITH_DOT, Boolean.valueOf(dotOnTheClasspath));

		Properties properties = getBuildProperties();
		customBuildCallbacks = properties.getProperty(PROPERTY_CUSTOM_BUILD_CALLBACKS);
		if (TRUE.equalsIgnoreCase(customBuildCallbacks))
			customBuildCallbacks = DEFAULT_CUSTOM_BUILD_CALLBACKS_FILE;
		else if (FALSE.equalsIgnoreCase(customBuildCallbacks))
			customBuildCallbacks = null;

		customCallbacksBuildpath = properties.getProperty(PROPERTY_CUSTOM_CALLBACKS_BUILDPATH, "."); //$NON-NLS-1$
		customCallbacksFailOnError = properties.getProperty(PROPERTY_CUSTOM_CALLBACKS_FAILONERROR, FALSE);
		customCallbacksInheritAll = properties.getProperty(PROPERTY_CUSTOM_CALLBACKS_INHERITALL);
		
		// AspectJ change begin
		String inpathProp = getBuildProperties().getProperty(PROPERTY_INPATH);
		if (inpathProp != null) {
			inpath = new ArrayList();
			inpath.add(inpathProp);
		}
		String aspectpathProp = getBuildProperties().getProperty(PROPERTY_ASPECTPATH);
		if (aspectpathProp != null) {
			aspectpath = new ArrayList();
			aspectpath.add(aspectpathProp);
		}
		// AspectJ change end
	}

	protected static boolean findAndReplaceDot(String[] classpathInfo) {
		for (int i = 0; i < classpathInfo.length; i++) {
			if (DOT.equals(classpathInfo[i])) {
				classpathInfo[i] = EXPANDED_DOT;
				return true;
			}
		}
		return false;
	}

	public static boolean specialDotProcessing(Properties properties, String[] classpathInfo) {
		findAndReplaceDot(classpathInfo);

		String outputValue = properties.getProperty(PROPERTY_OUTPUT_PREFIX + DOT);
		if (outputValue != null) {
			properties.setProperty(PROPERTY_OUTPUT_PREFIX + EXPANDED_DOT, outputValue);
			properties.remove(PROPERTY_OUTPUT_PREFIX + DOT);
		}

		String sourceFolder = properties.getProperty(PROPERTY_SOURCE_PREFIX + DOT);
		if (sourceFolder != null) {
			properties.setProperty(PROPERTY_SOURCE_PREFIX + EXPANDED_DOT, sourceFolder);
			properties.remove(PROPERTY_SOURCE_PREFIX + DOT);

			String excludedFromJar = properties.getProperty(PROPERTY_EXCLUDE_PREFIX + DOT);
			if (excludedFromJar != null) {
				properties.setProperty(PROPERTY_EXCLUDE_PREFIX + EXPANDED_DOT, excludedFromJar);
				properties.remove(PROPERTY_EXCLUDE_PREFIX + DOT);
			}
			String buildOrder = properties.getProperty(PROPERTY_JAR_ORDER);
			if (buildOrder != null) {
				String[] order = Utils.getArrayFromString(buildOrder);
				for (int i = 0; i < order.length; i++)
					if (order[i].equals(DOT))
						order[i] = EXPANDED_DOT;
				properties.setProperty(PROPERTY_JAR_ORDER, Utils.getStringFromArray(order, ",")); //$NON-NLS-1$
			}

			String extraEntries = properties.getProperty(PROPERTY_EXTRAPATH_PREFIX + '.');
			if (extraEntries != null) {
				properties.setProperty(PROPERTY_EXTRAPATH_PREFIX + EXPANDED_DOT, extraEntries);
			}

			String includeString = properties.getProperty(PROPERTY_BIN_INCLUDES);
			if (includeString != null) {
				String[] includes = Utils.getArrayFromString(includeString);
				for (int i = 0; i < includes.length; i++)
					if (includes[i].equals(DOT))
						includes[i] = EXPANDED_DOT + '/';
				properties.setProperty(PROPERTY_BIN_INCLUDES, Utils.getStringFromArray(includes, ",")); //$NON-NLS-1$
			}
			return true;
		}
		return false;
	}

	/**
	 * Main call for generating the script.
	 * 
	 * @throws CoreException
	 */
	private void generateBuildScript() throws CoreException {
		generatePrologue();
		generateBuildUpdateJarTarget();

		if (getBuildProperties().getProperty(SOURCE_PLUGIN, null) == null) {
			generateBuildJarsTarget(model);
		} else {
			generateBuildJarsTargetForSourceGathering();
			generateEmptyBuildSourcesTarget();
		}
		generateGatherBinPartsTarget();
		generateBuildZipsTarget();
		generateGatherSourcesTarget();
		generateGatherIndividualSourcesTarget();
		generateCopySourcesTarget();
		generateGatherLogTarget();
		generateCleanTarget();
		generateRefreshTarget();
		generateZipPluginTarget();
		generateEpilogue();
	}

	/**
	 * Method generateEmptyBuildSourceTarget.
	 */
	private void generateEmptyBuildSourcesTarget() {
		script.printTargetDeclaration(TARGET_BUILD_SOURCES, null, null, null, null);
		script.printTargetEnd();
	}

	/**
	 * Method generateBuildJarsTargetForSourceGathering.
	 */
	private void generateBuildJarsTargetForSourceGathering() {
		script.printTargetDeclaration(TARGET_BUILD_JARS, null, null, null, null);
		compiledJarNames = new ArrayList(0);

		Set pluginsToGatherSourceFrom = (Set) featureGenerator.getSourceToGather().getElementEntries().get(model.getSymbolicName());
		if (pluginsToGatherSourceFrom != null) {
			for (Iterator iter = pluginsToGatherSourceFrom.iterator(); iter.hasNext();) {
				BundleDescription plugin = (BundleDescription) iter.next();
				if (plugin.getSymbolicName().equals(model.getSymbolicName())) // We are not trying to gather the source from ourself since we are generated and we know we don't have source...
					continue;

				IPath location = Utils.makeRelative(new Path(getLocation(plugin)), new Path(getLocation(model)));
				if (!Utils.isSourceBundle(model)) {
					// The two steps are required, because some plugins (xerces, junit, ...) don't build their source: the source already comes zipped
					script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, location.toOSString(), TARGET_BUILD_SOURCES, null, null, null);
					HashMap params = new HashMap(1);
					params.put(PROPERTY_DESTINATION_TEMP_FOLDER, Utils.getPropertyFormat(PROPERTY_BASEDIR) + "/src"); //$NON-NLS-1$
					script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, location.toOSString(), TARGET_GATHER_SOURCES, null, null, params);
				}
			}
		}
		script.printTargetEnd();
	}

	/**
	 * Add the <code>clean</code> target to the given Ant script.
	 * 
	 * @throws CoreException
	 */
	private void generateCleanTarget() throws CoreException {
		script.println();
		Properties properties = getBuildProperties();
		ModelBuildScriptGenerator.CompiledEntry[] availableJars = extractEntriesToCompile(properties); // AspectJ change
		script.printTargetDeclaration(TARGET_CLEAN, TARGET_INIT, null, null, NLS.bind(Messages.build_plugin_clean, model.getSymbolicName()));

		Map params = null;
		if (customBuildCallbacks != null) {
			params = new HashMap(3);
			params.put(PROPERTY_PLUGIN_DESTINATION, PLUGIN_DESTINATION);
			params.put(PROPERTY_TEMP_FOLDER, Utils.getPropertyFormat(PROPERTY_TEMP_FOLDER));
			params.put(PROPERTY_BUILD_RESULT_FOLDER, Utils.getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER));
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_PRE + TARGET_CLEAN, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, null);
		}
		for (int i = 0; i < availableJars.length; i++) {
			String jarName = ((CompiledEntry)availableJars[i]).getName(true); // AspectJ change
			String jarLocation = getJARLocation(jarName);
			//avoid destructive cleans
			if (jarLocation.equals("") || jarLocation.startsWith(DOT + DOT) || jarLocation.equals(Utils.getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER))) //$NON-NLS-1$
				continue;
			if (((CompiledEntry)availableJars[i]).type == CompiledEntry.JAR) { // AspectJ change
				script.printDeleteTask(null, jarLocation, null);
			} else {
				script.printDeleteTask(jarLocation, null, null);
			}
			script.printDeleteTask(null, getSRCLocation(jarName), null);
		}
		script.printDeleteTask(null, pluginUpdateJarDestination, null);
		script.printDeleteTask(null, pluginZipDestination, null);
		script.printDeleteTask(Utils.getPropertyFormat(IXMLConstants.PROPERTY_TEMP_FOLDER), null, null);

		if (customBuildCallbacks != null) {
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_POST + TARGET_CLEAN, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, null);
		}
		script.printTargetEnd();
	}

	/**
	 * Add the <code>gather.logs</code> target to the given Ant script.
	 * 
	 * @throws CoreException
	 */
	private void generateGatherLogTarget() throws CoreException {
		script.println();
		script.printTargetDeclaration(TARGET_GATHER_LOGS, TARGET_INIT, PROPERTY_DESTINATION_TEMP_FOLDER, null, null);
		IPath baseDestination = new Path(Utils.getPropertyFormat(PROPERTY_DESTINATION_TEMP_FOLDER));
		baseDestination = baseDestination.append(fullName);
		Map params = null;
		if (customBuildCallbacks != null) {
			params = new HashMap(1);
			params.put(PROPERTY_DESTINATION_TEMP_FOLDER, baseDestination.toString());
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_PRE + TARGET_GATHER_LOGS, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, null);
		}
		List destinations = new ArrayList(5);
		Properties properties = getBuildProperties();
		ModelBuildScriptGenerator.CompiledEntry[] availableJars = extractEntriesToCompile(properties); // AspectJ change
		for (int i = 0; i < availableJars.length; i++) {
			String name = ((CompiledEntry)availableJars[i]).getName(true);  // AspectJ change
			IPath destination = baseDestination.append(name).removeLastSegments(1); // remove the jar name
			if (!destinations.contains(destination)) {
				script.printMkdirTask(destination.toString());
				destinations.add(destination);
			}
			Path logPath = new Path(getTempJARFolderLocation(name) + Utils.getPropertyFormat(PROPERTY_LOG_EXTENSION));
			FileSet logSet = new FileSet(logPath.removeLastSegments(1).toString(), null, logPath.lastSegment(), null, null, null, null);
			script.printCopyTask(null, destination.toString(), new FileSet[] {logSet}, false, false);
		}

		if (customBuildCallbacks != null) {
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_POST + TARGET_GATHER_LOGS, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, null);
		}
		script.printTargetEnd();
	}

	/**
	 * 
	 * @param zipName
	 * @param source
	 */
	private void generateZipIndividualTarget(String zipName, String source) {
		script.println();
		script.printTargetDeclaration(zipName, TARGET_INIT, null, null, null);
		IPath root = new Path(Utils.getPropertyFormat(IXMLConstants.PROPERTY_BASEDIR));
		script.printZipTask(root.append(zipName).toString(), root.append(source).toString(), false, false, null);
		script.printTargetEnd();
	}

	/**
	 * Add the <code>gather.sources</code> target to the given Ant script.
	 * 
	 * @throws CoreException
	 */
	private void generateGatherSourcesTarget() throws CoreException {
		script.println();
		script.printTargetDeclaration(TARGET_GATHER_SOURCES, TARGET_INIT, PROPERTY_DESTINATION_TEMP_FOLDER, null, null);

		IPath baseDestination = new Path(Utils.getPropertyFormat(PROPERTY_DESTINATION_TEMP_FOLDER));
		baseDestination = baseDestination.append(fullName);
		Map params = null;
		if (customBuildCallbacks != null) {
			params = new HashMap(1);
			params.put(PROPERTY_TARGET_FOLDER, baseDestination.toString());
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_PRE + TARGET_GATHER_SOURCES, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, null);
		}
		List destinations = new ArrayList(5);
		Properties properties = getBuildProperties();
		ModelBuildScriptGenerator.CompiledEntry[] availableJars = extractEntriesToCompile(properties); // AspectJ Change
		for (int i = 0; i < availableJars.length; i++) {
			String jar = ((CompiledEntry) availableJars[i]).getName(true); // AspectJ Change
			IPath destination = baseDestination.append(jar).removeLastSegments(1); // remove the jar name
			if (!destinations.contains(destination)) {
				script.printMkdirTask(destination.toString());
				destinations.add(destination);
			}
			script.printCopyTask(getSRCLocation(jar), destination.toString(), null, false, false);
		}

		Properties copyParams = new Properties();
		copyParams.put(PROPERTY_SOURCE_DESTINATION_FOLDER, baseDestination.toString());
		script.printAntCallTask(TARGET_COPY_SRC_INCLUDES, true, copyParams);

		if (customBuildCallbacks != null) {
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_POST + TARGET_GATHER_SOURCES, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, null);
		}
		script.printTargetEnd();
	}

	private void generateGatherIndividualSourcesTarget() throws CoreException {
		script.println();
		script.printTargetDeclaration(TARGET_GATHER_INDIVIDUAL_SOURCES, TARGET_INIT, null, null, null);

		IPath baseDestination = new Path(Utils.getPropertyFormat(PROPERTY_DESTINATION_TEMP_FOLDER));

		Map params = null;
		if (customBuildCallbacks != null) {
			params = new HashMap(1);
			params.put(PROPERTY_TARGET_FOLDER, baseDestination.toString());
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_PRE + TARGET_GATHER_SOURCES, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, null);
		}

		Properties copyParams = new Properties();
		copyParams.put(PROPERTY_SOURCE_DESTINATION_FOLDER, baseDestination.toString());

		Properties properties = getBuildProperties();
		ModelBuildScriptGenerator.CompiledEntry[] availableJars = extractEntriesToCompile(properties); // AspectJ Change
		for (int i = 0; i < availableJars.length; i++) {
			String jar = ((CompiledEntry) availableJars[i]).getName(true); // AspectJ Change
			String srcName = getSRCName(jar);

			script.printAntCallTask("copy." + srcName, true, copyParams); //$NON-NLS-1$
		}

		script.printAntCallTask(TARGET_COPY_SRC_INCLUDES, true, copyParams);

		if (customBuildCallbacks != null) {
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_POST + TARGET_GATHER_SOURCES, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, null);
		}
		script.printTargetEnd();
	}

	private void generateCopySourcesTarget() throws CoreException {
		script.println();
		script.printTargetDeclaration(TARGET_COPY_SRC_INCLUDES, TARGET_INIT, null, null, null);

		IPath baseDestination = new Path(Utils.getPropertyFormat(PROPERTY_SOURCE_DESTINATION_FOLDER));
		String include = (String) getBuildProperties().get(PROPERTY_SRC_INCLUDES);
		String exclude = (String) getBuildProperties().get(PROPERTY_SRC_EXCLUDES);
		if (include != null || exclude != null) {
			FileSet fileSet = new FileSet(Utils.getPropertyFormat(PROPERTY_BASEDIR), null, include, null, exclude, null, null);
			script.printCopyTask(null, baseDestination.toString(), new FileSet[] {fileSet}, false, false);
		}
		script.printTargetEnd();
	}

	/**
	 * Add the <code>gather.bin.parts</code> target to the given Ant script.
	 * 
	 * @throws CoreException
	 */
	private void generateGatherBinPartsTarget() throws CoreException {
		script.println();
		script.printTargetDeclaration(TARGET_GATHER_BIN_PARTS, TARGET_INIT, PROPERTY_DESTINATION_TEMP_FOLDER, null, null);
		IPath destination = new Path(Utils.getPropertyFormat(PROPERTY_DESTINATION_TEMP_FOLDER));
		destination = destination.append(fullName);
		String root = destination.toString();
		script.printMkdirTask(root);

		Map params = null;
		if (customBuildCallbacks != null) {
			params = new HashMap(3);
			params.put(PROPERTY_TARGET_FOLDER, root);
			params.put(PROPERTY_BUILD_RESULT_FOLDER, Utils.getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER));
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_PRE + TARGET_GATHER_BIN_PARTS, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, null);
		}

		List destinations = new ArrayList(5);
		destinations.add(destination);
		String include = (String) getBuildProperties().get(PROPERTY_BIN_INCLUDES);
		String exclude = (String) getBuildProperties().get(PROPERTY_BIN_EXCLUDES);

		//Copy only the jars that has been compiled and are listed in the includes
		String[] splitIncludes = Utils.getArrayFromString(include);
		String[] fileSetValues = new String[compiledJarNames.size()];
		int count = 0;

		boolean dotIncluded = false; //This flag indicates if . should be gathered
		int pos = Utils.isStringIn(splitIncludes, EXPANDED_DOT + '/');
		if (pos != -1) {
			splitIncludes[pos] = null;
			dotIncluded = true;
		}

		//Iterate over the classpath
		for (Iterator iter = compiledJarNames.iterator(); iter.hasNext();) {
			CompiledEntry entry = (CompiledEntry) iter.next();
			String formatedName = entry.getName(false) + (entry.getType() == CompiledEntry.FOLDER ? "/" : ""); //$NON-NLS-1$//$NON-NLS-2$
			if (dotOnTheClasspath && formatedName.startsWith(EXPANDED_DOT)) {
				dotIncluded = dotIncluded & true;
				continue;
			}
			fileSetValues[count++] = formatedName;
			continue;
		}
		if (count != 0) {
			FileSet fileSet = new FileSet(Utils.getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER), null, Utils.getStringFromArray(fileSetValues, ","), null, replaceVariables(exclude, true), null, null); //$NON-NLS-1$
			script.printCopyTask(null, root, new FileSet[] {fileSet}, true, false);
		}
		//Dot on the classpath need to be copied in a special way
		if (dotIncluded) {
			FileSet fileSet = new FileSet(Utils.getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER) + '/' + EXPANDED_DOT, null, "**", null, replaceVariables(exclude, true), null, null); //$NON-NLS-1$
			script.printCopyTask(null, root, new FileSet[] {fileSet}, true, false);
		}
		//General copy of the files listed in the includes
		if (include != null || exclude != null) {
			String includeSet = replaceVariables(Utils.getStringFromArray(splitIncludes, ","), true); //$NON-NLS-1$
			if (includeSet != null && includeSet.length() > 0) {
				FileSet fileSet = new FileSet(Utils.getPropertyFormat(PROPERTY_BASEDIR), null, includeSet, null, replaceVariables(exclude, true), null, null);
				script.printCopyTask(null, root, new FileSet[] {fileSet}, true, false);
			}
		}

		if (Utils.isSourceBundle(model)) {
			Set pluginsToGatherSourceFrom = (Set) featureGenerator.getSourceToGather().getElementEntries().get(model.getSymbolicName());
			if (pluginsToGatherSourceFrom != null) {
				for (Iterator iter = pluginsToGatherSourceFrom.iterator(); iter.hasNext();) {
					BundleDescription plugin = (BundleDescription) iter.next();
					IPath location = Utils.makeRelative(new Path(getLocation(plugin)), new Path(getLocation(model)));
					HashMap taskParams = new HashMap(1);
					taskParams.put(PROPERTY_DESTINATION_TEMP_FOLDER, root);
					script.printAntTask(DEFAULT_BUILD_SCRIPT_FILENAME, location.toOSString(), TARGET_GATHER_INDIVIDUAL_SOURCES, null, null, taskParams);
				}
			}
		}

		generatePermissionProperties(root);
		genarateIdReplacementCall(destination.toString());

		if (customBuildCallbacks != null) {
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_POST + TARGET_GATHER_BIN_PARTS, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, null);
		}

		script.printTargetEnd();
	}

	private void genarateIdReplacementCall(String location) {
		Properties bundleProperties = (Properties) model.getUserObject();
		if (bundleProperties == null)
			return;

		String qualifier = bundleProperties.getProperty(PROPERTY_QUALIFIER);
		if (qualifier == null)
			return;
		script.println("<eclipse.versionReplacer path=\"" + AntScript.getEscaped(location) + "\" version=\"" + model.getVersion() + "\"/>"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
	}

	private void generatePermissionProperties(String directory) throws CoreException {
		getPermissionProperties();
		for (Iterator iter = permissionProperties.entrySet().iterator(); iter.hasNext();) {
			Map.Entry permission = (Map.Entry) iter.next();
			String instruction = (String) permission.getKey();
			String parameters = (String) permission.getValue();
			int index;
			if ((index = instruction.indexOf(PERMISSIONS)) != -1) {
				generateChmodInstruction(directory, instruction.substring(index + PERMISSIONS.length() + 1), parameters);
				continue;
			}
			if (instruction.startsWith(LINK)) {
				generateLinkInstruction(directory, parameters);
			}
		}
	}

	private void generateChmodInstruction(String dir, String rights, String files) {
		// TO CHECK We only consider rights specified with numbers
		if (rights.equals(EXECUTABLE)) {
			rights = "755"; //$NON-NLS-1$
		}
		script.printChmod(dir, rights, files);
	}

	private void generateLinkInstruction(String dir, String files) {
		String[] links = Utils.getArrayFromString(files, ","); //$NON-NLS-1$
		List arguments = new ArrayList(2);
		for (int i = 0; i < links.length; i += 2) {
			arguments.add(links[i]);
			arguments.add(links[i + 1]);
			script.printExecTask("ln -s", dir, arguments, "Linux"); //$NON-NLS-1$ //$NON-NLS-2$
			arguments.clear();
		}
	}

	protected Properties getPermissionProperties() throws CoreException {
		if (permissionProperties == null) {
			permissionProperties = readProperties(getLocation(model), PERMISSIONS_FILE, IStatus.INFO);
		}
		return permissionProperties;
	}

	/**
	 * Add the <code>zip.plugin</code> target to the given Ant script.
	 */
	private void generateZipPluginTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_ZIP_PLUGIN, TARGET_INIT, null, null, NLS.bind(Messages.build_plugin_zipPlugin, model.getSymbolicName()));
		script.printDeleteTask(Utils.getPropertyFormat(PROPERTY_TEMP_FOLDER), null, null);
		script.printMkdirTask(Utils.getPropertyFormat(PROPERTY_TEMP_FOLDER));
		script.printAntCallTask(TARGET_BUILD_JARS, true, null);
		script.printAntCallTask(TARGET_BUILD_SOURCES, true, null);
		Map params = new HashMap(1);
		params.put(PROPERTY_DESTINATION_TEMP_FOLDER, Utils.getPropertyFormat(PROPERTY_TEMP_FOLDER) + '/');
		script.printAntCallTask(TARGET_GATHER_BIN_PARTS, true, params);
		script.printAntCallTask(TARGET_GATHER_SOURCES, true, params);
		FileSet fileSet = new FileSet(Utils.getPropertyFormat(PROPERTY_TEMP_FOLDER), null, "**/*.bin" + Utils.getPropertyFormat(PROPERTY_LOG_EXTENSION), null, null, null, null); //$NON-NLS-1$
		script.printDeleteTask(null, null, new FileSet[] {fileSet});
		script.printZipTask(pluginZipDestination, Utils.getPropertyFormat(PROPERTY_TEMP_FOLDER), true, false, null);
		script.printDeleteTask(Utils.getPropertyFormat(PROPERTY_TEMP_FOLDER), null, null);
		script.printTargetEnd();
	}

	/**
	 * Add the <code>build.update.jar</code> target to the given Ant script.
	 */
	private void generateBuildUpdateJarTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_BUILD_UPDATE_JAR, TARGET_INIT, null, null, NLS.bind(Messages.build_plugin_buildUpdateJar, model.getSymbolicName()));
		script.printDeleteTask(Utils.getPropertyFormat(PROPERTY_TEMP_FOLDER), null, null);
		script.printMkdirTask(Utils.getPropertyFormat(PROPERTY_TEMP_FOLDER));
		script.printAntCallTask(TARGET_BUILD_JARS, true, null);
		Map params = new HashMap(1);
		params.put(PROPERTY_DESTINATION_TEMP_FOLDER, Utils.getPropertyFormat(PROPERTY_TEMP_FOLDER) + '/');
		script.printAntCallTask(TARGET_GATHER_BIN_PARTS, true, params);
		script.printJarTask(pluginUpdateJarDestination, Utils.getPropertyFormat(PROPERTY_TEMP_FOLDER) + '/' + fullName, null, "merge"); //$NON-NLS-1$
		script.printDeleteTask(Utils.getPropertyFormat(PROPERTY_TEMP_FOLDER), null, null);
		if (signJars)
			script.println("<eclipse.jarProcessor sign=\"" + Utils.getPropertyFormat(PROPERTY_SIGN) + "\" pack=\"" + Utils.getPropertyFormat(PROPERTY_PACK) + "\" unsign=\"" + Utils.getPropertyFormat(PROPERTY_UNSIGN) + "\" jar=\"" + AntScript.getEscaped(pluginUpdateJarDestination) + "\" alias=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_ALIAS) + "\" keystore=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_KEYSTORE) + "\" storepass=\"" + Utils.getPropertyFormat(PROPERTY_SIGN_STOREPASS) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ 
		script.printTargetEnd();
	}

	/**
	 * Add the <code>refresh</code> target to the given Ant script.
	 */
	private void generateRefreshTarget() {
		script.println();
		script.printTargetDeclaration(TARGET_REFRESH, TARGET_INIT, PROPERTY_ECLIPSE_RUNNING, null, Messages.build_plugin_refresh);
		script.printConvertPathTask(new Path(getLocation(model)).removeLastSegments(0).toOSString().replace('\\', '/'), PROPERTY_RESOURCE_PATH, false);
		script.printRefreshLocalTask(Utils.getPropertyFormat(PROPERTY_RESOURCE_PATH), "infinite"); //$NON-NLS-1$
		script.printTargetEnd();
	}

	/**
	 * End the script by closing the project element.
	 */
	private void generateEpilogue() {
		script.println();
		script.printProjectEnd();
	}

	/**
	 * Defines, the XML declaration, Ant project and targets init and initTemplate.
	 */
	private void generatePrologue() {
		script.printProjectDeclaration(model.getSymbolicName(), TARGET_BUILD_JARS, DOT);
		script.println();

		script.printProperty(PROPERTY_BASE_WS, Utils.getPropertyFormat(PROPERTY_WS));
		script.printProperty(PROPERTY_BASE_OS, Utils.getPropertyFormat(PROPERTY_OS));
		script.printProperty(PROPERTY_BASE_ARCH, Utils.getPropertyFormat(PROPERTY_ARCH));
		script.printProperty(PROPERTY_BASE_NL, Utils.getPropertyFormat(PROPERTY_NL));
		script.printProperty(PROPERTY_BUNDLE_ID, model.getSymbolicName());
		script.printProperty(PROPERTY_BUNDLE_VERSION, model.getVersion().toString());
		script.println();

		if (customBuildCallbacks != null && !customBuildCallbacks.equals(FALSE)) {
			script.printAvailableTask(PROPERTY_CUSTOM_BUILD_CALLBACKS, customCallbacksBuildpath + '/' + customBuildCallbacks, customBuildCallbacks);
			script.println();
		}

		generateCompilerSettings();

		script.printTargetDeclaration(TARGET_INIT, TARGET_PROPERTIES, null, null, null);
		script.printConditionIsSet(PROPERTY_PLUGIN_TEMP, Utils.getPropertyFormat(PROPERTY_BUILD_TEMP) + '/' + DEFAULT_PLUGIN_LOCATION, PROPERTY_BUILD_TEMP);
		script.printProperty(PROPERTY_PLUGIN_TEMP, Utils.getPropertyFormat(PROPERTY_BASEDIR));
		script.printConditionIsSet(PROPERTY_BUILD_RESULT_FOLDER, Utils.getPropertyFormat(PROPERTY_PLUGIN_TEMP) + '/' + model.getSymbolicName() + '_' + model.getVersion(), PROPERTY_BUILD_TEMP);
		script.printProperty(PROPERTY_BUILD_RESULT_FOLDER, Utils.getPropertyFormat(PROPERTY_BASEDIR));

		script.printProperty(PROPERTY_TEMP_FOLDER, Utils.getPropertyFormat(PROPERTY_BASEDIR) + '/' + PROPERTY_TEMP_FOLDER);
		script.printProperty(PROPERTY_PLUGIN_DESTINATION, Utils.getPropertyFormat(PROPERTY_BASEDIR));
		script.printTargetEnd();
		script.println();
		script.printTargetDeclaration(TARGET_PROPERTIES, null, PROPERTY_ECLIPSE_RUNNING, null, null);
		script.printProperty(PROPERTY_BUILD_COMPILER, JDT_COMPILER_ADAPTER);
		script.println();

		script.printTargetEnd();
	}

	private void generateCompilerSettings() {
		String javacSource = null;
		String javacTarget = null;
		String bootClasspath = null;
		String jreProfile = null;
		try {
			Properties properties = getBuildProperties();
			javacSource = properties.getProperty(IBuildPropertiesConstants.PROPERTY_JAVAC_SOURCE);
			javacTarget = properties.getProperty(IBuildPropertiesConstants.PROPERTY_JAVAC_TARGET);
			bootClasspath = properties.getProperty(IBuildPropertiesConstants.PROPERTY_BOOT_CLASSPATH);
			jreProfile = properties.getProperty(IBuildPropertiesConstants.PROPERTY_JRE_COMPILATION_PROFILE);
		} catch (CoreException e) {
			//ignore
		}

		script.printComment(Messages.build_compilerSetting);
		script.printProperty(PROPERTY_JAVAC_FAIL_ON_ERROR, "false"); //$NON-NLS-1$
		script.printProperty(PROPERTY_JAVAC_DEBUG_INFO, "on"); //$NON-NLS-1$
		script.printProperty(PROPERTY_JAVAC_VERBOSE, "false"); //$NON-NLS-1$
		script.printProperty(PROPERTY_LOG_EXTENSION, ".log"); //$NON-NLS-1$
		script.printProperty(PROPERTY_JAVAC_COMPILERARG, ""); //$NON-NLS-1$  

		if (javacSource == null)
			script.printProperty(IXMLConstants.PROPERTY_JAVAC_SOURCE, "1.3"); //$NON-NLS-1$
		if (javacTarget == null)
			script.printProperty(IXMLConstants.PROPERTY_JAVAC_TARGET, "1.2"); //$NON-NLS-1$  
		if (bootClasspath == null) {
			script.println("<condition property=\"dir_bootclasspath\" value=\"${java.home}/../Classes\">");//$NON-NLS-1$  
			script.println("\t<os family=\"mac\"/>");//$NON-NLS-1$  
			script.println("</condition>");//$NON-NLS-1$  
			script.println("<property name=\"dir_bootclasspath\" value=\"${java.home}/lib\"/>");//$NON-NLS-1$  
			script.println("<path id=\"path_bootclasspath\">");//$NON-NLS-1$  
			script.println("\t<fileset dir=\"${dir_bootclasspath}\">");//$NON-NLS-1$  
			script.println("\t\t<include name=\"*.jar\"/>");//$NON-NLS-1$  
			script.println("\t</fileset>");//$NON-NLS-1$  
			script.println("</path>");//$NON-NLS-1$  
			script.printPropertyRefid(PROPERTY_BOOTCLASSPATH, "path_bootclasspath"); //$NON-NLS-1$
		}

		Properties environmentMappings = getExecutionEnvironmentMappings();
		if (jreProfile != null && !environmentMappings.containsKey(jreProfile + '.' + IXMLConstants.PROPERTY_JAVAC_SOURCE)) {
			if (reportResolutionErrors) {
				IStatus status = new Status(IStatus.ERROR, model.getSymbolicName(), IStatus.ERROR, NLS.bind(Messages.build_plugin_unrecognizedJRE, jreProfile), null);
				BundleHelper.getDefault().getLog().log(status);
			}
			jreProfile = null;
		}

		if (javacSource != null)
			script.printProperty(PROPERTY_BUNDLE_JAVAC_SOURCE, javacSource);
		if (javacTarget != null)
			script.printProperty(PROPERTY_BUNDLE_JAVAC_TARGET, javacTarget);
		if (bootClasspath != null)
			script.printProperty(PROPERTY_BUNDLE_BOOTCLASSPATH, bootClasspath);

		String source, target = null;
		String[] modelEnvironments = model.getExecutionEnvironments();
		String[] environments = null;
		if (jreProfile != null) {
			environments = new String[modelEnvironments.length + 1];
			environments[0] = jreProfile;
			System.arraycopy(modelEnvironments, 0, environments, 1, modelEnvironments.length);
		} else {
			environments = modelEnvironments;
		}
		for (int i = 0; i < environments.length; i++) {
			if (bootClasspath == null)
				script.printConditionIsSet(PROPERTY_BUNDLE_BOOTCLASSPATH, Utils.getPropertyFormat(environments[i]), environments[i]);

			source = (String) environmentMappings.get(environments[i] + '.' + IXMLConstants.PROPERTY_JAVAC_SOURCE);
			target = (String) environmentMappings.get(environments[i] + '.' + IXMLConstants.PROPERTY_JAVAC_TARGET);
			if (javacSource == null && source != null)
				script.printConditionIsSet(PROPERTY_BUNDLE_JAVAC_SOURCE, source, environments[i]);
			if (javacTarget == null && target != null)
				script.printConditionIsSet(PROPERTY_BUNDLE_JAVAC_TARGET, target, environments[i]);
		}

		if (javacSource == null)
			script.printProperty(PROPERTY_BUNDLE_JAVAC_SOURCE, Utils.getPropertyFormat(IXMLConstants.PROPERTY_JAVAC_SOURCE));
		if (javacTarget == null)
			script.printProperty(PROPERTY_BUNDLE_JAVAC_TARGET, Utils.getPropertyFormat(IXMLConstants.PROPERTY_JAVAC_TARGET));
		if (bootClasspath == null)
			script.printProperty(PROPERTY_BUNDLE_BOOTCLASSPATH, Utils.getPropertyFormat(PROPERTY_BOOTCLASSPATH));
		script.println();
	}

	/**
	 * Sets the PluginModel to generate script from.
	 * 
	 * @param model
	 * @throws CoreException
	 */
	public void setModel(BundleDescription model) throws CoreException {
		if (model == null) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ELEMENT_MISSING, Messages.error_missingElement, null));
		}
		model = getSite(false).getRegistry().getVersionReplacement(model);
		this.model = model;
		if (getBuildProperties() == AbstractScriptGenerator.MissingProperties.getInstance()) {
			//if there were no build.properties, then it is a binary plugin
			binaryPlugin = true;
		} else {
			getCompiledElements().add(model.getSymbolicName());
		}
		Properties bundleProperties = (Properties) model.getUserObject();
		if (bundleProperties == null) {
			bundleProperties = new Properties();
			model.setUserObject(bundleProperties);
		}
		bundleProperties.put(IS_COMPILED, binaryPlugin ? Boolean.FALSE : Boolean.TRUE);
	}

	/**
	 * Sets model to generate scripts from.
	 * 
	 * @param modelId
	 * @throws CoreException
	 */
	public void setModelId(String modelId, String modelVersion) throws CoreException {
		BundleDescription newModel = getModel(modelId, modelVersion);
		if (newModel == null) {
			String message = NLS.bind(Messages.exception_missingElement, modelId);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ELEMENT_MISSING, message, null));
		}
		setModel(newModel);
	}

	/**
	 * Add the <code>build.zips</code> target to the given Ant script.
	 * 
	 * @throws CoreException
	 */
	private void generateBuildZipsTarget() throws CoreException {
		StringBuffer zips = new StringBuffer();
		Properties props = getBuildProperties();
		for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			String key = (String) entry.getKey();
			if (key.startsWith(PROPERTY_SOURCE_PREFIX) && key.endsWith(PROPERTY_ZIP_SUFFIX)) {
				String zipName = key.substring(PROPERTY_SOURCE_PREFIX.length());
				zips.append(',');
				zips.append(zipName);
				generateZipIndividualTarget(zipName, (String) entry.getValue());
			}
		}
		script.println();
		script.printTargetDeclaration(TARGET_BUILD_ZIPS, TARGET_INIT + zips.toString(), null, null, null);
		script.printTargetEnd();
	}

	/**
	 * Sets the featureGenerator.
	 * @param featureGenerator The featureGenerator to set
	 */
	public void setFeatureGenerator(AJBuildDirector featureGenerator) {  // AspectJ Change
		this.featureGenerator = featureGenerator;
	}

	/**
	 * Add the "build.jars" target to the given Ant script using the specified plug-in model.
	 * 
	 * @param pluginModel the plug-in model to reference
	 * @throws CoreException
	 */
	private void generateBuildJarsTarget(BundleDescription pluginModel) throws CoreException {
		Properties properties = getBuildProperties();
		ModelBuildScriptGenerator.CompiledEntry[] availableJars = extractEntriesToCompile(properties); // AspectJ change
		compiledJarNames = new ArrayList(availableJars.length);
		Map jars = new HashMap(availableJars.length);
		for (int i = 0; i < availableJars.length; i++)
			jars.put(((CompiledEntry)availableJars[i]).getName(false), availableJars[i]); // AspectJ change

		// Put the jars in a correct compile order
		String jarOrder = (String) getBuildProperties().get(PROPERTY_JAR_ORDER);
		IClasspathComputer classpath;
		if (AbstractScriptGenerator.isBuildingOSGi())
			classpath = new ClasspathComputer3_0(this);
		else
			classpath = new ClasspathComputer2_1(this);

		if (jarOrder != null) {
			String[] order = Utils.getArrayFromString(jarOrder);
			for (int i = 0; i < order.length; i++) {
				CompiledEntry jar = (CompiledEntry) jars.get(order[i]);
				if (jar == null)
					continue;

				compiledJarNames.add(jar);
				generateCompilationTarget(classpath.getClasspath(pluginModel, jar), jar);
				generateSRCTarget(jar);
				jars.remove(order[i]);
			}
		}
		for (Iterator iterator = jars.values().iterator(); iterator.hasNext();) {
			CompiledEntry jar = (CompiledEntry) iterator.next();
			compiledJarNames.add(jar);
			generateCompilationTarget(classpath.getClasspath(pluginModel, jar), jar);
			generateSRCTarget(jar);
		}
		script.println();
		script.printTargetDeclaration(TARGET_BUILD_JARS, TARGET_INIT, null, null, NLS.bind(Messages.build_plugin_buildJars, pluginModel.getSymbolicName()));
		Map params = null;
		if (customBuildCallbacks != null) {
			params = new HashMap(1);
			params.put(PROPERTY_BUILD_RESULT_FOLDER, Utils.getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER));
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_PRE + TARGET_BUILD_JARS, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, null);
		}
		for (Iterator iter = compiledJarNames.iterator(); iter.hasNext();) {
			String name = ((CompiledEntry) iter.next()).getName(false);
			script.printAvailableTask(name, replaceVariables(getJARLocation(name), true));
			script.printAntCallTask(name, true, null);
		}
		if (customBuildCallbacks != null) {
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_POST + TARGET_BUILD_JARS, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, null);
		}
		script.printTargetEnd();

		script.println();
		script.printTargetDeclaration(TARGET_BUILD_SOURCES, TARGET_INIT, null, null, null);
		if (customBuildCallbacks != null) {
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_PRE + TARGET_BUILD_SOURCES, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, null);
		}
		for (Iterator iter = compiledJarNames.iterator(); iter.hasNext();) {
			String jarName = ((CompiledEntry) iter.next()).getName(false);
			String srcName = getSRCName(jarName);
			script.printAvailableTask(srcName, getSRCLocation(jarName));
			script.printAntCallTask(srcName, true, null);
		}
		if (customBuildCallbacks != null) {
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_POST + TARGET_BUILD_SOURCES, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, null);
		}
		script.printTargetEnd();
	}

	/**
	 * generate compile settings for compiling this entry
	 * warning levels, default encoding, custom encodings
	 * @param javac
	 * @param entry
	 */
	private void generateCompilerSettings(JavacTask javac, CompiledEntry entry, List classpath) {
		final String ADAPTER_ENCODING = "#ADAPTER#ENCODING#"; //$NON-NLS-1$
		final String ADAPTER_ACCESS = "#ADAPTER#ACCESS#"; //$NON-NLS-1$

		Properties properties = null;
		try {
			properties = getBuildProperties();
		} catch (CoreException e) {
			return;
		}
		if (properties == null && classpath.size() == 0)
			return;

		String name = entry.getName(false);
		if (name.equals(EXPANDED_DOT))
			name = DOT;

		String defaultEncodingVal = properties.getProperty(PROPERTY_JAVAC_DEFAULT_ENCODING_PREFIX + name);
		if (defaultEncodingVal != null)
			javac.setEncoding(defaultEncodingVal);

		String customEncodingsVal = properties.getProperty(PROPERTY_JAVAC_CUSTOM_ENCODINGS_PREFIX + name);
		String warningLevels = properties.getProperty(PROPERTY_JAVAC_WARNINGS_PREFIX + name);

		if (customEncodingsVal == null && warningLevels == null && classpath.size() == 0) {
			return;
		}

		String root = getLocation(model);
		File file = new File(root, "javaCompiler." + name.replaceAll("[\\\\/]", "_") + ".args"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		if (file.exists()) {
			file.delete();
		}
		Writer writer = null;
		try {
			try {
				//only create the file if we are going to write something in it
				if (warningLevels != null || customEncodingsVal != null)
					writer = new BufferedWriter(new FileWriter(file));

				if (warningLevels != null) {
					writer.write("-warn:" + warningLevels + "\n"); //$NON-NLS-1$//$NON-NLS-2$
				}

				if (customEncodingsVal != null) {
					String[] encodings = customEncodingsVal.split(","); //$NON-NLS-1$
					if (encodings.length > 0) {
						for (int i = 0; i < encodings.length; i++) {
							writer.write(ADAPTER_ENCODING + encodings[i] + "\n"); //$NON-NLS-1$
						}
					}
				}
				//handle access rules if we are using ClasspathComputer3_0
				Properties data = (Properties) model.getUserObject();
				if (data == null || !data.containsKey(PROPERTY_CONVERTED_MANIFEST)) {
					if (classpath.size() > 0 && classpath.get(0) instanceof ClasspathElement) {
						for (Iterator iterator = classpath.iterator(); iterator.hasNext();) {
							ClasspathElement element = (ClasspathElement) iterator.next();
							if (element.getPath() != null && element.getPath().length() > 0 && element.getAccessRules().length() > 0) {
								String path = element.getPath();
								if (path.startsWith(Utils.getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER))) {
									//remove leading ${build.result.folder}/
									path = path.substring(Utils.getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER).length() + 1);
								}
								//remove leading ../../..
								path = path.replaceFirst("^(\\.\\.[\\\\/])*", ""); //$NON-NLS-1$//$NON-NLS-2$
								if (writer == null)
									writer = new BufferedWriter(new FileWriter(file));
								writer.write(ADAPTER_ACCESS + path + element.getAccessRules() + "\n"); //$NON-NLS-1$
							}
						}
					}
				}
				if (writer != null)
					javac.setCompileArgsFile(Utils.getPropertyFormat(PROPERTY_BASEDIR) + "/" + file.getName()); //$NON-NLS-1$
			} finally {
				if (writer != null)
					writer.close();
			}
		} catch (IOException e1) {
			//ignore
		}
	}

	/**
	 * Add the "jar" target to the given Ant script using the given classpath and
	 * jar as parameters.
	 * 
	 * @param classpath the classpath for the jar command
	 * @param entry
	 */
	private void generateCompilationTarget(List classpath, CompiledEntry entry) {
		script.println();
		String name = entry.getName(false);
		script.printTargetDeclaration(name, TARGET_INIT, null, entry.getName(true), NLS.bind(Messages.build_plugin_jar, model.getSymbolicName() + ' ' + name));
		String destdir = getTempJARFolderLocation(entry.getName(true));
		script.printDeleteTask(destdir, null, null);
		script.printMkdirTask(destdir);
		script.printPathStructure("path", name + PROPERTY_CLASSPATH, classpath); //$NON-NLS-1$

		String[] sources = entry.getSource();
		Map params = null, references = null;
		if (customBuildCallbacks != null) {
			params = new HashMap(2);
			params.put(PROPERTY_TARGET_FOLDER, destdir);
			for (int i = 1; i <= sources.length; i++) {
				params.put(PROPERTY_SOURCE_FOLDER + i, sources[i - 1]);
			}

			references = new HashMap(1);
			references.put(name + PROPERTY_CLASSPATH, null);
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_PRE + name, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, references);
		}

		script.printComment("compile the source code"); //$NON-NLS-1$
		// AspectJ Change Begin		
		String[] ajdeClasspath = null;
		try {
			addedBundles = new ArrayList();
			ajdeClasspath = bundleToCP(getModel("org.aspectj.ajde", null)); //$NON-NLS-1$
		} catch (CoreException e) {
		}		
		AJCTask javac = new AJCTask(buildConfig, ajdeClasspath);
		javac.setAspectpath(aspectpath);
		javac.setInpath(inpath);
		// AspectJ Change End
		javac.setClasspathId(name + PROPERTY_CLASSPATH);
		javac.setBootClasspath(Utils.getPropertyFormat(PROPERTY_BUNDLE_BOOTCLASSPATH));
		javac.setDestdir(destdir);
		javac.setFailOnError(Utils.getPropertyFormat(PROPERTY_JAVAC_FAIL_ON_ERROR));
		javac.setDebug(Utils.getPropertyFormat(PROPERTY_JAVAC_DEBUG_INFO));
		javac.setVerbose(Utils.getPropertyFormat(PROPERTY_JAVAC_VERBOSE));
		javac.setIncludeAntRuntime("no"); //$NON-NLS-1$
		javac.setSource(Utils.getPropertyFormat(PROPERTY_BUNDLE_JAVAC_SOURCE));
		javac.setTarget(Utils.getPropertyFormat(PROPERTY_BUNDLE_JAVAC_TARGET));
		javac.setCompileArgs(Utils.getPropertyFormat(PROPERTY_JAVAC_COMPILERARG));
		javac.setSrcdir(sources);
		javac.setLogExtension(Utils.getPropertyFormat(PROPERTY_LOG_EXTENSION));
		generateCompilerSettings(javac, entry, classpath);

		script.print(javac);
		script.printComment("Copy necessary resources"); //$NON-NLS-1$
		FileSet[] fileSets = new FileSet[sources.length];
		for (int i = 0; i < sources.length; i++) {
			// AspectJ Change Begin
			// Also filter .aj files when copying resources
			String excludes = "**/*.aj, **/*.java, **/package.htm*"; //$NON-NLS-1$
			// AspectJ Change End
			String excludedFromJar = entry.getExcludedFromJar();
			if (excludedFromJar != null)
				excludes += ',' + excludedFromJar;

			fileSets[i] = new FileSet(sources[i], null, null, null, excludes, null, null);
		}

		script.printCopyTask(null, destdir, fileSets, true, false);

		if (customBuildCallbacks != null) {
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_POST_COMPILE + name, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, references);
		}

		String jarLocation = getJARLocation(entry.getName(true));
		script.printMkdirTask(new Path(jarLocation).removeLastSegments(1).toString());

		if (entry.getType() == CompiledEntry.FOLDER) {
			FileSet[] binFolder = new FileSet[] {new FileSet(destdir, null, null, null, null, null, null)};
			script.printCopyTask(null, jarLocation, binFolder, true, false);
		} else {
			script.printJarTask(jarLocation, destdir, getEmbeddedManifestFile(entry, destdir));
		}
		script.printDeleteTask(destdir, null, null);

		if (customBuildCallbacks != null) {
			params.clear();
			params.put(PROPERTY_JAR_LOCATION, jarLocation);
			script.printSubantTask(Utils.getPropertyFormat(PROPERTY_CUSTOM_BUILD_CALLBACKS), PROPERTY_POST + name, customCallbacksBuildpath, customCallbacksFailOnError, customCallbacksInheritAll, params, references);
		}
		script.printTargetEnd();
	}

	private String getEmbeddedManifestFile(CompiledEntry jarEntry, String destdir) {
		try {
			String manifestName = getBuildProperties().getProperty(PROPERTY_MANIFEST_PREFIX + jarEntry.getName(true));
			if (manifestName == null)
				return null;
			return destdir + '/' + manifestName;
		} catch (CoreException e) {
			return null;
		}
	}

	/**
	 * 
	 * @param properties
	 * @return JAR[]
	 */
	// AspectJ Change Begin
	protected ModelBuildScriptGenerator.CompiledEntry[] extractEntriesToCompile(Properties properties) throws CoreException {
	// AspectJ Change End
		return extractEntriesToCompile(properties, model);
	}

	// AspectJ Change Begin
	public static ModelBuildScriptGenerator.CompiledEntry[] extractEntriesToCompile(Properties properties, BundleDescription model) throws CoreException {
	// AspectJ Change End
		List result = new ArrayList(5);
		int prefixLength = PROPERTY_SOURCE_PREFIX.length();
		for (Iterator iterator = properties.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			String key = (String) entry.getKey();
			if (!(key.startsWith(PROPERTY_SOURCE_PREFIX)))
				continue;
			key = key.substring(prefixLength);
			String[] source = Utils.getArrayFromString((String) entry.getValue());
			if (source.length == 0) {
				String message = NLS.bind(Messages.error_missingSourceFolder, model.getSymbolicName(), entry.getKey());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_GENERIC, message, null));
			}
			String[] output = Utils.getArrayFromString(properties.getProperty(PROPERTY_OUTPUT_PREFIX + key));
			String[] extraClasspath = Utils.getArrayFromString(properties.getProperty(PROPERTY_EXTRAPATH_PREFIX + key));
			String excludedFromJar = properties.getProperty(PROPERTY_EXCLUDE_PREFIX + key);
			CompiledEntry newEntry = new CompiledEntry(key, source, output, extraClasspath, excludedFromJar, key.endsWith(PROPERTY_JAR_SUFFIX) ? CompiledEntry.JAR : CompiledEntry.FOLDER);
			result.add(newEntry);
		}
		return (CompiledEntry[]) result.toArray(new CompiledEntry[result.size()]);
	}

	/**
	 * Add the "src" target to the given Ant script.
	 * 
	 * @param jar
	 */
	private void generateSRCTarget(CompiledEntry jar) {
		script.println();
		String name = jar.getName(false);
		String srcName = getSRCName(name);
		script.printTargetDeclaration(srcName, TARGET_INIT, null, srcName, null);
		String[] sources = jar.getSource();
		filterNonExistingSourceFolders(sources);
		FileSet[] fileSets = new FileSet[sources.length];
		int count = 0;
		for (int i = 0; i < sources.length; i++) {
			if (sources[i] != null) {
				// AspectJ Change Begin
				// Also include .aj files in source zip
				fileSets[count++] = new FileSet(sources[i], null, "**/*.aj, **/*.java", null, null, null, null); //$NON-NLS-1$
				// AspectJ Change Emd
			}
		}

		String srcLocation = getSRCLocation(name);
		String srcParent = new Path(srcLocation).removeLastSegments(1).toString();
		script.printMkdirTask(srcParent);
		script.printAntCallTask("zip." + srcName, true, null); //$NON-NLS-1$
		script.printTargetEnd();

		script.printTargetDeclaration("zip." + srcName, null, null, null, null); //$NON-NLS-1$
		if (count != 0)
			script.printZipTask(srcLocation, null, false, false, fileSets);
		script.printTargetEnd();

		script.printTargetDeclaration("copy." + srcName, null, null, null, null); //$NON-NLS-1$
		if (count != 0) {
			String dest = null;
			if(srcName.equals(SRC_ZIP) )
				dest = new Path(srcName).removeLastSegments(1).toString();  //src.zip can go in the root
			else
				dest = srcName.substring(0, srcName.length() - 4 ); //remove .zip, the rest go in folders
			String toDir = Utils.getPropertyFormat(PROPERTY_SOURCE_DESTINATION_FOLDER) + '/' + dest;
			script.printCopyTask(null, toDir, fileSets, true, true);
		}
		script.printTargetEnd();
	}

	private void filterNonExistingSourceFolders(String[] sources) {
		File pluginRoot;
		pluginRoot = new File(getLocation(model));
		for (int i = 0; i < sources.length; i++) {
			File file = new File(pluginRoot, sources[i]);
			if (!file.exists()) {
				sources[i] = null;
				IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, EXCEPTION_SOURCE_LOCATION_MISSING, NLS.bind(Messages.warning_cannotLocateSource, file.getAbsolutePath()), null);
				BundleHelper.getDefault().getLog().log(status);
			}
		}
	}

	/**
	 * Return the name of the zip file for the source for the jar with
	 * the given name.
	 * 
	 * @param jarName the name of the jar file
	 * @return String
	 */
	protected String getSRCLocation(String jarName) {
		return getJARLocation(getSRCName(jarName));
	}

	/**
	 * Return the location for a temporary file for the jar file with
	 * the given name.
	 * 
	 * @param jarName the name of the jar file
	 * @return String
	 */
	protected String getTempJARFolderLocation(String jarName) {
		IPath destination = new Path(Utils.getPropertyFormat(PROPERTY_TEMP_FOLDER));
		destination = destination.append(jarName + ".bin"); //$NON-NLS-1$
		return destination.toString();
	}

	/**
	 * Return the full location of the jar file.
	 * 
	 * @param jarName the name of the jar file
	 * @return String
	 */
	protected String getJARLocation(String jarName) {
		return new Path(Utils.getPropertyFormat(PROPERTY_BUILD_RESULT_FOLDER)).append(jarName).toString();
	}

	protected String[] getClasspathEntries(BundleDescription lookedUpModel) throws CoreException {
		return (String[]) getSite(false).getRegistry().getExtraData().get(new Long(lookedUpModel.getBundleId()));
	}

	protected Properties getBuildProperties() throws CoreException {
		if (buildProperties == null)
			return buildProperties = readProperties(model.getLocation(), propertiesFileName, isIgnoreMissingPropertiesFile() ? IStatus.OK : IStatus.WARNING);

		return buildProperties;
	}

	/**
	 * Return the name of the zip file for the source from the given jar name.
	 * 
	 * @param jarName the name of the jar file
	 * @return String
	 */
	public static String getSRCName(String jarName) {
		if (jarName.endsWith(".jar")) { //$NON-NLS-1$
			return jarName.substring(0, jarName.length() - 4) + SRC_ZIP;
		}
		if (jarName.equals(EXPANDED_DOT) || jarName.equals(DOT))
			return SRC_ZIP;
		return jarName.replace('/', '.') + SRC_ZIP;
	}

	/**
	 * If the model defines its own custom script, we do not generate a new one
	 * but we do try to update the version number.
	 */
	private void updateExistingScript() throws CoreException {
		String root = getLocation(model);
		File buildFile = new File(root, buildScriptFileName);
		if (!buildFile.exists()) {
			String message = NLS.bind(Messages.error_missingCustomBuildFile, buildFile);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, message, null));
		}
		try {
			Utils.updateVersion(buildFile, PROPERTY_VERSION_SUFFIX, model.getVersion().toString());
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_writeScript, buildFile);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, message, e));
		}
		return;
	}

	/**
	 * Substitute the value of an element description variable (variables that
	 * are found in files like plugin.xml, e.g. $ws$) by an Ant property.
	 * 
	 * @param sourceString
	 * @return String
	 */
	static protected String replaceVariables(String sourceString, boolean compiledElement) {
		if (sourceString == null)
			return null;

		int i = -1;
		String result = sourceString;
		while ((i = result.indexOf(DESCRIPTION_VARIABLE_WS)) >= 0)
			result = result.substring(0, i) + "ws/" + Utils.getPropertyFormat(compiledElement ? PROPERTY_WS : PROPERTY_BASE_WS) + result.substring(i + DESCRIPTION_VARIABLE_WS.length()); //$NON-NLS-1$
		while ((i = result.indexOf(DESCRIPTION_VARIABLE_OS)) >= 0)
			result = result.substring(0, i) + "os/" + Utils.getPropertyFormat(compiledElement ? PROPERTY_OS : PROPERTY_BASE_OS) + result.substring(i + DESCRIPTION_VARIABLE_OS.length()); //$NON-NLS-1$
		while ((i = result.indexOf(DESCRIPTION_VARIABLE_ARCH)) >= 0)
			result = result.substring(0, i) + "arch/" + Utils.getPropertyFormat(compiledElement ? PROPERTY_ARCH : PROPERTY_BASE_ARCH) + result.substring(i + DESCRIPTION_VARIABLE_OS.length()); //$NON-NLS-1$		
		while ((i = result.indexOf(DESCRIPTION_VARIABLE_NL)) >= 0)
			result = result.substring(0, i) + "nl/" + Utils.getPropertyFormat(compiledElement ? PROPERTY_NL : PROPERTY_BASE_NL) + result.substring(i + DESCRIPTION_VARIABLE_NL.length()); //$NON-NLS-1$
		return result;
	}

	public BundleDescription getModel() {
		return model;
	}

	public String getPropertiesFileName() {
		return propertiesFileName;
	}

	public void setPropertiesFileName(String propertyFileName) {
		this.propertiesFileName = propertyFileName;
	}

	public String getBuildScriptFileName() {
		return buildScriptFileName;
	}

	public void setBuildScriptFileName(String buildScriptFileName) {
		this.buildScriptFileName = buildScriptFileName;
	}

	/**
	 * Sets whether or not to sign any constructed jars.
	 * 
	 * @param value whether or not to sign any constructed JARs
	 */
	public void setSignJars(boolean value) {
		signJars = value;
	}

	/**
	 * Returns the model object which is associated with the given identifier.
	 * Returns <code>null</code> if the model object cannot be found.
	 * 
	 * @param modelId the identifier of the model object to lookup
	 * @return the model object or <code>null</code>
	 */
	protected BundleDescription getModel(String modelId, String modelVersion) throws CoreException {
		if (modelVersion == null)
			return getSite(false).getRegistry().getResolvedBundle(modelId);
		return getSite(false).getRegistry().getResolvedBundle(modelId, modelVersion);
	}

	public FeatureEntry getAssociatedEntry() {
		return associatedEntry;
	}

	public void setAssociatedEntry(FeatureEntry associatedEntry) {
		this.associatedEntry = associatedEntry;
	}
	
	
	// AspectJ Change Begin - aspectpath and inpath support
	public void setAspectpath(List aspectpath) {
		this.aspectpath = aspectpath;
	}
	public void setInpath(List inpath) {
		this.inpath = inpath;
	}

	private String[] bundleToCP(BundleDescription bundle) throws CoreException {
		if (addedBundles.contains(bundle)) {
			return new String[]{};
		}
		addedBundles.add(bundle);
		if (bundle.getName().equals("org.apache.ant")) { //$NON-NLS-1$
			return new String[]{};
		}
		
		List pathList = new ArrayList();
		String loc = bundle.getLocation();
		Path absPath = new Path(loc);
		IPath basePath = Utils.makeRelative(absPath, new Path(getLocation(model)));
		
		if ("jar".equalsIgnoreCase(basePath.getFileExtension())) { //$NON-NLS-1$
			pathList.add(basePath.toString());
		} else {
			String[] cpe = getClasspathEntries(bundle);
			for (int i = 0; i < cpe.length; i++) {
				pathList.add(basePath.append(cpe[i]).toString());
			}
		}		
		
		// now add prerequisite bundles
		BundleDescription[] prereqs = bundle.getResolvedRequires();
		for (int i = 0; i < prereqs.length; i++) {
			String[] pcp = bundleToCP(prereqs[i]);
			pathList.addAll(Arrays.asList(pcp));
		}
		
		String[] path = new String[pathList.size()];
		pathList.toArray(path);
		return path;
	}
	// AspectJ Change End

}