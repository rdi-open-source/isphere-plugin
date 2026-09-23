/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifsfilecopy;

public interface IValidateItemMessageListener {

    /**
     * Methods called by the {@link ValidateStreamFilesJob} for each IFS file
     * error.
     * 
     * @param errorId - ID identifying the error
     * @param item - copy IFS file item in error
     * @param errorMessage - error message text
     * @return action that is performed by the job
     */
    public SynchronizeStreamFilesAction reportValidateIfsFileMessage(StreamFileCopyError errorId, CopyStreamFileItem item, String errorMessage);
}
