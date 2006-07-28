/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
import org.osgi.framework.*;

// This class provides a higher level API on the state
public class PDEState implements IPDEBuildConstants, IBuildPropertiesConstants {
	private static final String PROFILE_EXTENSION = ".profile"; //$NON-NLS-1$
	private static final String SYSTEM_PACKAGES = "org.osgi.framework.system.packages"; //$NON-NLS-1$

	private StateObjectFactory factory;
	protected State state;
	private long id;
	private Properties repositoryVersions;
	private HashMap bundleClasspaths;
	private Map patchBundles;
	private List addedBundle;
	private List unqualifiedBundles; //All the bundle description objects that have .qualifier in them 

	private String javaProfile;
	private String[] javaProfiles;

	protected long getNextId() {
		return ++id;
	}

	public PDEState(PDEUIStateWrapper initialState) {
		this();
		state = initialState.getState();
		factory = state.getFactory();
		id = initialState.getNextId();
		bundleClasspaths = initialState.getClasspaths();
		patchBundles = initialState.getPatchData();
		addedBundle = new ArrayList();
		unqualifiedBundles = new ArrayList();
		forceQualifiers();
	}

	public PDEState() {
		factory = Platform.getPlatformAdmin().getFactory();
		state = factory.createState();
		state.setResolver(Platform.getPlatformAdmin().getResolver());
		id = 0;
		bundleClasspaths = new HashMap();
		patchBundles = new HashMap();
		loadPluginTagFile();
	}

	public StateObjectFactory getFactory() {
		return factory;
	}

	public void addBundleDescription(BundleDescription toAdd) {
		state.addBundle(toAdd);
	}

	private PluginConverter acquirePluginConverter() throws Exception {
		return (PluginConverter) BundleHelper.getDefault().acquireService(PluginConverter.class.getName());
	}

	//Add a bundle to the state, updating the version number 
	public boolean addBundle(Dictionary enhancedManifest, File bundleLocation) {
		updateVersionNumber(enhancedManifest);
		try {
			BundleDescription descriptor;
			descriptor = factory.createBundleDescription(state, enhancedManifest, bundleLocation.getAbsolutePath(), getNextId());
			bundleClasspaths.put(new Long(descriptor.getBundleId()), getClasspath(enhancedManifest));
			String patchValue = fillPatchData(enhancedManifest);
			if (patchValue != null)
				patchBundles.put(new Long(descriptor.getBundleId()), patchValue);
			rememberQualifierTagPresence(descriptor);
			if (state.addBundle(descriptor) == true && addedBundle != null)
				addedBundle.add(descriptor);
		} catch (BundleException e) {
			IStatus status = new Status(IStatus.WARNING, IPDEBuildConstants.PI_PDEBUILD, EXCEPTION_STATE_PROBLEM, NLS.bind(Messages.exception_stateAddition, enhancedManifest.get(Constants.BUNDLE_NAME)), e);
			BundleHelper.getDefault().getLog().log(status);
			return false;
		}
		return true;
	}

	private void rememberQualifierTagPresence(BundleDescription descriptor) {
		Properties bundleProperties = null;
		bundleProperties = (Properties) descriptor.getUserObject();
		if (bundleProperties == null) {
			bundleProperties = new Properties();
			descriptor.setUserObject(bundleProperties);
		}
		bundleProperties.setProperty(PROPERTY_QUALIFIER, "marker"); //$NON-NLS-1$
	}

	private String[] getClasspath(Dictionary manifest) {
		String fullClasspath = (String) manifest.get(Constants.BUNDLE_CLASSPATH);
		String[] result = new String[0];
		try {
			if (fullClasspath != null) {
				ManifestElement[] classpathEntries;
				classpathEntries = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, fullClasspath);
				result = new String[classpathEntries.length];
				for (int i = 0; i < classpathEntries.length; i++) {
					result[i] = classpathEntries[i].getValue();
				}
			}
		} catch (BundleException e) {
			//Ignore
		}
		return result;
	}
	
	private String fillPatchData(Dictionary manifest) {
		if (manifest.get("Eclipse-ExtensibleAPI") != null) {
			return "Eclipse-ExtensibleAPI: true";
		}
		
		if (manifest.get("Eclipse-PatchFragment") != null) {
			return "Eclipse-PatchFragment: true";
		}
		return null;
	}

	private void loadPluginTagFile() {
		repositoryVersions = new Properties();
		try {
			InputStream input = new BufferedInputStream(new FileInputStream(AbstractScriptGenerator.getWorkingDirectory() + '/' + DEFAULT_PLUGIN_REPOTAG_FILENAME_DESCRIPTOR));
			try {
				repositoryVersions.load(input);
			} finally {
				input.close();
			}
		} catch (IOException e) {
			//Ignore
		}
	}

	public boolean addBundle(File bundleLocation) {
		Dictionary manifest;
		manifest = loadManifest(bundleLocation);
		if (manifest == null)
			return false;
		try {
			hasQualifier(bundleLocation, manifest);
		} catch (BundleException e) {
			//should not happen since we know the header
		}
		return addBundle(manifest, bundleLocation);
	}

	private void updateVersionNumber(Dictionary manifest) {
		String newVersion = null;
		try {
			String symbolicName = (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME);
			if (symbolicName == null)
				return;
			
			symbolicName = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicName)[0].getValue();
			newVersion = QualifierReplacer.replaceQualifierInVersion((String) manifest.get(Constants.BUNDLE_VERSION), symbolicName, (String) manifest.get(PROPERTY_QUALIFIER), repositoryVersions);
		} catch (BundleException e) {
			//ignore
		}
		if (newVersion != null)
			manifest.put(Constants.BUNDLE_VERSION, newVersion);
	}

	/**
	 * @param bundleLocation
	 * @param manifest
	 * @throws BundleException
	 */
	private void hasQualifier(File bundleLocation, Dictionary manifest) throws BundleException {
		ManifestElement[] versionInfo = ManifestElement.parseHeader(Constants.BUNDLE_VERSION, (String) manifest.get(Constants.BUNDLE_VERSION));
		if (versionInfo != null) {
			if (versionInfo[0].getValue().endsWith(PROPERTY_QUALIFIER)) {
				manifest.put(PROPERTY_QUALIFIER,  getQualifierPropery(bundleLocation.getAbsolutePath()));
			}
		}
	}

	private String getQualifierPropery(String bundleLocation) {
		String qualifierInfo = null;
		try {
			qualifierInfo = AbstractScriptGenerator.readProperties(bundleLocation, IPDEBuildConstants.PROPERTIES_FILE, IStatus.INFO).getProperty(PROPERTY_QUALIFIER);
		} catch (CoreException e) {
			//ignore
		}
		if (qualifierInfo == null)
			qualifierInfo = PROPERTY_CONTEXT;
		return qualifierInfo;
	}

	//Return a dictionary representing a manifest. The data may result from plugin.xml conversion  
	private Dictionary basicLoadManifest(File bundleLocation) {
		InputStream manifestStream = null;
		ZipFile jarFile = null;
		try {
			if ("jar".equalsIgnoreCase(new Path(bundleLocation.getName()).getFileExtension()) && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				manifestStream = new FileInputStream(new File(bundleLocation, JarFile.MANIFEST_NAME));
			}
		} catch (IOException e) {
			//ignore
		}

		//It is not a manifest, but a plugin or a fragment
		Dictionary manifest = null;
		if (manifestStream == null) {
			manifest = convertPluginManifest(bundleLocation, true);
			if (manifest == null)
				return null;
		}

		if (manifestStream != null) {
			try {
				Manifest m = new Manifest(manifestStream);
				manifest = manifestToProperties(m.getMainAttributes());
			} catch (IOException ioe) {
				return null;
			} finally {
				try {
					manifestStream.close();
				} catch (IOException e1) {
					//Ignore
				}
				try {
					if (jarFile != null)
						jarFile.close();
				} catch (IOException e2) {
					//Ignore
				}
			}
		}
		return manifest;
	}

	private void enforceSymbolicName(File bundleLocation, Dictionary initialManifest) {
		if (initialManifest.get(Constants.BUNDLE_SYMBOLICNAME) != null)
			return;

		Dictionary generatedManifest = convertPluginManifest(bundleLocation, false);
		if (generatedManifest == null)
			return;

		//merge manifests. The values from the generated manifest are added to the initial one. Values from the initial one are not deleted 
		Enumeration enumeration = generatedManifest.keys();
		while (enumeration.hasMoreElements()) {
			Object key = enumeration.nextElement();
			if (initialManifest.get(key) == null)
				initialManifest.put(key, generatedManifest.get(key));
		}
	}

	private void enforceClasspath(Dictionary manifest) {
		String classpath = (String) manifest.get(Constants.BUNDLE_CLASSPATH);
		if (classpath == null)
			manifest.put(Constants.BUNDLE_CLASSPATH, "."); //$NON-NLS-1$
	}

	private Dictionary loadManifest(File bundleLocation) {
		Dictionary manifest = basicLoadManifest(bundleLocation);
		if (manifest == null)
			return null;

		enforceSymbolicName(bundleLocation, manifest);
		enforceClasspath(manifest);
		return manifest;
	}

	private Dictionary convertPluginManifest(File bundleLocation, boolean logConversionException) {
		PluginConverter converter;
		try {
			converter = acquirePluginConverter();
			return converter.convertManifest(bundleLocation, false, AbstractScriptGenerator.isBuildingOSGi() ? null : "2.1", false, null); //$NON-NLS-1$
		} catch (PluginConversionException convertException) {
			if (bundleLocation.getName().equals("feature.xml")) //$NON-NLS-1$
				return null;
			if (logConversionException) {
				IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, 0, NLS.bind(Messages.exception_errorConverting, bundleLocation.getAbsolutePath()), convertException);
				BundleHelper.getDefault().getLog().log(status);
			}
			return null;
		} catch (Exception serviceException) {
			IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, 0, NLS.bind(Messages.exception_cannotAcquireService, "Plugin converter"), serviceException); //$NON-NLS-1$
			BundleHelper.getDefault().getLog().log(status);
			return null;
		}
	}

	private Properties manifestToProperties(Attributes d) {
		Iterator iter = d.keySet().iterator();
		Properties result = new Properties();
		while (iter.hasNext()) {
			Attributes.Name key = (Attributes.Name) iter.next();
			result.put(key.toString(), d.get(key));
		}
		return result;
	}

	public void addBundles(Collection bundles) {
		for (Iterator iter = bundles.iterator(); iter.hasNext();) {
			File bundle = (File) iter.next();
			addBundle(bundle);
		}
	}

	public void resolveState() {
		List configs = AbstractScriptGenerator.getConfigInfos();
		String[] os = new String[configs.size()];
		String[] ws = new String[configs.size()];
		String[] archs = new String[configs.size()];
		int i = 0;
		for (Iterator iter = configs.iterator(); iter.hasNext();) {
			Config aConfig = (Config) iter.next();
			os[i] = aConfig.getOs();
			ws[i] = aConfig.getWs();
			archs[i] = aConfig.getArch();
			i++;
		}
		Hashtable properties = new Hashtable(3);
		if (ws.length == 1 && Config.ANY.equalsIgnoreCase(ws[0])) {
			properties.put(OSGI_WS, CatchAllValue.singleton);
		} else {
			properties.put(OSGI_WS, ws);
		}

		if (os.length == 1 && Config.ANY.equalsIgnoreCase(os[0])) {
			properties.put(OSGI_OS, CatchAllValue.singleton);
		} else {
			properties.put(OSGI_OS, os);
		}

		if (archs.length == 1 && Config.ANY.equalsIgnoreCase(archs[0])) {
			properties.put(OSGI_ARCH, CatchAllValue.singleton);
		} else {
			properties.put(OSGI_ARCH, archs);
		}

		//Set the JRE profile
		if (javaProfile == null) {
			javaProfile = getDefaultJavaProfile();
		}
		Properties profileProps = getJavaProfileProperties();
		if (profileProps != null) {
			String systemPackages = profileProps.getProperty(SYSTEM_PACKAGES);
			if (systemPackages != null)
				properties.put(SYSTEM_PACKAGES, systemPackages);
			String ee = profileProps.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
			if (ee != null)
				properties.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, ee);
		}
		state.setPlatformProperties(properties);
		state.resolve(false);
	}

	private String getDefaultJavaProfile() {
		if (javaProfiles == null)
			setJavaProfiles(getOSGiLocation());
		if (javaProfiles != null && javaProfiles.length > 0)
			return javaProfiles[0];
		return null;
	}

	public State getState() {
		return state;
	}

	public BundleDescription[] getDependentBundles(String bundleId, Version version) {
		BundleDescription root = state.getBundle(bundleId, version);
		return getDependentBundles(root);
	}

	/**
	 * This methods return the bundleDescriptions to which imports have been
	 * bound to.
	 * 
	 * @param root
	 */
	public static BundleDescription[] getImportedBundles(BundleDescription root) {
		if (root == null)
			return new BundleDescription[0];
		ExportPackageDescription[] packages = root.getResolvedImports();
		ArrayList resolvedImports = new ArrayList(packages.length);
		for (int i = 0; i < packages.length; i++)
			if (!root.getLocation().equals(packages[i].getExporter().getLocation()) && !resolvedImports.contains(packages[i].getExporter()))
				resolvedImports.add(packages[i].getExporter());
		return (BundleDescription[]) resolvedImports.toArray(new BundleDescription[resolvedImports.size()]);
	}

	/**
	 * This methods return the bundleDescriptions to which required bundles
	 * have been bound to.
	 * 
	 * @param root
	 */
	public static BundleDescription[] getRequiredBundles(BundleDescription root) {
		if (root == null)
			return new BundleDescription[0];
		return root.getResolvedRequires();
	}

	public BundleDescription getResolvedBundle(String bundleId, String version) {
		if (IPDEBuildConstants.GENERIC_VERSION_NUMBER.equals(version) || version == null) {
			return getResolvedBundle(bundleId);
		}
		BundleDescription description = getState().getBundle(bundleId, Version.parseVersion(version));
		if (description != null && description.isResolved())
			return description;

		int qualifierIdx = -1;
		if ((qualifierIdx = version.indexOf('.' + IBuildPropertiesConstants.PROPERTY_QUALIFIER)) != -1) {
			BundleDescription[] bundles = getState().getBundles(bundleId);
			Version versionToMatch = Version.parseVersion(version.substring(0, qualifierIdx));
			for (int i = 0; i < bundles.length; i++) {
				Version bundleVersion = bundles[i].getVersion();
				if (bundleVersion.getMajor() == versionToMatch.getMajor() && bundleVersion.getMinor() == versionToMatch.getMinor() && bundleVersion.getMicro() >= versionToMatch.getMicro() && bundleVersion.getQualifier().compareTo(versionToMatch.getQualifier()) >= 0)
					return bundles[i];
			}
		}
		return null;
	}

	public BundleDescription getResolvedBundle(String bundleId) {
		BundleDescription[] description = getState().getBundles(bundleId);
		if (description == null)
			return null;
		for (int i = 0; i < description.length; i++) {
			if (description[i].isResolved())
				return description[i];
		}
		return null;
	}

	public static BundleDescription[] getDependentBundles(BundleDescription root) {
		BundleDescription[] imported = getImportedBundles(root);
		BundleDescription[] required = getRequiredBundles(root);
		BundleDescription[] dependents = new BundleDescription[imported.length + required.length];
		System.arraycopy(imported, 0, dependents, 0, imported.length);
		System.arraycopy(required, 0, dependents, imported.length, required.length);
		return dependents;
	}

	public static BundleDescription[] getDependentBundlesWithFragments(BundleDescription root) {
		BundleDescription[] imported = getImportedBundles(root);
		BundleDescription[] importedByFragments = getImportedByFragments(root);
		BundleDescription[] required = getRequiredBundles(root);
		BundleDescription[] requiredByFragments = getRequiredByFragments(root);
		BundleDescription[] dependents = new BundleDescription[imported.length + importedByFragments.length + required.length + requiredByFragments.length];
		System.arraycopy(imported, 0, dependents, 0, imported.length);
		System.arraycopy(importedByFragments, 0, dependents, imported.length, importedByFragments.length);
		System.arraycopy(required, 0, dependents, imported.length + importedByFragments.length, required.length);
		System.arraycopy(requiredByFragments, 0, dependents, imported.length + importedByFragments.length + required.length, requiredByFragments.length);
		return dependents;
	}

	public static BundleDescription[] getImportedByFragments(BundleDescription root) {
		BundleDescription[] fragments = root.getFragments();
		List importedByFragments = new ArrayList();
		for (int i = 0; i < fragments.length; i++) {
			if (!fragments[i].isResolved())
				continue;
			merge(importedByFragments, getImportedBundles(fragments[i]));
		}
		BundleDescription[] result = new BundleDescription[importedByFragments.size()];
		return (BundleDescription[]) importedByFragments.toArray(result);
	}

	public static BundleDescription[] getRequiredByFragments(BundleDescription root) {
		BundleDescription[] fragments = root.getFragments();
		List importedByFragments = new ArrayList();
		for (int i = 0; i < fragments.length; i++) {
			if (!fragments[i].isResolved())
				continue;
			merge(importedByFragments, getRequiredBundles(fragments[i]));
		}
		BundleDescription[] result = new BundleDescription[importedByFragments.size()];
		return (BundleDescription[]) importedByFragments.toArray(result);
	}

	public static void merge(List source, BundleDescription[] toAdd) {
		for (int i = 0; i < toAdd.length; i++) {
			if (!source.contains(toAdd[i]))
				source.add(toAdd[i]);
		}
	}

	public Properties loadPropertyFileIn(Map toMerge, File location) {
		Properties result = new Properties();
		result.putAll(toMerge);
		try {
			InputStream propertyStream = new BufferedInputStream(new FileInputStream(new File(location, PROPERTIES_FILE)));
			try {
				result.load(propertyStream);
			} finally {
				propertyStream.close();
			}
		} catch (Exception e) {
			//ignore because compiled plug-ins do not have such files
		}
		return result;
	}

	public HashMap getExtraData() {
		return bundleClasspaths;
	}

	public Map getPatchData() {
		return patchBundles;
	}
	
	public List getSortedBundles() {
		BundleDescription[] toSort = getState().getResolvedBundles();
		Platform.getPlatformAdmin().getStateHelper().sortBundles(toSort);
		return Arrays.asList(toSort);
	}

	public void cleanupOriginalState() {
		if (addedBundle == null && unqualifiedBundles == null)
			return;

		for (Iterator iter = addedBundle.iterator(); iter.hasNext();) {
			BundleDescription added = (BundleDescription) iter.next();
			state.removeBundle(added);
		}

		for (Iterator iter = unqualifiedBundles.iterator(); iter.hasNext();) {
			BundleDescription toAddBack = (BundleDescription) iter.next();
			state.removeBundle(toAddBack.getBundleId());
			state.addBundle(toAddBack);
		}

		BundleDescription[] allBundles = state.getBundles();
		for (int i = 0; i < allBundles.length; i++) {
			allBundles[i].setUserObject(null);
		}
		state.resolve();
	}

	private File getOSGiLocation() {
		BundleDescription osgiBundle = state.getBundle("org.eclipse.osgi", null); //$NON-NLS-1$
		if (osgiBundle == null)
			return null;
		return new File(osgiBundle.getLocation());
	}

	private void setJavaProfiles(File bundleLocation) {
		if (bundleLocation == null)
			return;
		if (bundleLocation.isDirectory())
			javaProfiles = getDirJavaProfiles(bundleLocation);
		else
			javaProfiles = getJarJavaProfiles(bundleLocation);
	}

	private String[] getDirJavaProfiles(File bundleLocation) {
		// try the profile list first
		File profileList = new File (bundleLocation, "profile.list");
		if (profileList.exists())
			try {
				return getJavaProfiles(new FileInputStream(profileList));
			} catch (IOException e) {
				// this should not happen because we just checked if the file exists
			}
		String[] profiles = bundleLocation.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(PROFILE_EXTENSION);
			}
		});
		return sortProfiles(profiles);
	}

	private String[] getJarJavaProfiles(File bundleLocation) {
		ZipFile zipFile = null;
		ArrayList results = new ArrayList(6);
		try {
			zipFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
			ZipEntry profileList = zipFile.getEntry("profile.list");
			if (profileList != null)
				try {
					return getJavaProfiles(zipFile.getInputStream(profileList));
				} catch (IOException e) {
					// this should not happen, just incase do the default
				}
			
			Enumeration entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				String entryName = ((ZipEntry) entries.nextElement()).getName();
				if (entryName.indexOf('/') < 0 && entryName.endsWith(PROFILE_EXTENSION))
					results.add(entryName);
			}
		} catch (IOException e) {
			// nothing to do
		} finally {
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (IOException e) {
					// nothing to do
				}
		}
		return sortProfiles((String[]) results.toArray(new String[results.size()]));
	}

	private String[] getJavaProfiles(InputStream is) throws IOException {
		Properties props = new Properties();
		props.load(is);
		return ManifestElement.getArrayFromList(props.getProperty("java.profiles"), ","); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String[] sortProfiles(String[] profiles) {
		Arrays.sort(profiles, new Comparator() {
			public int compare(Object profile1, Object profile2) {
				// need to make sure J2SE profiles are sorted ahead of all other profiles
				String p1 = (String) profile1;
				String p2 = (String) profile2;
				if (p1.startsWith("J2SE") && !p2.startsWith("J2SE"))
					return -1;
				if (!p1.startsWith("J2SE") && p2.startsWith("J2SE"))
					return 1;
				return -p1.compareTo(p2);
			}
		});
		return profiles;
	}

	private Properties getJavaProfileProperties() {
		if (javaProfile == null)
			return null;
		File location = getOSGiLocation();
		InputStream is = null;
		ZipFile zipFile = null;
		try {
			if (location.isDirectory()) {
				is = new FileInputStream(new File(location, javaProfile));
			} else {
				zipFile = null;
				try {
					zipFile = new ZipFile(location, ZipFile.OPEN_READ);
					ZipEntry entry = zipFile.getEntry(javaProfile);
					if (entry != null)
						is = zipFile.getInputStream(entry);
				} catch (IOException e) {
					// nothing to do
				}
			}
			Properties profile = new Properties();
			profile.load(is);
			return profile;
		} catch (IOException e) {
			// nothing to do
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					// nothing to do
				}
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (IOException e) {
					// nothing to do
				}
		}
		return null;
	}

	//Replace the version numbers that ends with .qualifier
	private void forceQualifiers() {
		BundleDescription[] resolvedBundles = state.getResolvedBundles(); //We only get the resolved bundles since, changing the qualifier should not change the resolution state 
		for (int i = 0; i < resolvedBundles.length; i++) {
			if (resolvedBundles[i].getVersion().getQualifier().equals(PROPERTY_QUALIFIER)) {
				BundleDescription b = resolvedBundles[i];
				unqualifiedBundles.add(state.removeBundle(b.getBundleId())); //We keep the removed bundle so we can reinsert it in the state when we are done
				String newVersion = QualifierReplacer.replaceQualifierInVersion(b.getVersion().toString(), b.getSymbolicName(), getQualifierPropery(b.getLocation()), null);
				
				//Here it is important to reuse the same bundle id than the bundle we are removing so that we don't loose the information about the classpath
				BundleDescription newBundle = state.getFactory().createBundleDescription(b.getBundleId(), b.getSymbolicName(), new Version(newVersion), b.getLocation(), b.getRequiredBundles(), b.getHost(), b.getImportPackages(), b.getExportPackages(), null, b.isSingleton());
				state.addBundle(newBundle);
				rememberQualifierTagPresence(newBundle);
			}
		}
		state.resolve();
	}
}
