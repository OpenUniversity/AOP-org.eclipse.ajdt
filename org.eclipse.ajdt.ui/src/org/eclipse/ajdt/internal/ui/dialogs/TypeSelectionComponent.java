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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.ajdt.ui.AspectJUIPlugin;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetFilterActionGroup;
import org.eclipse.jdt.ui.dialogs.TypeSelectionExtension;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

//Copied from org.eclipse.jdt.ui.dialogs - changes marked // AspectJ Change
public class TypeSelectionComponent extends Composite {
	
	private IDialogSettings fSettings;
	private boolean fMultipleSelection;
	private ITitleLabel fTitleLabel;
	
	private ToolBar fToolBar;
	private ToolItem fToolItem;
	private MenuManager fMenuManager;
	private WorkingSetFilterActionGroup fFilterActionGroup;
	
	private TypeSelectionExtension fTypeSelectionExtension;
	private Text fFilter;
	private String fInitialFilterText;
	private IJavaSearchScope fScope;
	private TypeInfoViewer fViewer;
	private ViewForm fForm;
	private CLabel fLabel;
	
	public static final int NONE= 0;
	public static final int CARET_BEGINNING= 1;
	public static final int FULL_SELECTION= 2;
	
	private static final String DIALOG_SETTINGS= "org.eclipse.jdt.internal.ui.dialogs.TypeSelectionComponent"; //$NON-NLS-1$
	private static final String SHOW_STATUS_LINE= "show_status_line"; //$NON-NLS-1$
	private static final String FULLY_QUALIFY_DUPLICATES= "fully_qualify_duplicates"; //$NON-NLS-1$
	private static final String WORKINGS_SET_SETTINGS= "workingset_settings"; //$NON-NLS-1$
	
	private class ToggleStatusLineAction extends Action {
		public ToggleStatusLineAction() {
			super(JavaUIMessages.TypeSelectionComponent_show_status_line_label, IAction.AS_CHECK_BOX);
		}
		public void run() {
			if (fForm == null)
				return;
			GridData gd= (GridData)fForm.getLayoutData();
			if (isChecked()) {
				gd.exclude= false;
			} else {
				gd.exclude= true;
			}
			fSettings.put(SHOW_STATUS_LINE, !gd.exclude);
			TypeSelectionComponent.this.layout();
		}
	}
	
	private class FullyQualifyDuplicatesAction extends Action {
		public FullyQualifyDuplicatesAction() {
			super(JavaUIMessages.TypeSelectionComponent_fully_qualify_duplicates_label, IAction.AS_CHECK_BOX);
		}
		public void run() {
			boolean checked= isChecked();
			fViewer.setFullyQualifyDuplicates(checked, true);
			fSettings.put(FULLY_QUALIFY_DUPLICATES, checked);
		}
	}
	
	/**
	 * Special interface to access a title lable in 
	 * a generic fashion.
	 */
	public interface ITitleLabel {
		/**
		 * Sets the title to the given text
		 * 
		 * @param text the title text
		 */
		public void setText(String text);
	}
	
	public TypeSelectionComponent(Composite parent, int style, String message, boolean multi, 
			IJavaSearchScope scope, int elementKind, String initialFilter, ITitleLabel titleLabel,
			TypeSelectionExtension extension) {
		super(parent, style);
		setFont(parent.getFont());
		fMultipleSelection= multi;
		fScope= scope;
		fInitialFilterText= initialFilter;
		fTitleLabel= titleLabel;
		fTypeSelectionExtension= extension;
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
		fSettings= settings.getSection(DIALOG_SETTINGS);
		if (fSettings == null) {
			fSettings= new DialogSettings(DIALOG_SETTINGS);
			settings.addSection(fSettings);
		}
		if (fSettings.get(SHOW_STATUS_LINE) == null) {
			fSettings.put(SHOW_STATUS_LINE, true);
		}
		createContent(message, elementKind);
	}
	
	public TypeInfo[] getSelection() {
		return fViewer.getSelection();
	}
	
	public IJavaSearchScope getScope() {
		return fScope;
	}
	
	private void createContent(String message, int elementKind) {
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0; layout.marginHeight= 0;
		setLayout(layout);
		Font font= getFont();
		
		Control header= createHeader(this, font, message);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		header.setLayoutData(gd);
		
		fFilter= new Text(this, SWT.BORDER | SWT.FLAT);
		fFilter.setFont(font);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 2;
		fFilter.setLayoutData(gd);
		fFilter.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				patternChanged((Text)e.widget);
			}
		});
		fFilter.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					fViewer.setFocus();
				}
			}
		});
		Label label= new Label(this, SWT.NONE);
		label.setFont(font);
		label.setText(JavaUIMessages.TypeSelectionComponent_label);
		label.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_MNEMONIC && e.doit) {
					e.detail= SWT.TRAVERSE_NONE;
					fViewer.setFocus();
				}
			}
		});
		label= new Label(this, SWT.RIGHT);
		label.setFont(font);
		gd= new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gd);
		fViewer= new TypeInfoViewer(this, fMultipleSelection ? SWT.MULTI : SWT.NONE, label, 
				fScope, elementKind, fInitialFilterText, 
				fTypeSelectionExtension != null ? fTypeSelectionExtension.getFilterExtension() : null,
				fTypeSelectionExtension != null ? fTypeSelectionExtension.getImageProvider() : null);
		gd= new GridData(GridData.FILL_BOTH);
		PixelConverter converter= new PixelConverter(fViewer.getTable());
		gd.widthHint= converter.convertWidthInCharsToPixels(70);
		gd.heightHint= SWTUtil.getTableHeightHint(fViewer.getTable(), 10);
		gd.horizontalSpan= 2;
		fViewer.getTable().setLayoutData(gd);
		fViewer.setFullyQualifyDuplicates(fSettings.getBoolean(FULLY_QUALIFY_DUPLICATES), false);
		if (!fMultipleSelection) {
			fForm= new ViewForm(this, SWT.BORDER | SWT.FLAT);
			fForm.setFont(font);
			gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan= 2;
			gd.exclude= !fSettings.getBoolean(SHOW_STATUS_LINE);
			fForm.setLayoutData(gd);
			fLabel= new CLabel(fForm, SWT.FLAT);
			fLabel.setFont(fForm.getFont());
			fForm.setContent(fLabel);
			fViewer.getTable().addSelectionListener(new SelectionAdapter() {
				// AspectJ Change Begin
				private TypeInfoLabelProvider fLabelProvider= new AJTypeInfoLabelProvider(
					TypeInfoLabelProvider.SHOW_TYPE_CONTAINER_ONLY + TypeInfoLabelProvider.SHOW_ROOT_POSTFIX);
				// AspectJ Change End
				public void widgetSelected(SelectionEvent event) {
					TypeInfo[] selection= fViewer.getSelection();
					if (selection.length != 1) {
						fLabel.setText(""); //$NON-NLS-1$
						fLabel.setImage(null);
					} else {
						TypeInfo type= selection[0];
						fLabel.setText(fViewer.getLabelProvider().getQualificationText(type));
						fLabel.setImage(fLabelProvider.getImage(type));
					}
				}
			});
		}
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent event) {
				disposeComponent();
			}
		});
	}

	public void addSelectionListener(SelectionListener listener) {
		fViewer.getTable().addSelectionListener(listener);
	}
	
	public void populate(int selectionMode) {
		if (fInitialFilterText != null) {
			fFilter.setText(fInitialFilterText);
			switch(selectionMode) {
				case CARET_BEGINNING:
					fFilter.setSelection(0, 0);
					break;
				case FULL_SELECTION:
					fFilter.setSelection(0, fInitialFilterText.length());
					break;
			}
		}
		fViewer.reset();
		fFilter.setFocus();
	}
	
	private void patternChanged(Text text) {
		fViewer.setSearchPattern(text.getText());
	}
	
	private Control createHeader(Composite parent, Font font, String message) {
		Composite header= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0; layout.marginHeight= 0;
		header.setLayout(layout);
		header.setFont(font);
		Label label= new Label(header, SWT.NONE);
		label.setText(message);
		label.setFont(font);
		label.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_MNEMONIC && e.doit) {
					e.detail= SWT.TRAVERSE_NONE;
					fFilter.setFocus();
				}
			}
		});
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gd);
		
		createViewMenu(header);
		return header;
	}
	
	private void createViewMenu(Composite parent) {
		fToolBar= new ToolBar(parent, SWT.FLAT);
		fToolItem= new ToolItem(fToolBar, SWT.PUSH, 0);

		GridData data= new GridData();
		data.horizontalAlignment= GridData.END;
		fToolBar.setLayoutData(data);

		fToolItem.setImage(JavaPluginImages.get(JavaPluginImages.IMG_ELCL_VIEW_MENU));
		fToolItem.setDisabledImage(JavaPluginImages.get(JavaPluginImages.IMG_DLCL_VIEW_MENU));
		fToolItem.setToolTipText(JavaUIMessages.TypeSelectionComponent_menu);
		fToolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showViewMenu();
			}
		});
		
		fMenuManager= new MenuManager();
		fillViewMenu(fMenuManager);

		// ICommandService commandService= (ICommandService)PlatformUI.getWorkbench().getAdapter(ICommandService.class);
		// IHandlerService handlerService= (IHandlerService)PlatformUI.getWorkbench().getAdapter(IHandlerService.class);
	}
	
	private void showViewMenu() {
		Menu menu = fMenuManager.createContextMenu(getShell());
		Rectangle bounds = fToolItem.getBounds();
		Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
		topLeft = fToolBar.toDisplay(topLeft);
		menu.setLocation(topLeft.x, topLeft.y);
		menu.setVisible(true);
	}
	
	private void fillViewMenu(IMenuManager viewMenu) {
		FullyQualifyDuplicatesAction fullyQualifyDuplicatesAction= new FullyQualifyDuplicatesAction();
		fullyQualifyDuplicatesAction.setChecked(fSettings.getBoolean(FULLY_QUALIFY_DUPLICATES));
		viewMenu.add(fullyQualifyDuplicatesAction);
		if (!fMultipleSelection) {
			viewMenu.add(new Separator());
			ToggleStatusLineAction showStatusLineAction= new ToggleStatusLineAction();
			showStatusLineAction.setChecked(fSettings.getBoolean(SHOW_STATUS_LINE));
			viewMenu.add(showStatusLineAction);
		}
		if (fScope == null) {
			fFilterActionGroup= new WorkingSetFilterActionGroup(getShell(), 
				AspectJUIPlugin.getDefault().getActiveWorkbenchWindow().getActivePage(),
				new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						IWorkingSet ws= (IWorkingSet)event.getNewValue();
						if (ws == null) {
							fScope= SearchEngine.createWorkspaceScope();
							fTitleLabel.setText(null);
						} else {
							fScope= JavaSearchScopeFactory.getInstance().createJavaSearchScope(ws, true);
							fTitleLabel.setText(ws.getName());
						}
						fViewer.setSearchScope(fScope, true);
					}
				});
			String setting= fSettings.get(WORKINGS_SET_SETTINGS);
			if (setting != null) {
				try {
					IMemento memento= XMLMemento.createReadRoot(new StringReader(setting));
					fFilterActionGroup.restoreState(memento);
				} catch (WorkbenchException e) {
				}
			}
			IWorkingSet ws= fFilterActionGroup.getWorkingSet();
			if (ws != null) {
				fScope= JavaSearchScopeFactory.getInstance().createJavaSearchScope(ws, true);
				fTitleLabel.setText(ws.getName());
			} else {
				fScope= SearchEngine.createWorkspaceScope();
				fTitleLabel.setText(null);
			}
			fFilterActionGroup.fillViewMenu(viewMenu);
		}
	}

	private void disposeComponent() {
		if (fFilterActionGroup != null) {
			XMLMemento memento= XMLMemento.createWriteRoot("workingSet"); //$NON-NLS-1$
			fFilterActionGroup.saveState(memento);
			fFilterActionGroup.dispose();
			StringWriter writer= new StringWriter();
			try {
				memento.save(writer);
				fSettings.put(WORKINGS_SET_SETTINGS, writer.getBuffer().toString());
			} catch (IOException e) {
				// don't do anythiung. Simply don't store the settings
			}
		}
	}
}