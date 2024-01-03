/*******************************************************************************
 * Copyright (c) 2012-2021 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.dataspaceeditor;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;

import biz.isphere.base.internal.UIHelper;
import biz.isphere.core.internal.handler.AbstractCommandHandler;

public class GoToHandler extends AbstractCommandHandler {

    public static final String ID = "biz.isphere.core.dataspaceeditor.GoToHandler";

    public Object execute(ExecutionEvent paramExecutionEvent) throws ExecutionException {

        AbstractDataSpaceEditor editor = getActiveEditor();
        if (editor == null) {
            return null;
        }

        editor.doGoTo();

        return null;
    }

    private AbstractDataSpaceEditor getActiveEditor() {

        IEditorPart editor = UIHelper.getActiveEditor();
        if (editor instanceof AbstractDataSpaceEditor) {
            return (AbstractDataSpaceEditor)editor;
        }

        return null;
    }

    /**
     * Called before the "Navigate" menu is shown to enable or disable the menu
     * item.
     */
    @Override
    public boolean isEnabled() {
        return getActiveEditor().canGoTo();
    }

    /**
     * Called by the command before the handler is called to check whether or
     * the request can be handled by the handler.
     */
    @Override
    public boolean isHandled() {
        return getActiveEditor().canGoTo();
    }
}
