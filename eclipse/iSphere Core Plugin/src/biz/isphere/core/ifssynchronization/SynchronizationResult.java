/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import biz.isphere.core.ifssynchronization.rse.StreamFileCompareItem;

public class SynchronizationResult {

    private String status;
    private int countCopied;
    private int countErrors;
    private String jobFinishedMessage;
    private List<String> errorMessages;
    private List<StreamFileCompareItem> dirtyIfsFiles;

    public SynchronizationResult() {
        this.errorMessages = new LinkedList<String>();
        this.dirtyIfsFiles = new ArrayList<StreamFileCompareItem>();
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

    public void addDirtyIfsFile(StreamFileCompareItem ifsFile) {
        dirtyIfsFiles.add(ifsFile);
    }

    public boolean hasDirtyIfsFiles() {
        if (dirtyIfsFiles.size() > 0) {
            return true;
        }

        return false;
    }

    public StreamFileCompareItem[] getDirtyIfsFiles() {
        return dirtyIfsFiles.toArray(new StreamFileCompareItem[dirtyIfsFiles.size()]);
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

        for (String errorMessages : errorMessages) {
            buffer.append(errorMessages);
            buffer.append("\n"); //$NON-NLS-1$
        }

        return buffer.toString();
    }
}
