/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ajdt.internal.ui.wizards.exports;

import org.eclipse.osgi.util.NLS;

/**
 * Copied from org.eclipse.jdt.internal.ui.jarpackager.JarPackagerMessages.
 * Changes marked with // AspectJ Change
 */
public final class JarPackagerMessages extends NLS {

	// AspectJ Change
	private static final String BUNDLE_NAME = "org.eclipse.ajdt.internal.ui.wizards.exports.JarPackagerMessages";//$NON-NLS-1$

	private JarPackagerMessages() {
		// Do not instantiate
	}

	public static String CreateJarActionDelegate_jarExportError_title;
	public static String CreateJarActionDelegate_jarExport_title;
	public static String CreateJarActionDelegate_jarExportError_message;
	public static String JarPackage_confirmCreate_title;
	public static String JarPackage_confirmCreate_message;
	public static String JarPackage_confirmReplace_title;
	public static String JarPackage_confirmReplace_message;
	public static String JarPackageWizard_windowTitle;
	public static String JarPackageWizard_jarExport_title;
	public static String JarPackageWizard_jarExportError_title;
	public static String JarPackageWizard_jarExportError_message;
	public static String OpenJarPackageWizardDelegate_badXmlFormat;
	public static String OpenJarPackageWizardDelegate_error_openJarPackager_title;
	public static String OpenJarPackageWizardDelegate_error_openJarPackager_message;
	public static String OpenJarPackageWizardDelegate_jarDescriptionReaderWarnings_title;
	public static String JarWriter_error_fileNotAccessible;
	public static String JarWriter_writeProblem;
	public static String JarWriter_writeProblemWithMessage;
	public static String JarFileExportOperation_exportFinishedWithInfo;
	public static String JarFileExportOperation_exportFinishedWithWarnings;
	public static String JarFileExportOperation_creationOfSomeJARsFailed;
	public static String JarFileExportOperation_jarCreationFailed;
	public static String JarFileExportOperation_underlyingResourceNotFound;
	public static String JarFileExportOperation_resourceNotFound;
	public static String JarFileExportOperation_resourceNotLocal;
	public static String JarFileExportOperation_projectNatureNotDeterminable;
	public static String JarFileExportOperation_javaPackageNotDeterminable;
	public static String JarFileExportOperation_coreErrorDuringExport;
	public static String JarFileExportOperation_errorDuringExport;
	public static String JarFileExportOperation_outputContainerNotAccessible;
	public static String JarFileExportOperation_classFileOnClasspathNotAccessible;
	public static String JarFileExportOperation_classFileWithoutSourceFileAttribute;
	public static String JarFileExportOperation_missingSourceFileAttributeExportedAll;
	public static String JarFileExportOperation_exportedWithCompileErrors;
	public static String JarFileExportOperation_exportedWithCompileWarnings;
	public static String JarFileExportOperation_notExportedDueToCompileErrors;
	public static String JarFileExportOperation_notExportedDueToCompileWarnings;
	public static String JarFileExportOperation_exporting;
	public static String JarFileExportOperation_jarCreationFailedSeeDetails;
	public static String JarFileExportOperation_savingFiles;
	public static String JarFileExportOperation_savingModifiedResources;
	public static String JarFileExportOperation_noExportTypeChosen;
	public static String JarFileExportOperation_noResourcesSelected;
	public static String JarFileExportOperation_invalidJarLocation;
	public static String JarFileExportOperation_manifestDoesNotExist;
	public static String JarFileExportOperation_invalidMainClass;
	public static String JarFileExportOperation_jarFileExistsAndNotWritable;
	public static String JarFileExportOperation_cantGetRootKind;
	public static String JarFileExportOperation_fileUnsaved;
	public static String JarFileExportOperation_didNotAddManifestToJar;
	public static String JarFileExportOperation_errorSavingManifest;
	public static String JarFileExportOperation_errorSavingDescription;
	public static String JarFileExportOperation_errorReadingFile;
	public static String JarFileExportOperation_errorClosingJarPackageDescriptionReader;
	public static String JarFileExportOperation_errorDuringProjectBuild;
	public static String JarFileExportOperation_errorSavingModifiedResources;
	public static String JarOptionsPage_title;
	public static String JarOptionsPage_description;
	public static String JarOptionsPage_howTreatProblems_label;
	public static String JarOptionsPage_exportErrors_text;
	public static String JarOptionsPage_exportWarnings_text;
	public static String JarOptionsPage_useSourceFoldersHierarchy;
	public static String JarOptionsPage_saveDescription_text;
	public static String JarOptionsPage_saveAsDialog_title;
	public static String JarOptionsPage_saveAsDialog_message;
	public static String JarOptionsPage_error_descriptionMustBeAbsolute;
	public static String JarOptionsPage_error_descriptionMustNotBeExistingContainer;
	public static String JarOptionsPage_error_descriptionContainerDoesNotExist;
	public static String JarOptionsPage_error_invalidDescriptionExtension;
	public static String JarOptionsPage_descriptionFile_label;
	public static String JarOptionsPage_browseButton_text;
	public static String JarOptionsPage_buildIfNeeded;
	public static String JarPackageWizardPage_title;
	public static String JarPackageWizardPage_description;
	public static String JarPackageWizardPage_whatToExport_label;
	public static String JarPackageWizardPage_whereToExport_label;
	public static String JarPackageWizardPage_options_label;
	public static String JarPackageWizardPage_compress_text;
	public static String JarPackageWizardPage_overwrite_text;
	public static String JarPackageWizardPage_includeDirectoryEntries_text;
	public static String JarPackageWizardPage_destination_label;
	public static String JarPackageWizardPage_browseButton_text;
	public static String JarPackageWizardPage_saveAsDialog_title;
	public static String JarPackageWizardPage_saveAsDialog_message;
	public static String JarPackageWizardPage_exportClassFiles_text;
	public static String JarPackageWizardPage_exportOutputFolders_text;
	public static String JarPackageWizardPage_exportJavaFiles_text;
	public static String JarPackageWizardPage_info_relativeExportDestination;
	public static String JarPackageWizardPage_error_exportDestinationMustNotBeDirectory;
	public static String JarPackageWizardPage_error_jarFileExistsAndNotWritable;
	public static String JarPackageWizardPage_error_noExportTypeChecked;
	public static String JarPackageWizardPage_error_cantExportJARIntoItself;
	public static String JarPackageReader_error_badFormat;
	public static String JarPackageReader_jarPackageReaderWarnings;
	public static String JarPackageReader_error_illegalValueForBooleanAttribute;
	public static String JarPackageReader_error_tagNameNotFound;
	public static String JarPackageReader_error_tagPathNotFound;
	public static String JarPackageReader_error_tagHandleIdentifierNotFoundOrEmpty;
	public static String JarPackageReader_warning_javaElementDoesNotExist;
	public static String JarPackageReader_error_duplicateTag;
	public static String JarPackageReader_warning_mainClassDoesNotExist;
	public static String JarManifestWizardPage_title;
	public static String JarManifestWizardPage_description;
	public static String JarManifestWizardPage_manifestSource_label;
	public static String JarManifestWizardPage_sealingHeader_label;
	public static String JarManifestWizardPage_mainClassHeader_label;
	public static String JarManifestWizardPage_genetateManifest_text;
	public static String JarManifestWizardPage_saveManifest_text;
	public static String JarManifestWizardPage_reuseManifest_text;
	public static String JarManifestWizardPage_useManifest_text;
	public static String JarManifestWizardPage_newManifestFile_text;
	public static String JarManifestWizardPage_newManifestFileBrowseButton_text;
	public static String JarManifestWizardPage_manifestFile_text;
	public static String JarManifestWizardPage_manifestFileBrowse_text;
	public static String JarManifestWizardPage_sealJar_text;
	public static String JarManifestWizardPage_unsealPackagesButton_text;
	public static String JarManifestWizardPage_sealPackagesButton_text;
	public static String JarManifestWizardPage_sealedPackagesDetailsButton_text;
	public static String JarManifestWizardPage_mainClass_label;
	public static String JarManifestWizardPage_mainClassBrowseButton_text;
	public static String JarManifestWizardPage_saveAsDialog_title;
	public static String JarManifestWizardPage_saveAsDialog_message;
	public static String JarManifestWizardPage_manifestSelectionDialog_title;
	public static String JarManifestWizardPage_manifestSelectionDialog_message;
	public static String JarManifestWizardPage_error_onlyOneManifestMustBeSelected;
	public static String JarManifestWizardPage_error_noResourceSelected;
	public static String JarManifestWizardPage_warning_noManifestVersion;
	public static String JarManifestWizardPage_mainTypeSelectionDialog_title;
	public static String JarManifestWizardPage_mainTypeSelectionDialog_message;
	public static String JarManifestWizardPage_sealedPackagesSelectionDialog_title;
	public static String JarManifestWizardPage_sealedPackagesSelectionDialog_message;
	public static String JarManifestWizardPage_unsealedPackagesSelectionDialog_title;
	public static String JarManifestWizardPage_unsealedPackagesSelectionDialog_message;
	public static String JarManifestWizardPage_jarSealed;
	public static String JarManifestWizardPage_jarSealedExceptOne;
	public static String JarManifestWizardPage_jarSealedExceptSome;
	public static String JarManifestWizardPage_nothingSealed;
	public static String JarManifestWizardPage_onePackageSealed;
	public static String JarManifestWizardPage_somePackagesSealed;
	public static String JarManifestWizardPage_error_manifestPathMustBeAbsolute;
	public static String JarManifestWizardPage_error_manifestMustNotBeExistingContainer;
	public static String JarManifestWizardPage_error_manifestContainerDoesNotExist;
	public static String JarManifestWizardPage_error_invalidManifestFile;
	public static String JarManifestWizardPage_error_noManifestFile;
	public static String JarManifestWizardPage_error_unsealedPackagesNotInSelection;
	public static String JarManifestWizardPage_error_sealedPackagesNotInSelection;
	public static String JarManifestWizardPage_error_jarPackageWizardError_title;
	public static String JarManifestWizardPage_error_jarPackageWizardError_message;
	public static String JarManifestWizardPage_error_mustContainPackages;
	public static String JarManifestWizardPage_error_invalidMainClass;
	public static String JarWriter_error_couldNotGetXmlBuilder;
	public static String JarWriter_error_couldNotTransformToXML;
	public static String JarWriter_output_title;
	public static String JarWriter_output_exportBin;
	public static String JarWriter_output_exportOutputFolders;
	public static String JarWriter_output_exportJava;
	public static String JarWriter_output_jarFileName;
	public static String JarWriter_output_compressed;
	public static String JarWriter_output_overwrite;
	public static String JarWriter_output_saveDescription;
	public static String JarWriter_output_descriptionFile;
	public static String JarWriter_output_lineSeparator;
	public static String JarWriter_output_generateManifest;
	public static String JarWriter_output_saveManifest;
	public static String JarWriter_output_reuseManifest;
	public static String JarWriter_output_manifestName;
	public static String JarWriter_output_jarSealed;
	public static String JarWriter_output_mainClass;
	public static String ConfirmSaveModifiedResourcesDialog_title;
	public static String ConfirmSaveModifiedResourcesDialog_message;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JarPackagerMessages.class);
	}
}