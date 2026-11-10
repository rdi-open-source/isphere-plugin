/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization.jobs;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import biz.isphere.core.Messages;
import biz.isphere.core.ifssynchronization.StreamFileDescription;
import biz.isphere.core.ifssynchronization.SynchronizeStreamFilesEditorInput;

public class CompareStreamFilesJob extends Job implements ICancelableJob {

    private SynchronizeStreamFilesEditorInput input;
    private CompareStreamFilesSharedJobValues sharedValues;
    private ICompareStreamFilesPostrun postRun;

    private SubMonitor subMonitor;

    public CompareStreamFilesJob(SynchronizeStreamFilesEditorInput input, CompareStreamFilesSharedJobValues sharedValues, ICompareStreamFilesPostrun postRun) {
        super(Messages.Job_Loading_stream_files);

        this.input = input;
        this.sharedValues = sharedValues;
        this.postRun = postRun;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

        subMonitor = SubMonitor.convert(monitor, 4 * 2);

        List<AbstractCompareStreamFilesJob> workerJobs = new LinkedList<AbstractCompareStreamFilesJob>();

        StartCompareStreamFilesJob startIfsFilesJob = new StartCompareStreamFilesJob(subMonitor, sharedValues, input.getLeftObject(),
            input.getRightObject());
        workerJobs.add(startIfsFilesJob);

        ResolveGenericFilesJob resolveGenericFilesJob = new ResolveGenericFilesJob(subMonitor, sharedValues);
        workerJobs.add(resolveGenericFilesJob);

        LoadCompareStreamFilesJob loadIfsFilesJob = new LoadCompareStreamFilesJob(subMonitor, sharedValues);
        workerJobs.add(loadIfsFilesJob);

        FinishCompareStreamFilesJob finishIfsFilesJob = new FinishCompareStreamFilesJob(subMonitor, sharedValues);
        workerJobs.add(finishIfsFilesJob);

        StreamFileDescription[] leftIfsFileDescriptions = new StreamFileDescription[0];
        StreamFileDescription[] rightIfsFileDescriptions = new StreamFileDescription[0];

        try {

            for (AbstractCompareStreamFilesJob workerJob : workerJobs) {
                workerJob.run();
            }

            leftIfsFileDescriptions = loadIfsFilesJob.getLeftIfsFiles();
            rightIfsFileDescriptions = loadIfsFilesJob.getRightIfsFiles();

        } finally {

            finishIfsFilesJob.run();

            subMonitor.done();

            postRun.compareIfsFilesPostRun(subMonitor.isCanceled(), leftIfsFileDescriptions, rightIfsFileDescriptions);
        }

        return Status.OK_STATUS;
    }

    public void cancelOperation() {
        subMonitor.setCanceled(true);
    }
}
