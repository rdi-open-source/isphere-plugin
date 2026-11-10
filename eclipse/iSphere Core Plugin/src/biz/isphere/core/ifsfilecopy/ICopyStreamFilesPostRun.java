/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifsfilecopy;

import biz.isphere.core.ifsfilecopy.rse.CopyIfsFilesJob;

public interface ICopyStreamFilesPostRun {

    /**
     * PostRun methods called by {@link CopyIfsFilesJob} at the end of the copy
     * IFS file process.
     *
     * @param isCanceled - true, if the job has been canceled;otherwise false
     * @param countTotal - total number of IFS files processed
     * @param countSkipped - number of IFS files skipped
     * @param countCopied - number of IFS files copied
     * @param countErrors - number of IFS files that could not be processed
     * @param averageTime - average processing time per IFS file
     * @param cancelErrorId - reason for canceling the job
     * @param cancelMessage - job cancel error message
     */
    public void returnCopyIfsFilesResult(boolean isCanceled, int countTotal, int countSkipped, int countCopied, int countErrors, long averageTime,
        StreamFileCopyError cancelErrorId, String cancelMessage);

}
