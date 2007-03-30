/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ajdt.internal.core.exports;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.build.BuildScriptGenerator;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.iproduct.IArgumentsInfo;
import org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo;
import org.eclipse.pde.internal.core.iproduct.IJREInfo;
import org.eclipse.pde.internal.core.iproduct.ILauncherInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.ISplashInfo;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ProductExportOperation extends FeatureExportOperation {

	private String fFeatureLocation;
	private String fRoot;
	private IProduct fProduct;

	public ProductExportOperation(FeatureExportInfo info, IProduct product, String root) {
		super(info);
		fProduct = product;
		fRoot = root;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		String[][] configurations = fInfo.targets;
		if (configurations == null)
			configurations = new String[][] { {TargetPlatform.getOS(), TargetPlatform.getWS(), TargetPlatform.getOSArch(), TargetPlatform.getNL() } };
		monitor.beginTask("", 10 * configurations.length); //$NON-NLS-1$
		for (int i = 0; i < configurations.length; i++) {
			try {
				String[] config = configurations[i];
				if (config[0].equals("macosx") && fInfo.targets == null) //$NON-NLS-1$
					createMacScript(config, new SubProgressMonitor(monitor, 1));
				// create a feature to wrap all plug-ins and features
				String featureID = "org.eclipse.pde.container.feature"; //$NON-NLS-1$
				fFeatureLocation = fBuildTempLocation + File.separator + featureID;
				
				createFeature(featureID, fFeatureLocation, config, true);
				createBuildPropertiesFile(fFeatureLocation, config);
				createConfigIniFile(config);
				createEclipseProductFile();
				createLauncherIniFile(config[0]);
				doExport(featureID, 
						null, 
						fFeatureLocation, 
						config[0], 
						config[1], 
						config[2], 
						new SubProgressMonitor(monitor, 8));
			} catch (IOException e) {
			} catch (InvocationTargetException e) {
				throwCoreException(e);
			} finally {
				for (int j = 0; j < fInfo.items.length; j++) {
					deleteBuildFiles(fInfo.items[j]);
				}
				cleanup(fInfo.targets == null ? null : configurations[i], new SubProgressMonitor(monitor, 1));
			}
		}
		monitor.done();
	}

	private File getCustomIniFile() {
		IConfigurationFileInfo info = fProduct.getConfigurationFileInfo();
		if (info != null  && info.getUse().equals("custom")) { //$NON-NLS-1$
			String path = getExpandedPath(info.getPath());
			if (path != null) {
				File file = new File(path);
				if (file.exists() && file.isFile())
					return file;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.exports.FeatureExportJob#getPaths()
	 */
	protected String[] getPaths() {
		String[] paths = super.getPaths();
		String[] all = new String[paths.length + 1];
		all[0] = fFeatureLocation + File.separator + "feature.xml"; //$NON-NLS-1$
		System.arraycopy(paths, 0, all, 1, paths.length);
		return all;
	}

	private void createBuildPropertiesFile(String featureLocation, String[] config) {
		File file = new File(featureLocation);
		if (!file.exists() || !file.isDirectory())
			file.mkdirs();

		boolean hasLaunchers = PDECore.getDefault().getFeatureModelManager().getDeltaPackFeature() != null;
		Properties properties = new Properties();
		properties.put(IBuildPropertiesConstants.ROOT, getRootFileLocations(hasLaunchers)); //To copy a folder
		if (!hasLaunchers) {
			properties.put("root.permissions.755", getLauncherName()); //$NON-NLS-1$
			if (TargetPlatform.getWS().equals("motif") && TargetPlatform.getOS().equals("linux")) { //$NON-NLS-1$ //$NON-NLS-2$
				properties.put("root.linux.motif.x86.permissions.755", "libXm.so.2"); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (TargetPlatform.getOS().equals("macosx")) { //$NON-NLS-1$
				properties.put(
						"root.macosx.carbon.ppc.permissions.755" ,  //$NON-NLS-1$
				"${launcherName}.app/Contents/MacOS/${launcherName}"); //$NON-NLS-1$
			}
		}
		
		IJREInfo jreInfo = fProduct.getJREInfo();
		String vm = jreInfo != null ? jreInfo.getJVMLocation(config[0]) : null;
		if(vm != null) {
			properties.put("root."+config[0]+ //$NON-NLS-1$
					"."+config[1]+ //$NON-NLS-1$
					"."+config[2]+ //$NON-NLS-1$
			        ".folder.jre", //$NON-NLS-1$
					"absolute:" + vm); //$NON-NLS-1$
			properties.put("root.permissions.755", "jre/bin/java"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		save(new File(file, "build.properties"), properties, "Build Configuration"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String getRootFileLocations(boolean hasLaunchers) {
		StringBuffer buffer = new StringBuffer();

		File homeDir = new File(TargetPlatform.getLocation());
		if (!hasLaunchers) {
			if (homeDir.exists() && homeDir.isDirectory()) {
				buffer.append("absolute:file:"); //$NON-NLS-1$
				buffer.append(new File(homeDir, "startup.jar").getAbsolutePath()); //$NON-NLS-1$
				if (!TargetPlatform.getOS().equals("macosx")) { //$NON-NLS-1$
					File file = new File(homeDir, "eclipse"); //$NON-NLS-1$
					if (file.exists()) {
						buffer.append(",absolute:file:"); //$NON-NLS-1$
						buffer.append(file.getAbsolutePath()); 
					}				
					file = new File(homeDir, "eclipse.exe"); //$NON-NLS-1$
					if (file.exists()) {
						buffer.append(",absolute:file:"); //$NON-NLS-1$
						buffer.append(file.getAbsolutePath()); 
					}
					file = new File(homeDir, "libXm.so.2"); //$NON-NLS-1$
					if (file.exists()) {
						buffer.append(",absolute:file:"); //$NON-NLS-1$
						buffer.append(file.getAbsolutePath()); 
					}
				}
			}	
		}
		// add content of temp folder (.eclipseproduct, configuration/config.ini)
		if (buffer.length() > 0)
			buffer.append(","); //$NON-NLS-1$
		buffer.append("/temp/"); //$NON-NLS-1$
		
		return buffer.toString();
	}
	
	private void createEclipseProductFile() {
		File dir = new File(fFeatureLocation, "temp"); //$NON-NLS-1$
		if (!dir.exists() || !dir.isDirectory())
			dir.mkdirs();
		Properties properties = new Properties();
		IPluginModelBase model = PluginRegistry.findModel(getBrandingPlugin());
		if (model != null)
			properties.put("name", model.getResourceString(fProduct.getName())); //$NON-NLS-1$
		else
			properties.put("name", fProduct.getName()); //$NON-NLS-1$
		properties.put("id", fProduct.getId()); //$NON-NLS-1$
		if (model != null)
			properties.put("version", model.getPluginBase().getVersion()); //$NON-NLS-1$
		save(new File(dir, ".eclipseproduct"), properties, "Eclipse Product File"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void createLauncherIniFile(String os) {
		String programArgs = getProgramArguments(os);
		String vmArgs = getVMArguments(os);

		if (programArgs.length() == 0 && vmArgs.length() == 0)
			return;

		File dir = new File(fFeatureLocation, "temp"); //$NON-NLS-1$
		if (!dir.exists() || !dir.isDirectory())
			dir.mkdirs();

		String lineDelimiter = Platform.OS_WIN32.equals(os)?"\r\n":"\n"; //$NON-NLS-1$ //$NON-NLS-2$

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File(dir, getLauncherName() + ".ini"))); //$NON-NLS-1$
			if (programArgs.length() > 0) {
				StringTokenizer tokenizer = new StringTokenizer(programArgs);
				while (tokenizer.hasMoreTokens()){
					writer.print(tokenizer.nextToken());
					writer.print(lineDelimiter);
				}
			}
			if (vmArgs.length() > 0) {
				writer.print("-vmargs"); //$NON-NLS-1$
				writer.print(lineDelimiter);
				StringTokenizer tokenizer = new StringTokenizer(vmArgs);
				while (tokenizer.hasMoreTokens()){
					writer.print(tokenizer.nextToken());
					writer.print(lineDelimiter);
				}

			}	
		} catch (IOException e) {
		} finally {
			if (writer != null) {
				writer.close();
			}			
		}
	}

	private String getProgramArguments(String os) {
		IArgumentsInfo info = fProduct.getLauncherArguments();
		return info != null ? CoreUtility.normalize(info.getCompleteProgramArguments(os)) : "";//$NON-NLS-1$
	}

	private String getVMArguments(String os) {
		IArgumentsInfo info = fProduct.getLauncherArguments();
		return (info != null) ? CoreUtility.normalize(info.getCompleteVMArguments(os)) : ""; //$NON-NLS-1$
	}	


	private void createConfigIniFile(String[] config) {
		File dir = new File(fFeatureLocation, "temp/configuration"); //$NON-NLS-1$
		if (!dir.exists() || !dir.isDirectory())
			dir.mkdirs();

		PrintWriter writer = null;

		File custom = getCustomIniFile();       
		if (custom != null) {
			String path = getExpandedPath(fProduct.getConfigurationFileInfo().getPath());
			BufferedReader in = null;
			try {
				in = new BufferedReader(new FileReader(path));
				writer = new PrintWriter(new FileWriter(new File(dir, "config.ini"))); //$NON-NLS-1$
				String line;
				while ((line = in.readLine()) != null) {
					writer.println(line);
				}
			} catch (IOException e) {
			} finally {
				try {
					if (in != null)
						in.close();
					if (writer != null)
						writer.close();
				} catch (IOException e) {
				}
			}
			return;
		} 
		try {
			writer = new PrintWriter(new FileWriter(new File(dir, "config.ini"))); //$NON-NLS-1$
			String location = getSplashLocation(config[0], config[1], config[2]);
			writer.println("#Product Runtime Configuration File"); //$NON-NLS-1$
			writer.println();
			if (location != null)
				writer.println("osgi.splashPath=" + location); //$NON-NLS-1$
			writer.println("eclipse.product=" + fProduct.getId()); //$NON-NLS-1$
			writer.println("osgi.bundles=" + getPluginList(config, TargetPlatform.getBundleList()));  //$NON-NLS-1$
			writer.println("osgi.bundles.defaultStartLevel=4"); //$NON-NLS-1$ 		
		} catch (IOException e) {
		} finally {
			if (writer != null)
				writer.close();
		}
	}

	private String getSplashLocation(String os, String ws, String arch) {
		ISplashInfo info = fProduct.getSplashInfo();
		String plugin = null;
		if (info != null) {
			plugin = info.getLocation();
		}
		if (plugin == null || plugin.trim().length() == 0)
			plugin = getBrandingPlugin();

		if (plugin == null)
			return null;

		StringBuffer buffer = new StringBuffer("platform:/base/plugins/"); //$NON-NLS-1$
		buffer.append(plugin.trim());

		State state = getState(os, ws, arch);
		BundleDescription bundle = state.getBundle(plugin, null);
		if (bundle != null) {
			BundleDescription[] fragments = bundle.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				String id = fragments[i].getSymbolicName();
				if (fProduct.containsPlugin(id)) {
					buffer.append(",platform:/base/plugins/"); //$NON-NLS-1$
					buffer.append(id);
				}
			}
		}	
		return buffer.toString();
	}

	private String getBrandingPlugin() {
		int dot = fProduct.getId().lastIndexOf('.');
		return (dot != -1) ? fProduct.getId().substring(0, dot) : null;
	}

	private String getPluginList(String[] config, String bundleList) {
		if (fProduct.useFeatures())
			return bundleList;

		StringBuffer buffer = new StringBuffer();

		// include only bundles that are actually in this product configuration
		Set initialBundleSet = new HashSet();
		StringTokenizer tokenizer = new StringTokenizer(bundleList, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			int index = token.indexOf('@');
			String id = index != -1 ? token.substring(0, index) : token;
			if (fProduct.containsPlugin(id)) {
				if (buffer.length() > 0)
					buffer.append(',');
				buffer.append(id);
				if (index != -1 && index < token.length() -1)
					buffer.append(token.substring(index));				
				initialBundleSet.add(id);
			}
		}

		if (!fProduct.containsPlugin("org.eclipse.update.configurator")) { //$NON-NLS-1$
			initialBundleSet.add("org.eclipse.osgi"); //$NON-NLS-1$

			Dictionary environment = new Hashtable(4);
			environment.put("osgi.os", config[0]); //$NON-NLS-1$
			environment.put("osgi.ws", config[1]); //$NON-NLS-1$
			environment.put("osgi.arch", config[2]); //$NON-NLS-1$
			environment.put("osgi.nl", config[3]); //$NON-NLS-1$

			BundleContext context = PDECore.getDefault().getBundleContext();	        
			for (int i = 0; i < fInfo.items.length; i++) {
				BundleDescription bundle = (BundleDescription)fInfo.items[i];
				String filterSpec = bundle.getPlatformFilter();
				try {
					if (filterSpec == null|| context.createFilter(filterSpec).match(environment)) {			
						String id = ((BundleDescription)fInfo.items[i]).getSymbolicName();				
						if (!initialBundleSet.contains(id)) {
							if (buffer.length() > 0)
								buffer.append(","); //$NON-NLS-1$
							buffer.append(id);
						}
					}
				} catch (InvalidSyntaxException e) {
				}
			}
		}
		return buffer.toString();
	}

	protected HashMap createAntBuildProperties(String os, String ws, String arch) {
		HashMap properties = super.createAntBuildProperties(os, ws, arch);
		properties.put(IXMLConstants.PROPERTY_LAUNCHER_NAME, getLauncherName());

		ILauncherInfo info = fProduct.getLauncherInfo();	
		if (info != null) {
			String images = null;
			if (os.equals("win32")) { //$NON-NLS-1$
				images = getWin32Images(info);
			} else if (os.equals("solaris")) { //$NON-NLS-1$
				images = getSolarisImages(info);
			} else if (os.equals("linux")) { //$NON-NLS-1$
				images = getExpandedPath(info.getIconPath(ILauncherInfo.LINUX_ICON));
			} else if (os.equals("macosx")) { //$NON-NLS-1$
				images = getExpandedPath(info.getIconPath(ILauncherInfo.MACOSX_ICON));
			}
			if (images != null && images.length() > 0)
				properties.put(IXMLConstants.PROPERTY_LAUNCHER_ICONS, images);
		}

		fAntBuildProperties.put(IXMLConstants.PROPERTY_COLLECTING_FOLDER, fRoot); 
		fAntBuildProperties.put(IXMLConstants.PROPERTY_ARCHIVE_PREFIX, fRoot); 
		return properties;
	}

	private String getLauncherName() {
		ILauncherInfo info = fProduct.getLauncherInfo();	
		if (info != null) {
			String name = info.getLauncherName();
			if (name != null && name.length() > 0) {
				name = name.trim();
				if (name.endsWith(".exe")) //$NON-NLS-1$
					name = name.substring(0, name.length() - 4);
				return name;
			}
		}
		return "eclipse";	 //$NON-NLS-1$
	}

	private String getWin32Images(ILauncherInfo info) {
		StringBuffer buffer = new StringBuffer();
		if (info.usesWinIcoFile()) {
			append(buffer, info.getIconPath(ILauncherInfo.P_ICO_PATH));
		} else {
			append(buffer, info.getIconPath(ILauncherInfo.WIN32_16_LOW));
			append(buffer, info.getIconPath(ILauncherInfo.WIN32_16_HIGH));
			append(buffer, info.getIconPath(ILauncherInfo.WIN32_32_HIGH));
			append(buffer, info.getIconPath(ILauncherInfo.WIN32_32_LOW));
			append(buffer, info.getIconPath(ILauncherInfo.WIN32_48_HIGH));
			append(buffer, info.getIconPath(ILauncherInfo.WIN32_48_LOW));
		}
		return buffer.length() > 0 ? buffer.toString() : null;
	}

	private String getSolarisImages(ILauncherInfo info) {
		StringBuffer buffer = new StringBuffer();
		append(buffer, info.getIconPath(ILauncherInfo.SOLARIS_LARGE));
		append(buffer, info.getIconPath(ILauncherInfo.SOLARIS_MEDIUM));
		append(buffer, info.getIconPath(ILauncherInfo.SOLARIS_SMALL));
		append(buffer, info.getIconPath(ILauncherInfo.SOLARIS_TINY));
		return buffer.length() > 0 ? buffer.toString() : null;
	}

	private void append(StringBuffer buffer, String path) {
		path = getExpandedPath(path);
		if (path != null) {
			if (buffer.length() > 0)
				buffer.append(","); //$NON-NLS-1$
			buffer.append(path);
		}
	}

	private String getExpandedPath(String path) {
		if (path == null || path.length() == 0)
			return null;
		IResource resource = PDECore.getWorkspace().getRoot().findMember(new Path(path));
		if (resource != null) {
			IPath fullPath = resource.getLocation();
			return fullPath == null ? null : fullPath.toOSString();
		}
		return null;
	}

	private void save(File file, Properties properties, String header) {
		try {
			FileOutputStream stream = new FileOutputStream(file);
			properties.store(stream, header); 
			stream.flush();
			stream.close();
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}

	protected void setupGenerator(BuildScriptGenerator generator, String featureID, String versionId, String os, String ws, String arch, String featureLocation) throws CoreException {
		super.setupGenerator(generator, featureID, versionId, os, ws, arch, featureLocation);
		if (fProduct != null)
			generator.setProduct(fProduct.getModel().getInstallLocation());
	}

	private void createMacScript(String[] config, IProgressMonitor monitor) {
		URL url = PDECore.getDefault().getBundle().getEntry("macosx/Info.plist");  //$NON-NLS-1$
		if (url == null)
			return;

		File scriptFile = null;
		File plist = null;
		InputStream in = null;
		String location = PDECore.getDefault().getStateLocation().toOSString();
		try {
			in = url.openStream();
			File dir = new File(location, "Eclipse.app/Contents"); //$NON-NLS-1$
			dir.mkdirs();
			plist = new File(dir, "Info.plist"); //$NON-NLS-1$
			CoreUtility.readFile(in, plist);
			scriptFile = createScriptFile("macbuild.xml"); //$NON-NLS-1$
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();

			Element root = doc.createElement("project"); //$NON-NLS-1$
			root.setAttribute("name", "project"); //$NON-NLS-1$ //$NON-NLS-2$
			root.setAttribute("default", "default"); //$NON-NLS-1$ //$NON-NLS-2$
			doc.appendChild(root);

			Element property = doc.createElement("property"); //$NON-NLS-1$
			property.setAttribute("name", "eclipse.base"); //$NON-NLS-1$ //$NON-NLS-2$
			property.setAttribute("value", "${assemblyTempDir}/${collectingFolder}"); //$NON-NLS-1$ //$NON-NLS-2$
			root.appendChild(property);

			Element target = doc.createElement("target"); //$NON-NLS-1$
			target.setAttribute("name", "default"); //$NON-NLS-1$ //$NON-NLS-2$
			root.appendChild(target);

			Element copy = doc.createElement("copy"); //$NON-NLS-1$
			StringBuffer toDir = new StringBuffer("${eclipse.base}/"); //$NON-NLS-1$
			toDir.append(config[0]);
			toDir.append("."); //$NON-NLS-1$
			toDir.append(config[1]);
			toDir.append("."); //$NON-NLS-1$
			toDir.append(config[2]);
			toDir.append("/${collectingFolder}"); //$NON-NLS-1$
			copy.setAttribute("todir", toDir.toString());  //$NON-NLS-1$ 
			copy.setAttribute("failonerror", "false"); //$NON-NLS-1$ //$NON-NLS-2$
			copy.setAttribute("overwrite", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			target.appendChild(copy);

			Element fileset = doc.createElement("fileset"); //$NON-NLS-1$
			fileset.setAttribute("dir", "${installFolder}"); //$NON-NLS-1$ //$NON-NLS-2$
			fileset.setAttribute("includes", "Eclipse.app/Contents/MacOS/eclipse"); //$NON-NLS-1$ //$NON-NLS-2$
			copy.appendChild(fileset);

			fileset = doc.createElement("fileset"); //$NON-NLS-1$
			fileset.setAttribute("dir", "${template}"); //$NON-NLS-1$ //$NON-NLS-2$
			fileset.setAttribute("includes", "Eclipse.app/Contents/Info.plist"); //$NON-NLS-1$ //$NON-NLS-2$
			copy.appendChild(fileset);

			XMLPrintHandler.writeFile(doc, scriptFile);

			AntRunner runner = new AntRunner();
			HashMap map = new HashMap();
			if (!fInfo.toDirectory) {
				String filename = fInfo.zipFileName;
				map.put(IXMLConstants.PROPERTY_ARCHIVE_FULLPATH, fInfo.destinationDirectory + File.separator + filename);
			} else {
				map.put(IXMLConstants.PROPERTY_ASSEMBLY_TMP, fInfo.destinationDirectory);
			}
			map.put(IXMLConstants.PROPERTY_COLLECTING_FOLDER, fRoot); 
			map.put("installFolder",  TargetPlatform.getLocation()); //$NON-NLS-1$
			map.put("template", location); //$NON-NLS-1$
			runner.addUserProperties(map);
			runner.setBuildFileLocation(scriptFile.getAbsolutePath());
			runner.setExecutionTargets(new String[] {"default"}); //$NON-NLS-1$
			runner.run(new SubProgressMonitor(monitor, 1));
		} catch (FactoryConfigurationError e) {
		} catch (ParserConfigurationException e) {
		} catch (CoreException e) {
		} catch (IOException e) {
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
			}
			CoreUtility.deleteContent(new File(location, "Eclipse.app"));		 //$NON-NLS-1$
			if (scriptFile != null && scriptFile.exists())
				scriptFile.delete();
			monitor.done();
		}	
	}

	protected void setAdditionalAttributes(Element plugin, BundleDescription bundle) {
		boolean unpack = CoreUtility.guessUnpack(bundle);
		plugin.setAttribute("unpack", Boolean.toString(unpack)); //$NON-NLS-1$
	}


}
