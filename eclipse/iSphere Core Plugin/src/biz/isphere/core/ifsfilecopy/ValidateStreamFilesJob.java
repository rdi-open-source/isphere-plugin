/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifsfilecopy;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import biz.isphere.core.Messages;
import biz.isphere.core.ifsfilecopy.rse.CopyIfsFilesJob;
import biz.isphere.core.ifsfilecopy.rse.ExistingIfsFileAction;
import biz.isphere.core.ifsfilecopy.rse.MissingDirectoryAction;
import biz.isphere.core.ifsfilerename.rules.IStreamFileRenamingRule;
import biz.isphere.core.sourcemembercopy.AbstractResult;

/**
 * This job validates a list of IFS files that are about to be copied from a
 * source directory to a target directory. It is the IFS counterpart of
 * {@link biz.isphere.core.sourcemembercopy.ValidateMembersJob}.
 * <p>
 * The job delegates the work to a {@link CopyIfsFilesJob} that is run in
 * validation mode.
 */
public class ValidateStreamFilesJob extends Job implements ICopyItemMessageListener {

    private Set<IValidateItemMessageListener> itemMessageListeners;

    private String fromConnectionName;
    private CopyStreamFileItem[] ifsFiles;
    private ExistingIfsFileAction existingIfsFileAction;
    private MissingDirectoryAction missingDirectoryAction;
    private boolean isIgnoreUnsavedChangesError;
    private boolean isFullErrorCheck;
    private boolean isRenameIfsFileCheck;
    private IValidateStreamFilesPostRun postRun;

    private IStreamFileRenamingRule ifsFileRenamingRule;

    private CopyIfsFilesJob copyIfsFilesJob;

    private IProgressMonitor monitor;

    private String toConnectionName;

    private CopyResult copyResult;

    public ValidateStreamFilesJob(String fromConnectionName, CopyStreamFileItem[] ifsFiles, IValidateStreamFilesPostRun postRun) {
        super(Messages.Validating_dots);

        this.itemMessageListeners = null;

        this.fromConnectionName = fromConnectionName;
        this.ifsFiles = ifsFiles;
        this.existingIfsFileAction = ExistingIfsFileAction.ERROR;
        this.missingDirectoryAction = MissingDirectoryAction.ERROR;
        this.isIgnoreUnsavedChangesError = false;
        this.isFullErrorCheck = false;
        this.isRenameIfsFileCheck = true;
        this.postRun = postRun;
    }

    public void addItemErrorListener(IValidateItemMessageListener listener) {
        getItemMessageListeners().add(listener);
    }

    public void setMissingDirectoryAction(MissingDirectoryAction missingDirectoryAction) {
        this.missingDirectoryAction = missingDirectoryAction;
    }

    public void setExistingIfsFileAction(ExistingIfsFileAction existingIfsFileAction) {
        this.existingIfsFileAction = existingIfsFileAction;
    }

    public void setIgnoreUnsavedChanges(boolean enabled) {
        this.isIgnoreUnsavedChangesError = enabled;
    }

    public void setFullErrorCheck(boolean enabled) {
        this.isFullErrorCheck = enabled;
    }

    public void setRenameIfsFileCheck(boolean enabled) {
        this.isRenameIfsFileCheck = enabled;
    }

    /**
     * Sets the rule that is used for producing the backup name of an IFS file
     * that is replaced.
     * 
     * @param ifsFileRenamingRule - IFS file renaming rule
     */
    public void setIfsFileRenamingRule(IStreamFileRenamingRule ifsFileRenamingRule) {
        this.ifsFileRenamingRule = ifsFileRenamingRule;
    }

    public void setToConnectionName(String connectionName) {
        this.toConnectionName = connectionName;
    }

    @Override
    public IStatus run(IProgressMonitor monitor) {

        SubMonitor subMonitor = SubMonitor.convert(monitor, ifsFiles.length);

        try {
            runInSameThread(subMonitor);
        } finally {
            subMonitor.done();
        }

        return Status.OK_STATUS;
    }

    public void runInSameThread(SubMonitor subMonitor) {

        monitor = subMonitor;

        try {

            copyResult = new CopyResult();

            copyIfsFilesJob = new CopyIfsFilesJob(this.fromConnectionName, this.toConnectionName, ifsFiles, null);
            this.copyIfsFilesJob.addItemErrorListener(this);
            this.copyIfsFilesJob.setValidationMode(true);
            this.copyIfsFilesJob.setMissingDirectoryAction(this.missingDirectoryAction);
            this.copyIfsFilesJob.setExistingIfsFileAction(this.existingIfsFileAction);
            this.copyIfsFilesJob.setIgnoreUnsavedChanges(this.isIgnoreUnsavedChangesError);
            this.copyIfsFilesJob.setFullErrorCheck(this.isFullErrorCheck);
            this.copyIfsFilesJob.setRenameIfsFileCheck(this.isRenameIfsFileCheck);
            this.copyIfsFilesJob.setIfsFileRenamingRule(this.ifsFileRenamingRule);
            copyIfsFilesJob.runInSameThread(subMonitor);

        } finally {
            if (postRun != null) {
                postRun.returnValidateIfsFilesResult(monitor.isCanceled(), copyResult.getTotal(), copyResult.getSkipped(), copyResult.getProcessed(),
                    copyResult.getErrors(), copyResult.getAverageTime(), copyResult.getCancelErrorId(), copyResult.getCancelMessage());
            }
        }
    }

    private Set<IValidateItemMessageListener> getItemMessageListeners() {

        if (itemMessageListeners == null) {
            itemMessageListeners = new HashSet<IValidateItemMessageListener>();
        }

        return itemMessageListeners;
    }

    public boolean isError() {
        return copyResult.isError();
    }

    public int getCountTotal() {
        return copyResult.getTotal();
    }

    public int getCountSkipped() {
        return copyResult.getSkipped();
    }

    public int getIfsFilesValidatedCount() {
        return copyResult.getProcessed();
    }

    public int getIfsFilesErrorCount() {
        return copyResult.getErrors();
    }

    public long getAverageTime() {
        return copyResult.getAverageTime();
    }

    public void cancelOperation() {
        monitor.setCanceled(true);
    }

    public boolean isCanceled() {
        return monitor.isCanceled();
    }

    /**
     * IFS file error callback of the {@link CopyIfsFilesJob} that is run in
     * validation mode. The messages are forwarded to the
     * {@link IValidateItemMessageListener} listeners of this job.
     * <p>
     * {@inheritDoc}
     */
    public SynchronizeStreamFilesAction reportCopyIfsFileMessage(StreamFileCopyError errorId, CopyStreamFileItem item, String errorMessage) {

        if (itemMessageListeners != null) {
            for (IValidateItemMessageListener listener : itemMessageListeners) {
                SynchronizeStreamFilesAction response = listener.reportValidateIfsFileMessage(errorId, item, errorMessage);
                if (response == SynchronizeStreamFilesAction.CANCEL) {
                    setItemErrorMessage(item, errorMessage);
                    copyResult.addError();
                    copyResult.setCancel(errorId, errorMessage);
                    cancelOperation();
                } else if (response == SynchronizeStreamFilesAction.CONTINUE_WITH_ERROR) {
                    setItemErrorMessage(item, errorMessage);
                    copyResult.addError();
                } else {
                    // Continue
                }
            }
        }

        return errorId.getDefaultAction();
    }

    /**
     * Assigns an error message to a given item. The item is <code>null</code>,
     * when the {@link CopyIfsFilesJob} reports a directory or connection error
     * that aborts the job.
     */
    private void setItemErrorMessage(CopyStreamFileItem item, String errorMessage) {

        if (item == null) {
            return;
        }

        item.setErrorMessage(errorMessage);
    }

    private class CopyResult extends AbstractResult<StreamFileCopyError> {
    };
}
