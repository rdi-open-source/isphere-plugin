/*******************************************************************************
 * Copyright (c) 2012-2022 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.dataareaeditor;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import biz.isphere.base.internal.UIHelper;
import biz.isphere.core.dataspace.WrappedDataSpace;
import biz.isphere.core.dataspace.rse.AbstractWrappedDataSpace;
import biz.isphere.core.dataspaceeditor.AbstractDataSpaceEditor;
import biz.isphere.core.dataspaceeditor.DataAreaEditorInput;
import biz.isphere.core.internal.ISphereHelper;
import biz.isphere.core.internal.RemoteObject;

public class DataAreaEditor extends AbstractDataSpaceEditor {

    public static final String ID = "biz.isphere.core.dataareaeditor.DataAreaEditor"; //$NON-NLS-1$

    @Override
    protected AbstractWrappedDataSpace createDataSpaceWrapper(RemoteObject remoteObject) throws Exception {
        return new WrappedDataSpace(remoteObject);
    }

    public static void openEditor(String connectionName, RemoteObject remoteObject, String aMode) throws PartInitException {
        if (ISphereHelper.checkISphereLibrary(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), connectionName)) {
            DataAreaEditorInput editorInput = new DataAreaEditorInput(connectionName, remoteObject, aMode);
            UIHelper.getActivePage().openEditor(editorInput, DataAreaEditor.ID);
        }
    }

}
