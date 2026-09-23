/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization.rse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.progress.UIJob;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.IFSFile;

import biz.isphere.base.internal.DialogSettingsManager;
import biz.isphere.base.internal.ExceptionHelper;
import biz.isphere.base.internal.IFSFileHelper;
import biz.isphere.base.internal.StringHelper;
import biz.isphere.base.internal.UIHelper;
import biz.isphere.base.swt.events.TableAutoSizeControlListener;
import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.Messages;
import biz.isphere.core.compareeditor.SourceMemberCompareEditorConfiguration;
import biz.isphere.core.externalapi.ISynchronizeStreamFilesEditorConfiguration;
import biz.isphere.core.ibmi.contributions.extension.handler.IBMiHostContributionsHandler;
import biz.isphere.core.ibmi.contributions.extension.point.BasicQualifiedConnectionName;
import biz.isphere.core.ifsfilecopy.CopyStreamFileItem;
import biz.isphere.core.ifsfilecopy.ICopyItemMessageListener;
import biz.isphere.core.ifsfilecopy.IValidateItemMessageListener;
import biz.isphere.core.ifsfilecopy.StreamFileCopyError;
import biz.isphere.core.ifsfilecopy.SynchronizeStreamFilesAction;
import biz.isphere.core.ifsfilecopy.ValidateStreamFilesJob;
import biz.isphere.core.ifsfilecopy.rse.CopyIfsFilesJob;
import biz.isphere.core.ifsfilecopy.rse.ExistingIfsFileAction;
import biz.isphere.core.ifsfilecopy.rse.MissingDirectoryAction;
import biz.isphere.core.ifssynchronization.CompareOptions;
import biz.isphere.core.ifssynchronization.SYNCIFS_retrieveItemAttributes;
import biz.isphere.core.ifssynchronization.SYNCIFS_retrieveItemAttributes.IfsFileAttributes;
import biz.isphere.core.ifssynchronization.StreamFileDescription;
import biz.isphere.core.ifssynchronization.SynchronizationResult;
import biz.isphere.core.ifssynchronization.SynchronizeStreamFilesEditorInput;
import biz.isphere.core.ifssynchronization.SynchronizeStreamFilesJob;
import biz.isphere.core.ifssynchronization.TableContentProvider;
import biz.isphere.core.ifssynchronization.TableFilter;
import biz.isphere.core.ifssynchronization.TableFilterData;
import biz.isphere.core.ifssynchronization.TableSorter;
import biz.isphere.core.ifssynchronization.TableStatistics;
import biz.isphere.core.ifssynchronization.jobs.CompareStreamFilesJob;
import biz.isphere.core.ifssynchronization.jobs.CompareStreamFilesSharedJobValues;
import biz.isphere.core.ifssynchronization.jobs.ICancelableJob;
import biz.isphere.core.ifssynchronization.jobs.ICompareStreamFilesPostrun;
import biz.isphere.core.ifssynchronization.jobs.ISynchronizeStreamFilesPostRun;
import biz.isphere.core.ifssynchronization.jobs.SyncIfsFileMode;
import biz.isphere.core.internal.IEditor;
import biz.isphere.core.internal.ISphereHelper;
import biz.isphere.core.internal.IStreamFileEditor;
import biz.isphere.core.internal.MessageDialogAsync;
import biz.isphere.core.internal.RemoteStreamFile;
import biz.isphere.core.internal.Size;
import biz.isphere.core.internal.StreamFile;
import biz.isphere.core.preferences.Preferences;
import biz.isphere.core.swt.widgets.HistoryCombo;
import biz.isphere.core.swt.widgets.WidgetFactory;
import biz.isphere.core.swt.widgets.dialogs.ConfirmationMessageDialog;

public abstract class AbstractSynchronizeStreamFilesEditor extends EditorPart
    implements ICompareStreamFilesPostrun, ISynchronizeStreamFilesPostRun, IValidateItemMessageListener, ICopyItemMessageListener {

    public static final String ID = "biz.isphere.core.ifssynchronization.rse.SynchronizeStreamFilesEditor"; //$NON-NLS-1$

    private static final String CHKBOX_IGNORE_DATE = "CHKBOX_IGNORE_DATE"; //$NON-NLS-1$
    private static final String CHKBOX_EMPTY_DIRECTORIES = "CHKBOX_EMPTY_DIRECTORIES"; //$NON-NLS-1$
    private static final String BUTTON_COPY_LEFT = "BUTTON_COPY_LEFT"; //$NON-NLS-1$
    private static final String BUTTON_COPY_RIGHT = "BUTTON_COPY_RIGHT"; //$NON-NLS-1$
    private static final String BUTTON_NO_COPY = "BUTTON_NO_COPY"; //$NON-NLS-1$
    private static final String BUTTON_EQUAL = "BUTTON_EQUAL"; //$NON-NLS-1$
    private static final String BUTTON_SINGLES = "BUTTON_SINGLES"; //$NON-NLS-1$
    private static final String BUTTON_DUPLICATES = "BUTTON_DUPLICATES"; //$NON-NLS-1$
    private static final String BUTTON_COMPARE_AFTER_SYNC = "BUTTON_COMPARE_AFTER_SYNC"; //$NON-NLS-1$

    private static final String STREAM_FILE_FILTER_HISTORY_KEY = "streamFileFilterHistory"; //$NON-NLS-1$
    private static final String REGEX_MARKER = "<";

    private static final int LEFT = 1;
    private static final int RIGHT = 2;

    private boolean isLeftObjectValid;
    private boolean isRightObjectValid;

    private TableViewer tableViewer;
    private TableFilter tableFilter;
    private TableFilterData filterData;
    private AbstractTableLabelProvider labelProvider;

    private Button btnCompare;
    private Button btnSynchronize;
    private Button btnCancel;
    private Button chkCompareAfterSync;
    private Button chkDisplayErrorsOnly;

    private DialogSettingsManager dialogSettingsManager;

    private Label lblLeftObject;
    private Button btnSelectLeftObject;
    private HistoryCombo cboIfsFileFilter;
    private Label lblRightObject;
    private Button btnSelectRightObject;

    private Button btnCopyRight;
    private Button btnEqual;
    private Button btnNoCopy;
    private Button btnCopyLeft;
    private Button btnDuplicates;
    private Button btnSingles;

    private Button chkIgnoreDate;
    private Button chkEmptyDirectories;

    private Group existingIfsFilesActionGroup;
    private Button chkBoxError;
    private Button chkBoxReplace;
    // private Link lnkPreferences;

    private Shell shell;

    private Composite headerArea;
    private Composite optionsArea;

    private StatusLine statusLine;
    private String statusMessage;
    private int numFilteredItems;

    private CompareStreamFilesSharedJobValues sharedValues;

    private boolean isComparing;
    private boolean isSynchronizing;
    private ICancelableJob jobToCancel;

    private SynchronizationResult synchronizationResult;

    private EditorCloseListener editorCloseListener;

    public AbstractSynchronizeStreamFilesEditor() {

        isLeftObjectValid = false;
        isRightObjectValid = false;

        dialogSettingsManager = new DialogSettingsManager(ISpherePlugin.getDefault().getDialogSettings(), getClass());
        shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    @Override
    public void createPartControl(Composite parent) {

        parent.setLayout(new GridLayout(1, false));

        createHeaderArea(parent);
        createOptionsArea(parent);
        createCompareArea(parent);
        createrFooterArea(parent);

        loadScreenValues();

        refreshAndCheckObjectNames();
        refreshTableFilter();

        if (getEditorInput().areSameObjects()) {
            MessageDialogAsync.displayNonBlockingError(getShell(), Messages.Warning_The_left_and_right_site_display_the_same_object);
        }

        getSite().setSelectionProvider(tableViewer);

        registerEditorListener(tableViewer.getTable());
    }

    private void registerEditorListener(Table table) {

        editorCloseListener = new EditorCloseListener(this);

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IPartService partService = window.getPartService();
        partService.addPartListener(editorCloseListener);

        debug("Editor close liestener added.");
    }

    private void unregisterEditorListener(EditorCloseListener editorCloseListener) {

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IPartService partService = window.getPartService();
        partService.removePartListener(editorCloseListener);

        debug("Editor close listener removed.");
    }

    private void createHeaderArea(Composite parent) {

        headerArea = new Composite(parent, SWT.NONE);
        headerArea.setLayout(createGridLayoutNoBorder(3, false));
        headerArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite leftHeaderArea = new Composite(headerArea, SWT.NONE);
        GridData leftHeaderAreaLayoutData = new GridData(GridData.FILL_HORIZONTAL);
        leftHeaderAreaLayoutData.minimumWidth = 120;
        leftHeaderArea.setLayoutData(leftHeaderAreaLayoutData);
        leftHeaderArea.setLayout(createGridLayoutNoBorder(2, false));

        lblLeftObject = new Label(leftHeaderArea, SWT.BORDER);
        GridData lblLeftObjectLayoutData = new GridData(GridData.FILL_HORIZONTAL);
        lblLeftObjectLayoutData.minimumWidth = 120;
        lblLeftObject.setLayoutData(lblLeftObjectLayoutData);

        btnSelectLeftObject = WidgetFactory.createPushButton(leftHeaderArea);
        btnSelectLeftObject.setToolTipText(Messages.Tooltip_Select_object);
        btnSelectLeftObject.setImage(getImage(ISpherePlugin.IMAGE_OPEN));
        btnSelectLeftObject.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent arg0) {
                String connectionName = null;
                String fileName = null;
                RemoteStreamFile defaultValues = getEditorInput().getLeftObject();
                if (defaultValues == null) {
                    defaultValues = getEditorInput().getRightObject();
                }
                if (defaultValues != null) {
                    connectionName = defaultValues.getConnectionName();
                    // libraryName = defaultValues.getLibrary();
                    fileName = defaultValues.getName();
                    // objectType = defaultValues.getObjectType();
                } else {
                    // objectType = ISeries.LIB;
                }
                RemoteStreamFile sourceFile = performSelectRemoteObject(connectionName, fileName);
                if (sourceFile != null) {
                    isLeftObjectValid = false;
                    getEditorInput().setLeftObject(sourceFile);
                    refreshAndCheckObjectNames(); // sets: isLeftObjectValid
                } else {
                    setButtonEnablementAndDisplayCompareStatus();
                }
            }
        });

        Composite middleHeaderArea = new Composite(headerArea, SWT.NONE);
        middleHeaderArea.setLayout(createGridLayoutNoBorder(1, false));
        GridData middleHeaderAreaLayoutData = new GridData();
        middleHeaderAreaLayoutData.minimumWidth = 120;
        middleHeaderArea.setLayoutData(middleHeaderAreaLayoutData);

        cboIfsFileFilter = WidgetFactory.createHistoryCombo(middleHeaderArea);
        GridData cboIfsFileFilterLayoutData = new GridData(GridData.FILL_HORIZONTAL);
        cboIfsFileFilterLayoutData.widthHint = 150;
        cboIfsFileFilter.setLayoutData(cboIfsFileFilterLayoutData);
        cboIfsFileFilter.setToolTipText(Messages.Tooltip_stream_file_name_and_type_filter);

        Composite rightHeaderArea = new Composite(headerArea, SWT.NONE);
        rightHeaderArea.setLayout(createGridLayoutNoBorder(2, false));
        GridData rightHeaderAreaLayoutData = new GridData(GridData.FILL_HORIZONTAL);
        rightHeaderAreaLayoutData.minimumWidth = 120;
        rightHeaderArea.setLayoutData(rightHeaderAreaLayoutData);

        lblRightObject = new Label(rightHeaderArea, SWT.BORDER);
        GridData lblRightObjectLayoutData = new GridData(GridData.FILL_HORIZONTAL);
        lblRightObjectLayoutData.minimumWidth = 120;
        lblRightObject.setLayoutData(lblRightObjectLayoutData);

        btnSelectRightObject = WidgetFactory.createPushButton(rightHeaderArea);
        btnSelectRightObject.setToolTipText(Messages.Tooltip_Select_object);
        btnSelectRightObject.setImage(getImage(ISpherePlugin.IMAGE_OPEN));
        btnSelectRightObject.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent arg0) {
                String connectionName = null;
                String fileName = null;
                RemoteStreamFile defaultValues = getEditorInput().getRightObject();
                if (defaultValues == null) {
                    defaultValues = getEditorInput().getLeftObject();
                }
                if (defaultValues != null) {
                    connectionName = defaultValues.getConnectionName();
                    fileName = defaultValues.getName();
                }
                RemoteStreamFile sourceFile = performSelectRemoteObject(connectionName, fileName);
                if (sourceFile != null) {
                    isRightObjectValid = false;
                    getEditorInput().setRightObject(sourceFile);
                    refreshAndCheckObjectNames(); // sets: isRightObjectValid
                } else {
                    setButtonEnablementAndDisplayCompareStatus();
                }
            }
        });
    }

    private void createOptionsArea(Composite parent) {

        optionsArea = new Composite(parent, SWT.NONE);
        optionsArea.setLayout(createGridLayoutNoBorder(5, false));
        optionsArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createCompareControlsArea(optionsArea);
        createFilterOptionsArea(optionsArea);
        new Composite(optionsArea, SWT.NONE).setLayoutData(new GridData(GridData.FILL_BOTH));
        createExistingIfsFilesActionArea(optionsArea);
        createSynchronizeControlsArea(optionsArea);
    }

    private void createCompareControlsArea(Composite parent) {

        Composite area = new Composite(parent, SWT.NONE);
        area.setLayout(createGridLayoutNoBorder(1, false));
        area.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, true));

        btnCompare = WidgetFactory.createPushButton(area);
        btnCompare.setLayoutData(createButtonLayoutData(1));
        btnCompare.setText(Messages.Compare);
        btnCompare.setToolTipText(Messages.Tooltip_start_compare_stream_files);
        btnCompare.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent event) {
                storeScreenValues();
                performCompareIfsFiles();
            }

            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });

        chkIgnoreDate = WidgetFactory.createCheckbox(area, Messages.Label_Ignore_date);
        chkIgnoreDate.setToolTipText(Messages.Tooltip_Ignore_date);

        chkEmptyDirectories = WidgetFactory.createCheckbox(area, Messages.Label_Empty_directories);
        chkEmptyDirectories.setToolTipText(Messages.Tooltip_Empty_directories);
        chkEmptyDirectories.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent event) {
                refreshTableFilter();
            }

            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });
    }

    private void createFilterOptionsArea(Composite parent) {

        int numColumns = 5;

        Group filterOptionsGroup = new Group(parent, SWT.NONE);
        filterOptionsGroup.setLayout(createGridLayoutNoBorder(numColumns, false));
        filterOptionsGroup.setLayoutData(new GridData(GridData.BEGINNING, GridData.FILL, false, true));
        filterOptionsGroup.setText(Messages.Display);

        filterData = new TableFilterData();

        btnCopyRight = WidgetFactory.createToggleButton(filterOptionsGroup, SWT.FLAT);
        btnCopyRight.setImage(getImage(ISpherePlugin.IMAGE_COPY_RIGHT));
        btnCopyRight.setToolTipText(Messages.Tooltip_display_copy_from_left_to_right);
        btnCopyRight.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                refreshTableFilter();
            }

            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });

        btnEqual = WidgetFactory.createToggleButton(filterOptionsGroup, SWT.FLAT);
        btnEqual.setImage(getImage(ISpherePlugin.IMAGE_COPY_EQUAL));
        btnEqual.setToolTipText(Messages.Tooltip_display_equal_items);
        btnEqual.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                refreshTableFilter();
            }

            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });

        btnNoCopy = WidgetFactory.createToggleButton(filterOptionsGroup, SWT.FLAT);
        btnNoCopy.setImage(getImage(ISpherePlugin.IMAGE_COPY_NOT_EQUAL));
        btnNoCopy.setToolTipText(Messages.Tooltip_display_unequal_items);
        btnNoCopy.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                refreshTableFilter();
            }

            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });

        btnCopyLeft = WidgetFactory.createToggleButton(filterOptionsGroup, SWT.FLAT);
        btnCopyLeft.setImage(getImage(ISpherePlugin.IMAGE_COPY_LEFT));
        btnCopyLeft.setToolTipText(Messages.Tooltip_display_copy_from_right_to_left);
        btnCopyLeft.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                refreshTableFilter();
            }

            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });

        Composite displayOccurences = new Composite(filterOptionsGroup, SWT.NONE);
        displayOccurences.setLayout(new GridLayout());
        displayOccurences.setLayoutData(new GridData());

        btnDuplicates = WidgetFactory.createToggleButton(displayOccurences);
        btnDuplicates.setLayoutData(createButtonLayoutData());
        btnDuplicates.setText(Messages.Duplicates);
        btnDuplicates.setToolTipText(Messages.Tooltip_display_duplicates);
        btnDuplicates.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                refreshTableFilter();
            }

            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });

        btnSingles = WidgetFactory.createToggleButton(displayOccurences);
        btnSingles.setLayoutData(createButtonLayoutData());
        btnSingles.setText(Messages.Singles);
        btnSingles.setToolTipText(Messages.Tooltip_display_singles);
        btnSingles.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                refreshTableFilter();
            }

            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });
    }

    private void createExistingIfsFilesActionArea(Composite parent) {

        if (isSynchronizationEnabled()) {

            existingIfsFilesActionGroup = new Group(parent, SWT.NONE);
            existingIfsFilesActionGroup.setLayout(new GridLayout(1, false));
            existingIfsFilesActionGroup.setLayoutData(new GridData(GridData.END, GridData.FILL, false, true));
            existingIfsFilesActionGroup.setText(Messages.Label_Existing_stream_files_action_colon);

            chkBoxError = WidgetFactory.createRadioButton(existingIfsFilesActionGroup, Messages.Label_Error);
            chkBoxError.setLayoutData(new GridData(GridData.BEGINNING));

            chkBoxReplace = WidgetFactory.createRadioButton(existingIfsFilesActionGroup, Messages.Label_Replace_existing_stream_files);
            chkBoxError.setLayoutData(new GridData(GridData.BEGINNING));
        }
    }

    private void createSynchronizeControlsArea(Composite parent) {

        Composite area = new Composite(parent, SWT.NONE);
        area.setLayout(createGridLayoutNoBorder(1, false));
        area.setLayoutData(new GridData(GridData.END, GridData.BEGINNING, false, true));

        if (isSynchronizationEnabled()) {

            btnSynchronize = WidgetFactory.createPushButton(area);
            btnSynchronize.setLayoutData(createButtonLayoutData(1, 1, SWT.RIGHT));
            btnSynchronize.setText(Messages.Synchronize);
            btnSynchronize.setToolTipText(Messages.Tooltip_start_synchronize_stream_files);
            btnSynchronize.addSelectionListener(new SelectionListener() {
                public void widgetSelected(SelectionEvent event) {
                    performSynchronizeIfsFiles();
                }

                public void widgetDefaultSelected(SelectionEvent event) {
                }
            });
        }

        btnCancel = WidgetFactory.createPushButton(area);
        btnCancel.setLayoutData(createButtonLayoutData(1, 1, SWT.RIGHT));
        btnCancel.setText(Messages.Cancel);
        btnCancel.setToolTipText(Messages.Tooltip_cancel_operation);
        btnCancel.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent event) {
                performCancelOperation();
            }

            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });

        if (isSynchronizationEnabled()) {

            chkCompareAfterSync = WidgetFactory.createCheckbox(area);
            chkCompareAfterSync.setText(Messages.Compare_after_synchronization);
            chkCompareAfterSync.setToolTipText(Messages.Tooltip_Compare_after_stream_file_synchronization);
            chkCompareAfterSync.addSelectionListener(new SelectionListener() {
                public void widgetSelected(SelectionEvent paramSelectionEvent) {
                    storeScreenValues();
                }

                public void widgetDefaultSelected(SelectionEvent paramSelectionEvent) {
                }
            });
        }

        chkDisplayErrorsOnly = WidgetFactory.createCheckbox(area, Messages.Errors_only);
        chkDisplayErrorsOnly.setLayoutData(createButtonLayoutData(1, 1));
        chkDisplayErrorsOnly.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                refreshTableFilter();
            }

            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });
    }

    private void createCompareArea(Composite parent) {

        Composite compareArea = new Composite(parent, SWT.NONE);
        compareArea.setLayout(new GridLayout(1, false));
        compareArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        tableViewer = new TableViewer(compareArea, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        /* First column is always RIGHT aligned, see bug 151342 */
        TableColumn tblClmnDummy = new TableColumn(tableViewer.getTable(), SWT.NONE);
        tblClmnDummy.setResizable(true);
        tblClmnDummy.setWidth(Size.getSize(0));

        final TableColumn tblClmnLeftFile = new TableColumn(tableViewer.getTable(), SWT.LEFT);
        tblClmnLeftFile.setText(Messages.File);
        tblClmnLeftFile.setResizable(true);
        tblClmnLeftFile.setWidth(Size.getSize(80));

        final TableColumn tblClmnLeftLastChanges = new TableColumn(tableViewer.getTable(), SWT.LEFT);
        tblClmnLeftLastChanges.setText(Messages.Last_changed);
        tblClmnLeftLastChanges.setResizable(true);
        tblClmnLeftLastChanges.setWidth(Size.getSize(100));

        final TableColumn tblClmnCompareResult = new TableColumn(tableViewer.getTable(), SWT.CENTER);
        tblClmnCompareResult.setResizable(true);
        tblClmnCompareResult.setWidth(Size.getSize(25));

        final TableColumn tblClmnRightFile = new TableColumn(tableViewer.getTable(), SWT.LEFT);
        tblClmnRightFile.setText(Messages.File);
        tblClmnRightFile.setResizable(tblClmnLeftFile.getResizable());
        tblClmnRightFile.setWidth(tblClmnLeftFile.getWidth());

        final TableColumn tblClmnRightLastChanges = new TableColumn(tableViewer.getTable(), SWT.LEFT);
        tblClmnRightLastChanges.setText(Messages.Last_changed);
        tblClmnRightLastChanges.setResizable(tblClmnLeftLastChanges.getResizable());
        tblClmnRightLastChanges.setWidth(tblClmnLeftLastChanges.getWidth());

        tableViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                ISelection selection = event.getSelection();
                if (selection instanceof StructuredSelection) {
                    StructuredSelection structuredSelection = (StructuredSelection)selection;
                    for (Iterator<?> iterator = structuredSelection.iterator(); iterator.hasNext();) {
                        Object item = (Object)iterator.next();
                        if (item instanceof StreamFileCompareItem) {
                            StreamFileCompareItem compareItem = (StreamFileCompareItem)item;
                            performOpenCompareIfsFilesDialog(compareItem);
                        }
                    }
                }
            }
        });

        TableStatistics tableStatistics = new TableStatistics();
        tableFilter = new TableFilter(tableStatistics);

        tableViewer.setContentProvider(new TableContentProvider(tableStatistics));
        tableViewer.addFilter(tableFilter);
        labelProvider = getTableLabelProvider(tableViewer);
        tableViewer.setLabelProvider(labelProvider);
        Menu menuTableViewerContextMenu = new Menu(tableViewer.getTable());
        menuTableViewerContextMenu.addMenuListener(new TableContextMenu(menuTableViewerContextMenu, getEditorInput().getConfiguration()));
        tableViewer.getTable().setMenu(menuTableViewerContextMenu);

        addRowPainter(tableViewer.getTable());

        TableAutoSizeControlListener tableAutoSizeAdapter = new TableAutoSizeControlListener(tableViewer.getTable());
        tableAutoSizeAdapter.addResizableColumn(tblClmnLeftFile, 1);
        tableAutoSizeAdapter.addResizableColumn(tblClmnRightFile, 1);
        tableViewer.getTable().addControlListener(tableAutoSizeAdapter);

        tableViewer.setSorter(new TableSorter(SyncIfsFileMode.LEFT_SYSTEM));

        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                String errorMessage = null;
                Object source = event.getSource();
                if (source instanceof TableViewer) {
                    TableViewer tableViewer = (TableViewer)source;
                    int countSelected = tableViewer.getTable().getSelectionCount();
                    if (countSelected == 1) {
                        TableItem tableItem = tableViewer.getTable().getItem(tableViewer.getTable().getSelectionIndex());
                        StreamFileCompareItem item = (StreamFileCompareItem)tableItem.getData();
                        errorMessage = item.getErrorMessage();
                    }
                }
                displayCompareStatus(errorMessage);
            }
        });
    }

    /**
     * Installs the custom row painter of the table.
     * <p>
     * The painter takes over drawing the background and the selection of all
     * rows. On newer Windows themes the native selection highlight is drawn as
     * a narrow, rounded shape that does not reliably cover the full row, and as
     * soon as owner draw is active Windows reserves a state-icon area at the
     * start of every row that is painted in the classic, non-themed highlight
     * color. Painting the selection ourselves gives all cells a uniform look.
     * Text and images of regular rows are still drawn natively, using the
     * foreground color set on the GC.
     * <p>
     * Directory rows additionally span the full width of the table and are
     * painted with a grey background, similar to a button, instead of showing
     * their values in the individual columns.
     */
    private void addRowPainter(final Table table) {

        Listener rowPainter = new Listener() {
            public void handleEvent(Event event) {

                TableItem tableItem = (TableItem)event.item;
                StreamFileCompareItem directoryItem = getDirectoryCompareItem(tableItem);

                if (event.type == SWT.EraseItem) {
                    /*
                     * Read the selection state before clearing the flags,
                     * because clearing SWT.SELECTED is what prevents the native
                     * selection from being drawn.
                     */
                    boolean isSelected = (event.detail & SWT.SELECTED) != 0;
                    if (directoryItem != null) {
                        event.detail &= ~(SWT.FOREGROUND | SWT.BACKGROUND | SWT.SELECTED | SWT.HOT);
                    } else {
                        event.detail &= ~(SWT.BACKGROUND | SWT.SELECTED | SWT.HOT);
                        paintCellBackground(table, event, isSelected);
                    }
                } else if (event.type == SWT.PaintItem) {
                    if (directoryItem != null && event.index == 1) {
                        paintDirectoryRow(table, event, directoryItem);
                    }
                }
            }
        };

        table.addListener(SWT.EraseItem, rowPainter);
        table.addListener(SWT.PaintItem, rowPainter);
    }

    /**
     * Paints the background of a single cell of a regular (non-directory) row
     * and sets the foreground color that SWT uses for drawing the native text
     * and images of the cell afterwards.
     */
    private void paintCellBackground(Table table, Event event, boolean isSelected) {

        GC gc = event.gc;
        Display display = table.getDisplay();

        Color background;
        Color foreground;
        if (isSelected) {
            background = display.getSystemColor(SWT.COLOR_LIST_SELECTION);
            foreground = display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
        } else {
            background = table.getBackground();
            foreground = table.getForeground();
        }

        gc.setBackground(background);
        gc.fillRectangle(event.x, event.y, event.width, event.height);
        gc.setForeground(foreground);
    }

    private StreamFileCompareItem getDirectoryCompareItem(TableItem tableItem) {

        if (tableItem == null) {
            return null;
        }

        Object data = tableItem.getData();
        if (!(data instanceof StreamFileCompareItem)) {
            return null;
        }

        StreamFileCompareItem compareItem = (StreamFileCompareItem)data;
        if (!isDirectoryItem(compareItem)) {
            return null;
        }

        return compareItem;
    }

    private boolean isDirectoryItem(StreamFileCompareItem compareItem) {
        return compareItem.isDirectory();
    }

    /**
     * Selects all currently visible rows that are direct children of the given
     * directory row, identified via the
     * {@link StreamFileDescription#getParentDirectory()} reference set by
     * {@code LoadCompareIfsFilesJob} while loading the compare data. A path
     * prefix match was tried first but also matched entries of nested
     * subdirectories, which is not wanted here - only direct children of the
     * clicked directory should be selected.
     */
    private void expandDirectorySelection() {

        StreamFileCompareItem[] selectedItems = getSelectedItems();
        if (selectedItems.length != 1 || !isDirectoryItem(selectedItems[0])) {
            return;
        }

        StreamFileCompareItem directoryItem = selectedItems[0];

        List<StreamFileCompareItem> itemsToSelect = new ArrayList<StreamFileCompareItem>();
        itemsToSelect.add(directoryItem);

        for (TableItem tableItem : tableViewer.getTable().getItems()) {
            Object data = tableItem.getData();
            if (!(data instanceof StreamFileCompareItem) || data == directoryItem) {
                continue;
            }
            StreamFileCompareItem compareItem = (StreamFileCompareItem)data;
            if (isChildOf(compareItem, directoryItem)) {
                itemsToSelect.add(compareItem);
            }
        }

        if (itemsToSelect.size() > 1) {
            tableViewer.setSelection(new StructuredSelection(itemsToSelect), false);
        }
    }

    private boolean isChildOf(StreamFileCompareItem candidate, StreamFileCompareItem directoryItem) {

        if (isDirectoryItem(candidate)) {
            return false;
        }

        StreamFileDescription leftParent = directoryItem.getLeftIfsFileDescription();
        if (leftParent != null && candidate.getLeftIfsFileDescription() != null
            && candidate.getLeftIfsFileDescription().getParentDirectory() == leftParent) {
            return true;
        }

        StreamFileDescription rightParent = directoryItem.getRightIfsFileDescription();
        if (rightParent != null && candidate.getRightIfsFileDescription() != null
            && candidate.getRightIfsFileDescription().getParentDirectory() == rightParent) {
            return true;
        }

        return false;
    }

    private void paintDirectoryRow(Table table, Event event, StreamFileCompareItem compareItem) {

        GC gc = event.gc;
        Rectangle clientArea = table.getClientArea();

        /*
         * The GC is clipped to the bounds of the column being painted (column 0
         * has a width of 0), so the clipping must be widened to the full row,
         * otherwise nothing gets painted outside that sliver.
         */
        Rectangle oldClipping = gc.getClipping();
        Color oldBackground = gc.getBackground();
        Color oldForeground = gc.getForeground();

        Rectangle rowBounds = new Rectangle(clientArea.x, event.y, clientArea.width, event.height);
        gc.setClipping(rowBounds);

        /*
         * Always paint the background ourselves, instead of relying on the
         * native selection highlight: on newer Windows themes it is drawn as a
         * narrow, rounded shape that does not reliably cover the full row,
         * which left the text unreadable when a directory row was selected.
         */
        boolean isSelected = (event.detail & SWT.SELECTED) != 0;

        Color background;
        Color foreground;
        if (isSelected) {
            background = table.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
            foreground = table.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
        } else {
            background = table.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
            foreground = table.getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
        }

        gc.setBackground(background);
        gc.fillRectangle(rowBounds);
        gc.setForeground(foreground);

        int x = clientArea.x + 4;

        /*
         * Directories that are copied to the other side are marked with the
         * image of their compare status, which tells the side the directory is
         * created on.
         */
        Image compareStatusImage = getDirectoryCompareStatusImage(compareItem);
        if (compareStatusImage != null) {
            Rectangle imageBounds = compareStatusImage.getBounds();
            int imageY = event.y + (event.height - imageBounds.height) / 2;
            gc.drawImage(compareStatusImage, x, imageY);
            x = x + imageBounds.width + 4;
        }

        String text = compareItem.getIfsFileName();
        int textY = event.y + (event.height - gc.textExtent(text).y) / 2;
        gc.drawText(text, x, textY, true);

        gc.setClipping(oldClipping);
        gc.setBackground(oldBackground);
        gc.setForeground(oldForeground);
    }

    /**
     * Returns the image a directory row is marked with, or <code>null</code>,
     * when the directory is not marked at all. Directories are not compared,
     * hence only a directory that is missing on one side or that is in error
     * gets an image.
     */
    private Image getDirectoryCompareStatusImage(StreamFileCompareItem compareItem) {

        if (labelProvider == null || sharedValues == null) {
            return null;
        }

        int compareStatus = compareItem.getCompareStatus(sharedValues.getCompareOptions());
        if (compareStatus != StreamFileCompareItem.LEFT_MISSING && compareStatus != StreamFileCompareItem.RIGHT_MISSING
            && compareStatus != StreamFileCompareItem.ERROR) {
            return null;
        }

        Image compareStatusImage = labelProvider.getCompareStatusImage(compareItem);
        if (compareStatusImage == null || compareStatusImage.isDisposed()) {
            return null;
        }

        return compareStatusImage;
    }

    private void createrFooterArea(Composite parent) {

        Composite footerArea = new Composite(parent, SWT.NONE);
        footerArea.setLayout(new GridLayout(1, false));
        footerArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    private GridLayout createGridLayoutNoBorder(int numColumns, boolean makeColumnsEqualWidth) {

        GridLayout layout = new GridLayout(numColumns, makeColumnsEqualWidth);
        layout.marginHeight = 0;
        layout.marginWidth = 0;

        return layout;
    }

    private GridData createButtonLayoutData() {
        return createButtonLayoutData(1);
    }

    private GridData createButtonLayoutData(int verticalSpan) {
        return createButtonLayoutData(verticalSpan, 1);
    }

    private GridData createButtonLayoutData(int verticalSpan, int horizontalSpan) {
        return createButtonLayoutData(verticalSpan, horizontalSpan, SWT.LEFT);
    }

    private GridData createButtonLayoutData(int verticalSpan, int horizontalSpan, int horizontalAlignment) {

        GridData gridData = new GridData(horizontalAlignment, SWT.TOP, false, false, 1, 1);
        gridData.widthHint = 120;
        gridData.verticalSpan = verticalSpan;
        gridData.horizontalSpan = horizontalSpan;

        return gridData;
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) {
        setSite(site);
        setInput(input);
        setPartName(input.getName());
        setTitleImage(((SynchronizeStreamFilesEditorInput)input).getTitleImage());
    }

    @Override
    public void setFocus() {
    }

    protected Shell getShell() {
        return shell;
    }

    public SynchronizeStreamFilesEditorInput getEditorInput() {

        IEditorInput input = super.getEditorInput();
        if (input instanceof SynchronizeStreamFilesEditorInput) {
            return (SynchronizeStreamFilesEditorInput)input;
        }

        return null;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        return;
    }

    @Override
    public void doSaveAs() {
        return;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public boolean isSaveOnCloseNeeded() {
        return true;
    }

    public static void openEditor(RemoteStreamFile leftRemoteObject, RemoteStreamFile rightRemoteObject,
        ISynchronizeStreamFilesEditorConfiguration configuration) throws PartInitException {

        if (leftRemoteObject != null) {
            String leftConnectionName = leftRemoteObject.getConnectionName();
            if (!ISphereHelper.checkISphereLibrary(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), leftConnectionName)) {
                return;
            }
        }

        if (rightRemoteObject != null) {
            String rightConnectionName = rightRemoteObject.getConnectionName();
            if (!ISphereHelper.checkISphereLibrary(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), rightConnectionName)) {
                return;
            }
        }

        SynchronizeStreamFilesEditorInput editorInput = new SynchronizeStreamFilesEditorInput(leftRemoteObject, rightRemoteObject, configuration);
        UIHelper.getActivePage().openEditor(editorInput, AbstractSynchronizeStreamFilesEditor.ID);
    }

    private void refreshAndCheckObjectNames() {

        SynchronizeStreamFilesEditorInput editorInput = getEditorInput();
        if (editorInput != null) {
            lblLeftObject.setText(getEditorInput().getLeftObjectName());
            lblRightObject.setText(getEditorInput().getRightObjectName());
        } else {
            lblLeftObject.setText(Messages.EMPTY);
            lblRightObject.setText(Messages.EMPTY);
        }

        headerArea.layout(true);

        setButtonEnablementAndDisplayCompareStatus();
    }

    private void refreshTableFilter() {

        if (tableViewer != null) {

            // long startTime = System.currentTimeMillis();

            Object input = null;

            try {

                tableViewer.getControl().setRedraw(false);
                input = tableViewer.getInput();
                tableViewer.setInput(null);

                if (tableFilter != null) {
                    tableViewer.removeFilter(tableFilter);
                    clearTableStatistics();
                }

                if (filterData != null) {

                    filterData.setCopyLeft(btnCopyLeft.getSelection());
                    filterData.setCopyRight(btnCopyRight.getSelection());
                    filterData.setEqual(btnEqual.getSelection());
                    filterData.setNoCopy(btnNoCopy.getSelection());
                    filterData.setSingles(btnSingles.getSelection());
                    filterData.setDuplicates(btnDuplicates.getSelection());

                    if (isSynchronizationEnabled()) {
                        filterData.setErrorsOnly(chkDisplayErrorsOnly.getSelection());
                    } else {
                        filterData.setErrorsOnly(false);
                    }

                    if (tableFilter == null) {
                        tableFilter = new TableFilter(getTableStatistics());
                    }

                    clearTableStatistics();
                    tableFilter.setFilterData(filterData);

                    /*
                     * Unlike the IFS file filter, which is evaluated by the
                     * host program, the 'empty directories' option is applied
                     * by the table filter and hence takes effect immediately.
                     */
                    if (sharedValues != null) {
                        sharedValues.getCompareOptions().setIncludeEmptyDirectories(chkEmptyDirectories.getSelection());
                        tableFilter.setCompareOptions(sharedValues.getCompareOptions());
                    }

                    tableViewer.addFilter(tableFilter);
                }

                storeScreenValues();

            } finally {
                if (input != null) {
                    tableViewer.setInput(input);
                }
                tableViewer.getControl().setRedraw(true);

                setButtonEnablementAndDisplayCompareStatus();

                // System.out.println("Time in msecs: " +
                // (System.currentTimeMillis() - startTime));
            }
        }
    }

    private TableContentProvider getTableContentProvider() {

        return (TableContentProvider)tableViewer.getContentProvider();
    }

    private TableStatistics getTableStatistics() {

        return getTableContentProvider().getTableStatistics();
    }

    private void clearTableStatistics() {

        getTableStatistics().clearStatistics();
    }

    private boolean isSynchronizationEnabled() {
        ISynchronizeStreamFilesEditorConfiguration config = getEditorInput().getConfiguration();
        return config.isLeftEditorEnabled() || config.isRightEditorEnabled();
    }

    private synchronized void setButtonEnablementAndDisplayCompareStatus() {

        boolean isCompareEnabled = true;
        boolean isSynchronizeEnabled = true;

        if (getEditorInput().getLeftObject() != null && !isLeftObjectValid) {
            String connectionName = getEditorInput().getLeftObject().getConnectionName();
            if (!ISphereHelper.checkISphereLibrary(getShell(), connectionName)) {
                isCompareEnabled = false;
                isSynchronizeEnabled = false;
                isLeftObjectValid = false;
            } else {
                isLeftObjectValid = true;
            }
        }

        if (getEditorInput().getRightObject() != null && !isRightObjectValid) {
            String connectionName = getEditorInput().getRightObject().getConnectionName();
            if (!ISphereHelper.checkISphereLibrary(getShell(), connectionName)) {
                isCompareEnabled = false;
                isSynchronizeEnabled = false;
                isRightObjectValid = false;
            } else {
                isRightObjectValid = true;
            }
        }

        if (getEditorInput().getLeftObject() == null || getEditorInput().getRightObject() == null) {
            isCompareEnabled = false;
        }

        if (tableViewer.getTable().getItems().length <= 0) {
            isSynchronizeEnabled = false;
        }

        if (isWorking()) {
            setChildrenEnabled(headerArea, false);
            setChildrenEnabled(optionsArea, false);
            isCompareEnabled = false;
        } else {
            setChildrenEnabled(headerArea, true);
            setChildrenEnabled(optionsArea, true);
        }

        if (jobToCancel == null) {
            btnCancel.setEnabled(false);
        } else {
            btnCancel.setEnabled(true);
        }

        btnCompare.setEnabled(isCompareEnabled);

        ISynchronizeStreamFilesEditorConfiguration config = getEditorInput().getConfiguration();

        if (existingIfsFilesActionGroup != null) {
            if (isWorking()) {
                chkBoxError.setEnabled(false);
                chkBoxReplace.setEnabled(false);
            } else {
                if (isSynchronizationEnabled()) {
                    chkBoxError.setEnabled(isSynchronizeEnabled);
                    chkBoxReplace.setEnabled(isSynchronizeEnabled);
                } else {
                    chkBoxError.setEnabled(false);
                    chkBoxReplace.setEnabled(false);
                }
            }
        }

        if (btnSynchronize != null && chkCompareAfterSync != null) {
            if (isWorking()) {
                btnSynchronize.setEnabled(false);
                chkCompareAfterSync.setEnabled(false);
            } else {
                if (isSynchronizationEnabled()) {
                    btnSynchronize.setEnabled(isSynchronizeEnabled);
                    chkCompareAfterSync.setEnabled(isSynchronizeEnabled);
                } else {
                    btnSynchronize.setEnabled(false);
                    chkCompareAfterSync.setEnabled(false);
                }
            }
        }

        if (config.isLeftSelectObjectEnabled()) {
            btnSelectLeftObject.setEnabled(true);
        } else {
            btnSelectLeftObject.setEnabled(false);
        }

        if (config.isRightSelectObjectEnabled()) {
            btnSelectRightObject.setEnabled(true);
        } else {
            btnSelectRightObject.setEnabled(false);
        }

        displayCompareStatus();
    }

    private void setChildrenEnabled(Composite parent, boolean enabled) {
        for (Control control : parent.getChildren()) {
            if (control instanceof Button) {
                control.setEnabled(enabled);
            } else if (control instanceof Composite) {
                setChildrenEnabled((Composite)control, enabled);
            }
        }
    }

    private synchronized boolean isWorking() {
        return isComparing || isSynchronizing;
    }

    private synchronized void setIsComparing(boolean isComparing) {
        this.isComparing = isComparing;
    }

    private synchronized void setIsSynchronizing(boolean isSynchronizing) {
        this.isSynchronizing = isSynchronizing;
    }

    private void displayCompareStatus() {
        displayCompareStatus(null);
    }

    private void displayCompareStatus(String errorMessage) {

        if (isWorking()) {
            statusMessage = Messages.Working;
        } else {
            TableStatistics tableStatistics = getTableStatistics();

            if (StringHelper.isNullOrEmpty(errorMessage)) {
                statusMessage = tableStatistics.toString();
            } else {
                statusMessage = errorMessage;
            }

            numFilteredItems = tableStatistics.getFilteredElements();
        }

        updateStatusLine();
    }

    public void loadHistory() {
        cboIfsFileFilter.load(dialogSettingsManager, STREAM_FILE_FILTER_HISTORY_KEY);
    }

    public void updateHistory() {
        cboIfsFileFilter.updateHistory(cboIfsFileFilter.getText());
    }

    public void storeHistory() {
        cboIfsFileFilter.store();
    }

    /**
     * Restores the screen values of the last search search.
     */
    private void loadScreenValues() {

        chkIgnoreDate.setSelection(dialogSettingsManager.loadBooleanValue(CHKBOX_IGNORE_DATE, false));
        chkEmptyDirectories.setSelection(dialogSettingsManager.loadBooleanValue(CHKBOX_EMPTY_DIRECTORIES, false));

        btnCopyLeft.setSelection(dialogSettingsManager.loadBooleanValue(BUTTON_COPY_LEFT, true));
        btnCopyRight.setSelection(dialogSettingsManager.loadBooleanValue(BUTTON_COPY_RIGHT, true));
        btnEqual.setSelection(dialogSettingsManager.loadBooleanValue(BUTTON_EQUAL, true));
        btnNoCopy.setSelection(dialogSettingsManager.loadBooleanValue(BUTTON_NO_COPY, true));
        btnSingles.setSelection(dialogSettingsManager.loadBooleanValue(BUTTON_SINGLES, true));
        btnDuplicates.setSelection(dialogSettingsManager.loadBooleanValue(BUTTON_DUPLICATES, true));

        if (isSynchronizationEnabled()) {
            chkCompareAfterSync.setSelection(dialogSettingsManager.loadBooleanValue(BUTTON_COMPARE_AFTER_SYNC, true));
            setDisplayErrorsOnly(false);
        }

        if (chkBoxError != null) {
            chkBoxError.setSelection(true);
        }

        loadHistory();

        if (StringHelper.isNullOrEmpty(cboIfsFileFilter.getText())) {
            cboIfsFileFilter.setText("*.*");
            storeHistory();
        }
    }

    /**
     * Stores the screen values that are preserved for the next search.
     */
    private void storeScreenValues() {

        dialogSettingsManager.storeValue(CHKBOX_IGNORE_DATE, chkIgnoreDate.getSelection());
        dialogSettingsManager.storeValue(CHKBOX_EMPTY_DIRECTORIES, chkEmptyDirectories.getSelection());

        dialogSettingsManager.storeValue(BUTTON_COPY_LEFT, btnCopyLeft.getSelection());
        dialogSettingsManager.storeValue(BUTTON_COPY_RIGHT, btnCopyRight.getSelection());
        dialogSettingsManager.storeValue(BUTTON_EQUAL, btnEqual.getSelection());
        dialogSettingsManager.storeValue(BUTTON_NO_COPY, btnNoCopy.getSelection());
        dialogSettingsManager.storeValue(BUTTON_SINGLES, btnSingles.getSelection());
        dialogSettingsManager.storeValue(BUTTON_DUPLICATES, btnDuplicates.getSelection());
        if (isSynchronizationEnabled()) {
            dialogSettingsManager.storeValue(BUTTON_COMPARE_AFTER_SYNC, chkCompareAfterSync.getSelection());
        }

        updateHistory();
        storeHistory();
    }

    private StreamFileCompareItem[] getSelectedItems() {

        List<StreamFileCompareItem> selectedItems = new ArrayList<StreamFileCompareItem>();

        if (tableViewer.getSelection() instanceof StructuredSelection) {
            StructuredSelection selection = (StructuredSelection)tableViewer.getSelection();
            for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
                Object selectedItem = (Object)iterator.next();
                if (selectedItem instanceof StreamFileCompareItem) {
                    StreamFileCompareItem compareItem = (StreamFileCompareItem)selectedItem;
                    selectedItems.add(compareItem);
                }
            }
        }

        return selectedItems.toArray(new StreamFileCompareItem[selectedItems.size()]);
    }

    private void changeCompareStatus(int newStatus) {

        StreamFileCompareItem[] selectedItems = getSelectedItems();

        for (StreamFileCompareItem compareItem : selectedItems) {
            compareItem.setCompareStatus(newStatus, this.sharedValues.getCompareOptions());
            tableViewer.update(compareItem, null);
        }
        tableViewer.getTable().redraw();
        setButtonEnablementAndDisplayCompareStatus();
    }

    private void performEditIfsFile(StreamFileDescription ifsFileDescription, StreamFileCompareItem parent, Rectangle editorBounds) {

        String connectionName = ifsFileDescription.getConnectionName();
        String filePath = ifsFileDescription.getAbsolutePath();
        String directory = IFSFileHelper.getPathName(filePath);
        String fileName = IFSFileHelper.getFileName(filePath);

        IStreamFileEditor editor = ISpherePlugin.getStreamFileEditor();
        if (editor != null) {
            editor.openEditor(connectionName, directory, fileName, 0, IEditor.EDIT);
            IEditorPart editorPart = editor.findEditorPart(connectionName, directory, fileName);
            if (editorPart != null) {
                editorCloseListener.addIfsFile(editorPart, new WatchedIfsFile(ifsFileDescription, parent));
                if (Preferences.getInstance().isSyncMembersEditorDetached()) {
                    UIHelper.detachEditor(editorPart, editorBounds);
                }
            }
        }
    }

    private void performDisplayIfsFile(StreamFileDescription ifsFileDescription, Rectangle editorBounds) {

        String connectionName = ifsFileDescription.getConnectionName();
        String filePath = ifsFileDescription.getAbsolutePath();
        String directory = IFSFileHelper.getPathName(filePath);
        String fileName = IFSFileHelper.getFileName(filePath);

        IStreamFileEditor editor = ISpherePlugin.getStreamFileEditor();
        if (editor != null) {
            editor.openEditor(connectionName, directory, fileName, 0, IEditor.DISPLAY);
            IEditorPart editorPart = editor.findEditorPart(connectionName, directory, fileName);
            if (editorPart != null) {
                editorCloseListener.addIfsFile(editorPart, new WatchedIfsFile(ifsFileDescription, null));
                if (Preferences.getInstance().isSyncMembersEditorDetached()) {
                    UIHelper.detachEditor(editorPart, editorBounds);
                }
            }
        }
    }

    private Rectangle getEditorBounds(Shell shell, int side) {

        Rectangle drawingArea;

        boolean centerOnScreen = Preferences.getInstance().isSyncMembersCenterOnScreen();
        boolean sideBySide = Preferences.getInstance().isSyncMembersSideBySide();

        Rectangle monitorArea = shell.getMonitor().getClientArea();

        if (centerOnScreen || shell.getMaximized()) {
            drawingArea = shell.getMonitor().getClientArea();
        } else {
            drawingArea = shell.getBounds();
        }

        Rectangle editorBounds = getEditorBoundsInternal(side, monitorArea, drawingArea, sideBySide);

        // Check left and right edges
        int diffX = 0;
        if (side == LEFT) {
            Rectangle leftEditorBounds = editorBounds;
            Rectangle rightEditorBounds = getEditorBoundsInternal(RIGHT, monitorArea, drawingArea, sideBySide);
            if (leftEditorBounds.x < monitorArea.x) {
                diffX = leftEditorBounds.x - monitorArea.x;
            } else if (rightEditorBounds.x + leftEditorBounds.width > monitorArea.width + monitorArea.x) {
                diffX = (rightEditorBounds.x + leftEditorBounds.width) - (monitorArea.width + monitorArea.x);
            }
        } else {
            Rectangle leftEditorBounds = getEditorBoundsInternal(LEFT, monitorArea, drawingArea, sideBySide);
            Rectangle rightEditorBounds = editorBounds;
            if (rightEditorBounds.x + rightEditorBounds.width > monitorArea.width + monitorArea.x) {
                diffX = (rightEditorBounds.x + rightEditorBounds.width) - (monitorArea.width + monitorArea.x);
            } else if (leftEditorBounds.x < monitorArea.x) {
                diffX = leftEditorBounds.x - monitorArea.x;
            }
        }

        editorBounds.x = editorBounds.x - diffX;

        return editorBounds;
    }

    private Rectangle getEditorBoundsInternal(int side, Rectangle monitorArea, Rectangle drawingArea, boolean sideBySide) {

        int editorHeight;
        int editorWidth;
        int editorPosX;
        int editorPosY;

        // Set preferred editor dimensions
        final int maxHeight = (int)Math.abs(drawingArea.height * .8);
        final int maxWidth = (int)Math.abs(drawingArea.width * .8);

        editorHeight = 900;
        if (editorHeight > maxHeight) {
            debug("Max. height exceeded: " + editorHeight);
            editorHeight = maxHeight;
        }
        editorWidth = 800;
        if (editorWidth > maxWidth) {
            debug("Max. width exceeded: " + editorWidth);
            editorWidth = maxWidth;
        }

        int minHeight = 450;
        if (editorHeight < minHeight) {
            debug("Min. height succeeded: " + editorHeight);
            editorHeight = minHeight;
        }
        int minWidth = 600;
        if (editorWidth < minWidth) {
            debug("Min. width succeeded: " + editorWidth);
            editorWidth = minWidth;
        }

        // Ensure editor fits on screen
        final int maxMonitorHeight = (int)Math.abs(monitorArea.height * .9);
        final int maxMonitorWidth = (int)Math.abs(monitorArea.width * .9);

        if (sideBySide) {
            if (editorWidth * 2 > maxMonitorWidth) {
                editorWidth = (int)Math.abs(maxMonitorWidth / 2);
            }
        } else {
            if (editorWidth > maxMonitorWidth) {
                editorWidth = maxMonitorWidth;
            }
        }

        if (editorHeight > maxMonitorHeight) {
            editorHeight = (int)Math.abs(maxMonitorHeight * .9);
        }

        // Position editor on screen
        if (sideBySide) {
            if (side == LEFT) {
                editorPosX = Math.abs(drawingArea.width / 2) - Math.abs(editorWidth / 1);
            } else {
                editorPosX = Math.abs(drawingArea.width / 2);
            }
        } else {
            editorPosX = Math.abs(drawingArea.width / 2) - Math.abs(editorWidth / 2);
        }

        editorPosY = Math.abs(drawingArea.height / 2) - Math.abs(editorHeight / 2);

        // Add monitor offset
        editorPosX = editorPosX + drawingArea.x;
        editorPosY = editorPosY + drawingArea.y;

        return new Rectangle(editorPosX, editorPosY, editorWidth, editorHeight);
    }

    private boolean performDeleteIfsFile(StreamFileDescription ifsFileDescription) {

        try {

            AS400 system = IBMiHostContributionsHandler.getSystem(ifsFileDescription.getConnectionName());
            String ifsFileName = ifsFileDescription.getAbsolutePath();

            IFSFile ifsFile = new IFSFile(system, ifsFileName);
            if (!ifsFile.exists()) {
                // Nothing to do. The item has already been deleted.
                return true;
            }

            if (ifsFile.isDirectory()) {
                String[] items = ifsFile.list();
                if (items != null && items.length > 0) {
                    MessageDialog.openError(getShell(), Messages.E_R_R_O_R, Messages.bind(Messages.Directory_A_is_not_empty, ifsFileName));
                    return false;
                }
            }

            if (!ifsFile.delete()) {
                MessageDialog.openError(getShell(), Messages.E_R_R_O_R, Messages.bind(Messages.Could_not_delete_A, ifsFileName));
                return false;
            }

            return true;

        } catch (Exception e) {
            MessageDialog.openError(getShell(), Messages.E_R_R_O_R, ExceptionHelper.getLocalizedMessage(e));
        }

        return false;
    }

    private void performCompareIfsFiles() {

        final SynchronizeStreamFilesEditorInput editorInput = getEditorInput();

        if (editorInput.getLeftObject() == null) {
            MessageDialog.openError(getShell(), Messages.E_R_R_O_R, Messages.Left_source_file_or_library_is_missing);
            return;
        }

        if (editorInput.getRightObject() == null) {
            MessageDialog.openError(getShell(), Messages.E_R_R_O_R, Messages.Right_source_file_or_library_is_missing);
            return;
        }

        if (editorInput.getLeftObjectName().equals(editorInput.getRightObjectName())) {
            MessageDialog dialog = new MessageDialog(getShell(), Messages.Warning, null, Messages.Warning_Both_sides_show_the_same_stream_file,
                MessageDialog.WARNING, new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
            if (dialog.open() == 1) {
                return;
            }
        }

        tableViewer.setInput(getEditorInput().clearAll());

        if (isSynchronizationEnabled()) {
            setDisplayErrorsOnly(false);
        }

        setIsComparing(true);
        setButtonEnablementAndDisplayCompareStatus();

        sharedValues = createSharedValues();

        jobToCancel = new CompareStreamFilesJob(getEditorInput(), sharedValues, this);
        setButtonEnablementAndDisplayCompareStatus();
        jobToCancel.schedule();
    }

    public void compareIfsFilesPostRun(boolean isCanceled, StreamFileDescription[] leftIfsFileDescriptions,
        StreamFileDescription[] rightIfsFileDescriptions) {

        if (isCanceled) {
            getEditorInput().setLeftIfsFileDescriptions(new StreamFileDescription[0]);
            getEditorInput().setRightIfsFileDescriptions(new StreamFileDescription[0]);
        } else {
            getEditorInput().setLeftIfsFileDescriptions(leftIfsFileDescriptions);
            getEditorInput().setRightIfsFileDescriptions(rightIfsFileDescriptions);
        }

        jobToCancel = null;

        EndLoadIfsFilesUIJob job = new EndLoadIfsFilesUIJob(isCanceled);
        job.schedule();
    }

    private CompareStreamFilesSharedJobValues createSharedValues() {

        sharedValues = new CompareStreamFilesSharedJobValues(new CompareOptions());
        updateCompareOptions();

        return sharedValues;
    }

    protected void updateCompareOptions() {

        if (sharedValues == null) {
            return;
        }

        CompareOptions compareOptions = sharedValues.getCompareOptions();

        boolean ignoreDate = chkIgnoreDate.getSelection();
        String ifsFileFilter = cboIfsFileFilter.getText();

        boolean isRegEx;
        if (ifsFileFilter.startsWith(REGEX_MARKER)) {
            isRegEx = true;
            ifsFileFilter = ifsFileFilter.substring(1);
        } else {
            isRegEx = false;
        }

        compareOptions.setIgnoreDate(ignoreDate);
        compareOptions.setIncludeEmptyDirectories(chkEmptyDirectories.getSelection());
        compareOptions.setIfsFileFilter(ifsFileFilter);
        compareOptions.setIsRegEx(isRegEx);

        labelProvider.setCompareOptions(sharedValues.getCompareOptions());
        tableFilter.setCompareOptions(sharedValues.getCompareOptions());
    }

    private void performSynchronizeIfsFiles() {

        synchronizationResult = new SynchronizationResult();

        RemoteStreamFile leftObject = getEditorInput().getLeftObject();
        RemoteStreamFile rightObject = getEditorInput().getRightObject();

        SynchronizeStreamFilesJob synchronizeIfsFilesJob = new SynchronizeStreamFilesJob(leftObject, rightObject, this);
        synchronizeIfsFilesJob.setValidateItemErrorListener(this);
        synchronizeIfsFilesJob.setCopyItemErrorListener(this);
        synchronizeIfsFilesJob.setMissingDirectoryAction(MissingDirectoryAction.ASK_USER);

        if (chkBoxReplace.getSelection()) {
            synchronizeIfsFilesJob.setExistingIfsFileAction(ExistingIfsFileAction.REPLACE);
        } else {
            synchronizeIfsFilesJob.setExistingIfsFileAction(ExistingIfsFileAction.ERROR);
        }

        CompareOptions compareOptions = sharedValues.getCompareOptions();
        for (int i = 0; i < tableViewer.getTable().getItemCount(); i++) {
            StreamFileCompareItem compareItem = (StreamFileCompareItem)tableViewer.getTable().getItem(i).getData();
            if (compareItem.getOriginalCompareStatus(compareOptions) == StreamFileCompareItem.LEFT_MISSING) {
                synchronizeIfsFilesJob.addCopyRightToLeftIfsFile(compareItem);
            } else if (compareItem.getOriginalCompareStatus(compareOptions) == StreamFileCompareItem.RIGHT_MISSING) {
                synchronizeIfsFilesJob.addCopyLeftToRightIfsFile(compareItem);
            }
        }

        if (synchronizeIfsFilesJob.getNumCopyLeftToRight() == 0 && synchronizeIfsFilesJob.getNumCopyRightToLeft() == 0) {
            MessageDialog.openError(getShell(), Messages.E_R_R_O_R, Messages.No_items_selected_for_processing);
            return;
        }

        String leftToRight = Messages.bind(Messages.Copy_A_stream_files_from_left_to_right, synchronizeIfsFilesJob.getNumCopyLeftToRight());
        String rightToLeft = Messages.bind(Messages.Copy_A_stream_files_from_right_to_left, synchronizeIfsFilesJob.getNumCopyRightToLeft());

        if (MessageDialog.openConfirm(getShell(), Messages.Confirmation,
            Messages.Do_you_want_to_start_synchronizing_stream_files + "\n\n" + leftToRight + "\n" + rightToLeft)) { //$NON-NLS-1$ //$NON-NLS-2$
            setIsSynchronizing(true);
            jobToCancel = synchronizeIfsFilesJob;
            setButtonEnablementAndDisplayCompareStatus();
            synchronizeIfsFilesJob.schedule();
        }
    }

    /**
     * IFS file error callback of {@link ValidateStreamFilesJob}.
     * <p>
     * {@inheritDoc}
     */
    public SynchronizeStreamFilesAction reportValidateIfsFileMessage(StreamFileCopyError errorId, CopyStreamFileItem item, String errorMessage) {

        // Directory or connection error.
        if (item == null) {
            return errorId.getDefaultAction();
        }

        debug("ValidateIfsFilesJob -> Validation error: " + item.getFromIfsFile() + " - " + errorMessage);

        final StreamFileCompareItem compareItem = (StreamFileCompareItem)item.getData();

        compareItem.resetErrorStatus();

        if (StreamFileCopyError.ERROR_NONE == errorId) {
            // Nothing to do here.
            // Let the CopyIfsFilesJob decide what to do.
        } else {
            compareItem.setErrorStatus(errorMessage, sharedValues.getCompareOptions());
            synchronizationResult.addErrorMessage(errorMessage);
        }

        synchronizationResult.addDirtyIfsFile(compareItem);

        return errorId.getDefaultAction();
    }

    /**
     * IFS file error callback of {@link CopyIfsFilesJob}.
     * <p>
     * {@inheritDoc}
     */
    public SynchronizeStreamFilesAction reportCopyIfsFileMessage(StreamFileCopyError errorId, CopyStreamFileItem item, String errorMessage) {

        // Directory or connection error.
        if (item == null) {
            return errorId.getDefaultAction();
        }

        if (StreamFileCopyError.ERROR_NONE == errorId) {
            debug("CopyIfsFilesJob -> Copied: " + item.getFromIfsFile());
        } else {
            debug("CopyIfsFilesJob -> Copy error: " + item.getFromIfsFile() + " - " + errorMessage);
        }

        final StreamFileCompareItem compareItem = (StreamFileCompareItem)item.getData();

        compareItem.resetErrorStatus();

        // Update copy status...
        if (StreamFileCopyError.ERROR_NONE == errorId) {
            if (compareItem.getCompareStatus(sharedValues.getCompareOptions()) == StreamFileCompareItem.LEFT_MISSING) {
                compareItem.setLeftIfsFileDescription(
                    produceCopiedIfsFileDescription(compareItem, compareItem.getRightIfsFileDescription(), getEditorInput().getLeftObject()));
            } else if (compareItem.getCompareStatus(sharedValues.getCompareOptions()) == StreamFileCompareItem.RIGHT_MISSING) {
                compareItem.setRightIfsFileDescription(
                    produceCopiedIfsFileDescription(compareItem, compareItem.getLeftIfsFileDescription(), getEditorInput().getRightObject()));
            }
            compareItem.clearCompareStatus();
        } else {
            compareItem.setErrorStatus(errorMessage, sharedValues.getCompareOptions());
            synchronizationResult.addErrorMessage(errorMessage);
        }

        synchronizationResult.addDirtyIfsFile(compareItem);

        return errorId.getDefaultAction();
    }

    /**
     * Produces the description of the item that has just been copied to the
     * other side.
     * <p>
     * The description is a copy of the source description, except for the
     * connection name and the root directory, which are those of the target
     * side. The <i>relative path</i> is taken from the compare item, because
     * that is the path both sides of a compare item are identified by.
     * 
     * @param compareItem - item that has been copied
     * @param fromIfsFileDescription - description of the source item
     * @param toObject - directory the item has been copied to
     * @return description of the copied item
     */
    private StreamFileDescription produceCopiedIfsFileDescription(StreamFileCompareItem compareItem, StreamFileDescription fromIfsFileDescription,
        RemoteStreamFile toObject) {

        StreamFileDescription toIfsFileDescription;
        if (fromIfsFileDescription.isDirectory()) {
            toIfsFileDescription = StreamFileDescription.newDirectoryDescription();
        } else {
            toIfsFileDescription = StreamFileDescription.newFileDescription();
        }

        toIfsFileDescription.setConnectionName(toObject.getConnectionName());
        toIfsFileDescription.setRootDirectory(toObject.getName());
        toIfsFileDescription.setRelativePath(compareItem.getIfsFileName());
        toIfsFileDescription.setLastChangedDate(fromIfsFileDescription.getLastChangedDate());
        toIfsFileDescription.setChecksum(fromIfsFileDescription.getChecksum());

        return toIfsFileDescription;
    }

    /**
     * PostRun of {@link SynchronizeStreamFilesJob}.
     * <p>
     * {@inheritDoc}
     */
    public void synchronizeIfsFilesPostRun(final String status, final int countCopied, final int countErrors, final String message) {

        synchronizationResult.setStatus(status);
        synchronizationResult.setCountCopied(countCopied);
        synchronizationResult.setCountErrors(countErrors);
        synchronizationResult.setJobFinishedMessage(message);

        UIJob uiJob = new EndSynchronisationUIJob(synchronizationResult);
        uiJob.schedule();

        debug("\nSynchronizeIfsFilesEditor.synchronizeIfsFilesPostRun:");
        debug("status:         " + status);
        debug("copied #:       " + countCopied);
        debug("errors #:       " + countErrors);
        debug("message:        " + message);
    }

    protected void performOpenCompareIfsFilesDialog(StreamFileCompareItem compareItem) {

        if (compareItem.isSingle()) {
            return;
        }

        try {

            StreamFileDescription leftIfsFileDescription = compareItem.getLeftIfsFileDescription();
            StreamFileDescription rightIfsFileDescription = compareItem.getRightIfsFileDescription();

            StreamFile leftIfsFile = createRemoteObject(leftIfsFileDescription);
            StreamFile rightIfsFile = createRemoteObject(rightIfsFileDescription);

            SourceMemberCompareEditorConfiguration cc = new SourceMemberCompareEditorConfiguration();
            cc.setIgnoreCase(false);
            cc.setIgnoreChangesLeft(false);
            cc.setIgnoreChangesRight(false);
            cc.setConsiderDate(false);
            cc.setThreeWay(false);
            cc.setLeftEditable(false);
            cc.setRightEditable(false);
            cc.setLeftLabel(createLabel(leftIfsFile));
            cc.setRightLabel(createLabel(rightIfsFile));
            cc.setOpenInEditor(false);
            cc.setShowDialog(false);

            List<StreamFile> streamFiles = new LinkedList<StreamFile>();
            streamFiles.add(leftIfsFile);
            streamFiles.add(rightIfsFile);

            // TODO: synchronize IFS files: open editor without dialog
            IBMiHostContributionsHandler.compareStreamFiles(streamFiles, cc);

        } catch (Throwable e) {
            ISpherePlugin.logError("*** Could not open stream file compare editor ***", e); //$NON-NLS-1$
        }
    }

    private String createLabel(StreamFile ifsFile) {

        StringBuilder buffer = new StringBuilder();

        buffer.append(ifsFile.getStreamFile());

        return buffer.toString();
    }

    private StreamFile createRemoteObject(StreamFileDescription ifsFileDescription) throws Exception {

        String connectionName = ifsFileDescription.getConnectionName();
        String filePath = ifsFileDescription.getAbsolutePath();

        StreamFile remoteIfsFile = IBMiHostContributionsHandler.getStreamFile(connectionName, filePath);

        return remoteIfsFile;
    }

    private void performCancelOperation() {

        if (jobToCancel != null) {
            jobToCancel.cancelOperation();
        }
    }

    private void setDisplayErrorsOnly(boolean enabled) {
        chkDisplayErrorsOnly.setSelection(enabled);
        refreshTableFilter();
    }

    @Override
    public void dispose() {

        if (jobToCancel != null) {
            jobToCancel.cancelOperation();
        }

        super.dispose();
    }

    private Image getImage(String name) {

        Image image = ISpherePlugin.getDefault().getImage(name);

        return image;
    }

    protected abstract RemoteStreamFile performSelectRemoteObject(String connectionName, String objectName);

    protected abstract AbstractTableLabelProvider getTableLabelProvider(TableViewer tableViewer);

    private class WatchedIfsFile {

        private StreamFileDescription ifsFileDescription;
        private StreamFileCompareItem parent;

        public WatchedIfsFile(StreamFileDescription ifsFileDescription, StreamFileCompareItem parent) {
            this.ifsFileDescription = ifsFileDescription;
            this.parent = parent;
        }

        public StreamFileDescription getIfsFileDescription() {
            return ifsFileDescription;
        }

        public StreamFileCompareItem getParent() {
            return parent;
        }
    }

    private class EditorCloseListener implements IPartListener2 {

        private AbstractSynchronizeStreamFilesEditor owner;
        Map<IEditorPart, WatchedIfsFile> ifsFiles;

        public EditorCloseListener(AbstractSynchronizeStreamFilesEditor editor) {
            this.owner = editor;
            this.ifsFiles = new HashMap<IEditorPart, WatchedIfsFile>();
        }

        public void addIfsFile(IEditorPart editorPart, WatchedIfsFile watchedIfsFile) {
            ifsFiles.put(editorPart, watchedIfsFile);
            debug("IFS file added: " + watchedIfsFile.getIfsFileDescription().getQualifiedIfsFileName());
        }

        public void removeIfsFile(IEditorPart editorPart) {
            WatchedIfsFile watchedIfsFile = ifsFiles.remove(editorPart);
            debug("IFS file removed: " + watchedIfsFile.getIfsFileDescription().getQualifiedIfsFileName());
        }

        public void partClosed(IWorkbenchPartReference partRef) {

            IWorkbenchPart closedPart = partRef.getPart(false);
            if (closedPart == owner) {
                debug("Closing synchronize IFS files editor.");
                unregisterEditorListener(this);
                for (Object entry : ifsFiles.keySet().toArray()) {
                    final IEditorPart editorPart = (IEditorPart)entry;
                    // Schedule closing the stream file editor, because
                    // otherwise DdsDocumentListener.partClosed() throws a NPE
                    // on line 934 in RDi 9.8.0.7.
                    // partClosed() is called twice and this._editor is null on
                    // the second call, as it was set to null on the first call.
                    new UIJob("") {
                        @Override
                        public IStatus runInUIThread(IProgressMonitor arg0) {
                            debug("Closing edit stream file popup editor.");
                            closeEditor(editorPart);
                            return Status.OK_STATUS;
                        }
                    }.schedule();
                }
            } else {
                if (closedPart instanceof IEditorPart) {
                    debug("Closing edit stream file popup editor.");
                    IEditorPart closedEditorPart = (IEditorPart)closedPart;
                    WatchedIfsFile watchedIfsFile = ifsFiles.get(closedEditorPart);
                    updateAndRemoveWatchedIfsFile(closedEditorPart, watchedIfsFile);
                }
            }
        }

        private void closeEditor(IEditorPart editorPart) {
            editorPart.getEditorSite().getPage().closeEditor(editorPart, true);
        }

        private void updateAndRemoveWatchedIfsFile(IEditorPart closedEditorPart, WatchedIfsFile watchedIfsFile) {
            if (watchedIfsFile != null) {
                StreamFileDescription ifsFileDescription = watchedIfsFile.getIfsFileDescription();
                StreamFileCompareItem ifsCompareItem = watchedIfsFile.getParent();
                if (ifsCompareItem != null) {
                    performUpdateIfsFileDescription(ifsFileDescription, ifsCompareItem);
                }
                removeIfsFile(closedEditorPart);
            }
        }

        private void performUpdateIfsFileDescription(StreamFileDescription ifsFileDescription, StreamFileCompareItem parent) {

            String connectionName = ifsFileDescription.getConnectionName();
            AS400 system = IBMiHostContributionsHandler.getSystem(connectionName);
            String iSphereLibrary = ISpherePlugin.getISphereLibrary(connectionName);

            if (ISphereHelper.checkISphereLibrary(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), connectionName)) {

                String currentLibrary = null;
                try {
                    currentLibrary = ISphereHelper.getCurrentLibrary(system);
                } catch (Exception e) {
                    ISpherePlugin.logError("*** Could not retrieve current library ***", e); //$NON-NLS-1$
                }

                if (currentLibrary != null) {

                    try {

                        boolean ok = false;
                        try {
                            ok = ISphereHelper.setCurrentLibrary(system, iSphereLibrary);
                        } catch (Exception e) {
                            ISpherePlugin.logError("Could not set current library to: " + iSphereLibrary, e); //$NON-NLS-1$
                        }

                        if (ok) {
                            String file = ifsFileDescription.getAbsolutePath();
                            IfsFileAttributes ifsFileAttributes = new SYNCIFS_retrieveItemAttributes().run(system, file);
                            if (ifsFileAttributes != null) {
                                ifsFileDescription.setLastChangedDate(ifsFileAttributes.getLastChanged());
                                ifsFileDescription.setChecksum(ifsFileAttributes.getCheckSum());
                                parent.clearCompareStatus();
                                owner.tableViewer.refresh(parent);
                                debug("IFS file updated: " + ifsFileDescription.getQualifiedIfsFileName());
                            }
                        }

                    } finally {
                        try {
                            ISphereHelper.setCurrentLibrary(system, currentLibrary);
                        } catch (Exception e) {
                            ISpherePlugin.logError("Could not restore current library to: " + currentLibrary, e); //$NON-NLS-1$
                        }
                    }

                }
            }
        }

        public void partActivated(IWorkbenchPartReference arg0) {
        }

        public void partBroughtToTop(IWorkbenchPartReference arg0) {
        }

        public void partDeactivated(IWorkbenchPartReference arg0) {
        }

        public void partHidden(IWorkbenchPartReference arg0) {
        }

        public void partInputChanged(IWorkbenchPartReference arg0) {
        }

        public void partOpened(IWorkbenchPartReference arg0) {
        }

        public void partVisible(IWorkbenchPartReference arg0) {
        }
    }

    private class EndLoadIfsFilesUIJob extends UIJob {

        private boolean isCanceled;

        public EndLoadIfsFilesUIJob(boolean isCanceled) {
            super(Messages.EMPTY);
            this.isCanceled = isCanceled;
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor arg0) {

            if (tableViewer.getTable().isDisposed()) {
                return Status.OK_STATUS;
            }

            tableViewer.setInput(getEditorInput());
            setIsComparing(false);
            setButtonEnablementAndDisplayCompareStatus();

            if (isCanceled) {
                MessageDialogAsync.displayNonBlockingInformation(getShell(), Messages.Operation_has_been_canceled_by_the_user);
            }

            return Status.OK_STATUS;
        }
    }

    private class EndSynchronisationUIJob extends UIJob {

        private SynchronizationResult result;

        public EndSynchronisationUIJob(SynchronizationResult result) {
            super(Messages.EMPTY);

            this.result = result;
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {

            debug("-- End synchronisation job: --");

            debug("Copied #: " + result.getCountCopied());
            debug("Errors #: " + result.getCountErrors());

            debug("Errors:");
            String[] errorMessages = synchronizationResult.getErrorMesssages();
            for (String message : errorMessages) {
                debug("   " + message);
            }

            if (!ISynchronizeStreamFilesPostRun.OK.equals(result.getStatus())) {
                MessageDialogAsync.displayNonBlockingError(getShell(), result.getJobFinishedMessage());
            } else {
                MessageDialogAsync.displayNonBlockingInformation(getShell(), result.getJobFinishedMessage());
            }

            if (tableViewer.getTable().isDisposed()) {
                return Status.OK_STATUS;
            }

            jobToCancel = null;
            setIsSynchronizing(false);
            setButtonEnablementAndDisplayCompareStatus();

            if (isSynchronizationEnabled()) {

                if (result.getCountErrors() == 0 && chkCompareAfterSync.getSelection()) {
                    performCompareIfsFiles();
                } else {
                    if (synchronizationResult.hasDirtyIfsFiles()) {
                        try {
                            tableViewer.getTable().setRedraw(false);
                            for (StreamFileCompareItem ifsFileCompareItem : synchronizationResult.getDirtyIfsFiles()) {
                                tableViewer.refresh(ifsFileCompareItem);
                            }
                        } finally {
                            tableViewer.getTable().setRedraw(true);
                        }
                    }
                }
            }

            synchronizationResult = null;

            return Status.OK_STATUS;
        }
    }

    /**
     * Class that implements the context menu for the table rows.
     */
    private class TableContextMenu extends MenuAdapter {

        private Menu parent;
        private ISynchronizeStreamFilesEditorConfiguration configuration;

        private MenuItem menuItemRemoveSelection;
        private MenuItem menuItemSelectForCopyingToTheRight;
        private MenuItem menuItemSelectForCopyingToTheLeft;
        private MenuItem menuItemEditLeft;
        private MenuItem menuItemEditRight;
        private MenuItem menuItemCompareLeftAndRight;
        private MenuItem menuItemSeparator;
        private MenuItem menuItemDeleteLeft;
        private MenuItem menuItemDeleteRight;

        public TableContextMenu(Menu parent, ISynchronizeStreamFilesEditorConfiguration configuration) {
            this.parent = parent;
            this.configuration = configuration;
        }

        @Override
        public void menuShown(MenuEvent event) {
            expandDirectorySelection();
            destroyMenuItems();
            createMenuItems();
        }

        private void destroyMenuItems() {
            if (!((menuItemRemoveSelection == null) || (menuItemRemoveSelection.isDisposed()))) {
                menuItemRemoveSelection.dispose();
            }
            if (!((menuItemSelectForCopyingToTheRight == null) || (menuItemSelectForCopyingToTheRight.isDisposed()))) {
                menuItemSelectForCopyingToTheRight.dispose();
            }
            if (!((menuItemSelectForCopyingToTheLeft == null) || (menuItemSelectForCopyingToTheLeft.isDisposed()))) {
                menuItemSelectForCopyingToTheLeft.dispose();
            }
            if (!((menuItemEditLeft == null) || (menuItemEditLeft.isDisposed()))) {
                menuItemEditLeft.dispose();
            }
            if (!((menuItemEditRight == null) || (menuItemEditRight.isDisposed()))) {
                menuItemEditRight.dispose();
            }
            if (!((menuItemCompareLeftAndRight == null) || (menuItemCompareLeftAndRight.isDisposed()))) {
                menuItemCompareLeftAndRight.dispose();
            }
            if (!((menuItemSeparator == null) || (menuItemSeparator.isDisposed()))) {
                menuItemSeparator.dispose();
            }
            if (!((menuItemDeleteLeft == null) || (menuItemDeleteLeft.isDisposed()))) {
                menuItemDeleteLeft.dispose();
            }
            if (!((menuItemDeleteRight == null) || (menuItemDeleteRight.isDisposed()))) {
                menuItemDeleteRight.dispose();
            }
        }

        private void createMenuItems() {

            if (tableViewer.getTable().getItems().length <= 0) {
                return;
            }

            boolean isLeftEditorEnabled = configuration.isLeftEditorEnabled();
            boolean isRightEditorEnabled = configuration.isRightEditorEnabled();

            createMenuItemRemoveSelection();

            if (isLeftEditorEnabled) createMenuItemSelectForCopyingToTheLeft(getTheSelectedItem());
            if (isRightEditorEnabled) createMenuItemSelectForCopyingToTheRight(getTheSelectedItem());

            createMenuItemEditLeft(getTheSelectedItem(), isLeftEditorEnabled);
            createMenuItemEditRight(getTheSelectedItem(), isRightEditorEnabled);

            createMenuItemCompareLeftAndRight(getTheSelectedItem());

            if (isLeftEditorEnabled || isRightEditorEnabled) createMenuItemSeparator();

            if (isLeftEditorEnabled) createMenuItemDeleteLeft(getTheSelectedItem());
            if (isRightEditorEnabled) createMenuItemDeleteRight(getTheSelectedItem());
        }

        private void createMenuItemSeparator() {
            menuItemSeparator = new MenuItem(parent, SWT.SEPARATOR);
        }

        private void createMenuItemRemoveSelection() {
            menuItemRemoveSelection = new MenuItem(parent, SWT.NONE);
            menuItemRemoveSelection.setText(Messages.Remove_selection);
            menuItemRemoveSelection.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    changeCompareStatus(StreamFileCompareItem.NO_ACTION);
                }
            });
        }

        private void createMenuItemSelectForCopyingToTheLeft(StreamFileCompareItem compareItem) {
            menuItemSelectForCopyingToTheLeft = new MenuItem(parent, SWT.NONE);
            menuItemSelectForCopyingToTheLeft.setText(Messages.Select_for_copying_right_to_left);
            menuItemSelectForCopyingToTheLeft.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    changeCompareStatus(StreamFileCompareItem.LEFT_MISSING);
                }
            });

            if (compareItem != null && compareItem.getRightIfsFileDescription() == null) {
                menuItemSelectForCopyingToTheLeft.setEnabled(false);
            }
        }

        private void createMenuItemSelectForCopyingToTheRight(StreamFileCompareItem compareItem) {
            menuItemSelectForCopyingToTheRight = new MenuItem(parent, SWT.NONE);
            menuItemSelectForCopyingToTheRight.setText(Messages.Select_for_copying_left_to_right);
            menuItemSelectForCopyingToTheRight.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    changeCompareStatus(StreamFileCompareItem.RIGHT_MISSING);
                }
            });

            if (compareItem != null && compareItem.getLeftIfsFileDescription() == null) {
                menuItemSelectForCopyingToTheRight.setEnabled(false);
            }
        }

        private void createMenuItemEditLeft(StreamFileCompareItem compareItem, final boolean isEditable) {

            String label;
            if (isEditable) {
                label = Messages.Edit_left;
            } else {
                label = Messages.Display_left;
            }

            menuItemEditLeft = new MenuItem(parent, SWT.NONE);
            menuItemEditLeft.setText(label);
            menuItemEditLeft.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    performEditIfsFile(LEFT, isEditable);
                }
            });

            if (isDirectoryItem(getFirstSelectedItem()) || (compareItem != null && compareItem.getLeftIfsFileDescription() == null)) {
                menuItemEditLeft.setEnabled(false);
            }
        }

        private void createMenuItemEditRight(StreamFileCompareItem compareItem, final boolean isEditable) {

            String label;
            if (isEditable) {
                label = Messages.Edit_right;
            } else {
                label = Messages.Display_right;
            }

            menuItemEditRight = new MenuItem(parent, SWT.NONE);
            menuItemEditRight.setText(label);
            menuItemEditRight.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    performEditIfsFile(RIGHT, isEditable);
                }
            });

            if (isDirectoryItem(getFirstSelectedItem()) || (compareItem != null && compareItem.getRightIfsFileDescription() == null)) {
                menuItemEditRight.setEnabled(false);
            }
        }

        private void createMenuItemCompareLeftAndRight(StreamFileCompareItem compareItem) {
            menuItemCompareLeftAndRight = new MenuItem(parent, SWT.NONE);
            menuItemCompareLeftAndRight.setText(Messages.Compare_left_AND_right);
            menuItemCompareLeftAndRight.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    performOpenCompareIfsFilesDialog(getTheSelectedItem());
                }
            });

            if (compareItem == null || compareItem.getLeftIfsFileDescription() == null || compareItem.getRightIfsFileDescription() == null) {
                menuItemCompareLeftAndRight.setEnabled(false);
            }
        }

        private void createMenuItemDeleteLeft(StreamFileCompareItem compareItem) {
            menuItemDeleteLeft = new MenuItem(parent, SWT.NONE);
            menuItemDeleteLeft.setText(Messages.Delete_left);
            menuItemDeleteLeft.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    performDeleteIfsFile(LEFT);
                }
            });

            if (compareItem != null && compareItem.getLeftIfsFileDescription() == null) {
                menuItemDeleteLeft.setEnabled(false);
            }
        }

        private void createMenuItemDeleteRight(StreamFileCompareItem compareItem) {
            menuItemDeleteRight = new MenuItem(parent, SWT.NONE);
            menuItemDeleteRight.setText(Messages.Delete_right);
            menuItemDeleteRight.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    performDeleteIfsFile(RIGHT);
                }
            });

            if (compareItem != null && compareItem.getRightIfsFileDescription() == null) {
                menuItemDeleteRight.setEnabled(false);
            }
        }

        private StreamFileCompareItem getTheSelectedItem() {

            StreamFileCompareItem[] selectedItems = getSelectedItems();
            if (selectedItems.length == 1) {
                return selectedItems[0];
            }

            return null;
        }

        /**
         * Returns the first of the currently selected items (in table row
         * order), regardless of how many items are selected, or
         * <code>null</code> if nothing is selected.
         */
        private StreamFileCompareItem getFirstSelectedItem() {

            StreamFileCompareItem[] selectedItems = getSelectedItems();
            if (selectedItems.length > 0) {
                return selectedItems[0];
            }

            return null;
        }

        private void performEditIfsFile(int side, boolean isEditable) {

            StreamFileCompareItem[] selectedItems = getSelectedItems();
            for (StreamFileCompareItem selectedItem : selectedItems) {

                Rectangle editorBounds = getEditorBounds(getShell(), side);

                if (side == LEFT) {
                    if (isEditable) {
                        StreamFileDescription leftIfsFileDescription = selectedItem.getLeftIfsFileDescription();
                        AbstractSynchronizeStreamFilesEditor.this.performEditIfsFile(leftIfsFileDescription, selectedItem, editorBounds);
                    } else {
                        AbstractSynchronizeStreamFilesEditor.this.performDisplayIfsFile(selectedItem.getLeftIfsFileDescription(), editorBounds);
                    }
                } else if (side == RIGHT) {
                    if (isEditable) {
                        StreamFileDescription rightIfsFileDescription = selectedItem.getRightIfsFileDescription();
                        AbstractSynchronizeStreamFilesEditor.this.performEditIfsFile(rightIfsFileDescription, selectedItem, editorBounds);
                    } else {
                        AbstractSynchronizeStreamFilesEditor.this.performDisplayIfsFile(selectedItem.getRightIfsFileDescription(), editorBounds);
                    }
                }
            }
        }

        private void performDeleteIfsFile(int side) {

            boolean isYesToAll = false;

            StreamFileCompareItem[] selectedItems = getSelectedItems();
            for (StreamFileCompareItem selectedItem : selectedItems) {

                StreamFileDescription IfsFileDescription;
                String qualifiedConnectionName;
                String qualifiedIfsFileName;

                StringBuilder confirmationMessage = new StringBuilder();

                if (side == LEFT) {
                    IfsFileDescription = selectedItem.getLeftIfsFileDescription();
                    if (IfsFileDescription == null) {
                        continue;
                    }
                    qualifiedConnectionName = new BasicQualifiedConnectionName(IfsFileDescription.getConnectionName()).getUIConnectionName();
                    qualifiedIfsFileName = IfsFileDescription.getQualifiedIfsFileName();
                    confirmationMessage.append("<-- "); //$NON-NLS-1$
                    confirmationMessage.append(Messages.Delete_left_stream_file_colon);
                } else if (side == RIGHT) {
                    IfsFileDescription = selectedItem.getRightIfsFileDescription();
                    if (IfsFileDescription == null) {
                        continue;
                    }
                    qualifiedConnectionName = new BasicQualifiedConnectionName(IfsFileDescription.getConnectionName()).getUIConnectionName();
                    qualifiedIfsFileName = IfsFileDescription.getQualifiedIfsFileName();
                    confirmationMessage.append(Messages.Delete_right_stream_file_colon);
                    confirmationMessage.append(" -->"); //$NON-NLS-1$
                } else {
                    throw new IllegalArgumentException("Unexpected value in 'side': " + side);
                }

                confirmationMessage.append("\n"); //$NON-NLS-1$
                confirmationMessage.append("\n"); //$NON-NLS-1$
                confirmationMessage.append(qualifiedConnectionName);
                confirmationMessage.append("\n"); //$NON-NLS-1$
                confirmationMessage.append(qualifiedIfsFileName);

                int rc;
                if (isYesToAll) {
                    rc = IDialogConstants.YES_TO_ALL_ID;
                } else {
                    ConfirmationMessageDialog dialog = new ConfirmationMessageDialog(getShell(), confirmationMessage.toString());
                    rc = dialog.open();
                }

                if (rc == IDialogConstants.YES_TO_ALL_ID) {
                    rc = IDialogConstants.YES_ID;
                    isYesToAll = true;
                }

                if (rc == IDialogConstants.YES_ID) {

                    if (AbstractSynchronizeStreamFilesEditor.this.performDeleteIfsFile(IfsFileDescription)) {
                        if (side == LEFT) {
                            selectedItem.setLeftIfsFileDescription(null);
                        } else {
                            selectedItem.setRightIfsFileDescription(null);
                        }
                        selectedItem.clearCompareStatus();
                        tableViewer.refresh(selectedItem);
                    }
                } else {
                    break;
                }
            }
        }
    }

    public void setStatusLine(StatusLine statusLine) {
        this.statusLine = statusLine;
    }

    public void updateActionsStatusAndStatusLine() {
        updateStatusLine();
    }

    private void updateStatusLine() {

        if (statusLine == null) {
            return;
        }

        statusLine.setShowNumItems(true);
        statusLine.setShowMessage(true);

        if (statusLine != null) {
            statusLine.setMessage(statusMessage);
            statusLine.setNumItems(numFilteredItems);
        }
    }

    private void debug(String message) {
        // System.out.println(message);
    }
}
