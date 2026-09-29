/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.messagefilecompare;

import biz.isphere.core.messagefileeditor.MessageDescription;

/**
 * Item that describes a single message description that is copied from one
 * message file to another one.
 */
public class CopyMessageDescriptionItem implements Comparable<CopyMessageDescriptionItem> {

    private MessageDescription fromMessageDescription;

    private String toConnectionName;
    private String toLibrary;
    private String toMessageFile;

    /**
     * Description of the message that has been copied to the target message
     * file. It is retrieved by the {@link SynchronizeMessageFilesJob}, after
     * the message description has successfully been copied.
     */
    private MessageDescription copiedMessageDescription;

    private String errorMessage;

    private Object data;

    public CopyMessageDescriptionItem(MessageDescription fromMessageDescription, String toConnectionName, String toLibrary, String toMessageFile) {

        this.fromMessageDescription = fromMessageDescription;

        this.toConnectionName = toConnectionName;
        this.toLibrary = toLibrary;
        this.toMessageFile = toMessageFile;
    }

    public MessageDescription getFromMessageDescription() {
        return fromMessageDescription;
    }

    public String getMessageId() {
        return fromMessageDescription.getMessageId();
    }

    public String getFromConnectionName() {
        return fromMessageDescription.getConnection();
    }

    public String getFromLibrary() {
        return fromMessageDescription.getLibrary();
    }

    public String getFromMessageFile() {
        return fromMessageDescription.getMessageFile();
    }

    public String getToConnectionName() {
        return toConnectionName;
    }

    public String getToLibrary() {
        return toLibrary;
    }

    public String getToMessageFile() {
        return toMessageFile;
    }

    public MessageDescription getCopiedMessageDescription() {
        return copiedMessageDescription;
    }

    public void setCopiedMessageDescription(MessageDescription copiedMessageDescription) {
        this.copiedMessageDescription = copiedMessageDescription;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getQualifiedFromMessageFileName() {
        return getFromLibrary() + "/" + getFromMessageFile(); //$NON-NLS-1$
    }

    public String getQualifiedToMessageFileName() {
        return getToLibrary() + "/" + getToMessageFile(); //$NON-NLS-1$
    }

    public int compareTo(CopyMessageDescriptionItem other) {

        int result = getToConnectionName().compareTo(other.getToConnectionName());
        if (result != 0) {
            return result;
        }

        result = getQualifiedToMessageFileName().compareTo(other.getQualifiedToMessageFileName());
        if (result != 0) {
            return result;
        }

        return getMessageId().compareTo(other.getMessageId());
    }

    @Override
    public String toString() {
        return getMessageId() + "[" + getQualifiedFromMessageFileName() + " -> " + getQualifiedToMessageFileName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
