/*******************************************************************************
 * Copyright (c) 2012-2022 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.messagefileeditor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import biz.isphere.base.internal.UIHelper;
import biz.isphere.core.internal.ISphereHelper;
import biz.isphere.core.internal.RemoteObject;

public class MessageFileEditor extends EditorPart {

    public static final String ID = "biz.isphere.core.messagefileeditor.MessageFileEditor";

    private MessageFileEditorInput input;

    @Override
    public void createPartControl(Composite parent) {

        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());

        MessageDescriptionViewer _messageDescriptionViewer = new MessageDescriptionViewer(input.getAS400(), input.getConnection(),
            input.getObjectLibrary(), input.getMessageFile(), input.getMode(), getSite());

        _messageDescriptionViewer.createContents(container);

    }

    @Override
    public void init(IEditorSite site, IEditorInput input) {
        setSite(site);
        setInput(input);
        setPartName(input.getName());
        setTitleImage(((MessageFileEditorInput)input).getTitleImage());
        this.input = (MessageFileEditorInput)input;
    }

    @Override
    public void setFocus() {
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    public void doSaveAs() {
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
        return false;
    }

    public static void openEditor(String connectionName, RemoteObject remoteObject, String mode) throws PartInitException {
        if (ISphereHelper.checkISphereLibrary(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), connectionName)) {
            MessageFileEditorInput editorInput = new MessageFileEditorInput(connectionName, remoteObject, mode);
            UIHelper.getActivePage().openEditor(editorInput, MessageFileEditor.ID);
        }
    }
}
