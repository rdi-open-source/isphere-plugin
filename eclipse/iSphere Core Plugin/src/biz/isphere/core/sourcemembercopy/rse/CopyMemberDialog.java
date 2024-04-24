/*******************************************************************************
 * Copyright (c) 2012-2024 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.sourcemembercopy.rse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.progress.UIJob;

import com.ibm.as400.access.AS400;

import biz.isphere.base.internal.StringHelper;
import biz.isphere.base.jface.dialogs.XDialog;
import biz.isphere.base.swt.widgets.UpperCaseOnlyVerifier;
import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.Messages;
import biz.isphere.core.ibmi.contributions.extension.handler.IBMiHostContributionsHandler;
import biz.isphere.core.internal.ISphereHelper;
import biz.isphere.core.internal.Validator;
import biz.isphere.core.memberrename.rules.IMemberRenamingRule;
import biz.isphere.core.preferences.Preferences;
import biz.isphere.core.sourcemembercopy.Columns;
import biz.isphere.core.sourcemembercopy.CopyMemberItem;
import biz.isphere.core.sourcemembercopy.CopyMemberItemTableCellModifier;
import biz.isphere.core.sourcemembercopy.ErrorContext;
import biz.isphere.core.sourcemembercopy.IValidateItemMessageListener;
import biz.isphere.core.sourcemembercopy.IValidateMembersPostRun;
import biz.isphere.core.sourcemembercopy.MemberCopyError;
import biz.isphere.core.sourcemembercopy.SynchronizeMembersAction;
import biz.isphere.core.sourcemembercopy.ValidateMembersJob;
import biz.isphere.core.swt.widgets.WidgetFactory;
import biz.isphere.core.swt.widgets.connectioncombo.ConnectionCombo;
import biz.isphere.core.swt.widgets.tableviewer.TableViewerKeyBoardSupporter;
import biz.isphere.core.swt.widgets.tableviewer.TooltipProvider;

public class CopyMemberDialog extends XDialog implements IValidateItemMessageListener, IValidateMembersPostRun {

    public static String FROMFILE = "*FROMFILE";
    private static final String SINGLE_QUOTE = "'";

    private static int BUTTON_COPY_ID = IDialogConstants.OK_ID;
    private static int BUTTON_RESET_ID = IDialogConstants.RETRY_ID;
    private static int BUTTON_CLOSE_CANCEL = IDialogConstants.CANCEL_ID;

    private CopyMemberService copyMemberService;
    private ValidateMembersJob validateMembersJob;

    private ConnectionCombo comboToConnection;
    private Combo comboToFile;
    private Text textToLibrary;
    private TableViewer tableViewer;
    private Button chkBoxError;
    private Button chkBoxReplace;
    private Button chkBoxRename;
    private Link lnkPreferences;
    private Button chkBoxIgnoreDataLostError;
    private Button chkBoxIgnoreDirtyFilesError;
    private Label labelNumElem;

    private Composite mainArea;

    public CopyMemberDialog(Shell parentShell) {
        super(parentShell);
    }

    public void setContent(CopyMemberService jobDescription) {

        this.copyMemberService = jobDescription;

        setControlEnablement();
    }

    @Override
    protected void okPressed() {

        storeScreenValues();

        validateUserInputAndPerformCopyOperation();
    }

    @Override
    protected void cancelPressed() {

        if (isValidating()) {
            validateMembersJob.cancelOperation();
            return;
        }

        if (isCopying()) {
            copyMemberService.cancel();
            return;
        }

        storeScreenValues();

        super.cancelPressed();
    }

    protected boolean canHandleShellCloseEvent() {

        boolean canCloseDialog;

        if (isValidating()) {
            canCloseDialog = false;
        } else if (copyMemberService != null && copyMemberService.isActive()) {
            canCloseDialog = false;
        } else {
            canCloseDialog = true;
        }

        if (!canCloseDialog) {
            MessageDialog.openInformation(getShell(), Messages.E_R_R_O_R, Messages.Operation_in_progress_Cannot_close_dialog);
        }

        return canCloseDialog;
    }

    private boolean isValidating() {

        if (validateMembersJob != null) {
            return true;
        }

        return false;
    }

    private boolean isCopying() {

        if (copyMemberService != null && copyMemberService.isActive()) {
            return true;
        }

        return false;
    }

    private ExistingMemberAction getExistingMemberAction() {

        if (chkBoxReplace.getSelection()) {
            return ExistingMemberAction.REPLACE;
        } else if (chkBoxRename.getSelection()) {
            return ExistingMemberAction.RENAME;
        } else {
            return ExistingMemberAction.ERROR;
        }
    }

    private void validateUserInputAndPerformCopyOperation() {

        tableViewer.setSelection(null);

        String qualifiedConnectionName = getToConnectionName();
        String connectionNameUI = comboToConnection.getText();
        String libraryName = getToLibraryName();
        String fileName = getToFileName();
        int ccsid = copyMemberService.getToConnectionCcsid();

        if (!isConnectionValid(qualifiedConnectionName, connectionNameUI)) {
            return;
        }

        if (!isLibraryValid(qualifiedConnectionName, libraryName, ccsid)) {
            return;
        }

        if (!isFileValid(qualifiedConnectionName, libraryName, fileName, ccsid)) {
            return;
        }

        boolean isRenameMemberCheck = Preferences.getInstance().isMemberRenamingPrecheck();

        copyMemberService.setToConnection(getToConnectionName());
        copyMemberService.setExistingMemberAction(getExistingMemberAction());
        copyMemberService.setMissingFileAction(MissingFileAction.ERROR);
        copyMemberService.setIgnoreDataLostError(chkBoxIgnoreDataLostError.getSelection());
        copyMemberService.setIgnoreUnsavedChanges(chkBoxIgnoreDirtyFilesError.getSelection());
        copyMemberService.setFullErrorCheck(false);
        copyMemberService.setRenameMemberCheck(isRenameMemberCheck);

        String fromConnectionName = copyMemberService.getFromConnectionName();
        CopyMemberItem[] fromMembers = copyMemberService.getItems();
        updateMembersWithTargetSourceFile(getToLibraryName(), getToFileName(), fromMembers);

        validateMembersJob = new ValidateMembersJob(fromConnectionName, fromMembers, this);
        validateMembersJob.addItemErrorListener(this);
        validateMembersJob.setToConnectionName(getToConnectionName());

        validateMembersJob.setExistingMemberAction(getExistingMemberAction());
        validateMembersJob.setMissingFileAction(MissingFileAction.ERROR);
        validateMembersJob.setIgnoreDataLostError(chkBoxIgnoreDataLostError.getSelection());
        validateMembersJob.setIgnoreUnsavedChanges(chkBoxIgnoreDirtyFilesError.getSelection());
        validateMembersJob.setFullErrorCheck(false);
        validateMembersJob.setRenameMemberCheck(isRenameMemberCheck);

        setControlEnablement();
        validateMembersJob.schedule();
    }

    private boolean isConnectionValid(String qualifiedConnectionName, String connectionNameUI) {

        Set<String> connectionNames = new HashSet<String>(Arrays.asList(IBMiHostContributionsHandler.getConnectionNames()));
        boolean hasConnection = connectionNames.contains(qualifiedConnectionName);

        if (!hasConnection) {
            String message = Messages.bind(Messages.Connection_A_not_found, connectionNameUI);
            setErrorMessage(message);
            comboToConnection.setFocus();
        }

        return hasConnection;
    }

    private boolean isLibraryValid(String qualifiedConnectionName, String libraryName, int ccsid) {

        Validator nameValidator = Validator.getNameInstance(ccsid);
        if (!nameValidator.validate(libraryName)) {
            String message = Messages.bind(Messages.Invalid_library_name, libraryName);
            setErrorMessage(message);
            textToLibrary.setFocus();
            return false;
        }

        AS400 system = IBMiHostContributionsHandler.getSystem(qualifiedConnectionName);
        boolean isLibrary = ISphereHelper.checkLibrary(system, libraryName);

        if (!isLibrary) {
            String message = Messages.bind(Messages.Library_A_not_found, libraryName);
            setErrorMessage(message);
            textToLibrary.setFocus();
        }

        return isLibrary;
    }

    private boolean isFileValid(String qualifiedConnectionName, String libraryName, String fileName, int ccsid) {

        Validator nameValidator = Validator.getNameInstance(ccsid);
        if (!nameValidator.validate(fileName)) {
            String message = Messages.bind(Messages.Invalid_file_name, libraryName);
            setErrorMessage(message);
            comboToFile.setFocus();
            return false;
        }

        AS400 system = IBMiHostContributionsHandler.getSystem(qualifiedConnectionName);
        boolean isFile = ISphereHelper.checkFile(system, libraryName, fileName);

        if (!isFile) {
            String message = Messages.bind(Messages.File_A_not_found, fileName);
            setErrorMessage(message);
            comboToFile.setFocus();
        }

        return isFile;
    }

    public void updateMembersWithTargetSourceFile(String toLibraryName, String toFileName, CopyMemberItem[] fromMembers) {

        for (CopyMemberItem member : fromMembers) {
            member.setToLibrary(toLibraryName);
            if (FROMFILE.equals(toFileName)) {
                member.setToFile(member.getFromFile());
            } else {
                member.setToFile(toFileName);
            }
            member.setErrorMessage(null);
        }

    }

    public SynchronizeMembersAction reportValidateFileMessage(MemberCopyError errorId, ErrorContext errorContext, String errorMessage) {
        return SynchronizeMembersAction.CANCEL;
    }

    public SynchronizeMembersAction reportValidateMemberMessage(MemberCopyError errorId, CopyMemberItem item, String errorMessage) {
        return SynchronizeMembersAction.CONTINUE_WITH_ERROR;
    }

    public void returnValidateMembersResult(final boolean isCanceled, final int countTotal, final int countSkipped, final int countValidated,
        final int countErrors, final long averageTime, final MemberCopyError errorId, final String cancelMessage) {

        validateMembersJob = null;

        new UIJob(Messages.EMPTY) {

            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {

                setControlEnablement();

                if (cancelMessage != null) {

                    if (errorId == MemberCopyError.ERROR_TO_CONNECTION_NOT_FOUND) {
                        setErrorMessage(cancelMessage);
                        comboToConnection.setFocus();
                    } else if (errorId == MemberCopyError.ERROR_TO_LIBRARY_NAME_NOT_VALID) {
                        setErrorMessage(cancelMessage);
                        textToLibrary.setFocus();
                    } else if (errorId == MemberCopyError.ERROR_TO_LIBRARY_NOT_FOUND) {
                        setErrorMessage(cancelMessage);
                        textToLibrary.setFocus();
                    } else if (errorId == MemberCopyError.ERROR_TO_FILE_NAME_NOT_VALID) {
                        setErrorMessage(cancelMessage);
                        comboToFile.setFocus();
                    } else if (errorId == MemberCopyError.ERROR_TO_FILE_NOT_FOUND) {
                        setErrorMessage(cancelMessage);
                        comboToFile.setFocus();
                    } else if (errorId == MemberCopyError.ERROR_TO_FILE_DATA_LOST) {
                        setErrorMessage(cancelMessage);
                        comboToFile.setFocus();
                    }

                } else {
                    if (isCanceled) {
                        setErrorMessage(Messages.Operation_has_been_canceled_by_the_user);
                        comboToFile.setFocus();
                    } else {
                        setErrorMessage(null);
                        copyMemberService.execute();
                    }
                }

                return Status.OK_STATUS;
            }
        }.schedule();
    }

    @Override
    public boolean close() {

        return super.close();
    }

    /**
     * Overridden to set the window title.
     */
    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.Copy_Members_headline);
    }

    @Override
    protected Control createDialogArea(Composite parent) {

        mainArea = new Composite(parent, SWT.NONE);
        mainArea.setLayout(new GridLayout(4, false));
        mainArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label labelToConnection = new Label(mainArea, SWT.NONE);
        labelToConnection.setText(Messages.To_connection_colon);

        comboToConnection = WidgetFactory.createConnectionCombo(mainArea);
        comboToConnection.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));
        comboToConnection.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setControlEnablement();
            }
        });

        // Create spacer
        new Label(mainArea, SWT.NONE).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label textInfo = new Label(mainArea, SWT.NONE);
        textInfo.setAlignment(SWT.RIGHT);
        textInfo.setLayoutData(new GridData(SWT.FILL, SWT.END, true, false, 1, 3));
        textInfo.setText(Messages.bind(Messages.CopyMemberDialog_Info, SINGLE_QUOTE + Messages.To_member_colhdg + SINGLE_QUOTE));

        Label labelToFile = new Label(mainArea, SWT.NONE);
        labelToFile.setText(Messages.To_file_colon);

        comboToFile = WidgetFactory.createCombo(mainArea);
        comboToFile.setItems(new String[] { FROMFILE });
        comboToFile.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        comboToFile.setTextLimit(10);
        comboToFile.addVerifyListener(new UpperCaseOnlyVerifier());

        // Create spacer
        new Label(mainArea, SWT.NONE).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label labelToLibrary = new Label(mainArea, SWT.NONE);
        labelToLibrary.setText(Messages.To_library_colon);

        textToLibrary = WidgetFactory.createUpperCaseText(mainArea);
        textToLibrary.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        textToLibrary.setTextLimit(10);

        // Create spacer
        new Label(mainArea, SWT.NONE).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        tableViewer = new TableViewer(mainArea, SWT.FULL_SELECTION | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        Table table = tableViewer.getTable();
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, getNumberOfLayoutColumns(mainArea), 1));
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        MinColumnSizeListener listener = new MinColumnSizeListener(10);

        addTableColumn(tableViewer.getTable(), Columns.FROM_LIBRARY).addControlListener(listener);
        addTableColumn(tableViewer.getTable(), Columns.FROM_FILE).addControlListener(listener);
        addTableColumn(tableViewer.getTable(), Columns.FROM_MEMBER).addControlListener(listener);
        addTableColumn(tableViewer.getTable(), Columns.TO_MEMBER).addControlListener(listener);
        addTableColumn(tableViewer.getTable(), Columns.ERROR_MESSAGE, 400).addControlListener(listener);

        tableViewer.setCellModifier(new CopyMemberItemTableCellModifier(tableViewer));
        tableViewer.setContentProvider(new ContentProviderMemberItems());
        tableViewer.setLabelProvider(new LabelProviderMemberItems());

        TableViewerKeyBoardSupporter supporter = new TableViewerKeyBoardSupporter(tableViewer, true);
        supporter.startSupport();

        labelNumElem = new Label(mainArea, SWT.NONE);
        labelNumElem.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false, getNumberOfLayoutColumns(mainArea), 1));
        int numItems;
        if (copyMemberService != null) {
            numItems = copyMemberService.getItems().length;
        } else {
            numItems = 0;
        }
        labelNumElem.setText(Messages.Items_colon + " " + numItems); //$NON-NLS-1$

        new Label(mainArea, SWT.NONE).setVisible(false);

        Group existingMembersActionGroup = new Group(mainArea, SWT.NONE);
        existingMembersActionGroup.setLayout(new GridLayout(2, false));
        existingMembersActionGroup.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, true, false, 4, 1));
        existingMembersActionGroup.setText(Messages.Label_Existing_members_action_colon);

        chkBoxError = WidgetFactory.createRadioButton(existingMembersActionGroup, Messages.Label_Error);
        chkBoxError.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false, 2, 1));

        chkBoxReplace = WidgetFactory.createRadioButton(existingMembersActionGroup, Messages.Label_Replace_existing_members);
        chkBoxReplace.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false, 2, 1));

        chkBoxRename = WidgetFactory.createRadioButton(existingMembersActionGroup, Messages.Label_Rename_existing_members);
        chkBoxRename.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false, 1, 1));

        lnkPreferences = new Link(existingMembersActionGroup, SWT.MULTI | SWT.WRAP);
        setLinkPreferencesText();

        lnkPreferences.pack();
        lnkPreferences.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(), e.text, null, null);
                dialog.open();
                setLinkPreferencesText();
            }
        });

        chkBoxIgnoreDataLostError = WidgetFactory.createCheckbox(mainArea);
        chkBoxIgnoreDataLostError.setText(Messages.Ignore_data_lost_error);
        chkBoxIgnoreDataLostError.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false, getNumberOfLayoutColumns(mainArea), 1));

        chkBoxIgnoreDirtyFilesError = WidgetFactory.createCheckbox(mainArea);
        chkBoxIgnoreDirtyFilesError.setText(Messages.Ignore_unsaved_changes_error);
        chkBoxIgnoreDirtyFilesError.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false, getNumberOfLayoutColumns(mainArea), 1));

        createStatusLine(mainArea);

        loadScreenValues();

        return mainArea;
    }

    private void setLinkPreferencesText() {

        IMemberRenamingRule rule = Preferences.getInstance().getMemberRenamingRule();
        String ruleLabel = rule.getLabel();

        String lnkLabel = Messages.bind(Messages.Link_to_copy_member_preferences_A_B,
            new Object[] { "<a href=\"biz.isphere.core.preferencepages.ISphereCopyMembers\">", "</a>", ruleLabel });

        lnkPreferences.setLayoutData(new GridData());
        lnkPreferences.setText(lnkLabel);

        mainArea.layout();
    }

    private int getNumberOfLayoutColumns(Composite composite) {

        if (composite.getLayout() instanceof GridLayout) {
            GridLayout gridLayout = (GridLayout)composite.getLayout();
            return gridLayout.numColumns;
        }

        return 0;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button btnReset = createButton(parent, BUTTON_RESET_ID, Messages.Reset, false);
        btnReset.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                copyMemberService.reset();
                setControlEnablement();
                setFocus();
            }

            public void widgetDefaultSelected(SelectionEvent event) {
                widgetSelected(event);
            }
        });

        super.createButtonsForButtonBar(parent);
        getButton(BUTTON_COPY_ID).setText(Messages.Copy);
    }

    @Override
    protected Control createContents(Composite parent) {

        Control control = super.createContents(parent);

        // Enable control after dialog area and buttons have been created.
        setControlEnablement();

        return control;
    }

    @Override
    public void setFocus() {

        if (!comboToConnection.hasConnection()) {
            comboToConnection.setFocus();
        } else if (StringHelper.isNullOrEmpty(getToFileName())) {
            comboToFile.setFocus();
        } else if (StringHelper.isNullOrEmpty(getToLibraryName())) {
            textToLibrary.setFocus();
        } else {
            if (FROMFILE.equals(getToFileName())) {
                comboToFile.setFocus();
            } else {
                comboToConnection.setFocus();
            }
        }
    }

    /**
     * Restores the screen values of the last copy operation.
     */
    private void loadScreenValues() {

        comboToConnection.setQualifiedConnectionName(copyMemberService.getToConnectionName());

        if (copyMemberService.getFromLibraryNamesCount() == 1) {
            textToLibrary.setText(copyMemberService.getFromLibraryNames()[0]);
        } else {
            textToLibrary.setText(Messages.EMPTY);
        }

        if (copyMemberService.getFromFileNamesCount() == 1) {
            comboToFile.setText(copyMemberService.getFromFileNames()[0]);
        } else {
            comboToFile.setText(FROMFILE);
        }

        tableViewer.setInput(copyMemberService);

        chkBoxError.setSelection(true);
    }

    /**
     * Stores the screen values that are preserved for the next copy operation.
     */
    private void storeScreenValues() {
    }

    private TableColumn addTableColumn(Table table, Columns column) {
        return addTableColumn(table, column, column.width);
    }

    private TableColumn addTableColumn(Table table, Columns column, int width) {

        TableColumn tableColumn = getDialogSettingsManager().createResizableTableColumn(tableViewer.getTable(), SWT.LEFT, column.name, width);
        tableColumn.setText(column.label);

        return tableColumn;
    }

    private String getToFileName() {
        return comboToFile.getText();
    }

    private String getToLibraryName() {
        return textToLibrary.getText();
    }

    private String getToConnectionName() {
        return comboToConnection.getQualifiedConnectionName();
    }

    private void setControlEnablement() {

        if (copyMemberService == null) {
            setButtonEnablement(getButton(BUTTON_COPY_ID), false);
            setButtonEnablement(getButton(BUTTON_RESET_ID), false);
            setButtonEnablement(getButton(BUTTON_CLOSE_CANCEL), true);
            setButtonLabel(getButton(BUTTON_CLOSE_CANCEL), IDialogConstants.CLOSE_LABEL);
            setControlsEnables(true);
        } else {

            if (isValidating()) {
                setButtonEnablement(getButton(BUTTON_COPY_ID), false);
                setButtonEnablement(getButton(BUTTON_RESET_ID), false);
                setButtonEnablement(getButton(BUTTON_CLOSE_CANCEL), true);
                setButtonLabel(getButton(BUTTON_CLOSE_CANCEL), IDialogConstants.CANCEL_LABEL);
                setControlsEnables(false);
                setErrorMessage(null);
                setStatusMessage(Messages.Validating_dots);
            } else if (isCopying()) {
                setButtonEnablement(getButton(BUTTON_COPY_ID), false);
                setButtonEnablement(getButton(BUTTON_RESET_ID), false);
                setButtonEnablement(getButton(BUTTON_CLOSE_CANCEL), true);
                setButtonLabel(getButton(BUTTON_CLOSE_CANCEL), IDialogConstants.CANCEL_LABEL);
                setControlsEnables(false);
                setErrorMessage(null);
                setStatusMessage(Messages.Copying_dots);
            } else {

                if (copyMemberService.hasItemsToCopy()) {
                    setButtonEnablement(getButton(BUTTON_COPY_ID), true);
                } else {
                    setButtonEnablement(getButton(BUTTON_COPY_ID), false);
                }

                if (copyMemberService.getItemsCopiedCount() > 0) {
                    setButtonEnablement(getButton(BUTTON_RESET_ID), true);
                    setButtonEnablement(getButton(BUTTON_CLOSE_CANCEL), true);
                } else {
                    setButtonEnablement(getButton(BUTTON_RESET_ID), false);
                    setButtonEnablement(getButton(BUTTON_CLOSE_CANCEL), true);
                }

                if (copyMemberService.getItemsCopiedCount() > 0 && copyMemberService.hasItemsToCopy()) {
                    setButtonLabel(getButton(BUTTON_CLOSE_CANCEL), IDialogConstants.CANCEL_LABEL);
                } else {
                    setButtonLabel(getButton(BUTTON_CLOSE_CANCEL), IDialogConstants.CLOSE_LABEL);
                }

                setControlsEnables(true);

                if (copyMemberService.isCanceled()) {
                    setErrorMessage(Messages.Operation_has_been_canceled_by_the_user);
                } else {
                    setStatusMessage(Messages.EMPTY);
                }
            }
        }
    }

    private void setControlsEnables(boolean enabled) {

        if (mainArea == null) {
            // not yet created
            return;
        }

        comboToConnection.setEnabled(enabled);
        comboToFile.setEnabled(enabled);
        textToLibrary.setEnabled(enabled);
        tableViewer.getTable().setEnabled(enabled);
        chkBoxError.setEnabled(enabled);
        chkBoxReplace.setEnabled(enabled);
        chkBoxRename.setEnabled(enabled);
        chkBoxIgnoreDataLostError.setEnabled(enabled);
        chkBoxIgnoreDirtyFilesError.setEnabled(enabled);
    }

    private void setButtonEnablement(Button button, boolean enabled) {
        if (button == null) {
            return;
        }

        button.setEnabled(enabled);
    }

    private void setButtonLabel(Button button, String label) {
        if (button == null) {
            return;
        }

        button.setText(label);
    }

    /**
     * Overridden to make this dialog resizable.
     */
    @Override
    protected boolean isResizable() {
        return true;
    }

    /**
     * Overridden to provide a default size to {@link XDialog}.
     */
    @Override
    protected Point getDefaultSize() {
        return new Point(910, 600);
    }

    /**
     * Overridden to let {@link XDialog} store the state of this dialog in a
     * separate section of the dialog settings file.
     */
    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return super.getDialogBoundsSettings(ISpherePlugin.getDefault().getDialogSettings());
    }

    private class MinColumnSizeListener extends ControlAdapter {
        private int minWidth;

        public MinColumnSizeListener(int minWidth) {
            this.minWidth = minWidth;
        }

        @Override
        public void controlResized(ControlEvent event) {
            TableColumn column = (TableColumn)event.getSource();
            if (column.getWidth() < minWidth) {
                column.setWidth(minWidth);
            }
        }
    }

    /**
     * Content provider for the member list table.
     */
    private class ContentProviderMemberItems implements IStructuredContentProvider, CopyMemberService.ModifiedListener {

        private TableViewer viewer;

        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof CopyMemberService) {
                return ((CopyMemberService)inputElement).getItems();
            }
            return new Object[0];
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

            this.viewer = (TableViewer)viewer;

            if (oldInput != null) {
                ((CopyMemberService)oldInput).removeModifiedListener(ContentProviderMemberItems.this);
            }

            if (newInput != null) {
                ((CopyMemberService)newInput).addModifiedListener(ContentProviderMemberItems.this);
            }
        }

        public void modified(final CopyMemberItem item) {

            if (Display.getCurrent() == null) {
                UIJob job = new UIJob("") {
                    @Override
                    public IStatus runInUIThread(IProgressMonitor arg0) {
                        updateStatus(item);
                        return Status.OK_STATUS;
                    }
                };
                job.schedule();
            } else {
                updateStatus(item);
            }
        }

        private void updateStatus(CopyMemberItem item) {

            if (item == null) {
                viewer.refresh(true);
            } else {
                viewer.update(item, null);
                if (isCopying()) {
                    viewer.reveal(item);
                    viewer.setSelection(new StructuredSelection(item));
                } else if (isValidating()) {
                    if (item.isError() && viewer.getSelection().isEmpty()) {
                        viewer.reveal(item);
                        viewer.setSelection(new StructuredSelection(item));
                    }
                }
            }
            setControlEnablement();
            mainArea.update();
        }
    }

    /**
     * Content provider for the member list table.
     */
    private class LabelProviderMemberItems extends LabelProvider implements TooltipProvider, ITableLabelProvider {

        public String getColumnText(Object element, int columnIndex) {

            CopyMemberItem member = (CopyMemberItem)element;

            if (columnIndex == Columns.FROM_LIBRARY.ordinal()) {
                return member.getFromLibrary();
            } else if (columnIndex == Columns.FROM_FILE.ordinal()) {
                return member.getFromFile();
            } else if (columnIndex == Columns.FROM_MEMBER.ordinal()) {
                return member.getFromMember();
            } else if (columnIndex == Columns.TO_MEMBER.ordinal()) {
                return member.getToMember();
            } else if (columnIndex == Columns.ERROR_MESSAGE.ordinal()) {
                return getErrorMessage(member);
            } else {
                return Messages.EMPTY;
            }
        }

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getTooltipText(Object element, int columnIndex) {

            CopyMemberItem member = (CopyMemberItem)element;

            if (columnIndex == Columns.ERROR_MESSAGE.ordinal()) {
                return getErrorMessage(member);
            } else {
                return null;
            }
        }

        private String getErrorMessage(CopyMemberItem member) {
            if (member.isCopied()) {
                return Messages.C_O_P_I_E_D;
            } else {
                String errorMessage = member.getErrorMessage();
                if (!StringHelper.isNullOrEmpty(errorMessage)) {
                    return errorMessage.replaceAll("\n", " :: "); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    return Messages.EMPTY;
                }
            }
        }
    }
}
