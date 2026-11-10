/*******************************************************************************
 * Copyright (c) 2012-2024 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.objectsynchronization;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.data.PcmlException;
import com.ibm.as400.data.ProgramCallDocument;

import biz.isphere.core.ISpherePlugin;

public class SYNCMBR_resolveGenericMembers {

    private String errorMessage;

    private static final String STRING_FALSE = "0";
    private static final String STRING_TRUE = "1";

    public int run(AS400 _as400, int _handle, String mode, String memberFilter, boolean isRegEx) {

        int errno = 0;
        errorMessage = null;

        try {

            // Debug options:
            // Trace.setTraceOn(true); // Turn on tracing function.
            // Trace.setTracePCMLOn(true); // Turn on PCML tracing.

            ProgramCallDocument pcml = new ProgramCallDocument(_as400, "biz.isphere.core.objectsynchronization.SYNCMBR_resolveGenericMembers", //$NON-NLS-1$
                this.getClass().getClassLoader());

            pcml.setIntValue("SYNCMBR_resolveGenericMembers.handle", _handle); //$NON-NLS-1$
            pcml.setStringValue("SYNCMBR_resolveGenericMembers.mode", mode); //$NON-NLS-1$
            pcml.setStringValue("SYNCMBR_resolveGenericMembers.memberFilter", memberFilter); //$NON-NLS-1$
            pcml.setStringValue("SYNCMBR_resolveGenericMembers.isRegEx", toString(isRegEx)); //$NON-NLS-1$

            boolean rc = pcml.callProgram("SYNCMBR_resolveGenericMembers"); //$NON-NLS-1$

            if (rc == false) {

                AS400Message[] msgs = pcml.getMessageList("SYNCMBR_resolveGenericMembers"); //$NON-NLS-1$
                StringBuilder messages = new StringBuilder();
                for (int idx = 0; idx < msgs.length; idx++) {
                    ISpherePlugin.logError(msgs[idx].getID() + " - " + msgs[idx].getText(), null); //$NON-NLS-1$
                    if (messages.length() > 0) {
                        messages.append(" "); //$NON-NLS-1$
                    }
                    messages.append(msgs[idx].getID() + ": " + msgs[idx].getText()); //$NON-NLS-1$
                }
                errorMessage = messages.toString();
                ISpherePlugin.logError("*** Call to SYNCMBR_resolveGenericMembers failed. See messages above ***", null); //$NON-NLS-1$

                errno = -1;

            } else {

                errno = 1;

            }

        } catch (PcmlException e) {

            errno = -1;
            errorMessage = e.getLocalizedMessage();
            ISpherePlugin.logError("*** Call to SYNCMBR_resolveGenericMembers failed. See messages above ***", e); //$NON-NLS-1$
        }

        return errno;

    }

    /**
     * Returns the messages of the host, when the last call of
     * {@link #run(AS400, int, String, String, boolean)} failed.
     * 
     * @return host messages or <code>null</code>
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    private String toString(boolean isTrue) {
        if (isTrue) {
            return STRING_TRUE;
        }
        return STRING_FALSE;
    }
}