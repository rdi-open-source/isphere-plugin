/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.messagefilecompare;

import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import biz.isphere.base.internal.ExceptionHelper;
import biz.isphere.base.internal.StringHelper;
import biz.isphere.core.Messages;
import biz.isphere.core.ibmi.contributions.extension.handler.IBMiHostContributionsHandler;
import biz.isphere.core.internal.MessageDescriptionHelper;
import biz.isphere.core.internal.RemoteObject;
import biz.isphere.core.messagefilecompare.jobs.ICancelableJob;
import biz.isphere.core.messagefilecompare.jobs.ISynchronizeMessageFilesPostRun;
import biz.isphere.core.messagefilecompare.rse.ExistingMessageDescriptionAction;
import biz.isphere.core.messagefilecompare.rse.MessageFileCompareItem;
import biz.isphere.core.messagefileeditor.MessageDescription;

/**
 * Job that copies the message descriptions that have been selected for copying
 * in the iSphere Compare Message Files editor.
 * <p>
 * Unlike the source member and the stream file synchronization, the copy
 * process is not split into a validation and a copy job, because copying a
 * message description is a single host command and there is no second caller
 * that could reuse the parts on its own. The interface of this job is the same
 * as that of {@code SynchronizeMembersJob} and
 * {@code SynchronizeStreamFilesJob}, so that the editors all look alike.
 */
public class SynchronizeMessageFilesJob extends Job implements ICancelableJob {

    private RemoteObject leftMessageFile;
    private RemoteObject rightMessageFile;
    private ISynchronizeMessageFilesPostRun postRun;

    private SortedSet<MessageFileCompareItem> copyToLeftItems;
    private SortedSet<MessageFileCompareItem> copyToRightItems;

    private IProgressMonitor monitor;

    private ICopyItemMessageListener copyItemErrorListener;

    private ExistingMessageDescriptionAction existingMessageDescriptionAction;

    private SyncResult syncResult;

    public SynchronizeMessageFilesJob(RemoteObject leftMessageFile, RemoteObject rightMessageFile, ISynchronizeMessageFilesPostRun postRun) {
        super(Messages.Copying_message_descriptions);

        this.leftMessageFile = leftMessageFile;
        this.rightMessageFile = rightMessageFile;
        this.postRun = postRun;

        this.copyToLeftItems = new TreeSet<MessageFileCompareItem>();
        this.copyToRightItems = new TreeSet<MessageFileCompareItem>();

        this.existingMessageDescriptionAction = ExistingMessageDescriptionAction.ERROR;

        this.syncResult = null;
    }

    public void setCopyItemErrorListener(ICopyItemMessageListener itemErrorListener) {
        this.copyItemErrorListener = itemErrorListener;
    }

    public void setExistingMessageDescriptionAction(ExistingMessageDescriptionAction existingMessageDescriptionAction) {
        this.existingMessageDescriptionAction = existingMessageDescriptionAction;
    }

    public void addCopyRightToLeftMessageDescription(MessageFileCompareItem compareItem) {
        compareItem.resetErrorStatus();
        this.copyToLeftItems.add(compareItem);
    }

    public void addCopyLeftToRightMessageDescription(MessageFileCompareItem compareItem) {
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

            CopyMessageDescriptionItem[] toLeftMessageDescriptions = createCopyItemArray(leftMessageFile, copyToLeftItems,
                MessageFileCompareItem.LEFT_MISSING);
            CopyMessageDescriptionItem[] toRightMessageDescriptions = createCopyItemArray(rightMessageFile, copyToRightItems,
                MessageFileCompareItem.RIGHT_MISSING);

            int totalMessageDescriptions = toLeftMessageDescriptions.length + toRightMessageDescriptions.length;

            SubMonitor progress = SubMonitor.convert(monitor).setWorkRemaining(totalMessageDescriptions);

            if (!checkConnections(toLeftMessageDescriptions, toRightMessageDescriptions)) {
                return Status.OK_STATUS;
            }

            copyMessageDescriptions(getSubMonitor(progress, toRightMessageDescriptions.length), toRightMessageDescriptions);
            if (isCanceled()) {
                return Status.OK_STATUS;
            }

            copyMessageDescriptions(getSubMonitor(progress, toLeftMessageDescriptions.length), toLeftMessageDescriptions);
            if (isCanceled()) {
                return Status.OK_STATUS;
            }

        } finally {
            monitor.done();

            if (isCanceled()) {
                String cancelMessage = getCancelMessage();
                if (!StringHelper.isNullOrEmpty(cancelMessage)) {
                    postRun.synchronizeMessageFilesPostRun(ISynchronizeMessageFilesPostRun.CANCELED, getCountCopied(), getCountErrors(),
                        Messages.bind(Messages.Operation_has_been_canceled_Reason_A, cancelMessage));
                } else {
                    postRun.synchronizeMessageFilesPostRun(ISynchronizeMessageFilesPostRun.CANCELED, getCountCopied(), getCountErrors(),
                        Messages.Operation_has_been_canceled_by_the_user);
                }
            } else {
                if (isError()) {
                    postRun.synchronizeMessageFilesPostRun(ISynchronizeMessageFilesPostRun.ERROR, getCountCopied(), getCountErrors(),
                        getMessageDescriptionErrorMessage(getCountCopied(), getCountErrors()));
                } else {
                    postRun.synchronizeMessageFilesPostRun(ISynchronizeMessageFilesPostRun.OK, getCountCopied(), getCountErrors(),
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

    /**
     * Ensures that the systems of both sides can be reached, before the first
     * message description is copied. The error is reported without an item,
     * because it is not related to a single message description.
     */
    private boolean checkConnections(CopyMessageDescriptionItem[] toLeftMessageDescriptions,
        CopyMessageDescriptionItem[] toRightMessageDescriptions) {

        if (toRightMessageDescriptions.length > 0) {
            if (!checkConnection(MessageDescriptionCopyError.ERROR_FROM_CONNECTION_NOT_FOUND, leftMessageFile.getConnectionName())) {
                return false;
            }
            if (!checkConnection(MessageDescriptionCopyError.ERROR_TO_CONNECTION_NOT_FOUND, rightMessageFile.getConnectionName())) {
                return false;
            }
        }

        if (toLeftMessageDescriptions.length > 0) {
            if (!checkConnection(MessageDescriptionCopyError.ERROR_FROM_CONNECTION_NOT_FOUND, rightMessageFile.getConnectionName())) {
                return false;
            }
            if (!checkConnection(MessageDescriptionCopyError.ERROR_TO_CONNECTION_NOT_FOUND, leftMessageFile.getConnectionName())) {
                return false;
            }
        }

        return true;
    }

    private boolean checkConnection(MessageDescriptionCopyError errorId, String connectionName) {

        if (IBMiHostContributionsHandler.getSystem(connectionName) != null) {
            return true;
        }

        String errorMessage = Messages.bind(Messages.Connection_A_not_found, connectionName);

        syncResult.countErrors = syncResult.countErrors + 1;
        syncResult.cancelMessage = errorMessage;

        reportMessage(errorId, null, errorMessage);
        cancelOperation();

        return false;
    }

    private void copyMessageDescriptions(SubMonitor subMonitor, CopyMessageDescriptionItem[] copyItems) {

        for (CopyMessageDescriptionItem copyItem : copyItems) {

            if (isCanceled()) {
                return;
            }

            subMonitor.subTask(copyItem.getMessageId());

            copyMessageDescription(copyItem);

            subMonitor.worked(1);
        }
    }

    private void copyMessageDescription(CopyMessageDescriptionItem copyItem) {

        try {

            if (ExistingMessageDescriptionAction.ERROR == existingMessageDescriptionAction && MessageDescriptionHelper
                .exists(copyItem.getToConnectionName(), copyItem.getToMessageFile(), copyItem.getToLibrary(), copyItem.getMessageId())) {
                reportError(MessageDescriptionCopyError.ERROR_TO_MESSAGE_DESCRIPTION_EXISTS, copyItem, Messages.bind(
                    Messages.Message_description_A_already_exists_in_B, copyItem.getMessageId(), copyItem.getQualifiedToMessageFileName()));
                return;
            }

            String errorMessage = MessageDescriptionHelper.mergeMessageDescription(copyItem.getFromMessageDescription(),
                copyItem.getToConnectionName(), copyItem.getToMessageFile(), copyItem.getToLibrary());
            if (errorMessage != null) {
                reportError(MessageDescriptionCopyError.ERROR_MERGE_COMMAND, copyItem, errorMessage);
                return;
            }

            /*
             * The description of the copied message is retrieved here, in the
             * job, so that the editor does not have to go to the host again,
             * when it updates the compare item.
             */
            copyItem.setCopiedMessageDescription(MessageDescriptionHelper.retrieveMessageDescription(copyItem.getToConnectionName(),
                copyItem.getToMessageFile(), copyItem.getToLibrary(), copyItem.getMessageId()));

            syncResult.countCopied = syncResult.countCopied + 1;

            reportMessage(MessageDescriptionCopyError.ERROR_NONE, copyItem, null);

        } catch (Exception e) {
            reportError(MessageDescriptionCopyError.ERROR_EXCEPTION, copyItem, ExceptionHelper.getLocalizedMessage(e));
        }
    }

    private void reportError(MessageDescriptionCopyError errorId, CopyMessageDescriptionItem copyItem, String errorMessage) {

        copyItem.setErrorMessage(errorMessage);

        syncResult.countErrors = syncResult.countErrors + 1;

        SynchronizeMessageFilesAction response = reportMessage(errorId, copyItem, errorMessage);
        if (response == SynchronizeMessageFilesAction.CANCEL) {
            syncResult.cancelMessage = errorMessage;
            cancelOperation();
        }
    }

    private SynchronizeMessageFilesAction reportMessage(MessageDescriptionCopyError errorId, CopyMessageDescriptionItem copyItem, String message) {

        if (copyItemErrorListener == null) {
            return errorId.getDefaultAction();
        }

        return copyItemErrorListener.reportCopyMessageDescriptionMessage(errorId, copyItem, message);
    }

    private CopyMessageDescriptionItem[] createCopyItemArray(RemoteObject toMessageFile, SortedSet<MessageFileCompareItem> compareItems,
        int compareStatus) {

        SortedSet<CopyMessageDescriptionItem> itemsToCopy = new TreeSet<CopyMessageDescriptionItem>();

        for (MessageFileCompareItem compareItem : compareItems) {

            MessageDescription fromMessageDescription;
            if (compareStatus == MessageFileCompareItem.LEFT_MISSING) {
                fromMessageDescription = compareItem.getRightMessageDescription();
            } else if (compareStatus == MessageFileCompareItem.RIGHT_MISSING) {
                fromMessageDescription = compareItem.getLeftMessageDescription();
            } else {
                continue;
            }

            if (fromMessageDescription == null) {
                continue;
            }

            CopyMessageDescriptionItem copyItem = new CopyMessageDescriptionItem(fromMessageDescription, toMessageFile.getConnectionName(),
                toMessageFile.getLibrary(), toMessageFile.getName());
            copyItem.setData(compareItem);

            itemsToCopy.add(copyItem);
        }

        return itemsToCopy.toArray(new CopyMessageDescriptionItem[itemsToCopy.size()]);
    }

    private boolean isError() {
        if (syncResult.countErrors > 0) {
            return true;
        }
        return false;
    }

    private String getJobSuccessfulMessage() {
        return Messages.bind(Messages.Successfully_copied_A_message_descriptions, getCountCopied());
    }

    private String getMessageDescriptionErrorMessage(int countCopied, int countErrors) {
        if (countCopied == 0) {
            return Messages.bind(Messages.Could_not_copy_A_message_descriptions_due_to_errors, countErrors);
        } else {
            return Messages.bind(Messages.A_message_descriptions_copied_B_message_descriptions_not_copied_due_to_errors, countCopied, countErrors);
        }
    }

    private int getCountCopied() {
        return syncResult.countCopied;
    }

    private int getCountErrors() {
        return syncResult.countErrors;
    }

    private String getCancelMessage() {
        return syncResult.cancelMessage;
    }

    public void cancelOperation() {
        monitor.setCanceled(true);
    }

    public boolean isCanceled() {
        return monitor.isCanceled();
    }

    private class SyncResult {
        public String cancelMessage;
        public int countCopied;
        public int countErrors;
    }
}
