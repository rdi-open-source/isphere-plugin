/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.messagefilecompare;

public interface ICopyItemMessageListener {

    /**
     * Method called by the {@link SynchronizeMessageFilesJob} for each message
     * description that has been copied and for each message description that is
     * in error.
     *
     * @param errorId - ID identifying the error
     * @param item - copy message description item the message is reported for.
     *        It is <code>null</code>, when the message is not related to a
     *        single item, such as a message file or connection error.
     * @param errorMessage - error message text
     * @return action that tells the job how to continue
     */
    public SynchronizeMessageFilesAction reportCopyMessageDescriptionMessage(MessageDescriptionCopyError errorId, CopyMessageDescriptionItem item,
        String errorMessage);
}
