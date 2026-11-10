/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.swt.widgets.objectselector;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.IFSFile;

import biz.isphere.base.internal.StringHelper;
import biz.isphere.base.jface.dialogs.XDialog;
import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.Messages;
import biz.isphere.core.ibmi.contributions.extension.handler.IBMiHostContributionsHandler;
import biz.isphere.core.swt.widgets.HistoryCombo;
import biz.isphere.core.swt.widgets.WidgetFactory;
import biz.isphere.core.swt.widgets.connectioncombo.ConnectionCombo;

/**
 * This class produces a dialog for selecting a connection and a directory of
 * the IFS of an IBM i.
 */
public class SelectRemoteIFSDirectoryDialog extends XDialog implements ISelectRemoteIFSDirectoryDialog {

    private static final String CONNECTION_NAME = "CONNECTION_NAME"; //$NON-NLS-1$
    private static final String DIRECTORY = "DIRECTORY"; //$NON-NLS-1$

    private static final String EMPTY = ""; //$NON-NLS-1$

    private String connectionName;
    private String directory;

    private ConnectionCombo cboConnectionName;
    private HistoryCombo cboDirectory;
    private Button btnSelectDirectory;

    public SelectRemoteIFSDirectoryDialog(Shell parentShell, String connectionName) {
        super(parentShell);

        this.connectionName = connectionName;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);

        newShell.setText(Messages.bind(Messages.Select_A, Messages.Directory));
    }

    @Override
    public Control createDialogArea(Composite parent) {

        Composite dialogArea = new Composite(parent, SWT.NONE);
        dialogArea.setLayout(new GridLayout(3, false));
        dialogArea.setLayoutData(new GridData(GridData.FILL_BOTH));

        Label lblConnectionName = new Label(dialogArea, SWT.NONE);
        lblConnectionName.setText(Messages.Connection);

        cboConnectionName = WidgetFactory.createConnectionCombo(dialogArea, SWT.NONE);
        cboConnectionName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(dialogArea, SWT.NONE);

        Label lblDirectory = new Label(dialogArea, SWT.NONE);
        lblDirectory.setText(Messages.Directory);

        cboDirectory = WidgetFactory.createHistoryCombo(dialogArea);
        GridData cboDirectoryLayoutData = new GridData(GridData.FILL_HORIZONTAL);
        cboDirectoryLayoutData.widthHint = 300;
        cboDirectory.setLayoutData(cboDirectoryLayoutData);

        btnSelectDirectory = WidgetFactory.createPushButton(dialogArea, Messages.Browse);

        loadScreenValues();

        configureControls();

        setControlsEnablement();

        return dialogArea;
    }

    private void setControlsEnablement() {

        if (!cboConnectionName.hasConnection()) {
            btnSelectDirectory.setEnabled(false);
        } else {
            btnSelectDirectory.setEnabled(true);
        }
    }

    private void configureControls() {

        ControlEnablementListener controlsEnablementListerner = new ControlEnablementListener();

        cboConnectionName.addSelectionListener(controlsEnablementListerner);
        cboDirectory.addModifyListener(controlsEnablementListerner);

        btnSelectDirectory.addSelectionListener(new BrowseDirectoryListener(cboConnectionName, cboDirectory));
    }

    @Override
    protected void okPressed() {

        try {

            connectionName = cboConnectionName.getQualifiedConnectionName();
            directory = cboDirectory.getText().trim();

            if (StringHelper.isNullOrEmpty(connectionName)) {
                MessageDialog.openError(getShell(), Messages.E_R_R_O_R, Messages.Invalid_or_missing_value);
                return;
            }

            if (StringHelper.isNullOrEmpty(directory)) {
                MessageDialog.openError(getShell(), Messages.E_R_R_O_R, Messages.Invalid_or_missing_value);
                return;
            }

            AS400 system = IBMiHostContributionsHandler.getSystem(connectionName);

            IFSFile ifsDirectory = new IFSFile(system, directory);
            if (!ifsDirectory.exists() || !ifsDirectory.isDirectory()) {
                MessageDialog.openError(getShell(), Messages.E_R_R_O_R, Messages.bind(Messages.Directory_A_not_found, directory));
                return;
            }

        } catch (Exception e) {
            MessageDialog.openError(getShell(), Messages.E_R_R_O_R, e.getLocalizedMessage());
            return;
        }

        saveScreenValues();

        // Close dialog
        super.okPressed();
    }

    public String getConnectionName() {
        return connectionName;
    }

    public String getDirectory() {
        return directory;
    }

    private void loadScreenValues() {

        if (connectionName == null) {
            connectionName = loadValue(CONNECTION_NAME, EMPTY);
        }
        if (!StringHelper.isNullOrEmpty(connectionName)) {
            cboConnectionName.setQualifiedConnectionName(connectionName);
        }

        if (directory == null) {
            directory = loadValue(DIRECTORY, EMPTY);
        }
        if (!StringHelper.isNullOrEmpty(directory)) {
            cboDirectory.setText(directory);
        }
        cboDirectory.load(getDialogSettingsManager(), DIRECTORY);
    }

    private void saveScreenValues() {

        storeValue(CONNECTION_NAME, cboConnectionName.getQualifiedConnectionName());

        storeValue(DIRECTORY, cboDirectory.getText());
        if (!StringHelper.isNullOrEmpty(cboDirectory.getText())) {
            cboDirectory.updateHistory(cboDirectory.getText());
            cboDirectory.store();
        }
    }

    /**
     * Overridden make this dialog resizable {@link XDialog}.
     */
    @Override
    protected boolean isResizable() {
        return false;
    }

    /**
     * Overridden to let {@link XDialog} store the state of this dialog in a
     * separate section of the dialog settings file.
     */
    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return super.getDialogBoundsSettings(ISpherePlugin.getDefault().getDialogSettings());
    }

    private class ControlEnablementListener extends SelectionAdapter implements ModifyListener {
        @Override
        public void widgetSelected(SelectionEvent e) {
            setControlsEnablement();
        }

        public void modifyText(ModifyEvent e) {
            setControlsEnablement();
        }
    }

    private class BrowseDirectoryListener extends SelectionAdapter {

        private ConnectionCombo comboConnectionName;
        private HistoryCombo comboDirectory;

        public BrowseDirectoryListener(ConnectionCombo comboConnectionName, HistoryCombo comboDirectory) {
            this.comboConnectionName = comboConnectionName;
            this.comboDirectory = comboDirectory;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {

            if (!comboConnectionName.hasConnection()) {
                return;
            }

            SelectIFSDirectoryDialog dialog = new SelectIFSDirectoryDialog(getShell(), comboConnectionName.getQualifiedConnectionName());
            dialog.setDirectory(comboDirectory.getText().trim());

            if (dialog.open() == Dialog.OK) {
                comboDirectory.setText(dialog.getSelectedDirectory());
            }
        }
    }
}
