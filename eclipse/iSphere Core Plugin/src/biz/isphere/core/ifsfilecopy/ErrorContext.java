/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifsfilecopy;

import biz.isphere.core.internal.RemoteStreamFile;

public class ErrorContext {

    private RemoteStreamFile fromObject;
    private RemoteStreamFile toObject;
    private CopyStreamFileItem copyIfsFileItem;

    public static ErrorContext newFromIfsFile(String fromConnectionName, String fromIfsFile) {
        ErrorContext errorContext = new ErrorContext();
        errorContext.setFromObject(RemoteStreamFile.newFile(fromConnectionName, fromIfsFile));
        return errorContext;
    }

    public static ErrorContext newToIfsFile(String toConnectionName, String toIfsFile) {
        ErrorContext errorContext = new ErrorContext();
        errorContext.setToObject(RemoteStreamFile.newFile(toConnectionName, toIfsFile));
        return errorContext;
    }

    public RemoteStreamFile getFromObject() {
        return fromObject;
    }

    public void setFromObject(RemoteStreamFile fromObject) {
        this.fromObject = fromObject;
    }

    public RemoteStreamFile getToObject() {
        return toObject;
    }

    public void setToObject(RemoteStreamFile toObject) {
        this.toObject = toObject;
    }

    public CopyStreamFileItem getCopyIfsFileItem() {
        return copyIfsFileItem;
    }

    public void setCopyIfsFileItem(CopyStreamFileItem copyIfsFileItem) {
        this.copyIfsFileItem = copyIfsFileItem;
    }
}
