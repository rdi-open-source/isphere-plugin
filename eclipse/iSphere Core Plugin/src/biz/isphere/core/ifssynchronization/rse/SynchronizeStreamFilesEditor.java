/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization.rse;

import org.eclipse.jface.viewers.TableViewer;

import biz.isphere.core.internal.RemoteStreamFile;
import biz.isphere.core.swt.widgets.objectselector.ISelectRemoteQSYSObjectDialog;
import biz.isphere.core.swt.widgets.objectselector.ISelectedObject;
import biz.isphere.core.swt.widgets.objectselector.SelectRemoteQSYSObjectDialog;

public class SynchronizeStreamFilesEditor extends AbstractSynchronizeStreamFilesEditor {

    public SynchronizeStreamFilesEditor() {
        super();
    }

    @Override
    protected RemoteStreamFile performSelectRemoteObject(String connectionName, String objectName) {

        ISelectRemoteQSYSObjectDialog dialog;
        dialog = SelectRemoteQSYSObjectDialog.createSelectSourceFileDialog(getShell(), connectionName);
        dialog.setObjectName(objectName);

        if (dialog.open() == SelectRemoteQSYSObjectDialog.CANCEL) {
            return null;
        }

        ISelectedObject selectedObject = dialog.getObject();

        String connection = selectedObject.getConnectionName();
        String name = selectedObject.getName();

        return new RemoteStreamFile(connection, name);
    }

    @Override
    protected AbstractTableLabelProvider getTableLabelProvider(TableViewer tableViewer) {
        return new TableLabelProvider(tableViewer);
    }

    /**
     * Class the provides the content for the cells of the table.
     */
    private class TableLabelProvider extends AbstractTableLabelProvider {

        public TableLabelProvider(TableViewer tableViewer) {
            super(tableViewer);
        }
    }
}
