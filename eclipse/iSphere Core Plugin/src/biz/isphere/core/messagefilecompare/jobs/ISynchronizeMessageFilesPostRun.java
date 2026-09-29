/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.messagefilecompare.jobs;

import biz.isphere.core.messagefilecompare.SynchronizationResult;
import biz.isphere.core.messagefilecompare.SynchronizeMessageFilesJob;

public interface ISynchronizeMessageFilesPostRun {

    /**
     * Job finished successfully.
     */
    public static final String OK = "OK"; //$NON-NLS-1$

    /**
     * Job ended with errors.
     */
    public static final String ERROR = "ERROR"; //$NON-NLS-1$

    /**
     * Job has been canceled.
     */
    public static final String CANCELED = "CANCELED"; //$NON-NLS-1$

    /**
     * PostRun method called by {@link SynchronizeMessageFilesJob} at the end of
     * the copy message description process.
     *
     * @param status - status of the synchronization operation. See:
     *        {@link SynchronizationResult}
     * @param countCopied - number of message descriptions that have been copied
     * @param countErrors - number of message descriptions that have errors
     * @param message - error or canceled message
     */
    public void synchronizeMessageFilesPostRun(String status, int countCopied, int countErrors, String message);
}
