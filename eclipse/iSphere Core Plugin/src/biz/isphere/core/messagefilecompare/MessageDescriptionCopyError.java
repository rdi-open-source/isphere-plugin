/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.messagefilecompare;

public enum MessageDescriptionCopyError {
    ERROR_NONE (false, SynchronizeMessageFilesAction.CONTINUE),
    ERROR_EXCEPTION (true, SynchronizeMessageFilesAction.CANCEL),

    ERROR_FROM_CONNECTION_NOT_FOUND (true, SynchronizeMessageFilesAction.CANCEL),
    ERROR_TO_CONNECTION_NOT_FOUND (true, SynchronizeMessageFilesAction.CANCEL),

    ERROR_TO_MESSAGE_DESCRIPTION_EXISTS (true, SynchronizeMessageFilesAction.CONTINUE_WITH_ERROR),
    ERROR_MERGE_COMMAND (true, SynchronizeMessageFilesAction.CONTINUE_WITH_ERROR);

    private boolean isError;
    private SynchronizeMessageFilesAction action;

    private MessageDescriptionCopyError(boolean isError, SynchronizeMessageFilesAction action) {
        this.isError = isError;
        this.action = action;
    }

    public boolean isError() {
        return isError;
    }

    public SynchronizeMessageFilesAction getDefaultAction() {
        return action;
    }
}
