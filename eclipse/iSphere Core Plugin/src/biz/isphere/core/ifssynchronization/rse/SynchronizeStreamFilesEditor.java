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
import biz.isphere.core.swt.widgets.objectselector.ISelectRemoteIFSDirectoryDialog;
import biz.isphere.core.swt.widgets.objectselector.SelectRemoteIFSDirectoryDialog;

public class SynchronizeStreamFilesEditor extends AbstractSynchronizeStreamFilesEditor {

    public SynchronizeStreamFilesEditor() {
        super();
    }

    @Override
    protected RemoteStreamFile performSelectRemoteObject(String connectionName, String objectName) {

        ISelectRemoteIFSDirectoryDialog dialog = new SelectRemoteIFSDirectoryDialog(getShell(), connectionName);
        dialog.setDirectory(objectName);

        if (dialog.open() == SelectRemoteIFSDirectoryDialog.CANCEL) {
            return null;
        }

        return new RemoteStreamFile(dialog.getConnectionName(), dialog.getDirectory());
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
