/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization.jobs;

import biz.isphere.core.ifssynchronization.StreamFileDescription;

public interface ICompareStreamFilesPostrun {

    /**
     * PostRun methods called by {@link CompareStreamFilesJob} at the end of the
     * IFS files compare and load process.
     * 
     * @param isCanceled - true, if the job has been canceled;otherwise false
     * @param leftIfsFileDescriptions - IFS file descriptions of the left side
     * @param rightIfsFileDescriptions - IFS file descriptions of the right side
     */
    public void compareIfsFilesPostRun(boolean isCanceled, StreamFileDescription[] leftIfsFileDescriptions,
        StreamFileDescription[] rightIfsFileDescriptions);
}
