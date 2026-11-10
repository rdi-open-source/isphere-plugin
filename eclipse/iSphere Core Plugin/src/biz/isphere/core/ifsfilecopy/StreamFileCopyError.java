/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifsfilecopy;

public enum StreamFileCopyError {
    ERROR_NONE (false, SynchronizeStreamFilesAction.CONTINUE),
    ERROR_EXCEPTION (true, SynchronizeStreamFilesAction.CANCEL),

    ERROR_FROM_CONNECTION_NOT_FOUND (true, SynchronizeStreamFilesAction.CANCEL),
    ERROR_TO_CONNECTION_NOT_FOUND (true, SynchronizeStreamFilesAction.CANCEL),

    ERROR_FROM_PATH_NAME_NOT_VALID (true, SynchronizeStreamFilesAction.CANCEL),
    ERROR_FROM_DIRECTORY_NOT_FOUND (true, SynchronizeStreamFilesAction.CANCEL),
    ERROR_FROM_IFS_FILE_NOT_FOUND (true, SynchronizeStreamFilesAction.CONTINUE_WITH_ERROR),

    ERROR_FROM_IFS_FILE_IS_DIRTY (true, SynchronizeStreamFilesAction.CONTINUE_WITH_ERROR),
    ERROR_TO_IFS_FILE_COPY_TO_SAME_NAME (true, SynchronizeStreamFilesAction.CONTINUE_WITH_ERROR),

    ERROR_TO_PATH_NAME_NOT_VALID (true, SynchronizeStreamFilesAction.CONTINUE_WITH_ERROR),
    ERROR_TO_DIRECTORY_NOT_FOUND (true, SynchronizeStreamFilesAction.CONTINUE_WITH_ERROR),

    ERROR_TO_IFS_FILE_EXISTS (true, SynchronizeStreamFilesAction.CONTINUE_WITH_ERROR),
    ERROR_TO_IFS_FILE_RENAME_EXCEPTION (true, SynchronizeStreamFilesAction.CONTINUE_WITH_ERROR),
    ERROR_COPY_IFS_FILE_COMMAND (true, SynchronizeStreamFilesAction.CONTINUE_WITH_ERROR);

    private boolean isError;
    private SynchronizeStreamFilesAction action;

    private StreamFileCopyError(boolean isError, SynchronizeStreamFilesAction action) {
        this.isError = isError;
        this.action = action;
    }

    public boolean isError() {
        return isError;
    }

    public SynchronizeStreamFilesAction getDefaultAction() {
        return action;
    }
}
