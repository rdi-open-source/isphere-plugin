/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.rse.actions;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.etools.iseries.subsystems.ifs.files.IFSRemoteFile;
import com.ibm.etools.iseries.subsystems.qsys.objects.QSYSRemoteObject;

import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.externalapi.Access;
import biz.isphere.core.ifssynchronization.rse.SynchronizeStreamFilesEditorConfiguration;
import biz.isphere.core.internal.RemoteStreamFile;
import biz.isphere.rse.Messages;
import biz.isphere.rse.connection.ConnectionManager;

public class OpenSynchronizeStreamFileEditorAction implements IObjectActionDelegate {

    public static final String ID = "biz.isphere.rse.actions.OpenSynchronizeStreamFileEditorAction"; //$NON-NLS-1$

    protected IStructuredSelection structuredSelection;
    protected Shell shell;
    protected Object firstSelectedObject;

    public void run(IAction arg0) {

        if (structuredSelection != null && !structuredSelection.isEmpty()) {

            if (isValidSelection(structuredSelection)) {

                SynchronizeStreamFilesEditorConfiguration configuration = SynchronizeStreamFilesEditorConfiguration.getDefaultConfiguration();
                RemoteStreamFile[] remoteObjects = getSelectedObjects(structuredSelection);

                try {
                    if (remoteObjects.length == 1) {
                        Access.openSynchronizeIfsFilesEditor(shell, remoteObjects[0], null, configuration);
                    } else if (remoteObjects.length == 2) {
                        Access.openSynchronizeIfsFilesEditor(shell, remoteObjects[0], remoteObjects[1], configuration);
                    }
                } catch (Exception e) {
                    ISpherePlugin.logError("*** Could not open synchronize IFS files editor ***", e); //$NON-NLS-1$
                }
            }
        }
    }

    private RemoteStreamFile[] getSelectedObjects(IStructuredSelection selectedObject) {

        List<?> objects = selectedObject.toList();
        RemoteStreamFile[] remoteObjects = new RemoteStreamFile[objects.size()];

        int i = 0;
        for (Object object : objects) {
            IFSRemoteFile qsysRemoteObject = (IFSRemoteFile)object;
            remoteObjects[i] = createRemoteIfsFile(qsysRemoteObject);
            i++;
        }

        return remoteObjects;
    }

    private RemoteStreamFile createRemoteIfsFile(IFSRemoteFile qsysRemoteObject) {

        IHost host = qsysRemoteObject.getParentRemoteFileSubSystem().getHost();
        String qualifiedConnectionName = ConnectionManager.getConnectionName(host);

        File fullPath = new File(qsysRemoteObject.getParentPath(), qsysRemoteObject.getName());
        String name = fullPath.getPath();

        return new RemoteStreamFile(qualifiedConnectionName, name);
    }

    private boolean isValidSelection(IStructuredSelection selectedObject) {

        firstSelectedObject = null;

        for (Iterator<?> iterator = selectedObject.iterator(); iterator.hasNext();) {

            Object object = iterator.next();
            if (object instanceof QSYSRemoteObject) {
                if (firstSelectedObject == null) {
                    firstSelectedObject = object;
                } else {
                    if (!firstSelectedObject.getClass().equals(object.getClass())) {
                        MessageDialog.openError(shell, Messages.E_R_R_O_R, Messages.Invalid_selection_Objects_must_be_of_the_same_type);
                        return false;
                    }
                }
                // QSYSRemoteObject qsysRemoteObject = (QSYSRemoteObject)object;
                // System.out.println("isValidSelection(): " +
                // qsysRemoteObject.getClass().getSimpleName());
            }
        }

        return true;
    }

    public void selectionChanged(IAction action, ISelection selection) {

        if (selection instanceof IStructuredSelection) {
            structuredSelection = ((IStructuredSelection)selection);
        } else {
            structuredSelection = null;
        }

        if (structuredSelection.size() >= 1 && structuredSelection.size() <= 2) {
            action.setEnabled(true);
        } else {
            action.setEnabled(false);
        }
    }

    public void setActivePart(IAction action, IWorkbenchPart workbenchPart) {
        shell = workbenchPart.getSite().getShell();
    }

}
