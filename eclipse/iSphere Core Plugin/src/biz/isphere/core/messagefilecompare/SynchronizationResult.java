/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.messagefilecompare;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import biz.isphere.core.messagefilecompare.rse.MessageFileCompareItem;

public class SynchronizationResult {

    private String status;
    private int countCopied;
    private int countErrors;
    private String jobFinishedMessage;
    private List<String> errorMessages;
    private List<MessageFileCompareItem> dirtyMessageDescriptions;

    public SynchronizationResult() {
        this.errorMessages = new LinkedList<String>();
        this.dirtyMessageDescriptions = new ArrayList<MessageFileCompareItem>();
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }

    public void setCountCopied(int countCopied) {
        this.countCopied = countCopied;
    }

    public int getCountCopied() {
        return this.countCopied;
    }

    public void setCountErrors(int countErrors) {
        this.countErrors = countErrors;
    }

    public int getCountErrors() {
        return this.countErrors;
    }

    public void setJobFinishedMessage(String jobFinishedMessage) {
        this.jobFinishedMessage = jobFinishedMessage;
    }

    public String getJobFinishedMessage() {
        return this.jobFinishedMessage;
    }

    public void addDirtyMessageDescription(MessageFileCompareItem compareItem) {
        dirtyMessageDescriptions.add(compareItem);
    }

    public boolean hasDirtyMessageDescriptions() {
        if (dirtyMessageDescriptions.size() > 0) {
            return true;
        }

        return false;
    }

    public MessageFileCompareItem[] getDirtyMessageDescriptions() {
        return dirtyMessageDescriptions.toArray(new MessageFileCompareItem[dirtyMessageDescriptions.size()]);
    }

    public void addErrorMessage(String message) {
        errorMessages.add(message);
    }

    public boolean hasErrorMessages() {
        if (errorMessages.size() > 0) {
            return true;
        }

        return false;
    }

    public String[] getErrorMesssages() {
        return errorMessages.toArray(new String[errorMessages.size()]);
    }

    @Override
    public String toString() {

        StringBuilder buffer = new StringBuilder();

        for (String errorMessage : errorMessages) {
            buffer.append(errorMessage);
            buffer.append("\n"); //$NON-NLS-1$
        }

        return buffer.toString();
    }
}
