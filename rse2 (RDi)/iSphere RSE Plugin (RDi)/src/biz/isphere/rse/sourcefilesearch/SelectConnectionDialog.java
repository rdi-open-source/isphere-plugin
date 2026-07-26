/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.rse.sourcefilesearch;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.rse.core.model.IHost;

import com.ibm.etools.iseries.rse.ui.widgets.IBMiConnectionCombo;
import com.ibm.etools.iseries.subsystems.qsys.api.IBMiConnection;

import biz.isphere.base.jface.dialogs.XDialog;
import biz.isphere.core.Messages;
import biz.isphere.rse.ibmi.contributions.extension.point.QualifiedConnectionName;

/**
 * Modal dialog that lets the user pick an existing RSE connection or create a
 * new one via the built-in "New..." button of {@link IBMiConnectionCombo}.
 * <p>
 * Lives in the RSE adapter plug-in because {@link IBMiConnectionCombo} is
 * provided by <code>com.ibm.etools.iseries.rse.ui</code>, which the iSphere
 * Core plug-in intentionally does not depend on.
 */
public class SelectConnectionDialog extends XDialog {

    private IBMiConnectionCombo comboConnection;
    private String qualifiedConnectionName;

    public SelectConnectionDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {

        Composite container = (Composite)super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));

        comboConnection = new IBMiConnectionCombo(container, SWT.NONE, (IHost)null, true);
        comboConnection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        comboConnection.getCombo().addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent event) {
                setOkButtonEnablement();
            }
        });

        return container;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, Messages.OK, true);
        createButton(parent, IDialogConstants.CANCEL_ID, Messages.Cancel, false);
        setOkButtonEnablement();
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.Select_Connection);
    }

    @Override
    protected void okPressed() {

        IBMiConnection connection = comboConnection.getISeriesConnection();
        if (connection == null) {
            return;
        }

        qualifiedConnectionName = new QualifiedConnectionName(connection).getQualifiedName();
        super.okPressed();
    }

    /**
     * Returns the qualified name of the connection selected by the user. Only
     * meaningful when the dialog was closed with OK.
     *
     * @return qualified connection name in the form <code>profile:connection</code>
     */
    public String getQualifiedConnectionName() {
        return qualifiedConnectionName;
    }

    private void setOkButtonEnablement() {
        if (getButton(IDialogConstants.OK_ID) != null) {
            getButton(IDialogConstants.OK_ID).setEnabled(comboConnection != null && comboConnection.getISeriesConnection() != null);
        }
    }
}
