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
 package org.eclipse.ajdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aspectj.asm.IProgramElement;
import org.eclipse.ajdt.core.AspectJPlugin;
import org.eclipse.ajdt.core.javaelements.AJCompilationUnit;
import org.eclipse.ajdt.core.javaelements.AJCompilationUnitManager;
import org.eclipse.ajdt.core.javaelements.AspectElement;
import org.eclipse.ajdt.internal.ui.resources.AJDTIcon;
import org.eclipse.ajdt.internal.ui.resources.AspectJImages;
import org.eclipse.ajdt.internal.utils.AJDTUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.TypeFilter;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.corext.util.TypeInfoFilter;
import org.eclipse.jdt.internal.corext.util.UnresolvableTypeInfo;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.progress.UIJob;

/**
 * A viewer to present type queried form the type history and form the
 * search engine. All viewer updating takes place in the UI thread.
 * Therefore no synchronization of the methods is necessary.
 * 
 * @since 3.1
 */
// Copied from org.eclipse.jdt.internal.ui.dialogs - changes marked // AspectJ Change
public class TypeInfoViewer {
	
	private static class SearchRequestor extends TypeNameRequestor {
		private volatile boolean fStop;
		
		private TypeInfoFilter fFilter;
		private Set fHistory;
		
		private AJTypeInfoFactory factory= new AJTypeInfoFactory();
		private List fResult;
		
		public SearchRequestor(TypeInfoFilter filter) {
			super();
			fFilter= filter;
			fResult= new ArrayList(2048);
		}
		public TypeInfo[] getResult() {
			return (TypeInfo[])fResult.toArray(new TypeInfo[fResult.size()]);
		}
		public void cancel() {
			fStop= true;
		}
		public void setHistory(Set history) {
			fHistory= history;
		}
		public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
			if (fStop)
				return;
			if (TypeFilter.isFiltered(packageName, simpleTypeName))
				return;
			TypeInfo type= factory.create(packageName, simpleTypeName, enclosingTypeNames, modifiers, path);
			if (fHistory.contains(type))
				return;
			if (fFilter.matchesSearchResult(type))
				fResult.add(type);
		}
	}
	
	protected static class TypeInfoComparator implements Comparator {
		private TypeInfoLabelProvider fLabelProvider;
		private TypeInfoFilter fFilter;
		public TypeInfoComparator(TypeInfoLabelProvider labelProvider, TypeInfoFilter filter) {
			fLabelProvider= labelProvider;
			fFilter= filter;
		}
	    public int compare(Object left, Object right) {
	     	TypeInfo leftInfo= (TypeInfo)left;
	     	TypeInfo rightInfo= (TypeInfo)right;
	     	int leftCategory= getCategory(leftInfo);
	     	int rightCategory= getCategory(rightInfo);
	     	if (leftCategory < rightCategory)
	     		return -1;
	     	if (leftCategory > rightCategory)
	     		return +1;
	     	int result= compareName(leftInfo.getTypeName(), rightInfo.getTypeName());
	     	if (result != 0)
	     		return result;
	     	result= compareTypeContainerName(leftInfo.getTypeContainerName(), rightInfo.getTypeContainerName());
	     	if (result != 0)
	     		return result;
	     	return compareContainerName(leftInfo, rightInfo);
	    }
		private int compareName(String leftString, String rightString) {
			int result= leftString.compareToIgnoreCase(rightString);
			if (result != 0) {
				return result;
			} else if (Strings.isLowerCase(leftString.charAt(0)) && 
				!Strings.isLowerCase(rightString.charAt(0))) {
	     		return +1;
			} else if (Strings.isLowerCase(rightString.charAt(0)) &&
	     		!Strings.isLowerCase(leftString.charAt(0))) {
	     		return -1;
			} else {
				return leftString.compareTo(rightString);
			}
		}
		private int compareTypeContainerName(String leftString, String rightString) {
			int leftLength= leftString.length();
			int rightLength= rightString.length();
			if (leftLength == 0 && rightLength > 0)
				return -1;
			if (leftLength == 0 && rightLength == 0)
				return 0;
			if (leftLength > 0 && rightLength == 0)
				return +1;
			return compareName(leftString, rightString);
		}
		private int compareContainerName(TypeInfo leftType, TypeInfo rightType) {
			return fLabelProvider.getContainerName(leftType).compareTo(
				fLabelProvider.getContainerName(rightType));
		}
		private int getCategory(TypeInfo type) {
			if (fFilter == null)
				return 0;
			if (!fFilter.isCamcelCasePattern())
				return 0;
			return fFilter.matchesNameExact(type) ? 0 : 1;
		}
	}
	
	protected static class TypeInfoLabelProvider extends LabelProvider {
		
		private Map fLib2Name= new HashMap();
		private String[] fInstallLocations;
		private String[] fVMNames;
		
		// AspectJ Change
		private static Image ASPECT_ICON = ((AJDTIcon)AspectJImages.instance().getIcon(IProgramElement.Kind.ASPECT)).getImageDescriptor().createImage();

		private boolean fFullyQualifyDuplicates;
		
		public TypeInfoLabelProvider() {
			List locations= new ArrayList();
			List labels= new ArrayList();
			IVMInstallType[] installs= JavaRuntime.getVMInstallTypes();
			for (int i= 0; i < installs.length; i++) {
				processVMInstallType(installs[i], locations, labels);
			}
			fInstallLocations= (String[])locations.toArray(new String[locations.size()]);
			fVMNames= (String[])labels.toArray(new String[labels.size()]);
			
		}
		public void setFullyQualifyDuplicates(boolean value) {
			fFullyQualifyDuplicates= value;
		}
		private void processVMInstallType(IVMInstallType installType, List locations, List labels) {
			if (installType != null) {
				IVMInstall[] installs= installType.getVMInstalls();
				for (int i= 0; i < installs.length; i++) {
					String label= getFormattedLabel(installs[i].getName());
					LibraryLocation[] libLocations= installs[i].getLibraryLocations();
					if (libLocations != null) {
						processLibraryLocation(libLocations, label);
					} else {
						String filePath= installs[i].getInstallLocation().getAbsolutePath();
						locations.add(Path.fromOSString(filePath).toString());
						labels.add(label);
					}
				}
			}
		}
		private void processLibraryLocation(LibraryLocation[] libLocations, String label) {
			for (int l= 0; l < libLocations.length; l++) {
				LibraryLocation location= libLocations[l];
				fLib2Name.put(location.getSystemLibraryPath().toString(), label);
			}
		}
		private String getFormattedLabel(String name) {
			return Messages.format(JavaUIMessages.TypeInfoViewer_library_name_format, name);
		}
		public String getText(Object element) {
			return ((TypeInfo)element).getTypeName();
		}
		public String getQualifiedText(TypeInfo type) {
			StringBuffer result= new StringBuffer();
			result.append(type.getTypeName());
			String containerName= type.getTypeContainerName();
			result.append(JavaElementLabels.CONCAT_STRING);
			if (containerName.length() > 0) {
				result.append(containerName);
			} else {
				result.append(JavaUIMessages.TypeInfoViewer_default_package);
			}
			return result.toString();
		}
		public String getFullyQualifiedText(TypeInfo type) {
			StringBuffer result= new StringBuffer();
			result.append(type.getTypeName());
			String containerName= type.getTypeContainerName();
			if (containerName.length() > 0) {
				result.append(JavaElementLabels.CONCAT_STRING);
				result.append(containerName);
			}
			result.append(JavaElementLabels.CONCAT_STRING);
			result.append(getContainerName(type));
			return result.toString();
		}
		public String getText(TypeInfo last, TypeInfo current, TypeInfo next) {
			StringBuffer result= new StringBuffer();
			int qualifications= 0;
			String currentTN= current.getTypeName();
			result.append(currentTN);
			String currentTCN= getTypeContainerName(current);
			if (last != null) {
				String lastTN= last.getTypeName();
				String lastTCN= getTypeContainerName(last);
				if (currentTCN.equals(lastTCN)) {
					if (currentTN.equals(lastTN)) {
						result.append(JavaElementLabels.CONCAT_STRING);
						result.append(currentTCN);
						result.append(JavaElementLabels.CONCAT_STRING);
						result.append(getContainerName(current));
						return result.toString();
					}
				} else if (currentTN.equals(lastTN)) {
					qualifications= 1;
				}
			}
			if (next != null) {
				String nextTN= next.getTypeName();
				String nextTCN= getTypeContainerName(next);
				if (currentTCN.equals(nextTCN)) {
					if (currentTN.equals(nextTN)) {
						result.append(JavaElementLabels.CONCAT_STRING);
						result.append(currentTCN);
						result.append(JavaElementLabels.CONCAT_STRING);
						result.append(getContainerName(current));
						return result.toString();
					}
				} else if (currentTN.equals(nextTN)) {
					qualifications= 1;
				}
			}
			if (qualifications > 0) {
				result.append(JavaElementLabels.CONCAT_STRING);
				result.append(currentTCN);
				if (fFullyQualifyDuplicates) {
					result.append(JavaElementLabels.CONCAT_STRING);
					result.append(getContainerName(current));
				}
			}
			return result.toString();
		}
		public String getQualificationText(TypeInfo type) {
			StringBuffer result= new StringBuffer();
			String containerName= type.getTypeContainerName();
			if (containerName.length() > 0) {
				result.append(containerName);
				result.append(JavaElementLabels.CONCAT_STRING);
			}
			result.append(getContainerName(type));
			return result.toString();
		}
		public Image getImage(Object element) {
			TypeInfo type= (TypeInfo)element;
			// AspectJ Change Begin
			if(type instanceof AJCUTypeInfo) {
				if (((AJCUTypeInfo)type).isAspect()) {
					return ASPECT_ICON;					
				} 
			}
			// AspectJ Change End			
			int modifiers= type.getModifiers();
			ImageDescriptor descriptor= JavaElementImageProvider.
				getTypeImageDescriptor(type.isInnerType(), false, modifiers, false);
			return JavaPlugin.getImageDescriptorRegistry().get(descriptor);
		}
		
		private String getTypeContainerName(TypeInfo info) {
			String result= info.getTypeContainerName();
			if (result.length() > 0)
				return result;
			return JavaUIMessages.TypeInfoViewer_default_package;
		}
		
		private String getContainerName(TypeInfo type) {
			String name= type.getPackageFragmentRootName();
			for (int i= 0; i < fInstallLocations.length; i++) {
				if (name.startsWith(fInstallLocations[i])) {
					return fVMNames[i];
				}
			}
			String lib= (String)fLib2Name.get(name);
			if (lib != null)
				return lib;
			return name;
		}
	}

	private static class ProgressUpdateJob extends UIJob {
		private TypeInfoViewer fViewer;
		private boolean fStopped;
		public ProgressUpdateJob(Display display, TypeInfoViewer viewer) {
			super(display, JavaUIMessages.TypeInfoViewer_progressJob_label);
			fViewer= viewer;
		}
		public void stop() {
			fStopped= true;
			cancel();
		}
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (stopped()) 
				return new Status(IStatus.CANCEL, JavaPlugin.getPluginId(), IStatus.CANCEL, "", null); //$NON-NLS-1$
			fViewer.updateProgressMessage();
			if (!stopped())
				schedule(300);
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		}
		private boolean stopped() {
			return fStopped || fViewer.getTable().isDisposed();
		}
	}
	
	private static class ProgressMonitor extends ProgressMonitorWrapper {
		private TypeInfoViewer fViewer;
		private String fName;
		private int fTotalWork;
		private double fWorked;
		private boolean fDone;
		
		public ProgressMonitor(IProgressMonitor monitor, TypeInfoViewer viewer) {
			super(monitor);
			fViewer= viewer;
		}
		public void setTaskName(String name) {
			super.setTaskName(name);
			fName= name;
		}
		public void beginTask(String name, int totalWork) {
			super.beginTask(name, totalWork);
			if (fName == null)
				fName= name;
			fTotalWork= totalWork;
		}
		public void worked(int work) {
			super.worked(work);
			internalWorked(work);
		}
		public void done() {
			fDone= true;
			fViewer.setProgressMessage(""); //$NON-NLS-1$
			super.done();
		}
		public void internalWorked(double work) {
			fWorked= fWorked + work;
			fViewer.setProgressMessage(getMessage());
		}
		private String getMessage() {
			if (fDone) {
				return ""; //$NON-NLS-1$
			} else if (fTotalWork == 0) {
				return fName;
			} else {
				return Messages.format(
					JavaUIMessages.TypeInfoViewer_progress_label,
					new Object[] { fName, new Integer((int)((fWorked * 100) / fTotalWork)) });
			}
		}
	}

	private static abstract class AbstractJob extends Job {
		protected TypeInfoViewer fViewer;
		protected AbstractJob(String name, TypeInfoViewer viewer) {
			super(name);
			fViewer= viewer;
			setSystem(true);
		}
		protected final IStatus run(IProgressMonitor parent) {
			ProgressMonitor monitor= new ProgressMonitor(parent, fViewer);
			try {
				fViewer.scheduleProgressUpdateJob();
				return doRun(monitor);
			} finally {
				fViewer.stopProgressUpdateJob();
			}
		}
		protected abstract IStatus doRun(ProgressMonitor monitor);
	}
	
	private static abstract class AbstractSearchJob extends AbstractJob {
		private int fMode;
		
		protected int fTicket;
		protected TypeInfoLabelProvider fLabelProvider;
		
		protected TypeInfoFilter fFilter;
		// AspectJ Change
		protected AJTypeInfoHistory fHistory;
		
		// AspectJ Change
		protected AbstractSearchJob(int ticket, TypeInfoViewer viewer, TypeInfoFilter filter, AJTypeInfoHistory history, int numberOfVisibleItems, int mode) {
			super(JavaUIMessages.TypeInfoViewer_job_label, viewer);
			fMode= mode;
			fTicket= ticket;
			fViewer= viewer;
			fLabelProvider= fViewer.getLabelProvider();
			fFilter= filter;
			fHistory= history;
		}
		public void stop() {
			cancel();
		}
		protected IStatus doRun(ProgressMonitor monitor) {
			try {
				if (VIRTUAL) { 
					internalRunVirtual(monitor);
				} else {
					internalRun(monitor);
				}
			} catch (CoreException e) {
				fViewer.searchJobFailed(fTicket, e);
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, JavaUIMessages.TypeInfoViewer_job_error, e);
			} catch (InterruptedException e) {
				return canceled(e, true);
			} catch (OperationCanceledException e) {
				return canceled(e, false);
			}
			fViewer.searchJobDone(fTicket);
			return ok();	
		}
		protected abstract TypeInfo[] getSearchResult(Set filteredHistory, ProgressMonitor monitor) throws CoreException;
		
		private void internalRun(ProgressMonitor monitor) throws CoreException, InterruptedException {
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			
			fViewer.clear(fTicket);

			// local vars to speed up rendering
			TypeInfo last= null;
			TypeInfo type= null;
			TypeInfo next= null;
			List elements= new ArrayList();
			List images= new ArrayList();
			List labels= new ArrayList();
			
			Set filteredHistory= new HashSet();
			TypeInfo[] matchingTypes= fHistory.getFilteredTypeInfos(fFilter);
			if (matchingTypes.length > 0) {
				Arrays.sort(matchingTypes, new TypeInfoComparator(fLabelProvider, fFilter));
				type= matchingTypes[0];
				int i= 1;
				while(type != null) {
					next= (i == matchingTypes.length) ? null : matchingTypes[i];
					filteredHistory.add(type);
					elements.add(type);
					images.add(fLabelProvider.getImage(type));
					labels.add(fLabelProvider.getText(last, type, next));
					last= type;
					type= next;
					i++;
				}
			}
			matchingTypes= null;
			fViewer.fExpectedItemCount= elements.size();
			fViewer.addHistory(fTicket, elements, images, labels);
			
			if ((fMode & INDEX) == 0) {
				return;
			}
			TypeInfo[] result= getSearchResult(filteredHistory, monitor);
			fViewer.fExpectedItemCount+= result.length;
			if (result.length == 0) {
				return;
			}
			if (monitor.isCanceled())
				throw new OperationCanceledException();			
			int processed= 0;
			int nextIndex= 1;
			type= result[0];
			if (filteredHistory.size() > 0) {
				fViewer.addDashLineAndUpdateLastHistoryEntry(fTicket, type);
			}
			while (true) {
				long startTime= System.currentTimeMillis();
				elements.clear();
				images.clear();
				labels.clear();
	            int delta = Math.min(nextIndex == 1 ? fViewer.getNumberOfVisibleItems() : 10, result.length - processed);
				if (delta == 0)
					break;
				processed= processed + delta;
				while(delta > 0) {
					next= (nextIndex == result.length) ? null : result[nextIndex];
					elements.add(type);
					labels.add(fLabelProvider.getText(last, type, next));
					images.add(fLabelProvider.getImage(type));
					last= type;
					type= next;
					nextIndex++;
					delta--;
				}
				fViewer.addAll(fTicket, elements, images, labels);
				long sleep= 100 - (System.currentTimeMillis() - startTime);
				if (false)
					System.out.println("Sleeping for: " + sleep); //$NON-NLS-1$
				
				if (sleep > 0)
					Thread.sleep(sleep);
				
				if (monitor.isCanceled())
					throw new OperationCanceledException();
			}
		}
		private void internalRunVirtual(ProgressMonitor monitor) throws CoreException, InterruptedException {
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			
			fViewer.clear(fTicket);

			TypeInfo[] matchingTypes= fHistory.getFilteredTypeInfos(fFilter);
			fViewer.setHistoryResult(fTicket, matchingTypes);
			if ((fMode & INDEX) == 0)
				return;
				
			TypeInfo[] result= getSearchResult(new HashSet(Arrays.asList(matchingTypes)), monitor);
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			
			fViewer.setSearchResult(fTicket, result);
		}
		private IStatus canceled(Exception e, boolean removePendingItems) {
			fViewer.searchJobCanceled(fTicket, removePendingItems);
			return new Status(IStatus.CANCEL, JavaPlugin.getPluginId(), IStatus.CANCEL, JavaUIMessages.TypeInfoViewer_job_cancel, e);
		}
		private IStatus ok() {
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		}
	}
	
	private static class SearchEngineJob extends AbstractSearchJob {
		private IJavaSearchScope fScope;
		private int fElementKind;
		private SearchRequestor fReqestor;
		
		// AspectJ Change
		public SearchEngineJob(int ticket, TypeInfoViewer viewer, TypeInfoFilter filter, AJTypeInfoHistory history, int numberOfVisibleItems, int mode, 
				IJavaSearchScope scope, int elementKind) {
			super(ticket, viewer, filter, history, numberOfVisibleItems, mode);
			fScope= scope;
			fElementKind= elementKind;
			fReqestor= new SearchRequestor(filter);
		}
		public void stop() {
			fReqestor.cancel();
			super.stop();
		}
		protected TypeInfo[] getSearchResult(Set filteredHistory, ProgressMonitor monitor) throws CoreException {
			long start= System.currentTimeMillis();
			fReqestor.setHistory(filteredHistory);
			// consider primary working copies during searching
			SearchEngine engine= new SearchEngine((WorkingCopyOwner)null);
			String packPattern= fFilter.getPackagePattern();
			monitor.setTaskName(JavaUIMessages.TypeInfoViewer_searchJob_taskName);
			engine.searchAllTypeNames(
				packPattern == null ? null : packPattern.toCharArray(), 
				fFilter.getNamePattern().toCharArray(), 
				fFilter.getSearchFlags(), 
				fElementKind, 
				fScope, 
				fReqestor, 
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
				monitor);
			if (DEBUG)
				System.out.println("Time needed until search has finished: " + (System.currentTimeMillis() - start)); //$NON-NLS-1$
			TypeInfo[] result= fReqestor.getResult();
			// AspectJ Change Begin	
			List types = getAspectJTypes(fScope); 
			TypeInfo[] typesIncludingAspects = new TypeInfo[result.length + types.size()];			
			System.arraycopy(result, 0, typesIncludingAspects, 0, result.length);
			int index = result.length;
			for (Iterator iter = types.iterator(); iter.hasNext();) {
				TypeInfo info = (TypeInfo) iter.next();
				typesIncludingAspects[index] = info;
				index ++;
			}
			result = typesIncludingAspects;
			// AspectJ Change End
			Arrays.sort(result, new TypeInfoComparator(fLabelProvider, fFilter));
			if (DEBUG)
				System.out.println("Time needed until sort has finished: " + (System.currentTimeMillis() - start)); //$NON-NLS-1$
			fViewer.rememberResult(fTicket, result);
			
			return result;
		}
	}

	// AspectJ Change Begin
	/**
	 * @param scope
	 * @return
	 */
	private static List getAspectJTypes(IJavaSearchScope scope) {
		List ajTypes = new ArrayList();
		IProject[] projects = AspectJPlugin.getWorkspace().getRoot()
				.getProjects();
		for (int i = 0; i < projects.length; i++) {
			try {
				if(projects[i].hasNature("org.eclipse.ajdt.ui.ajnature")) { //$NON-NLS-1$ 		
					IJavaProject jp = JavaCore.create(projects[i]);
					if (jp != null) {
						IPath[] paths = scope.enclosingProjectsAndJars();
						for (int a = 0; a < paths.length; a++) {	
							if (paths[a].equals(jp.getPath())) { 
								List ajCus = AJCompilationUnitManager.INSTANCE.getAJCompilationUnits(jp);
								for (Iterator iter = ajCus.iterator(); iter
										.hasNext();) {
									AJCompilationUnit unit = (AJCompilationUnit) iter.next();
									IType[] types = unit.getAllTypes();
									for (int j = 0; j < types.length; j++) {
										IType type = types[j];
										if(type instanceof AspectElement) {
											// Part of 103131 - only add aspects to avoid duplicates
											char[][] enclosingTypes = AJDTUtils.getEnclosingTypes(type);
											int kind = type.getFlags(); // 103131 - pass in correct flags
											AJCUTypeInfo info = new AJCUTypeInfo(
													type.getPackageFragment().getElementName(),
													type.getElementName(),
													enclosingTypes,
													kind,
													type instanceof AspectElement,
													jp.getElementName(),
													unit.getPackageFragmentRoot().getElementName(),
													unit.getElementName().substring(0, unit.getElementName().lastIndexOf('.')),
													"aj", //$NON-NLS-1$
													unit);							
											ajTypes.add(info); 			
										}
									}
								}
							} 
						}
					}
				}	
			} catch (JavaModelException e) {
			} catch (CoreException e) {					
			}
		}
		return ajTypes;
	}
//	 AspectJ Change End

	private static class CachedResultJob extends AbstractSearchJob {
		private TypeInfo[] fLastResult;
		// AspectJ Change
		public CachedResultJob(int ticket, TypeInfo[] lastResult, TypeInfoViewer viewer, TypeInfoFilter filter, AJTypeInfoHistory history, int numberOfVisibleItems, int mode) {
			super(ticket, viewer, filter, history, numberOfVisibleItems, mode);
			fLastResult= lastResult;
		}
		protected TypeInfo[] getSearchResult(Set filteredHistory, ProgressMonitor monitor) throws CoreException {
			List result= new ArrayList(2048);
			for (int i= 0; i < fLastResult.length; i++) {
				TypeInfo type= fLastResult[i];
				if (filteredHistory.contains(type))
					continue;
				if (fFilter.matchesCachedResult(type))
					result.add(type);
			}
			// we have to sort if the filter is a camel case filter.
			TypeInfo[] types= (TypeInfo[])result.toArray(new TypeInfo[result.size()]);
			if (fFilter.isCamcelCasePattern()) {
				Arrays.sort(types, new TypeInfoComparator(fLabelProvider, fFilter));
			}
			return types;
		}
	}
	
	private static class SyncJob extends AbstractJob {
		public SyncJob(TypeInfoViewer viewer) {
			super(JavaUIMessages.TypeInfoViewer_syncJob_label, viewer);
		}
		public void stop() {
			cancel();
		}
		protected IStatus doRun(ProgressMonitor monitor) {
			try {
				monitor.setTaskName(JavaUIMessages.TypeInfoViewer_syncJob_taskName);
				new SearchEngine().searchAllTypeNames(
					null, 
					// make sure we search a concrete name. This is faster according to Kent  
					"_______________".toCharArray(), //$NON-NLS-1$
					SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE, 
					IJavaSearchConstants.ENUM,
					SearchEngine.createWorkspaceScope(), 
					new TypeNameRequestor() {}, 
					IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
					monitor);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, JavaUIMessages.TypeInfoViewer_job_error, e);
			} catch (OperationCanceledException e) {
				return new Status(IStatus.CANCEL, JavaPlugin.getPluginId(), IStatus.CANCEL, JavaUIMessages.TypeInfoViewer_job_cancel, e);
			} finally {
				fViewer.syncJobDone();
			}
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
		}
	}
	
	private static class DashLine {
		private int fSeparatorWidth;
		private String fMessage;
		private int fMessageLength;
		public String getText(int width) {
			StringBuffer dashes= new StringBuffer();
			int chars= (((width - fMessageLength) / fSeparatorWidth) / 2) -2;
			for (int i= 0; i < chars; i++) {
				dashes.append(SEPARATOR);
			}
			StringBuffer result= new StringBuffer();
			result.append(dashes);
			result.append(fMessage);
			result.append(dashes);
			return result.toString();
		}
		public void initialize(GC gc) {
			fSeparatorWidth= gc.getAdvanceWidth(SEPARATOR);
			fMessage= " " + JavaUIMessages.TypeInfoViewer_separator_message + " ";  //$NON-NLS-1$ //$NON-NLS-2$
			fMessageLength= gc.textExtent(fMessage).x;
		}
	}
	
	private Display fDisplay;
	
	private String fProgressMessage;
	private Label fProgressLabel;
	private int fProgressCounter;
	private ProgressUpdateJob fProgressUpdateJob;
	
	// AspectJ Change
	private AJTypeInfoHistory fHistory;

	/* non virtual table */
	private int fNextElement;
	private List fItems;
	
	/* virtual table */
	private TypeInfo[] fHistoryMatches;
	private TypeInfo[] fSearchMatches;
	
	private int fNumberOfVisibleItems;
	private int fExpectedItemCount;
	private Color fDashLineColor; 
	private int fScrollbarWidth;
	private int fTableWidthDelta;
	private int fDashLineIndex= -1;
	private Image fSeparatorIcon;
	private DashLine fDashLine= new DashLine();
	
	private boolean fFullyQualifySelection;
	/* remembers the last selection to restore unqualified labels */
	private TableItem[] fLastSelection;
	private String[] fLastLabels;
	
	private TypeInfoLabelProvider fLabelProvider;
	private Table fTable;
	
	private SyncJob fSyncJob;
	
	private TypeInfoFilter fTypeInfoFilter;
	private TypeInfo[] fLastCompletedResult;
	private TypeInfoFilter fLastCompletedFilter;
	
	private int fSearchJobTicket;
	protected int fElementKind;
	protected IJavaSearchScope fSearchScope;
	
	private AbstractSearchJob fSearchJob;

	private static final int HISTORY= 1;
	private static final int INDEX= 2;
	private static final int FULL= HISTORY | INDEX;
	
	private static final char SEPARATOR= '-'; 
	
	private static final boolean DEBUG= true;	
	private static final boolean VIRTUAL= false;
	
	private static final TypeInfo[] EMTPY_TYPE_INFO_ARRAY= new TypeInfo[0];
	// only needed when in virtual table mode
	private static final TypeInfo DASH_LINE= new UnresolvableTypeInfo(null, null, null, 0, null);
	
	public TypeInfoViewer(Composite parent, int flags, Label progressLabel, IJavaSearchScope scope, int elementKind, String initialFilter) {
		Assert.isNotNull(scope);
		fDisplay= parent.getDisplay();
		fProgressLabel= progressLabel;
		fSearchScope= scope;
		fElementKind= elementKind;
		fFullyQualifySelection= (flags & SWT.MULTI) != 0;
		fTable= new Table(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.FLAT | flags | (VIRTUAL ? SWT.VIRTUAL : SWT.NONE));
		fTable.setFont(parent.getFont());
		fLabelProvider= createLabelProvider();
		fItems= new ArrayList(500);
		fTable.setHeaderVisible(false);
		addPopupMenu();
		fTable.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent event) {
				int itemHeight= fTable.getItemHeight();
				Rectangle clientArea= fTable.getClientArea();
				fNumberOfVisibleItems= (clientArea.height / itemHeight) + 1;
			}
		});
		fTable.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.DEL) {
					deleteHistoryEntry();
				} else if (e.keyCode == SWT.ARROW_DOWN) {
					int index= fTable.getSelectionIndex();
					if (index == fDashLineIndex - 1) {
						e.doit= false;
						setTableSelection(index + 2);
					}
				} else if (e.keyCode == SWT.ARROW_UP) {
					int index= fTable.getSelectionIndex();
					if (fDashLineIndex != -1 && index == fDashLineIndex + 1) {
						e.doit= false;
						setTableSelection(index - 2);
					}
				}
			}
		});
		fTable.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fLastSelection != null) {
					for (int i= 0; i < fLastSelection.length; i++) {
						TableItem item= fLastSelection[i];
						// could be disposed by deleting element from 
						// type inof history
						if (!item.isDisposed())
							item.setText(fLastLabels[i]);
					}
				}
				TableItem[] items= fTable.getSelection();
				fLastSelection= new TableItem[items.length];
				fLastLabels= new String[items.length];
				for (int i= 0; i < items.length; i++) {
					TableItem item= items[i];
					fLastSelection[i]= item;
					fLastLabels[i]= item.getText();
					Object data= item.getData();
					if (data instanceof TypeInfo) {
						String qualifiedText= getQualifiedText((TypeInfo)data);
						if (qualifiedText.length() > fLastLabels[i].length())
							item.setText(qualifiedText);
					}
				}
			}
		});
		fTable.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				stop(true, true);
				fDashLineColor.dispose();
				fSeparatorIcon.dispose();
				if (fProgressUpdateJob != null) {
					fProgressUpdateJob.stop();
					fProgressUpdateJob= null;
				}
			}
		});
		if (VIRTUAL) {
			fHistoryMatches= EMTPY_TYPE_INFO_ARRAY;
			fSearchMatches= EMTPY_TYPE_INFO_ARRAY;
			fTable.addListener(SWT.SetData, new Listener() {
				public void handleEvent(Event event) {
					TableItem item= (TableItem)event.item;
					setData(item);
				}
			});
		}
		
		fDashLineColor= computeDashLineColor();
		fScrollbarWidth= computeScrollBarWidth();
		fTableWidthDelta= fTable.computeTrim(0, 0, 0, 0).width - fScrollbarWidth;
		fSeparatorIcon= JavaPluginImages.DESC_OBJS_TYPE_SEPARATOR.createImage(fTable.getDisplay());
		// Access the image descriptor registry from the UI thread to make
		// sure that first initialization takes place in UI thread. Otherwise
		// it could happen in search job which will result in an invalid thread access.
		JavaPlugin.getImageDescriptorRegistry();
		
		// AspectJ Change
		fHistory= AJTypeInfoHistory.getInstance();
		if (initialFilter != null && initialFilter.length() > 0)
			fTypeInfoFilter= createTypeInfoFilter(initialFilter);
		GC gc= null;
		try {
			gc= new GC(fTable);
			gc.setFont(fTable.getFont());
			fDashLine.initialize(gc);
		} finally {
			gc.dispose();
		}
		scheduleSyncJob();
	}

	public Table getTable() {
		return fTable;
	}
	
	/* package */ TypeInfoLabelProvider getLabelProvider() {
		return fLabelProvider;
	}
	
	private int getNumberOfVisibleItems() {
		return fNumberOfVisibleItems;
	}
	
	public void setFocus() {
		fTable.setFocus();
	}
	
	
	public void setQualificationStyle(boolean value) {
		if (fFullyQualifySelection == value)
			return;
		fFullyQualifySelection= value;
		if (fLastSelection != null) {
			for (int i= 0; i < fLastSelection.length; i++) {
				TableItem item= fLastSelection[i];
				Object data= item.getData();
				if (data instanceof TypeInfo) {
					item.setText(getQualifiedText((TypeInfo)data));
				}
			}
		}
	}
	
	public TypeInfo[] getSelection() {
		TableItem[] items= fTable.getSelection();
		List result= new ArrayList(items.length);
		for (int i= 0; i < items.length; i++) {
			Object data= items[i].getData();
			if (data instanceof TypeInfo) {
				result.add(data);
			}
		}
		return (TypeInfo[])result.toArray(new TypeInfo[result.size()]);
	}
	
	public void stop() {
		stop(true, false);
	}
	
	public void stop(boolean stopSyncJob, boolean dispose) {
		if (fSyncJob != null && stopSyncJob) {
			fSyncJob.stop();
			fSyncJob= null;
		}
		if (fSearchJob != null) {
			fSearchJob.stop();
			fSearchJob= null;
		}
	}
	
	public void setSearchPattern(String text) {
		stop(false, false);
		if (text.length() == 0 || "*".equals(text)) { //$NON-NLS-1$
			fTypeInfoFilter= null;
			reset();
		} else {
			fTypeInfoFilter= createTypeInfoFilter(text);
			scheduleSearchJob(isSyncJobRunning() ? HISTORY : FULL);
		}
	}
	
	public void setSearchScope(IJavaSearchScope scope, boolean refresh) {
		fSearchScope= scope;
		if (!refresh)
			return;
		stop(false, false);
		fLastCompletedFilter= null;
		fLastCompletedResult= null;
		if (fTypeInfoFilter == null) {
			reset();
		} else {
			scheduleSearchJob(isSyncJobRunning() ? HISTORY : FULL);
		}
	}

	public void setFullyQualifyDuplicates(boolean value, boolean refresh) {
		fLabelProvider.setFullyQualifyDuplicates(value);
		if (!refresh)
			return;
		stop(false, false);
		if (fTypeInfoFilter == null) {
			reset();
		} else {
			scheduleSearchJob(isSyncJobRunning() ? HISTORY : FULL);
		}
	}
	
	public void reset() {
		fLastSelection= null;
		fLastLabels= null;
		fExpectedItemCount= 0;
		fDashLineIndex= -1;
		TypeInfoFilter filter= (fTypeInfoFilter != null)
			? fTypeInfoFilter
			: new TypeInfoFilter("*", fSearchScope, fElementKind); //$NON-NLS-1$
		if (VIRTUAL) {
			fHistoryMatches= fHistory.getFilteredTypeInfos(filter);
			fExpectedItemCount= fHistoryMatches.length;
			fTable.setItemCount(fHistoryMatches.length);
			// bug under windows.
			if (fHistoryMatches.length == 0) {
				fTable.redraw();
			}
			fTable.clear(0, fHistoryMatches.length - 1);
		} else {
			fNextElement= 0;
			TypeInfo[] historyItems= fHistory.getFilteredTypeInfos(filter); 
			if (historyItems.length == 0) {
				shortenTable();
				return;
			}
			fExpectedItemCount= historyItems.length;
			int lastIndex= historyItems.length - 1;
			TypeInfo last= null;
			TypeInfo type= historyItems[0];
			for (int i= 0; i < historyItems.length; i++) {
				TypeInfo next= i == lastIndex ? null : historyItems[i + 1];
				addSingleElement(type,
					fLabelProvider.getImage(type),
					fLabelProvider.getText(last, type, next));
				last= type;
				type= next;
			}
			shortenTable();
		}
	}
	
	protected TypeInfoFilter createTypeInfoFilter(String text) {
		if ("**".equals(text)) //$NON-NLS-1$
			text= "*"; //$NON-NLS-1$
		return new TypeInfoFilter(text, fSearchScope, fElementKind);
	}
	
	protected TypeInfoLabelProvider createLabelProvider() {
		return new TypeInfoLabelProvider();
	}

	private void addPopupMenu() {
		Menu menu= new Menu(fTable.getShell(), SWT.POP_UP);
		fTable.setMenu(menu);
		final MenuItem remove= new MenuItem(menu, SWT.NONE);
		remove.setText(JavaUIMessages.TypeInfoViewer_remove_from_history);
		menu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				TableItem[] selection= fTable.getSelection();
				remove.setEnabled(canEnable(selection));
			}
		});
		remove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				deleteHistoryEntry();
			}
		});
	}
	
	private boolean canEnable(TableItem[] selection) {
		if (selection.length == 0)
			return false;
		for (int i= 0; i < selection.length; i++) {
			TableItem item= selection[i];
			Object data= item.getData();
			if (!(data instanceof TypeInfo))
				return false;
			if (!(fHistory.contains((TypeInfo)data)))
				return false; 
		}
		return true;
	}
	//---- History management -------------------------------------------------------
	
	private void deleteHistoryEntry() {
		int index= fTable.getSelectionIndex();
		if (index == -1)
			return;
		TableItem item= fTable.getItem(index);
		Object element= item.getData();
		if (!(element instanceof TypeInfo))
			return;
		if (fHistory.remove((TypeInfo)element) != null) {
			item.dispose();
			fItems.remove(index);
			int count= fTable.getItemCount();
			if (count > 0) {
				item= fTable.getItem(0);
				if (item.getData() instanceof DashLine) {
					item.dispose();
					fItems.remove(0);
					fDashLineIndex= -1;
					if (count > 1) {
						setTableSelection(0);
					}
				} else {
					if (index >= count) {
						index= count - 1;
					}
					setTableSelection(index);
				}
			} else {
				// send dummy selection
				fTable.notifyListeners(SWT.Selection, new Event());				
			}
		}
	}
	
	//-- Search result updating ----------------------------------------------------
	
	private void clear(int ticket) {
		syncExec(ticket, new Runnable() {
			public void run() {
				fNextElement= 0;
				fDashLineIndex= -1;
				fLastSelection= null;
				fLastLabels= null;
				fExpectedItemCount= 0;
			}
		});
	}
	
	private void rememberResult(int ticket, final TypeInfo[] result) {
		syncExec(ticket, new Runnable() {
			public void run() {
				if (fLastCompletedResult == null) {
					fLastCompletedFilter= fTypeInfoFilter;
					fLastCompletedResult= result;
				}
			}
		});
	}

	private void addHistory(int ticket, final List elements, final List images, final List labels) {
		addAll(ticket, elements, images, labels);
	}
	
	private void addAll(int ticket, final List elements, final List images, final List labels) {
		syncExec(ticket, new Runnable() {
			public void run() {
				int size= elements.size();
				for(int i= 0; i < size; i++) {
					addSingleElement(elements.get(i), (Image)images.get(i), (String)labels.get(i));
				}
			}
		});
	}
	
	private void addDashLineAndUpdateLastHistoryEntry(int ticket, final TypeInfo next) {
		syncExec(ticket, new Runnable() {
			public void run() {
				if (fNextElement > 0) {
					TableItem item= fTable.getItem(fNextElement - 1);
					String label= item.getText();
					String newLabel= fLabelProvider.getText(null, (TypeInfo)item.getData(), next);
					if (newLabel.length() > label.length())
						item.setText(newLabel);
					if (fLastSelection != null && fLastSelection.length > 0) {
						TableItem last= fLastSelection[fLastSelection.length - 1];
						if (last == item) {
							fLastLabels[fLastLabels.length - 1]= newLabel;
						}
					}
				}
				fDashLineIndex= fNextElement;
				addDashLine();
			}
		});
	}
	
	private void addDashLine() {
		TableItem item= null;
		if (fItems.size() > fNextElement) {
			item= (TableItem)fItems.get(fNextElement);
		} else {
			item= new TableItem(fTable, SWT.NONE);
			fItems.add(item);
		}
		fillDashLine(item);
		fNextElement++;
	}
	
	private void addSingleElement(Object element, Image image, String label) {
		TableItem item= null;
		Object old= null;
		if (fItems.size() > fNextElement) {
			item= (TableItem)fItems.get(fNextElement);
			old= item.getData();
			item.setForeground(null);
		} else {
			item= new TableItem(fTable, SWT.NONE);
			fItems.add(item);
		}
		item.setData(element);
		item.setImage(image);
		if (fNextElement == 0) {
			if (needsSelectionChange(old, element) || fLastSelection != null) {
				item.setText(label);
				fTable.setSelection(0);
	            fTable.notifyListeners(SWT.Selection, new Event());
			} else {
				fLastSelection= new TableItem[] { item };
				fLastLabels= new String[] { label };
			}
		} else {
			item.setText(label);
		}
		fNextElement++;
	}
	
	private boolean needsSelectionChange(Object oldElement, Object newElement) {
		int[] selected= fTable.getSelectionIndices();
		if (selected.length != 1)
			return true;
		if (selected[0] != 0)
			return true;
		if (oldElement == null)
			return true;
		return !oldElement.equals(newElement);
	}
	
	private void scheduleSearchJob(int mode) {
		fSearchJobTicket++;
		if (fLastCompletedFilter != null && fTypeInfoFilter.isSubFilter(fLastCompletedFilter.getText())) {
			fSearchJob= new CachedResultJob(fSearchJobTicket, fLastCompletedResult, this, fTypeInfoFilter, 
				fHistory, fNumberOfVisibleItems, 
				mode);
		} else {
			fLastCompletedFilter= null;
			fLastCompletedResult= null;
			fSearchJob= new SearchEngineJob(fSearchJobTicket, this, fTypeInfoFilter, 
				fHistory, fNumberOfVisibleItems, 
				mode, fSearchScope, fElementKind);
		}
		fSearchJob.schedule();
	}
	
	private void searchJobDone(int ticket) {
		syncExec(ticket, new Runnable() {
			public void run() {
				shortenTable();
				checkEmptyList();
				fSearchJob= null;
			}
		});
	}
	
	private void searchJobCanceled(int ticket, final boolean removePendingItems) {
		syncExec(ticket, new Runnable() {
			public void run() {
				if (removePendingItems) {
					shortenTable();
					checkEmptyList();
				}
				fSearchJob= null;
			}
		});
	}
	
	private synchronized void searchJobFailed(int ticket, CoreException e) {
		searchJobDone(ticket);
		JavaPlugin.log(e);
	}
	
	//-- virtual table support -------------------------------------------------------
	
	private void setHistoryResult(int ticket, final TypeInfo[] types) {
		syncExec(ticket, new Runnable() {
			public void run() {
				fExpectedItemCount= types.length;
				int lastHistoryLength= fHistoryMatches.length;
				fHistoryMatches= types;
				int length= fHistoryMatches.length + fSearchMatches.length; 
				int dash= (fHistoryMatches.length > 0 && fSearchMatches.length > 0) ? 1 : 0;
				fTable.setItemCount(length + dash);
				if (length == 0) {
					// bug under windows.
					fTable.redraw();
					return;
				} 
				int update= Math.max(lastHistoryLength, fHistoryMatches.length);
				if (update > 0) {
					fTable.clear(0, update + dash - 1);
				}
			}
		});
	}
	
	private void setSearchResult(int ticket, final TypeInfo[] types) {
		syncExec(ticket, new Runnable() {
			public void run() {
				fExpectedItemCount+= types.length;
				fSearchMatches= types;
				int length= fHistoryMatches.length + fSearchMatches.length; 
				int dash= (fHistoryMatches.length > 0 && fSearchMatches.length > 0) ? 1 : 0;
				fTable.setItemCount(length + dash);
				if (length == 0) {
					// bug under windows.
					fTable.redraw();
					return;
				}
				if (fHistoryMatches.length == 0) {
					fTable.clear(0, length + dash - 1);
				} else {
					fTable.clear(fHistoryMatches.length - 1, length + dash - 1);
				}
			}
		});
	}
	
	private void setData(TableItem item) {
		int index= fTable.indexOf(item);
		TypeInfo type= getTypeInfo(index);
		if (type == DASH_LINE) {
			item.setData(fDashLine);
			fillDashLine(item);
		} else {
			item.setData(type);
			item.setImage(fLabelProvider.getImage(type));
			item.setText(fLabelProvider.getText(
				getTypeInfo(index - 1), 
				type, 
				getTypeInfo(index + 1)));
			item.setForeground(null);
		}
	}
	
	private TypeInfo getTypeInfo(int index) {
		if (index < 0)
			return null;
		if (index < fHistoryMatches.length) {
			return fHistoryMatches[index];
		}
		int dash= (fHistoryMatches.length > 0 && fSearchMatches.length > 0) ? 1 : 0;
		if (index == fHistoryMatches.length && dash == 1) {
			return DASH_LINE;
		}
		index= index - fHistoryMatches.length - dash;
		if (index >= fSearchMatches.length)
			return null;
		return fSearchMatches[index];
	}
	
	//-- Sync Job updates ------------------------------------------------------------
	
	private void scheduleSyncJob() {
		fSyncJob= new SyncJob(this);
		fSyncJob.schedule();
	}
	
	private void syncJobDone() {
		syncExec(new Runnable() {
			public void run() {
				fSyncJob= null;
				if (fTypeInfoFilter != null) {
					scheduleSearchJob(FULL);
				}
			}
		});
	}

	private boolean isSyncJobRunning() {
		return fSyncJob != null;
	}
	
	//-- progress monitor updates -----------------------------------------------------
	
	private void scheduleProgressUpdateJob() {
		syncExec(new Runnable() {
			public void run() {
				if (fProgressCounter == 0) {
					clearProgressMessage();
					fProgressUpdateJob= new ProgressUpdateJob(fDisplay, TypeInfoViewer.this);
					fProgressUpdateJob.schedule(300);
				}
				fProgressCounter++;
			}
		});
	}
	
	private void stopProgressUpdateJob() {
		syncExec(new Runnable() {
			public void run() {
				fProgressCounter--;
				if (fProgressCounter == 0 && fProgressUpdateJob != null) {
					fProgressUpdateJob.stop();
					fProgressUpdateJob= null;
					clearProgressMessage();
				}
			}
		});
	}
	
	private void setProgressMessage(String message) {
		fProgressMessage= message;
	}
	
	private void clearProgressMessage() {
		fProgressMessage= ""; //$NON-NLS-1$
		fProgressLabel.setText(fProgressMessage);
	}
	
	private void updateProgressMessage() {
		fProgressLabel.setText(fProgressMessage);
	}
	
	//-- Helper methods --------------------------------------------------------------

	private void syncExec(final Runnable runnable) {
		if (fDisplay.isDisposed()) 
			return;
		fDisplay.syncExec(new Runnable() {
			public void run() {
				if (fTable.isDisposed())
					return;
				runnable.run();
			}
		});
	}
	
	private void syncExec(final int ticket, final Runnable runnable) {
		if (fDisplay.isDisposed()) 
			return;
		fDisplay.syncExec(new Runnable() {
			public void run() {
				if (fTable.isDisposed() || ticket != fSearchJobTicket)
					return;
				runnable.run();
			}
		});
	}
	
	private void fillDashLine(TableItem item) {
		Rectangle bounds= item.getImageBounds(0);
		Rectangle area= fTable.getBounds();
		boolean willHaveScrollBar= fExpectedItemCount + 1 > fNumberOfVisibleItems;
		item.setText(fDashLine.getText(area.width - bounds.x - bounds.width - fTableWidthDelta - 
			(willHaveScrollBar ? fScrollbarWidth : 0)));
		item.setImage(fSeparatorIcon);
		item.setForeground(fDashLineColor);
		item.setData(fDashLine);
	}

	private void shortenTable() {
		if (VIRTUAL)
			return;
        if (fNextElement < fItems.size()) {
            fTable.setRedraw(false);
            fTable.remove(fNextElement, fItems.size() - 1);
            fTable.setRedraw(true);
        }
		for (int i= fItems.size() - 1; i >= fNextElement; i--) {
			fItems.remove(i);
		}
	}
	
	private void checkEmptyList() {
		if (fTable.getItemCount() == 0) {
			fTable.notifyListeners(SWT.Selection, new Event());
		}
	}
	
	private void setTableSelection(int index) {
		fTable.setSelection(index);
		fTable.notifyListeners(SWT.Selection, new Event());
	}
	
	private Color computeDashLineColor() {
		Color fg= fTable.getForeground();
		int fGray= (int)(0.3*fg.getRed() + 0.59*fg.getGreen() + 0.11*fg.getBlue());
		Color bg= fTable.getBackground();
		int bGray= (int)(0.3*bg.getRed() + 0.59*bg.getGreen() + 0.11*bg.getBlue());
		int gray= (int)((fGray + bGray) * 0.66);
		return new Color(fDisplay, gray, gray, gray);
	}
	
	private int computeScrollBarWidth() {
		Composite t= new Composite(fTable.getShell(), SWT.V_SCROLL);            
		int result= t.computeTrim(0, 0, 0, 0).width;
		t.dispose();
		return result;
	}

	private String getQualifiedText(TypeInfo type) {
		return fFullyQualifySelection
			? fLabelProvider.getFullyQualifiedText(type)
			: fLabelProvider.getQualifiedText(type);
	}	
}