/**********************************************************************
Copyright (c) 2002 IBM Corporation and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
Contributors:
Adrian Colyer, Andy Clement, Tracy Gardner - initial version
Ian McGrath - updated compiler option retrieving methods
**********************************************************************/
package org.eclipse.ajdt.internal.ui.ajde;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.ajdt.core.AspectJPlugin;
import org.eclipse.ajdt.internal.core.AJDTEventTrace;
import org.eclipse.ajdt.internal.ui.preferences.AspectJPreferences;
import org.eclipse.ajdt.ui.AspectJUIPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaProject;

public class BuildOptionsAdapter
	implements org.aspectj.ajde.BuildOptionsAdapter {

//	private final static QualifiedName INPUTJARS =
//	    new QualifiedName(AspectJUIPlugin.PLUGIN_ID, "BuildOptions.inputJars"); 
//	private final static QualifiedName INPUTJARSBROWSEDIR =
//        new QualifiedName(AspectJUIPlugin.PLUGIN_ID,
//        "BuildOptions.inputJarsBrowseDir");    
//    private final static QualifiedName ASPECTJARSBROWSEDIR =
//        new QualifiedName(AspectJUIPlugin.PLUGIN_ID,
//        "BuildOptions.aspectJarsBrowseDir");    

    private final static QualifiedName INPATH = 
        new QualifiedName(AspectJUIPlugin.PLUGIN_ID, "BuildOptions.inpath");
    private final static QualifiedName INPATH_CON_KINDS = 
        new QualifiedName(AspectJUIPlugin.PLUGIN_ID, "BuildOptions.inpathConKinds");
    private final static QualifiedName INPATH_ENT_KINDS = 
        new QualifiedName(AspectJUIPlugin.PLUGIN_ID, "BuildOptions.inpathEntKinds");
    private final static QualifiedName ASPECTPATH = 
        new QualifiedName(AspectJUIPlugin.PLUGIN_ID, "BuildOptions.aspectpath");
    private final static QualifiedName ASPECTPATH_CON_KINDS = 
        new QualifiedName(AspectJUIPlugin.PLUGIN_ID, "BuildOptions.aspectpathConKinds");
    private final static QualifiedName ASPECTPATH_ENT_KINDS = 
        new QualifiedName(AspectJUIPlugin.PLUGIN_ID, "BuildOptions.aspectpathEntKinds");
    
	private final static QualifiedName OUTPUTJAR = 
	    new QualifiedName(AspectJUIPlugin.PLUGIN_ID, "BuildOptions.outputJar");	    
//	private final static QualifiedName SOURCEROOTS =
//	    new QualifiedName(AspectJUIPlugin.PLUGIN_ID, "BuildOptions.sourceRoots");
//	private final static QualifiedName ASPECTJARS =
//	    new QualifiedName(AspectJUIPlugin.PLUGIN_ID, "BuildOptions.aspectJars");
	
//		// Temporarily removing this option, will return nothing to the caller.
//	private final static QualifiedName CHAR_ENC =	
//		new QualifiedName(AspectJUIPlugin.PLUGIN_ID, "BuildOptions.characterEncoding");

//	private final static QualifiedName COMPILATION_STRICTNESS =
//		new QualifiedName(
//			AspectJUIPlugin.PLUGIN_ID,
//			"BuildOptions.compilationStrictness");

//	public final static QualifiedName JAVA_OR_AJ_EXT =
//			new QualifiedName(AspectJPlugin.PLUGIN_ID, "BuildOptions.javaOrAjExt");

	// Enumeration of values for COMPILATION_STRICTNESS key
//	public final static String COMPILATION_STRICTNESS_NORMAL = "NORMAL";
//	public final static String COMPILATION_STRICTNESS_LENIENT = "LENIENT";
//	public final static String COMPILATION_STRICTNESS_STRICT = "STRICT";

	// Default values for those options that are easy to default...
	public final static boolean INCREMENTAL_COMPILATION_DEFAULT = false;
	public final static boolean WEAVE_MESSAGES_DEFAULT = false;
	public final static boolean BUILD_ASM_DEFAULT = true;
//	public final static boolean PREPROCESS_DEFAULT = false;
//	public final static boolean SOURCE14_DEFAULT = false;
//	public final static boolean PORTING_MODE_DEFAULT = false;
//	public final static String COMPILATION_STRICTNESS_DEFAULT =
//		COMPILATION_STRICTNESS_NORMAL;
	public final static String NON_STANDARD_OPTS_DEFAULT = "-Xlint";
//	public final static String CHAR_ENC_DEFAULT =
//		System.getProperty("file.encoding");
	//	java.util.Locale.getDefault().getDisplayName();
	public final static boolean JAVA_OR_AJ_EXT_DEFAULT = false;

	/**
	 * Tries to get a project-specific nature options map if it exists.  
	 * If it is not found returns the JavaCore's map.
	 */
	public Map getJavaOptionsMap() {
		Map optionsMap = null;
			
		JavaProject project;
		try {
			project = (JavaProject)AspectJUIPlugin.getDefault().getCurrentProject().getNature(JavaCore.NATURE_ID);
			optionsMap = project.getOptions(true);
		} catch (CoreException e) {
		}
		
		if (optionsMap == null) {
			return JavaCore.getOptions();
		} else {
			return optionsMap;
		}
	}

	// Return formatted version of the current build options set
	public String toString() {
		StringBuffer formattedOptions = new StringBuffer();
		formattedOptions.append("Current Compiler options set:");
		formattedOptions.append(
			"[Incremental compilation=" + getIncrementalMode() + "]");
//		formattedOptions.append("[Just preprocess=" + getPreprocessMode() + "]");
//		formattedOptions.append(
//			"[Allow 1.4 assertions=" + getSourceOnePointFourMode() + "]");
//		formattedOptions.append("[Porting mode=" + getPortingMode() + "]");
//		formattedOptions.append("[WorkingDirectory='" + getWorkingOutputPath() + "']");
		formattedOptions.append(
			"[NonStandard options='" + getNonStandardOptions() + "']");
//		if (getLenientSpecMode())
//			formattedOptions.append("[Compilation Strictness=LENIENT]");
//		else if (this.getStrictSpecMode())
//			formattedOptions.append("[Compilation Strictness=STRICT]");
//		else
//			formattedOptions.append("[Compilation Strictness=NORMAL]");

		return formattedOptions.toString();
	}

	private boolean retrieveSettingBoolean(QualifiedName key) {

		IProject thisProject = AspectJUIPlugin.getDefault().getCurrentProject();

		try {
			String value = thisProject.getPersistentProperty(key);

			boolean valueB = new Boolean(value).booleanValue();
			return valueB;
		} catch (CoreException ce) {
			AspectJUIPlugin.getDefault().getErrorHandler().handleError(
				AspectJUIPlugin.getResourceString("buildOptionsAdapter.exceptionDuringRetrieve"),
				ce);
		}
		return false;
	}

	private String retrieveSettingString(QualifiedName key) {

		IProject thisProject = AspectJUIPlugin.getDefault().getCurrentProject();
		try {
			String value = thisProject.getPersistentProperty(key);
			if (value==null) {
				return "";
			}
			return value;
		} catch (CoreException ce) {
			AspectJUIPlugin.getDefault().getErrorHandler().handleError(
				AspectJUIPlugin.getResourceString("buildOptionsAdapter.exceptionDuringRetrieve"),
				ce);
		}
		return "";
	}

	/**
	 * @see BuildOptionsAdapter#getLenientSpecMode()
	 */
	public boolean getLenientSpecMode() {
		return false;
	}
//		ensurePropertiesInitialized();
//
//		String compilationStrictness = retrieveSettingString(COMPILATION_STRICTNESS);
//		boolean lenientMode =
//			compilationStrictness.equals(COMPILATION_STRICTNESS_LENIENT);
//
//		if (AspectJPlugin.DEBUG_BUILDER) {
//			System.out.println(
//				"BuildOptionsAdapter.getLenientSpecMode called, returning :"
//					+ new Boolean(lenientMode));
//		}
//
//		return lenientMode;
//	}

	/**
	 * @see BuildOptionsAdapter#getNonStandardOptions()
	 */
	public String getNonStandardOptions() {
		//ensurePropertiesInitialized();
		IProject currentProject = AspectJUIPlugin.getDefault().getCurrentProject();
		String nonStandardOptions = AspectJPreferences.getCompilerOptions(currentProject);
		if (AspectJUIPlugin.DEBUG_BUILDER) {
			System.out.println(
				"BuildOptionsAdapter.getNonStandardOptions called, returning :"
					+ nonStandardOptions);
		}
		
		nonStandardOptions += AspectJPreferences.getLintOptions(currentProject);
		nonStandardOptions += AspectJPreferences.getAdvancedOptions(currentProject);
		if (AspectJUIPlugin.getDefault().getAjdtBuildOptionsAdapter().getShowWeaveMessages()) {
			nonStandardOptions += " -showWeaveInfo";
		}
		return nonStandardOptions;
	}

	/**
	 * @see BuildOptionsAdapter#getPortingMode()
	 */
	public boolean getPortingMode() {
		return false;
	}
//		ensurePropertiesInitialized();
//
//		boolean portingMode = retrieveSettingBoolean(PORTING_MODE);
//		if (AspectJPlugin.DEBUG_BUILDER) {
//			System.out.println(
//				"BuildOptionsAdapter.getPortingMode called, returning :"
//					+ new Boolean(portingMode));
//		}
//		return portingMode;
//	}

	/**
	 * @see BuildOptionsAdapter#getPreprocessMode()
	 */
	public boolean getPreprocessMode() {
		return false;
	}
//		ensurePropertiesInitialized();
//
//		boolean preprocessMode = retrieveSettingBoolean(PREPROCESS);
//
//		if (AspectJPlugin.DEBUG_BUILDER) {
//			System.out.println(
//				"BuildOptionsAdapter.getPreprocessMode called, returning :"
//					+ new Boolean(preprocessMode));
//		}
//		return preprocessMode;
//
//	}

	/**
	 * @see BuildOptionsAdapter#getSourceOnePointFourMode()
	 */
	public boolean getSourceOnePointFourMode() {
		return false;
	}
//		ensurePropertiesInitialized();
//
//		boolean source14 = retrieveSettingBoolean(SOURCE14);
//		if (AspectJPlugin.DEBUG_BUILDER) {
//			System.out.println(
//				"BuildOptionsAdapter.getSourceOnePointFourMode called, returning :"
//					+ new Boolean(source14));
//		}
//		return source14;
//
//	}

	/**
	 * @see BuildOptionsAdapter#getStrictSpecMode()
	 */
	public boolean getStrictSpecMode() {
		return false;
	}
//		ensurePropertiesInitialized();
//
//		String compilationStrictness = retrieveSettingString(COMPILATION_STRICTNESS);
//		boolean strictMode =
//			compilationStrictness.equals(COMPILATION_STRICTNESS_STRICT);
//
//		if (AspectJPlugin.DEBUG_BUILDER) {
//			System.out.println(
//				"BuildOptionsAdapter.getStrictSpecMode called, returning :"
//					+ new Boolean(strictMode));
//		}
//
//		return strictMode;
//
//	}

	/**
	 * @see BuildOptionsAdapter#getUseJavacMode()
	 */
	public boolean getIncrementalMode() {
		//ensurePropertiesInitialized();
		IProject currentProject = AspectJUIPlugin.getDefault().getCurrentProject();			
		boolean incrementalMode = AspectJPreferences.getIncrementalOption(currentProject);
		
		if (AspectJUIPlugin.DEBUG_BUILDER) {
			System.out.println(
				"BuildOptionsAdapter.getIncrementalMode called, returning :"
					+ new Boolean(incrementalMode));
		}

		return incrementalMode;
	}
	
	public boolean getBuildAsm() {
		//ensurePropertiesInitialized();
		IProject currentProject = AspectJUIPlugin.getDefault().getCurrentProject();		
		boolean buildAsm = AspectJPreferences.getBuildASMOption(currentProject);

		if (AspectJUIPlugin.DEBUG_BUILDER) {
			System.out.println("BuildOptionsAdapter.getBuildAsm called, returning :"
				+ new Boolean(buildAsm));
		}
		return buildAsm;
	}
	
	public boolean getShowWeaveMessages() {
		//ensurePropertiesInitialized();
		IProject currentProject = AspectJUIPlugin.getDefault().getCurrentProject();	
		boolean showweavemessages =  AspectJPreferences.getShowWeaveMessagesOption(currentProject);
		if (AspectJUIPlugin.DEBUG_BUILDER) {
			System.out.println("BuildOptionsAdapter.getShowWeaveMessages called, returning :"
				+ new Boolean(showweavemessages));
		}
		return showweavemessages;
	}

//	public boolean getJavaOrAjExt() {
//		ensurePropertiesInitialized();
//		boolean javaOrAjExt = retrieveSettingBoolean(JAVA_OR_AJ_EXT);
//		if (AspectJPlugin.DEBUG_BUILDER) {
//			System.out.println("BuildOptionsAdapter.getJavaOrAjExt called, returning :"
//				+ new Boolean(javaOrAjExt));
//		}
//		return javaOrAjExt;
//	}

	/**
	 * @see BuildOptionsAdapter#getWorkingOutputPath()
	 */
	public String getWorkingOutputPath() {
		return "";
	}
//		ensurePropertiesInitialized();
//
//		String workingDir = retrieveSettingString(WORKING_DIR);
//
//		if (AspectJPlugin.DEBUG_BUILDER) {
//			System.out.println(
//				"BuildOptionsAdapter.getWorkingOutputPath called, returning :" + workingDir);
//		}
//
//		return workingDir;
//	}
	
	/*
	 * The next few options are not used, since they're all passed via getJavaOptionsMap()
	 */
	public String getComplianceLevel() {
		return null;
	}

	public Set getDebugLevel() {
		return null;
	}
	
	public boolean getNoImportError() {
		return false;		
	}

	public boolean getPreserveAllLocals() {
		return false;	
	}
	
	public String getSourceCompatibilityLevel() {
		return null;
	}
	
	public Set getWarnings() {
		return null;		
	}

	public boolean getUseJavacMode() {
		return false;
	}

	public String getCharacterEncoding() {
		return null;
	}

	/**
	* Static form of preserveSetting that takes a project name, a key and a boolean,
	* it then preserves the boolean value for the named key on the supplied project resource.
	* Actually converts the boolean to a string and delegates to the 
	* other implementation of preserveSetting() that takes a string as the
	* value to store.
	*/
	private static void preserveSetting(
		IProject project,
		QualifiedName key,
		boolean flag)
		throws CoreException {
		preserveSetting(project, key, new Boolean(flag).toString());
	}

	/**
	 * Static form of preserveSetting that takes a project name, a key and a value,
	 * it then preserves the key/value pair as a persistent property against the 
	 * supplied project resource.
	 */
	private static void preserveSetting(
		IProject project,
		QualifiedName key,
		String value)
		throws CoreException {
		project.setPersistentProperty(key, value);
	}

//	/** 
//	 * Ensure the properties are initialized before they are used.
//	 */
//	private void ensurePropertiesInitialized() { 
//		ensurePropertiesInitialized(AspectJUIPlugin.getDefault().getCurrentProject());
//	}

	public static String getProjectOutJar(IProject project) {
		try {
			String v = project.getPersistentProperty(BuildOptionsAdapter.OUTPUTJAR);
			if (v==null) {
				preserveSetting(project,BuildOptionsAdapter.OUTPUTJAR,"");
				return "";
			}
			return v;
		} catch (CoreException e) {
		}
		return "";
	}
	
	public static void setProjectOutJar(IProject project, String value) throws CoreException {
		preserveSetting(project,BuildOptionsAdapter.OUTPUTJAR,value);
	}
	
	public static String[] getProjectInPath(IProject project) {
		String[] v = new String[3];
		try {
			v[0] = project.getPersistentProperty(BuildOptionsAdapter.INPATH);
			if (v[0]==null) {
				preserveSetting(project,BuildOptionsAdapter.INPATH,"");
			}
			v[1] = project.getPersistentProperty(BuildOptionsAdapter.INPATH_CON_KINDS);
			if (v[1]==null) {
				preserveSetting(project,BuildOptionsAdapter.INPATH_CON_KINDS,"");
			}
			v[2] = project.getPersistentProperty(BuildOptionsAdapter.INPATH_ENT_KINDS);
			if (v[2]==null) {
				preserveSetting(project,BuildOptionsAdapter.INPATH_ENT_KINDS,"");
			}
		} catch (CoreException e) {
		}
		return v;
	}

	public static void setProjectInPath(IProject project, String path, String cKinds, String eKinds) {
		try {
			preserveSetting(project,BuildOptionsAdapter.INPATH,path);
			preserveSetting(project,BuildOptionsAdapter.INPATH_CON_KINDS,cKinds);
			preserveSetting(project,BuildOptionsAdapter.INPATH_ENT_KINDS,eKinds);
		} catch (CoreException e) {
		}
	}

	public static String[] getProjectAspectPath(IProject project) {
		String[] v = new String[3];
		try {
			v[0] = project.getPersistentProperty(BuildOptionsAdapter.ASPECTPATH);
			if (v[0]==null) {
				preserveSetting(project,BuildOptionsAdapter.ASPECTPATH,"");
			}
			v[1] = project.getPersistentProperty(BuildOptionsAdapter.ASPECTPATH_CON_KINDS);
			if (v[1]==null) {
				preserveSetting(project,BuildOptionsAdapter.ASPECTPATH_CON_KINDS,"");
			}
			v[2] = project.getPersistentProperty(BuildOptionsAdapter.ASPECTPATH_ENT_KINDS);
			if (v[2]==null) {
				preserveSetting(project,BuildOptionsAdapter.ASPECTPATH_ENT_KINDS,"");
			}
		} catch (CoreException e) {
		}
		return v;
	}

	public static void setProjectAspectPath(IProject project, String path, String cKinds, String eKinds) {
		try {
			preserveSetting(project,BuildOptionsAdapter.ASPECTPATH,path);
			preserveSetting(project,BuildOptionsAdapter.ASPECTPATH_CON_KINDS,cKinds);
			preserveSetting(project,BuildOptionsAdapter.ASPECTPATH_ENT_KINDS,eKinds);
		} catch (CoreException e) {
		}
	}
	
//	public static void ensurePropertiesInitialized(IProject project) {
//		try {           
//            if (project.getPersistentProperty(BuildOptionsAdapter.INPUTJARSBROWSEDIR) == null)
//              preserveSetting(project, BuildOptionsAdapter.INPUTJARSBROWSEDIR,
//                                       project.getLocation().toOSString());  
//            if (project.getPersistentProperty(BuildOptionsAdapter.ASPECTJARSBROWSEDIR) == null)
//              preserveSetting(project, BuildOptionsAdapter.ASPECTJARSBROWSEDIR,
//                                       project.getLocation().toOSString());                 

//            if (project.getPersistentProperty(BuildOptionsAdapter.INPATH) == null)
//                  preserveSetting(project,BuildOptionsAdapter.INPATH,"");
//            if (project.getPersistentProperty(BuildOptionsAdapter.INPATH_CON_KINDS) == null)
//                preserveSetting(project,BuildOptionsAdapter.INPATH_CON_KINDS,"");
//            if (project.getPersistentProperty(BuildOptionsAdapter.INPATH_ENT_KINDS) == null)
//                preserveSetting(project,BuildOptionsAdapter.INPATH_ENT_KINDS,"");
            
//            if (project.getPersistentProperty(BuildOptionsAdapter.ASPECTPATH) == null)
//                    preserveSetting(project, BuildOptionsAdapter.ASPECTPATH, "");
//            if (project
//                    .getPersistentProperty(BuildOptionsAdapter.ASPECTPATH_CON_KINDS) == null)
//                    preserveSetting(project,
//                            BuildOptionsAdapter.ASPECTPATH_CON_KINDS, "");
//            if (project
//                    .getPersistentProperty(BuildOptionsAdapter.ASPECTPATH_ENT_KINDS) == null)
//                    preserveSetting(project,
//                            BuildOptionsAdapter.ASPECTPATH_ENT_KINDS, "");
            
            
//			if (project.getPersistentProperty(BuildOptionsAdapter.INPUTJARS) == null)
//			  preserveSetting(project,BuildOptionsAdapter.INPUTJARS,"");
//			if (project.getPersistentProperty(BuildOptionsAdapter.OUTPUTJAR) == null)
//			  preserveSetting(project,BuildOptionsAdapter.OUTPUTJAR,"");
//			if (project.getPersistentProperty(BuildOptionsAdapter.ASPECTJARS) == null)
//			  preserveSetting(project,BuildOptionsAdapter.ASPECTJARS,"");
//			if (project.getPersistentProperty(BuildOptionsAdapter.SOURCEROOTS) == null)
//			  preserveSetting(project,BuildOptionsAdapter.SOURCEROOTS,"");
			  
//			if (project.getPersistentProperty(BuildOptionsAdapter.CHAR_ENC) == null)
//				// Is this right? It comes out as "English" when perhaps it ought to be "ISO8859-1"
//				preserveSetting(
//					project,
//					BuildOptionsAdapter.CHAR_ENC,
//					BuildOptionsAdapter.CHAR_ENC_DEFAULT);
//		} catch (CoreException ce) {
//			AspectJUIPlugin.getDefault().getErrorHandler().handleError(
//				AspectJUIPlugin.getResourceString(
//					"buildOptionsAdapter.exceptionInitializingProperties"),
//				ce);
//		}
//	}

	/**
	 * Method getOutJar.
	 * @return String
	 */
	public String getOutJar() {
		//ensurePropertiesInitialized();
		String outputJar = retrieveSettingString(BuildOptionsAdapter.OUTPUTJAR);
		
		// If outputJar does not start with a slash, we might need to prepend the project
		// work directory.
		if (outputJar.trim().length()>0 && !(outputJar.startsWith("\\") || outputJar.startsWith("/"))) {
			String trimmedName = outputJar.trim();
			boolean prependProject = true;
			
			// It might still be a fully qualified path if the 2nd char is a ':' (i.e. its
			// a windows absolute path with a drive letter in it !)
			if (trimmedName.length()>1) {
				if (trimmedName.charAt(1)==':') prependProject = false;
			}
			
			if (prependProject) {
			  // Its a relative path, it should be relative to the project.
			  IProject thisProject = AspectJUIPlugin.getDefault().getCurrentProject();
			  String projectBaseDirectory = thisProject.getLocation().toOSString();
			  outputJar = new String(projectBaseDirectory+File.separator+outputJar.trim());
			}
		}
		if (AspectJUIPlugin.DEBUG_BUILDER) {
			System.out.println(
				"BuildOptionsAdapter.getOutJar called, returning :"
					+ outputJar);
		}

		return outputJar;
	}



	/**
	 * Method getInJars.
	 * @return Set
	 */
	public Set getInJars() {
		return null;
//		ensurePropertiesInitialized();
//		String inputJars= retrieveSettingString(BuildOptionsAdapter.INPUTJARS);
//        
//		if (AspectJUIPlugin.DEBUG_BUILDER) {
//			System.out.println(
//				"BuildOptionsAdapter.getInJars called, returning :"
//					+ inputJars);
//		}
//		if (inputJars.length()==0) return null; 
//		
//		return mapStringToSet(inputJars,false);
	}
    
    public Set getInPath() {
        //ensurePropertiesInitialized();
		String inpath = retrieveSettingString(BuildOptionsAdapter.INPATH);

        // Ensure that every entry in the list is a fully qualified one.
        inpath = fullyQualifyPathEntries(inpath);

		if (AspectJUIPlugin.DEBUG_BUILDER) {
			System.out
					.println("BuildOptionsAdapter.getInPath called, returning :"
							+ inpath);
		}
		if (inpath.length() == 0)
			return null;

		return mapStringToSet(inpath, false);
    }
    
    /**
	 * @param inputPath
	 * @return
	 */
	private String fullyQualifyPathEntries(String inputPath) {
		StringBuffer resultBuffer = new StringBuffer();
		StringTokenizer strTok = new StringTokenizer(inputPath,
				File.pathSeparator);
		while (strTok.hasMoreTokens()) {
			String current = strTok.nextToken();
			File f = new File(current);
			if (f.exists() && f.isAbsolute()) {
				// entry not relative to workspace (it's fully qualifed)
				resultBuffer.append(current);
			} else {
			//if (current.startsWith(AspectJUIPlugin.NON_OS_SPECIFIC_SEPARATOR)) {
				// Try to resolve path relative to the workspace. Need to
				// replace part of the path string with a fully qualified
				// equivalent.
				String projectName = null;
				int slashPos = current.indexOf(
						AspectJUIPlugin.NON_OS_SPECIFIC_SEPARATOR, 1);
				if (slashPos != -1) {
					projectName = current.substring(1, slashPos);
				} else {
					projectName = current.substring(1);
				}

				IProject project = AspectJPlugin.getWorkspace().getRoot()
						.getProject(projectName);

				if (project != null && project.getLocation() != null) {
					String projectPath = project.getLocation().toString();

					if (slashPos != -1) {
						resultBuffer.append(projectPath
								+ AspectJUIPlugin.NON_OS_SPECIFIC_SEPARATOR
								+ current.substring(slashPos + 1));
					} else {
						resultBuffer.append(projectPath);
					}
				}// end if named project found
				else {
					// Inform user that the supplied path contains an
					// entry that does not now exist.
                    
                    // TODO : Open a message dialog warning user that the 
                    // path entry does not exist. Tricky at the moment as
                    // an AJ project build calls getInPath() (and hence this
                    // method) more than once resulting in more than one
                    // pop-ups.
                    // AspectJPlugin.getDefault().getErrorHandler().handleWarning(
					//		AspectJPlugin.getFormattedResourceString(
					//				"Path.entryNotFound.warningMessage",
					//				current));
                    AJDTEventTrace.generalEvent("AspectJ path entry " + current
							+ " does not exist. Ignoring.");
                    
					if (AspectJUIPlugin.DEBUG_BUILDER) {
						System.out
								.println("BuildOptionsAdapter.fullyQualifyPathEntries detected path entry "
										+ current + " does not exist");
					}
				}// end else entry not found in workspace
			}// end if entry is relative to workspace
			resultBuffer.append(File.pathSeparator);
		}// end while more tokens to process

		String result = resultBuffer.toString();
		if (result.endsWith(File.pathSeparator)) {
			result = result.substring(0, result.length() - 1);
		}

		return result;
	}

	public Set getAspectPath() {
        //ensurePropertiesInitialized();
        String aspectpath = retrieveSettingString(BuildOptionsAdapter.ASPECTPATH);

        // Ensure that every entry in the list is a fully qualified one.
        aspectpath = fullyQualifyPathEntries(aspectpath);
        
        if (AspectJUIPlugin.DEBUG_BUILDER) {
            System.out.println(
                "BuildOptionsAdapter.getAspectPath called, returning :"
                    + aspectpath);
        }
        if (aspectpath.length()==0) return null; 
        
        return mapStringToSet(aspectpath,false);
    }
    
    
	/**
	 * Utility method for converting a semicolon separated list of
	 * files stored in a string into a Set of java.io.File objects.
	 * 
	 */
	private Set mapStringToSet(String input, boolean validateFiles) {
		if (input.length()==0) return null;
		String inputCopy = input;
		
		StringBuffer invalidEntries = new StringBuffer();
		
		// For relative paths (they don't start with a File.separator
		// or a drive letter on windows) - we prepend the projectBaseDirectory
		String projectBaseDirectory = 
		  AspectJUIPlugin.getDefault().getCurrentProject().
		  getLocation().toOSString();
		  
		if (AspectJUIPlugin.DEBUG_BUILDER)
			System.out.println("Converting ]"+input+"[");
		
		Set fileSet = new HashSet();
		while (inputCopy.indexOf(java.io.File.pathSeparator)!=-1) {  //ASCFIXME - Bit too platform specific!
		  int idx = inputCopy.indexOf(java.io.File.pathSeparator);
		  String path = inputCopy.substring(0,idx);
		  
		  java.io.File f = new java.io.File(path);
		  if (!f.isAbsolute())
		  	f = new File(projectBaseDirectory+java.io.File.separator+path);
		  	if (validateFiles && !f.exists()) {
		  		invalidEntries.append(f+"\n");
		  		if (AspectJUIPlugin.DEBUG_BUILDER)System.out.println("Skipping file ]"+f.toString()+"[");  
		  	} else {
		  	  fileSet.add(f);
		  	  if (AspectJUIPlugin.DEBUG_BUILDER) System.out.println("Adding file ]"+f.toString()+"[");  
		  	}
		  inputCopy = inputCopy.substring(idx+1);	
		  
		}
		// Process the final element
		if (inputCopy.length()!=0) {
		  java.io.File f = new java.io.File(inputCopy);
		  if (!f.isAbsolute())
		  	f = new File(projectBaseDirectory+java.io.File.separator+inputCopy);
		  	if (validateFiles && !f.exists()) {
		  		invalidEntries.append(f+"\n");
		  		if (AspectJUIPlugin.DEBUG_BUILDER) System.out.println("Skipping file ]"+f.toString()+"[");
		  	} else {
		  fileSet.add(f);
		  if (AspectJUIPlugin.DEBUG_BUILDER) System.out.println("Adding file ]"+f.toString()+"[");  
	}
	
		}
		
		//ASCFIXME - Need to NLSify this string...
		if (validateFiles && invalidEntries.length()!=0) {
		  AspectJUIPlugin.getDefault().getErrorHandler().handleWarning(
		    "The following jar files do not exist and are being ignored:\n"+invalidEntries.toString());
		}
		return fileSet;
	}
	

	/**
	 * Method getSourceRoots.
	 * @return Set
	 */
	public Set getSourceRoots() {
		return null;
//		ensurePropertiesInitialized();
//		String sourceRoots= retrieveSettingString(BuildOptionsAdapter.SOURCEROOTS);
//
//		if (AspectJUIPlugin.DEBUG_BUILDER) {
//			System.out.println(
//				"BuildOptionsAdapter.getSourceRoots called, returning :"
//					+ sourceRoots);
//		}
//		
//		return mapStringToSet(sourceRoots,false);
	}
}