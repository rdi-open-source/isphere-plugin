/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization;

import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import biz.isphere.base.internal.IFSFileHelper;
import biz.isphere.base.internal.StringHelper;
import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.Messages;
import biz.isphere.core.ifsfilecopy.CopyStreamFileItem;
import biz.isphere.core.ifsfilecopy.ICopyItemMessageListener;
import biz.isphere.core.ifsfilecopy.ICopyStreamFilesPostRun;
import biz.isphere.core.ifsfilecopy.IValidateItemMessageListener;
import biz.isphere.core.ifsfilecopy.IValidateStreamFilesPostRun;
import biz.isphere.core.ifsfilecopy.StreamFileCopyError;
import biz.isphere.core.ifsfilecopy.ValidateStreamFilesJob;
import biz.isphere.core.ifsfilecopy.rse.CopyIfsFilesJob;
import biz.isphere.core.ifsfilecopy.rse.ExistingIfsFileAction;
import biz.isphere.core.ifsfilecopy.rse.MissingDirectoryAction;
import biz.isphere.core.ifssynchronization.jobs.ICancelableJob;
import biz.isphere.core.ifssynchronization.jobs.ISynchronizeStreamFilesPostRun;
import biz.isphere.core.ifssynchronization.rse.StreamFileCompareItem;
import biz.isphere.core.internal.RemoteStreamFile;

public class SynchronizeStreamFilesJob extends Job implements ICancelableJob, IValidateStreamFilesPostRun, ICopyStreamFilesPostRun {

    private RemoteStreamFile leftDirectory;
    private RemoteStreamFile rightDirectory;
    private ISynchronizeStreamFilesPostRun postRun;

    private SortedSet<StreamFileCompareItem> copyToLeftItems;
    private SortedSet<StreamFileCompareItem> copyToRightItems;

    private IProgressMonitor monitor;

    private IValidateItemMessageListener validateItemErrorListener;
    private ICopyItemMessageListener copyItemErrorListener;

    private SyncResult syncResult;

    private MissingDirectoryAction missingDirectoryAction;
    private ExistingIfsFileAction existingIfsFileAction;
    private boolean isPreCheck;

    public SynchronizeStreamFilesJob(RemoteStreamFile leftDirectory, RemoteStreamFile rightDirectory, ISynchronizeStreamFilesPostRun postRun) {
        super(Messages.Copying_stream_files);

        this.leftDirectory = leftDirectory;
        this.rightDirectory = rightDirectory;
        this.postRun = postRun;

        this.copyToLeftItems = new TreeSet<StreamFileCompareItem>();
        this.copyToRightItems = new TreeSet<StreamFileCompareItem>();

        this.missingDirectoryAction = MissingDirectoryAction.ASK_USER;
        this.existingIfsFileAction = ExistingIfsFileAction.ERROR;
        this.isPreCheck = false;

        this.syncResult = null;
    }

    public void setValidateItemErrorListener(IValidateItemMessageListener itemErrorListener) {
        this.validateItemErrorListener = itemErrorListener;
    }

    public void setCopyItemErrorListener(ICopyItemMessageListener itemErrorListener) {
        this.copyItemErrorListener = itemErrorListener;
    }

    public void setMissingDirectoryAction(MissingDirectoryAction missingDirectoryAction) {
        this.missingDirectoryAction = missingDirectoryAction;
    }

    public void setExistingIfsFileAction(ExistingIfsFileAction existingIfsFileAction) {
        this.existingIfsFileAction = existingIfsFileAction;
    }

    public void setMonitor(IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    public void addCopyRightToLeftIfsFile(StreamFileCompareItem compareItem) {
        compareItem.resetErrorStatus();
        this.copyToLeftItems.add(compareItem);
    }

    public void addCopyLeftToRightIfsFile(StreamFileCompareItem compareItem) {
        compareItem.resetErrorStatus();
        this.copyToRightItems.add(compareItem);
    }

    public int getNumCopyRightToLeft() {
        return copyToLeftItems.size();
    }

    public int getNumCopyLeftToRight() {
        return copyToRightItems.size();
    }

    @Override
    public IStatus run(IProgressMonitor monitor) {

        this.monitor = monitor;

        try {

            syncResult = new SyncResult();

            CopyStreamFileItem[] toLeftIfsFiles = createIfsFileArray(leftDirectory, copyToLeftItems, StreamFileCompareItem.LEFT_MISSING);
            CopyStreamFileItem[] toRightIfsFiles = createIfsFileArray(rightDirectory, copyToRightItems, StreamFileCompareItem.RIGHT_MISSING);

            int totalIfsFiles = toLeftIfsFiles.length + toRightIfsFiles.length;

            SubMonitor progress;
            if (!isPreCheck) {
                progress = SubMonitor.convert(monitor).setWorkRemaining(totalIfsFiles);
            } else {
                progress = SubMonitor.convert(monitor).setWorkRemaining(totalIfsFiles * 2);

                SubMonitor subMonitorValidate = progress.newChild(totalIfsFiles).setWorkRemaining(totalIfsFiles);

                validateIfsFilesSync(subMonitorValidate, toLeftIfsFiles, toRightIfsFiles);

                if (isCanceled()) {
                    return Status.OK_STATUS;
                }
            }

            SubMonitor subMonitorCopy = progress.newChild(totalIfsFiles).setWorkRemaining(totalIfsFiles);

            copyIfsFilesSync(subMonitorCopy, toLeftIfsFiles, toRightIfsFiles);

            if (isCanceled()) {
                return Status.OK_STATUS;
            }

        } finally {
            monitor.done();

            if (isCanceled()) {
                String cancelMessage = getCancelMessage();
                if (!StringHelper.isNullOrEmpty(cancelMessage)) {
                    postRun.synchronizeIfsFilesPostRun(ISynchronizeStreamFilesPostRun.CANCELED, getCountCopied(), getCountErrors(),
                        Messages.bind(Messages.Operation_has_been_canceled_Reason_A, cancelMessage));
                } else {
                    postRun.synchronizeIfsFilesPostRun(ISynchronizeStreamFilesPostRun.CANCELED, getCountCopied(), getCountErrors(),
                        Messages.Operation_has_been_canceled_by_the_user);
                }
            } else {
                if (isError()) {
                    postRun.synchronizeIfsFilesPostRun(ISynchronizeStreamFilesPostRun.ERROR, getCountCopied(), getCountErrors(),
                        getIfsFileErrorMessage(getCountCopied(), getCountErrors()));
                } else {
                    postRun.synchronizeIfsFilesPostRun(ISynchronizeStreamFilesPostRun.OK, getCountCopied(), getCountErrors(),
                        getJobSuccessfulMessage());
                }
            }
        }

        return Status.OK_STATUS;
    }

    /**
     * Produces the sub monitor of one of the two synchronization directions, so
     * that both directions report their own progress.
     */
    private SubMonitor getSubMonitor(SubMonitor subMonitor, int partialLength) {
        SubMonitor partialSubMonitor = subMonitor.newChild(partialLength).setWorkRemaining(partialLength);
        return partialSubMonitor;
    }

    private void copyIfsFilesSync(SubMonitor subMonitor, CopyStreamFileItem[] toLeftIfsFiles, CopyStreamFileItem[] toRightIfsFiles) {

        copyToLeftOrRight(subMonitor, leftDirectory, rightDirectory, toRightIfsFiles);
        if (isCanceled()) {
            return;
        }

        copyToLeftOrRight(subMonitor, rightDirectory, leftDirectory, toLeftIfsFiles);
        if (isCanceled()) {
            return;
        }
    }

    private void validateIfsFilesSync(SubMonitor subMonitor, CopyStreamFileItem[] toLeftIfsFiles, CopyStreamFileItem[] toRightIfsFiles) {

        validateLeftOrRightIfsFiles(subMonitor, rightDirectory, leftDirectory, toLeftIfsFiles);
        if (isCanceled()) {
            return;
        }

        validateLeftOrRightIfsFiles(subMonitor, leftDirectory, rightDirectory, toRightIfsFiles);
        if (isCanceled()) {
            return;
        }
    }

    private void validateLeftOrRightIfsFiles(SubMonitor subMonitor, RemoteStreamFile fromDirectory, RemoteStreamFile toDirectory,
        CopyStreamFileItem[] copyIfsFileItems) {

        if (copyIfsFileItems.length == 0) {
            return;
        }

        SubMonitor childMonitor = getSubMonitor(subMonitor, copyIfsFileItems.length);

        ValidateStreamFilesJob validatorJob = new ValidateStreamFilesJob(fromDirectory.getConnectionName(), copyIfsFileItems, this);
        validatorJob.addItemErrorListener(validateItemErrorListener);
        validatorJob.setToConnectionName(toDirectory.getConnectionName());

        validatorJob.setExistingIfsFileAction(existingIfsFileAction);
        validatorJob.setMissingDirectoryAction(missingDirectoryAction);
        validatorJob.setIgnoreUnsavedChanges(false);
        validatorJob.setFullErrorCheck(false);
        validatorJob.setRenameIfsFileCheck(true);

        validatorJob.runInSameThread(childMonitor);
    }

    private void copyToLeftOrRight(SubMonitor subMonitor, RemoteStreamFile fromDirectory, RemoteStreamFile toDirectory,
        CopyStreamFileItem[] copyIfsFileItems) {

        if (copyIfsFileItems.length == 0) {
            return;
        }

        SubMonitor childMonitor = getSubMonitor(subMonitor, copyIfsFileItems.length);

        CopyIfsFilesJob copyIfsFilesJob = new CopyIfsFilesJob(fromDirectory.getConnectionName(), toDirectory.getConnectionName(), copyIfsFileItems,
            this);
        if (copyItemErrorListener != null) {
            copyIfsFilesJob.addItemErrorListener(copyItemErrorListener);
        }

        copyIfsFilesJob.setExistingIfsFileAction(existingIfsFileAction);
        copyIfsFilesJob.setMissingDirectoryAction(missingDirectoryAction);
        copyIfsFilesJob.setIgnoreUnsavedChanges(false);
        copyIfsFilesJob.setFullErrorCheck(false);
        copyIfsFilesJob.setRenameIfsFileCheck(true);

        copyIfsFilesJob.runInSameThread(childMonitor);
    }

    /**
     * Produces the list of items that are copied to a given target directory.
     * <p>
     * The source path is the absolute path of the item, the target path is
     * produced from the root directory of the target side and the relative path
     * of the item.
     * 
     * @param toDirectory - directory the items are copied to
     * @param compareItems - items of the compare table that are synchronized
     * @param compareStatus - compare status that identifies the side the items
     *        are copied to
     * @return items that are copied
     */
    private CopyStreamFileItem[] createIfsFileArray(RemoteStreamFile toDirectory, SortedSet<StreamFileCompareItem> compareItems, int compareStatus) {

        SortedSet<CopyStreamFileItem> ifsFilesToCopy = new TreeSet<CopyStreamFileItem>();

        for (StreamFileCompareItem compareItem : compareItems) {

            StreamFileDescription fromIfsFileDescription;
            if (compareStatus == StreamFileCompareItem.LEFT_MISSING) {
                fromIfsFileDescription = compareItem.getRightIfsFileDescription();
            } else if (compareStatus == StreamFileCompareItem.RIGHT_MISSING) {
                fromIfsFileDescription = compareItem.getLeftIfsFileDescription();
            } else {
                continue;
            }

            if (fromIfsFileDescription == null) {
                continue;
            }

            String fromIfsFileName = fromIfsFileDescription.getAbsolutePath();
            String toIfsFileName = IFSFileHelper.toAbsolutePath(toDirectory.getName(), fromIfsFileDescription.getRelativePath());

            if (fromIfsFileName == null || toIfsFileName == null) {
                ISpherePlugin.logError("*** Could not resolve the paths of IFS item " + fromIfsFileDescription.getRelativePath() + " ***", null); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }

            CopyStreamFileItem ifsFileItem;
            if (fromIfsFileDescription.isDirectory()) {
                ifsFileItem = CopyStreamFileItem.newDirectory(fromIfsFileName);
            } else {
                ifsFileItem = CopyStreamFileItem.newFile(fromIfsFileName);
            }

            ifsFileItem.setToIfsFile(toIfsFileName);
            ifsFileItem.setData(compareItem);
            ifsFilesToCopy.add(ifsFileItem);
        }

        return ifsFilesToCopy.toArray(new CopyStreamFileItem[ifsFilesToCopy.size()]);
    }

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
     */
    public void returnValidateIfsFilesResult(boolean isCanceled, int countTotal, int countSkipped, int countValidated, int countErrors,
        long averageTime, StreamFileCopyError errorId, String cancelMessage) {

        syncResult.countErrors = syncResult.countErrors + countErrors;
        syncResult.countValidated = syncResult.countValidated + countValidated;
        syncResult.cancelMessage = cancelMessage;

        debug("\nSynchronizeIfsFilesJob.returnValidateIfsFilesResult:"); //$NON-NLS-1$
        debug("is canceled:    " + isCanceled); //$NON-NLS-1$
        debug("total #:        " + countTotal); //$NON-NLS-1$
        debug("skipped #:      " + countSkipped); //$NON-NLS-1$
        debug("validated #:    " + countValidated); //$NON-NLS-1$
        debug("errors #:       " + countErrors); //$NON-NLS-1$
        debug("average time:   " + averageTime + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
        debug("error id:       " + errorId); //$NON-NLS-1$
        debug("cancel message: " + cancelMessage); //$NON-NLS-1$
    }

    /**
     * PostRun methods called by {@link CopyIfsFilesJob} at the end of the copy
     * IFS file process.
     *
     * @param isCanceled - true, if the job has been canceled;otherwise false
     * @param countTotal - total number of IFS files processed
     * @param countSkipped - number of IFS files skipped
     * @param countCopied - number of IFS files processed fine
     * @param countErrors - number of IFS files that could not be processed
     * @param averageTime - average processing time per IFS file
     */
    public void returnCopyIfsFilesResult(final boolean isCanceled, final int countTotal, final int countSkipped, final int countCopied,
        final int countErrors, final long averageTime, final StreamFileCopyError errorId, final String cancelMessage) {

        syncResult.countErrors = syncResult.countErrors + countErrors;
        syncResult.countCopied = syncResult.countCopied + countCopied;
        syncResult.cancelMessage = cancelMessage;

        debug("\nSynchronizeIfsFilesJob.returnCopyIfsFilesResult:"); //$NON-NLS-1$
        debug("is canceled:    " + isCanceled); //$NON-NLS-1$
        debug("total #:        " + countTotal); //$NON-NLS-1$
        debug("skipped #:      " + countSkipped); //$NON-NLS-1$
        debug("copied #:       " + countCopied); //$NON-NLS-1$
        debug("errors #:       " + countErrors); //$NON-NLS-1$
        debug("average time:   " + averageTime + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
        debug("error id:       " + errorId); //$NON-NLS-1$
        debug("cancel message: " + cancelMessage); //$NON-NLS-1$
    }

    private boolean isError() {
        if (syncResult.countErrors > 0) {
            return true;
        }
        return false;
    }

    private String getJobSuccessfulMessage() {
        return Messages.bind(Messages.Successfully_copied_A_stream_files, getCountCopied());
    }

    private String getIfsFileErrorMessage(int countCopied, int countErrors) {
        if (countCopied == 0) {
            return Messages.bind(Messages.Could_not_copy_A_stream_files_due_to_errors, getCountErrors());
        } else {
            return Messages.bind(Messages.A_stream_files_copied_B_stream_files_not_copied_due_to_errors, countCopied, countErrors);
        }
    }

    private int getCountCopied() {
        int countCopied = syncResult.countCopied;
        return countCopied;
    }

    private int getCountErrors() {
        int countErrors = syncResult.countErrors;
        return countErrors;
    }

    private String getCancelMessage() {
        String message = syncResult.cancelMessage;
        return message;
    }

    public void cancelOperation() {
        monitor.setCanceled(true);
    }

    public boolean isCanceled() {
        return monitor.isCanceled();
    }

    private void debug(String message) {
        // System.out.println(message);
    }

    private class SyncResult {
        public String cancelMessage;
        public int countValidated;
        public int countCopied;
        public int countErrors;
    }
}
