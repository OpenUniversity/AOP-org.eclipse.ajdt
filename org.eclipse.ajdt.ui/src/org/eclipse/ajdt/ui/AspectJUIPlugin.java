/**********************************************************************
 Copyright (c) 2002, 2005 IBM Corporation and others.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Common Public License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/cpl-v10.html
 Contributors:
 Adrian Colyer, Andy Clement, Tracy Gardner - initial version
 ...
 **********************************************************************/
package org.eclipse.ajdt.ui;

// --- imports ---

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.aspectj.ajde.Ajde;
import org.eclipse.ajdt.buildconfigurator.BCResourceChangeListener;
import org.eclipse.ajdt.buildconfigurator.BCWorkbenchWindowInitializer;
import org.eclipse.ajdt.core.AspectJPlugin;
import org.eclipse.ajdt.core.builder.AJBuilder;
import org.eclipse.ajdt.core.javaelements.AJCompilationUnitManager;
import org.eclipse.ajdt.internal.EclipseVersion;
import org.eclipse.ajdt.internal.builder.UIBuildListener;
import org.eclipse.ajdt.internal.core.AJDTEventTrace;
import org.eclipse.ajdt.internal.core.AJDTStructureViewNodeFactory;
import org.eclipse.ajdt.internal.core.AJDTUtils;
import org.eclipse.ajdt.internal.ui.EventTraceLogger;
import org.eclipse.ajdt.internal.ui.actions.UICoreOperations;
import org.eclipse.ajdt.internal.ui.ajde.BuildOptionsAdapter;
import org.eclipse.ajdt.internal.ui.ajde.CompilerMonitor;
import org.eclipse.ajdt.internal.ui.ajde.CompilerTaskListManager;
import org.eclipse.ajdt.internal.ui.ajde.EditorAdapter;
import org.eclipse.ajdt.internal.ui.ajde.ErrorHandler;
import org.eclipse.ajdt.internal.ui.ajde.IdeUIAdapter;
import org.eclipse.ajdt.internal.ui.ajde.ProjectProperties;
import org.eclipse.ajdt.internal.ui.editor.AspectJTextTools;
import org.eclipse.ajdt.internal.ui.preferences.AJCompilerPreferencePage;
import org.eclipse.ajdt.internal.ui.preferences.AspectJPreferencePage;
import org.eclipse.ajdt.internal.ui.resources.AspectJImages;
import org.eclipse.ajdt.javamodel.ResourceChangeListener;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateReaderWriter;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.UIPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

// --- end imports ---
//hellos
/**
 * The main plugin class used in the desktop for AspectJ integration.
 * <p>
 * The AspectJPlugin (org.eclipse.ajdt.ui) provides the user interface and build
 * integration to enable use of Aspect-Oriented Software Development (AOSD)
 * using AspectJ within Eclipse.
 * </p>
 * <p>
 * This plugin depends on the org.aspectj.ajde (AJTools) plugin for the AspectJ
 * compiler and tools. The AJTools plugin is available from <a
 * href="http://www.aspectj.org">aspectj.org </a>
 * <p>
 * This class is also responsible for tracking the current selected resource in
 * the workspace (and its associated project). Other classes can access the
 * information via some static getter methods :- getCurrentProject() and
 * getCurrentResource()
 */
public class AspectJUIPlugin extends org.eclipse.ui.plugin.AbstractUIPlugin
		implements ISelectionListener {

	/*
	 * Capabilities inherited from AbstractUIPlugin (copied here for ease of
	 * reference) </p><p> Preferences <ul><li> Preferences are read the first
	 * time <code> getPreferenceStore </code> is called. </li><li> Preferences
	 * are found in the file whose name is given by the constant <code>
	 * FN_PREF_STORE </code> . A preference file is looked for in the plug-in's
	 * read/write state area. </li><li> Subclasses should reimplement <code>
	 * initializeDefaultPreferences </code> to set up any default values for
	 * preferences. These are the values typically used if the user presses the
	 * Default button in a preference dialog. </li><li> The plug-in's install
	 * directory is checked for a file whose name is given by <code>
	 * FN_DEFAULT_PREFERENCES </code> . This allows a plug-in to ship with a
	 * read-only copy of a preference file containing default values for certain
	 * settings different from the hard-wired default ones (perhaps as a result
	 * of localizing, or for a common configuration). </li><li> Plug-in code
	 * can call <code> savePreferenceStore </code> to cause non-default settings
	 * to be saved back to the file in the plug-in's read/write state area.
	 * </li><li> Preferences are also saved automatically on plug-in shutdown.
	 * </li></ul> Dialogs <ul><li> Dialog store are read the first time <code>
	 * getDialogSettings </code> is called. </li><li> The dialog store allows
	 * the plug-in to "record" important choices made by the user in a wizard or
	 * dialog, so that the next time the wizard/dialog is used the widgets can
	 * be defaulted to better values. A wizard could also use it to record the
	 * last 5 values a user entered into an editable combo - to show "recent
	 * values". </li><li> The dialog store is found in the file whose name is
	 * given by the constant <code> FN_DIALOG_STORE </code> . A dialog store
	 * file is first looked for in the plug-in's read/write state area; if not
	 * found there, the plug-in's install directory is checked. This allows a
	 * plug-in to ship with a read-only copy of a dialog store file containing
	 * initial values for certain settings. </li><li> Plug-in code can call
	 * <code> saveDialogSettings </code> to cause settings to be saved in the
	 * plug-in's read/write state area. A plug-in may opt to do this each time a
	 * wizard or dialog is closed to ensure the latest information is always
	 * safe on disk. </li><li> Dialog settings are also saved automatically on
	 * plug-in shutdown. </li></ul> Images <ul><li> A typical UI plug-in will
	 * have some images that are used very frequently and so need to be cached
	 * and shared. The plug-in's image registry provides a central place for a
	 * plug-in to store its common images. Images managed by the registry are
	 * created lazily as needed, and will be automatically disposed of when the
	 * plug-in shuts down. Note that the number of registry images should be
	 * kept to a minimum since many OSs have severe limits on the number of
	 * images that can be in memory at once. </ul><p> For easy access to your
	 * plug-in object, use the singleton pattern. Declare a static variable in
	 * your plug-in class for the singleton. Store the first (and only) instance
	 * of the plug-in class in the singleton when it is created. Then access the
	 * singleton when needed through a static <code> getDefault </code> method.
	 * </p>
	 */

	// VERSION-STRING - set when plugin is loaded
	public static String VERSION = "unset"; //$NON-NLS-1$

	// the id of this plugin
	public static final String PLUGIN_ID = "org.eclipse.ajdt.ui"; //$NON-NLS-1$

	public static final String ID_OUTLINE = PLUGIN_ID + ".ajoutlineview"; //$NON-NLS-1$

	// public static final String ID_NATURE = PLUGIN_ID + ".ajnature";
	// //$NON-NLS-1$

	private static final String AJDE_VERSION_KEY_CURRENT = "ajde.version"; //$NON-NLS-1$

	private static final String AJDE_VERSION_KEY_PREVIOUS = "ajde.version.at.previous.startup"; //$NON-NLS-1$

	/**
	 * General debug trace for the plug-in enabled through the master trace
	 * switch.
	 */
	public static boolean isDebugging = false;

	/**
	 * More detailed trace for the builder. Controlled by options flag
	 * org.eclipse.ajdt.ui/builderDebug
	 */
	public static boolean DEBUG_BUILDER = false;

	/**
	 * More detailed trace for the compiler monitor. Controlled by options flag
	 * org.eclipse.ajdt.ui/builderDebug
	 */
	public static boolean DEBUG_COMPILER = false;

	/**
	 * More detailed trace for the outline view. Controlled by options flag
	 * org.eclipse.ajdt.ui/outlineDebug
	 */
	public static boolean DEBUG_OUTLINE = false;

	/**
	 * shared single instance of the plugin
	 */
	private static AspectJUIPlugin plugin;

	/**
	 * plugin resource bundle used for NLS
	 */
	private ResourceBundle resourceBundle;

	/**
	 * ProjectPropertiesAdapter is required by the AJDT tools to initialise the
	 * AJDE environment.
	 */
	private ProjectProperties ajdtProjectProperties;

	/**
	 * Editor adapter used by AJDE tools to control editor when needed
	 */
	private EditorAdapter ajdtEditorAdapter;

	/**
	 * Compiler monitor listens to AspectJ compilation events (build progress
	 * and compilations errors/warnings)
	 */
	private CompilerMonitor ajdtCompilerMonitor;
	
	/**
	 * Build options passed to AJDE
	 */
	private BuildOptionsAdapter ajdtBuildOptions;

	/**
	 * AbstractIconRegistry used to manage all icons for AJDT.
	 */
	private AspectJImages ajdtImages;

	/**
	 * StructureViewManager used by AJDE to build tree structure
	 */
	private AJDTStructureViewNodeFactory ajdtStructureFactory;

	/**
	 * IDEAdapter used by AJDE to display status messages
	 */
	private IdeUIAdapter ajdtUIAdapter;

	/**
	 * Error handler used to display error messages issued from AJDE tools.
	 */
	private ErrorHandler ajdtErrorHandler;

	/**
	 * The text tools to use for AspectJ aware editing
	 */
	private AspectJTextTools aspectJTextTools;

	/**
	 * The currently selected resource
	 */
	private IResource currentResource;

	/**
	 * The workbench Display for use by asynchronous UI updates
	 */
	private Display display;

	/**
	 * A resource change listener that will listen for new resource additions.
	 */
	IResourceChangeListener resourceChangeListener;

	// custom attributes AJDT markers can have
	public static final String SOURCE_LOCATION_ATTRIBUTE = "sourceLocationOfAdvice";

	public static final String RELATED_LOCATIONS_ATTRIBUTE_PREFIX = "relatedLocations-";

	public static final String ACCKIND_ATTRIBUTE = "acckind";

	/**
	 * Return the single default instance of this plugin
	 */
	public static AspectJUIPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not
	 * found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = AspectJUIPlugin.getDefault()
				.getResourceBundle();
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * @param key
	 * @param arg
	 * @return string from plugin's resource bundle that includes the inserted
	 *         argument.
	 */
	public static String getFormattedResourceString(String key, String arg) {
		return getFormattedResourceString(key, new String[] { arg });
	}

	/**
	 * @param key
	 * @param arg
	 * @return string from plugin's resource bundle that includes all of the
	 *         inserted arguments.
	 */
	public static String getFormattedResourceString(String key, String[] args) {
		return MessageFormat.format(getResourceString(key), args);
	}

	private final static String defaultLstShouldBeUsed = "org.eclipse.ajdt.ui.buildConfig.useDefaultLst";

	public static final int PROGRESS_MONITOR_MAX = 100;

	/**
	 * Set the build configuration file to be used when building this project
	 */
	public static void setBuildConfigurationFile(IProject project,
			IFile buildfile) {

		// Preserve the build selection choice in the preference store, with a
		// name unique to this project.
		IPreferenceStore store = AspectJUIPlugin.getDefault()
				.getPreferenceStore();

		String propertyName = "org.eclipse.ajdt.ui." + project.getName()
				+ ".lst";

		if (buildfile == null)
			store.setValue(propertyName, defaultLstShouldBeUsed);
		else
			store.setValue(propertyName, buildfile.getLocation().toOSString());

		String cfg = AspectJPlugin.getBuildConfigurationFile(project);
		Ajde.getDefault().getConfigurationManager().setActiveConfigFile(cfg);
		AJDTEventTrace.buildConfigSelected(cfg, project);
	}

	public static void setBuildConfigurationFile(IProject project,
			IResource buildfile) {

		if (buildfile == null)
			setBuildConfigurationFile(project, (IFile) null);
		else if (buildfile instanceof IFile) {
			setBuildConfigurationFile(project, (IFile) buildfile);
		}
	}

	/**
	 * Creates an AspectJPlugin instance and initializes the supporting Ajde
	 * tools. - deprecated in 3.0
	 */
	// public AspectJPlugin(IPluginDescriptor descriptor) {
	// super(descriptor);
	//
	// PluginVersionIdentifier pvi = descriptor.getVersionIdentifier();
	// VERSION = pvi.getMajorComponent() + "." + pvi.getMinorComponent() + "." +
	// pvi.getServiceComponent();
	// initDebugging();
	// plugin = this;
	// ajdtProjectProperties = new ProjectProperties();
	// ajdtCompilerMonitor = new CompilerMonitor();
	// ajdtEditorAdapter = new EditorAdapter();
	// ajdtErrorHandler = new ErrorHandler();
	// ajdtBuildOptions = new BuildOptionsAdapter( );
	// ajdtImages = AspectJImages.registry( );
	// ajdtUIAdapter = new IdeUIAdapter( );
	// ajdtStructureFactory = new AJDTStructureViewNodeFactory( ajdtImages );
	//
	// //System.err.println("AspectJPlugin.new: adding selection listener..");
	// //System.err.println("Adding to selection service on workbench window:
	// "+plugin.getWorkbench().getActiveWorkbenchWindow().toString());
	//		
	// Ajde.init(
	// ajdtEditorAdapter
	// ,ajdtCompilerMonitor // task list manager
	// ,ajdtCompilerMonitor // build progress monitor
	// ,ajdtProjectProperties
	// ,ajdtBuildOptions
	// ,ajdtStructureFactory
	// ,ajdtUIAdapter
	// ,ajdtErrorHandler
	// );
	//
	// try {
	// resourceBundle =
	// ResourceBundle.getBundle(
	// "org.eclipse.ajdt.internal.core.resources.AspectJPluginResources");
	// } catch (MissingResourceException x) {
	// resourceBundle = null;
	// }
	//		
	// /* Moved to startup()
	// // Create an register the resource change listener if necessary, it will
	// be
	// // notified if resources are added/deleted or their content changed.
	// if (resourceChangeListener == null) {
	// resourceChangeListener = new AspectJResourceChangeListener();
	// getWorkspace().addResourceChangeListener(resourceChangeListener,IResourceChangeEvent.POST_CHANGE);
	// }*/
	//	    
	//
	// AJDTEventTrace.startup();
	// new AspectVisualiserPlugin();
	//	    
	// checkAspectJVersion();
	// }
	/**
	 * Creates an AspectJPlugin instance and initializes the supporting Ajde
	 * tools - Compatible with Eclipse 3.0. Note the rest of the contents of the
	 * 2.x constructor now resides in the start(BundleContext) method.
	 */
	public AspectJUIPlugin() {
		super();
		plugin = this;

		try {
			resourceBundle = ResourceBundle
					.getBundle("org.eclipse.ajdt.internal.ui.resources.AspectJPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * This function checks to see if the workbench is starting with a new
	 * version of AJDE (it persists the version previously used by the workbench
	 * in a preference) - if a new version of AJDE is in use then it goes
	 * through the projects and for any AspectJ project, it tries to ensure the
	 * path to any aspectjrt.jar that it references is correct. This fixes a
	 * migration issue we have where the path includes the AJDE version -
	 * without this method you have to do it manually by either editing the
	 * aspectjrt.jar entry or removing/readding the nature. We also add the
	 * AspectJ code templates here.
	 * 
	 */
	private void checkAspectJVersion() {
		IPreferenceStore store = AspectJUIPlugin.getDefault()
				.getPreferenceStore();

		// Version of AJDE now installed.
		String currentAjdeVersion = AspectJUIPlugin
				.getResourceString(AJDE_VERSION_KEY_CURRENT);

		// Version that the projects in this workspace used on the previous
		// execution of eclipse.
		String previousAjdeVersion = store.getString(AJDE_VERSION_KEY_PREVIOUS);
		if (previousAjdeVersion == null
				|| !currentAjdeVersion.equals(previousAjdeVersion)) {
			AJDTEventTrace.generalEvent("New version of AJDE detected (now:"
					+ currentAjdeVersion
					+ ") - checking aspectjrt.jar for each project.");

			IProject[] projects = AspectJPlugin.getWorkspace().getRoot()
					.getProjects();
			for (int i = 0; i < projects.length; i++) {
				if (projects[i].isOpen()) {
					IProject current = projects[i];
					if (AspectJPlugin.isAJProject(current)) {
						AJDTUtils.verifyAjrtVersion(current);
					}
				}
			}

			checkTemplatesInstalled();
			checkProblemMarkersVisible();
			// no! don't verify config here - it can be too early for
			// the supporting ui bits and pieces to be in place (race
			// condition)
			// AJDTUtils.verifyWorkbenchConfiguration();
			store.putValue(AJDE_VERSION_KEY_PREVIOUS, currentAjdeVersion);
		}
	}

	private void checkProblemMarkersVisible() {
		String TAG_DIALOG_SECTION = "org.eclipse.ui.views.problem";
		String problemMarker = "org.eclipse.ajdt.ui.problemmarker:";
		// AbstractUIPlugin plugin = (AbstractUIPlugin)
		// Platform.getPlugin(PlatformUI.PLUGIN_ID);
		AbstractUIPlugin plugin = UIPlugin.getDefault();
		IDialogSettings workbenchSettings = plugin.getDialogSettings();
		IDialogSettings settings = workbenchSettings
				.getSection(TAG_DIALOG_SECTION);
		if (settings != null) {
			IDialogSettings filterSettings = settings.getSection("filter");
			String enabledMarkers = filterSettings.get("selectedType");
			if (enabledMarkers.indexOf(problemMarker) == -1) {
				enabledMarkers = enabledMarkers + problemMarker;
				filterSettings.put("selectedType", enabledMarkers);
			}
		}
	}

	/**
	 * Install the AspectJ code templates. We'd like to do this by an extension
	 * point, but there doesn't seem to be one.
	 */
	private void checkTemplatesInstalled() {
		TemplateStore codeTemplates = JavaPlugin.getDefault()
				.getTemplateStore();
		// bug 90791: don't add templates if they are already there
		if (codeTemplates.findTemplate("adviceexecution") == null) { //$NON-NLS-1$
			try {
				URL loc = getBundle().getEntry("/aspectj_code_templates.xml"); //$NON-NLS-1$
				TemplateReaderWriter trw = new TemplateReaderWriter();
				TemplatePersistenceData[] templates = trw.read(
						loc.openStream(), null);
				if ((templates == null) || (templates.length == 0)) {
					AJDTEventTrace.generalEvent(AspectJUIPlugin
							.getResourceString("codeTemplates.couldNotLoad")); //$NON-NLS-1$
				} else {
					for (int i = 0; i < templates.length; i++) {
						codeTemplates.add(templates[i]);
					}
					codeTemplates.save();
				}
			} catch (IOException fnf) {
				AJDTEventTrace.generalEvent(AspectJUIPlugin
						.getResourceString("codeTemplates.couldNotLoad")); //$NON-NLS-1$
			}
		}
	}

	/**
	 * return the error handler used to popup error dialogs and store errors in
	 * the log.
	 */
	public ErrorHandler getErrorHandler() {
		return ajdtErrorHandler;
	}

//	/**
//	 * return the compiler monitor used for build progress monitoring and
//	 * compilation errors/warnings
//	 */
//	public CompilerMonitor getCompilerMonitor() {
//		return ajdtCompilerMonitor;
//	}

	/**
	 * Access the ProjectPropertiesAdapter required by the AJDE tools
	 */
	public ProjectProperties getAjdtProjectProperties() {
		return ajdtProjectProperties;
	}

	/**
	 * Access the build options adapter
	 */
	public BuildOptionsAdapter getAjdtBuildOptionsAdapter() {
		return ajdtBuildOptions;
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	// At startup, remember the display for later use and register to receive
	// information on part selection
	// public void startup() throws CoreException {
	// super.startup();
	//		
	// // BUG 23955. getCurrent() returned null if invoked from a menu.
	// display = Display.getDefault();
	//
	// //System.err.println("AspectJPlugin.startup: Adding selection
	// listener...");
	//		
	// // BUG 23955. getActiveWorkbenchWindow() returns null, and fails the
	// plugin startup,
	// // if this is set here. The method returns null if the active window
	// isn't a workbench window.
	// //plugin
	// // .getWorkbench()
	// // .getActiveWorkbenchWindow()
	// // .getSelectionService()
	// // .addSelectionListener(this);
	//					
	//			
	// // Create and register the resource change listener if necessary, it will
	// be
	// // notified if resources are added/deleted or their content changed.
	// if (resourceChangeListener == null) {
	// resourceChangeListener = new AspectJResourceChangeListener();
	// getWorkspace().addResourceChangeListener(resourceChangeListener,IResourceChangeEvent.POST_CHANGE);
	// }
	// }

	// At startup, remember the display for later use and register to receive
	// information on part selection - Eclipse 3.0 requires the use of start
	// rather than startup.
	public void start(BundleContext context) throws Exception {
		super.start(context);

		// listen for builds of AJ projects
		AJBuilder.addAJBuildListener(new UIBuildListener());
		
		// Update project menu and listen for project selections
		new BCWorkbenchWindowInitializer();

		// BUG 23955. getCurrent() returned null if invoked from a menu.
		display = Display.getDefault();

		// BUG 23955. getActiveWorkbenchWindow() returns null, and fails the
		// plugin startup,
		// if this is set here. The method returns null if the active window
		// isn't a workbench window.
		// plugin
		// .getWorkbench()
		// .getActiveWorkbenchWindow()
		// .getSelectionService()
		// .addSelectionListener(this);

		// Create and register the resource change listener if necessary, it
		// will be
		// notified if resources are added/deleted or their content changed.

		if (resourceChangeListener == null) {
			resourceChangeListener = new BCResourceChangeListener();
			// listener for build configurator
			enableBuildConfiguratorResourceChangeListener();
			// listener for aspectj model
			AspectJPlugin.getWorkspace().addResourceChangeListener(
					new ResourceChangeListener(),
					IResourceChangeEvent.PRE_CLOSE
							| IResourceChangeEvent.PRE_DELETE
							| IResourceChangeEvent.POST_CHANGE
							| IResourceChangeEvent.PRE_BUILD);
		}

		// the following came from the 2.x constructor - needs to be put here
		// because plugin is initialized when start(BundleContext) is called.
		Bundle bundle = AspectJUIPlugin.getDefault().getBundle();
		String version = (String) bundle.getHeaders().get(
				Constants.BUNDLE_VERSION);
		PluginVersionIdentifier pvi = new PluginVersionIdentifier(version);

		VERSION = pvi.getMajorComponent() + "." + pvi.getMinorComponent() + "."
				+ pvi.getServiceComponent();

		initDebugging();

		// set the UI version of core operations
		AspectJPlugin.getDefault().setCoreOperations(new UICoreOperations());
		AspectJPlugin.getDefault().setAJLogger(new EventTraceLogger());
		
		ajdtProjectProperties = new ProjectProperties();
		
		// replace the core compiler monitor with the UI one
		AspectJPlugin.getDefault().setCompilerMonitor(new CompilerMonitor());
		
		ajdtEditorAdapter = new EditorAdapter();
		ajdtErrorHandler = new ErrorHandler();
		ajdtBuildOptions = new BuildOptionsAdapter();
		ajdtImages = AspectJImages.registry();
		ajdtUIAdapter = new IdeUIAdapter();
		ajdtStructureFactory = new AJDTStructureViewNodeFactory(ajdtImages);

		Ajde.init(ajdtEditorAdapter, CompilerTaskListManager.getInstance(), // task list manager
				AspectJPlugin.getDefault().getCompilerMonitor(), // build progress monitor
				ajdtProjectProperties, ajdtBuildOptions,
				ajdtStructureFactory, ajdtUIAdapter, ajdtErrorHandler);

		checkEclipseVersion();

		AJDTEventTrace.startup();
		checkAspectJVersion();

		// don't want to check on startup for .aj resource filter
		// because handled by the migration wizard
		// FileFilter.checkIfFileFilterEnabledAndAsk();

		// Find out whether or not we have migrated this workspace.
		IPreferenceStore store = AspectJUIPlugin.getDefault().getPreferenceStore();
	    IWorkspace currentWorkspace = ResourcesPlugin.getWorkspace();
	    String workspaceLocation = currentWorkspace.getRoot().getLocation().toString();
	    setMigrationWizardHasRun(store.getBoolean(workspaceLocation));

		AJCompilationUnitManager.INSTANCE.initCompilationUnits(AspectJPlugin
				.getWorkspace());
		
		AJDTUtils.refreshPackageExplorer();
	}

	// have we run the migration wizard for this workspace or for this session?
	private static boolean migrationWizardRun = false;
	
	public static boolean migrationWizardHasRun() {
	    return migrationWizardRun;
	}
	
	public static void setMigrationWizardHasRun(boolean hasRun) {
	    migrationWizardRun = hasRun;
	}
	
	private void checkEclipseVersion() {
		Bundle bundle = Platform.getBundle("org.eclipse.platform"); //$NON-NLS-1$
		String version = (String) bundle.getHeaders().get(
				Constants.BUNDLE_VERSION);
		PluginVersionIdentifier pvi = new PluginVersionIdentifier(version);
		if ((pvi.getMajorComponent() != EclipseVersion.MAJOR_VERSION)
				|| (pvi.getMinorComponent() != EclipseVersion.MINOR_VERSION)) {
			getErrorHandler().handleError(
					getFormattedResourceString("wrong.eclipse.version", //$NON-NLS-1$
							new String[] {
									EclipseVersion.MAJOR_VERSION + "."
											+ EclipseVersion.MINOR_VERSION,
									pvi.getMajorComponent() + "."
											+ pvi.getMinorComponent() }));
		}
	}

	/**
	 * get the active window in the workbench, or null if no window is active
	 */
	public org.eclipse.ui.IWorkbenchWindow getActiveWorkbenchWindow() {
		return plugin.getWorkbench().getActiveWorkbenchWindow();

	}

	/**
	 * get the main workbench display
	 */
	public Display getDisplay() {
		return display;
	}

	/**
	 * get the text tools to be used by the AspectJ editor
	 */
	public AspectJTextTools getAspectJTextTools() {
		IPreferenceStore textToolPreferences;

		if (aspectJTextTools == null) {
			// text tools deliberately use the JavaPlugin settings
			textToolPreferences = JavaPlugin.getDefault().getPreferenceStore();
			aspectJTextTools = new AspectJTextTools(textToolPreferences);
		}

		return aspectJTextTools;
	}


	/**
	 * get the current resource. This method can return null if a resource has
	 * not been selected or the resource selected has no individual physical
	 * representation in the workspace. For example, selecting an 'external' jar
	 * file within a project will cause currentProject to be set appropriately
	 * but there is no real resource representing that jar (its an artefact from
	 * outside the workbench) so currentResource will be null.
	 */
	public IResource getCurrentResource() {
		return currentResource;
	}

//	/**
//	 * set the current project by name
//	 */
//	public void setCurrentProject(String projectName) {
//		boolean matched = false;
//		IProject[] projects = AspectJPlugin.getWorkspace().getRoot()
//				.getProjects();
//		for (int i = 0; i < projects.length; i++) {
//			if (projects[i].getName().equals(projectName)) {
//				currentProject = projects[i];
//				matched = true;
//				break;
//			}
//		}
//		if (!matched) {
//			getErrorHandler().handleWarning(
//					getResourceString("bad.project") + " " + projectName);
//		}
//	}

	/**
	 * initialize the default preferences for this plugin
	 */
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		AspectJPreferencePage.initDefaults(store);
		AJCompilerPreferencePage.initDefaults(store);
	}

	/**
	 * initialize the plug-in image registry
	 */
	protected void initializeImageRegistry(ImageRegistry reg) {
	}

	/**
	 * Initialize plug-in debugging
	 */
	private void initDebugging() {
		if (isDebugging()) {
			System.out.println("AJP START: " + PLUGIN_ID + " " + VERSION);
			isDebugging = true;

			String option;
			option = Platform.getDebugOption(PLUGIN_ID + "/builderDebug");
			if (option != null && option.equals("true")) {
				System.out.println("AJP builderDebug ON");
				DEBUG_BUILDER = true;
			} else {
				System.out.println("AJP builderDebug OFF");
			}
			option = Platform.getDebugOption(PLUGIN_ID + "/compilerDebug");
			if (option != null && option.equals("true")) {
				System.out.println("AJP compilerDebug ON");
				DEBUG_COMPILER = true;
			} else {
				System.out.println("AJP compilerDebug OFF");
			}
			option = Platform.getDebugOption(PLUGIN_ID + "/outlineDebug");
			if (option != null && option.equals("true")) {
				System.out.println("AJP outlineDebug ON");
				DEBUG_OUTLINE = true;
			} else {
				System.out.println("AJP outlineDebug OFF");
			}
		}

	}

	// Implementation of ISelectionListener follows

	/**
	 * Keeps the currentResource and currentProject information up to date in
	 * this class, as this method is called whenever a user changes their
	 * selection in the workspace.
	 */
	public void selectionChanged(IWorkbenchPart iwp, ISelection is) {
		try {
			// If we want to check only for selection changes in the Packages
			// view, then we could check the WorkbenchPart:
			// if (iwp.getTitle().equals("Packages")) {
			// But there are so many places where the resources are exposed
			// navigator view, etc - that if we can be more generic and cope
			// with selection of the resources occurring anywhere, then we
			// should always
			// have the current project correct.
			// System.out.println( "Selection changed: " +
			// iwp.getTitle() + " " + is.toString() );

			// AMC note: GM1 build is firing an ITextSelection event only
			// clicking on the tab for an open file in the editor (to change
			// the current file being viewed). This does *not* give us the
			// information we need to update the current project :-(

			if (is instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection) is;
				Object o = structuredSelection.getFirstElement();

				if (o != null) {
					if (o instanceof IResource) {
						currentResource = (IResource) o;
						AspectJPlugin.getDefault().setCurrentProject(currentResource.getProject());

					} else if (o instanceof IJavaElement) {
						IJavaElement je = (IJavaElement) o;
						if (je.getJavaProject() != null) {
							currentResource = je.getUnderlyingResource(); // Might
							// be
							// null!
							AspectJPlugin.getDefault().setCurrentProject(je.getJavaProject().getProject());
						}

						// } else {
						// System.err.println("Unrecognized selection is a ]" +
						// o.toString() + "[");
					}
				}
			} else if (is instanceof ITextSelection) {
				// ITextSelection ts = (ITextSelection) is;
				// System.out.println( "Selected: " + ts.getText() );
			}

		} catch (JavaModelException jme) {
			getErrorHandler().handleError(
					"Exception in AspectJPlugin.selectionChanged processing",
					jme);
		}

	}

	/**
	 * Build a list of .lst files in the currently selected project - EXCEPT the
	 * default.lst file. It uses the helper method getLstFiles() defined below
	 * to perform recursion.
	 * 
	 * @return List of IResource objects that represent .lst files in the
	 *         current project
	 */
	public List getListOfConfigFilesForCurrentProject() {
		List allLstFiles = new ArrayList();
		try {
			IResource[] files = AspectJPlugin.getDefault().getCurrentProject().members();
			getLstFiles(files, allLstFiles);
		} catch (CoreException ce) {
			AspectJUIPlugin
					.getDefault()
					.getErrorHandler()
					.handleError(
							"Exception occurred whilst retrieving all .lst files in a project",
							ce);
		}
		return allLstFiles;
	}

	/**
	 * Find all the ".lst" files in the project. Populates the List parameter
	 * passed in using recursion to traverse the whole resource hierarchy for
	 * the project.
	 */
	private void getLstFiles(IResource[] resource_list, List allLstFiles) {
		try {
			for (int i = 0; i < resource_list.length; i++) {
				IResource ir = resource_list[i];
				// Add lst files to the list, but NOT default.lst
				if (ir.getName().endsWith(".lst")
						&& !ir.getName().equals("default.lst"))
					allLstFiles.add(ir);
				if (ir instanceof IContainer)
					getLstFiles(((IContainer) ir).members(), allLstFiles);
			}
		} catch (CoreException ce) {
			AspectJUIPlugin
					.getDefault()
					.getErrorHandler()
					.handleError(
							"Exception occurred whilst retrieiving all .lst files in a project",
							ce);
		}
	}

	/**
	 * Disable the build configurator's resource change listener.
	 * 
	 */
	public void disableBuildConfiguratorResourceChangeListener() {
		AspectJPlugin.getWorkspace().removeResourceChangeListener(
				resourceChangeListener);
	}

	/**
	 * Enable the build configurator's resource change listener.
	 * 
	 */
	public void enableBuildConfiguratorResourceChangeListener() {
		AspectJPlugin.getWorkspace().addResourceChangeListener(
				resourceChangeListener,
				IResourceChangeEvent.PRE_CLOSE
						| IResourceChangeEvent.PRE_DELETE
						| IResourceChangeEvent.POST_CHANGE
						| IResourceChangeEvent.PRE_BUILD);
	}

}