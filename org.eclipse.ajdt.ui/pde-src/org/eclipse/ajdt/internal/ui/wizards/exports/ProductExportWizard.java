/*******************************************************************************
 * Copyright (c) 2005, 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ajdt.internal.ui.wizards.exports;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.ajdt.internal.ui.build.ProductExportJob;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.core.product.WorkspaceProductModel;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.exports.CrossPlatformExportPage;
import org.eclipse.pde.internal.ui.wizards.product.SynchronizationOperation;
import org.eclipse.ui.progress.IProgressConstants;


public class ProductExportWizard extends BaseExportWizard {
	
	private static final String STORE_SECTION = "ProductExportWizard"; //$NON-NLS-1$
	private WorkspaceProductModel fProductModel;
	private CrossPlatformExportPage fPage2;
	private ProductExportWizardPage fPage;
	private IProject fProject;

	public ProductExportWizard() {
		this(null);
	}

	public ProductExportWizard(IProject project) {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_PRODUCT_EXPORT_WIZ);
		fProject = project;
	}
	
	public void addPages() {
		fPage = new ProductExportWizardPage(getSelection());
		addPage(fPage);
		
		IFeatureModel model = PDECore.getDefault().getFeatureModelManager().getDeltaPackFeature();
		if (model != null) {
			fPage2 = new CrossPlatformExportPage("environment", model); //$NON-NLS-1$
			addPage(fPage2);
		}	
	}
	
	protected String getSettingsSectionName() {
		return STORE_SECTION;
	}

	protected void scheduleExportJob() {
		FeatureExportInfo info = new FeatureExportInfo();
		info.toDirectory = fPage.doExportToDirectory();
		info.exportSource = fPage.doExportSource();
		info.destinationDirectory = fPage.getDestination();
		info.zipFileName = fPage.getFileName();
		if (fPage2 != null && fPage.doMultiPlatform())
			info.targets = fPage2.getTargets();
		if (fProductModel.getProduct().useFeatures())
			info.items = getFeatureModels();
		else
			info.items = getPluginModels();
		
		String rootDirectory = fPage.getRootDirectory();
		if ("".equals(rootDirectory.trim())) //$NON-NLS-1$
			rootDirectory = ".";  //$NON-NLS-1$
		ProductExportJob job = new ProductExportJob(info, fProductModel, rootDirectory);
		job.setUser(true);
		job.schedule();
		job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_FEATURE_OBJ);
	}
	
	private IFeatureModel[] getFeatureModels() {
		ArrayList list = new ArrayList();
		FeatureModelManager manager = PDECore.getDefault()
				.getFeatureModelManager();
		IProductFeature[] features = fProductModel.getProduct().getFeatures();
		for (int i = 0; i < features.length; i++) {
			IFeatureModel model = manager.findFeatureModel(features[i].getId(),
					features[i].getVersion());
			if (model != null)
				list.add(model);
		}
		return (IFeatureModel[]) list.toArray(new IFeatureModel[list.size()]);
	}

	private BundleDescription[] getPluginModels() {
		ArrayList list = new ArrayList();
		State state = TargetPlatformHelper.getState();
		IProductPlugin[] plugins = fProductModel.getProduct().getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			BundleDescription bundle = state.getBundle(plugins[i].getId(), null);
			if (bundle != null)
				list.add(bundle);
		}
		// implicitly add the new launcher plug-in/fragment if we are to use the
		// new launching story and the launcher plug-in/fragment are not already included in the .product file
		IPluginModelBase launcherPlugin = PluginRegistry.findModel("org.eclipse.equinox.launcher"); //$NON-NLS-1$
		if (launcherPlugin != null) {
			BundleDescription bundle = launcherPlugin.getBundleDescription();
			if (bundle != null && !list.contains(bundle)) {
				list.add(bundle);
				BundleDescription[] fragments = bundle.getFragments();
				for (int i = 0; i < fragments.length; i++) {
					if (!list.contains(fragments[i])) {
						list.add(fragments[i]);
					}
				}
			}
		}
		return (BundleDescription[]) list.toArray(new BundleDescription[list.size()]);
	}
	
	protected boolean performPreliminaryChecks() {
		fProductModel = new WorkspaceProductModel(fPage.getProductFile(), false);
		try {
			fProductModel.load();
			if (!fProductModel.isLoaded()) {
				MessageDialog.openError(getContainer().getShell(), PDEUIMessages.ProductExportWizard_error, PDEUIMessages.ProductExportWizard_corrupt); // 
				return false;
			}
		} catch (CoreException e) {
			MessageDialog.openError(getContainer().getShell(), PDEUIMessages.ProductExportWizard_error, PDEUIMessages.ProductExportWizard_corrupt); // 
			return false;
		}

		if (fPage.doSync()) {
			try {
				getContainer().run(false, false, new SynchronizationOperation(fProductModel.getProduct(), getContainer().getShell(), fProject));
			} catch (InvocationTargetException e) {
				MessageDialog.openError(getContainer().getShell(), PDEUIMessages.ProductExportWizard_syncTitle, e.getTargetException().getMessage()); 
				return false;
			} catch (InterruptedException e) {
				return false;
			}
		}		
		return true;
	}
	
	protected boolean confirmDelete() {
		if (!fPage.doExportToDirectory()) {
			File zipFile = new File(fPage.getDestination(), fPage.getFileName());
			if (zipFile.exists()) {
				if (!MessageDialog.openQuestion(getContainer().getShell(),
						PDEUIMessages.BaseExportWizard_confirmReplace_title,  
						NLS.bind(PDEUIMessages.BaseExportWizard_confirmReplace_desc, zipFile.getAbsolutePath())))
					return false;
				zipFile.delete();
			}
		}
		return true;
	}
	
	public boolean canFinish() {
		return (fPage.getNextPage() != null) ? super.canFinish() : fPage.isPageComplete();
	}

}
