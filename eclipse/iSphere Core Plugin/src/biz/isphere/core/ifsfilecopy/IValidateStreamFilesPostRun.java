/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifsfilecopy;

public interface IValidateStreamFilesPostRun {

    /**
     * PostRun method called by {@link ValidateStreamFilesJob} at the end of the
     * validation process.
     * 
     * @param isCanceled - true, if the job has been canceled;otherwise false
     * @param countTotal - total number of IFS files processed
     * @param countSkipped - number of IFS files skipped
     * @param countValidated - number of IFS files validated
     * @param countErrors - number of IFS files that could not be processed
     * @param averageTime - average processing time per IFS file
     * @param cancelErrorId - reason for canceling the job
     * @param cancelMessage - job cancel error message
     */
    public void returnValidateIfsFilesResult(boolean isCanceled, int countTotal, int countSkipped, int countValidated, int countErrors,
        long averageTime, StreamFileCopyError cancelErrorId, String cancelMessage);

}
