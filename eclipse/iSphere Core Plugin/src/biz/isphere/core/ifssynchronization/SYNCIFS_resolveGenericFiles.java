/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.data.PcmlException;
import com.ibm.as400.data.ProgramCallDocument;

import biz.isphere.core.ISpherePlugin;

public class SYNCIFS_resolveGenericFiles {

    private static final String STRING_FALSE = "0";
    private static final String STRING_TRUE = "1";

    public int run(AS400 _as400, int _handle, String mode, String itemFilter, boolean isRegEx, int maxDepth) {

        int errno = 0;

        try {

            // Debug options:
            // Trace.setTraceOn(true); // Turn on tracing function.
            // Trace.setTracePCMLOn(true); // Turn on PCML tracing.

            ProgramCallDocument pcml = new ProgramCallDocument(_as400, "biz.isphere.core.ifssynchronization.SYNCIFS_resolveGenericFiles", //$NON-NLS-1$
                this.getClass().getClassLoader());

            pcml.setIntValue("SYNCIFS_resolveGenericFiles.handle", _handle); //$NON-NLS-1$
            pcml.setStringValue("SYNCIFS_resolveGenericFiles.mode", mode); //$NON-NLS-1$
            pcml.setStringValue("SYNCIFS_resolveGenericFiles.itemFilter", itemFilter); //$NON-NLS-1$
            pcml.setStringValue("SYNCIFS_resolveGenericFiles.isRegEx", toString(isRegEx)); //$NON-NLS-1$
            pcml.setIntValue("SYNCIFS_resolveGenericFiles.maxDepth", maxDepth); //$NON-NLS-1$

            boolean rc = pcml.callProgram("SYNCIFS_resolveGenericFiles"); //$NON-NLS-1$

            if (rc == false) {

                AS400Message[] msgs = pcml.getMessageList("SYNCIFS_resolveGenericFiles"); //$NON-NLS-1$
                for (int idx = 0; idx < msgs.length; idx++) {
                    ISpherePlugin.logError(msgs[idx].getID() + " - " + msgs[idx].getText(), null); //$NON-NLS-1$
                }
                ISpherePlugin.logError("*** Call to SYNCIFS_resolveGenericFiles failed. See messages above ***", null); //$NON-NLS-1$

                errno = -1;

            } else {

                errno = 1;

            }

        } catch (PcmlException e) {

            errno = -1;
            ISpherePlugin.logError("*** Call to SYNCIFS_resolveGenericFiles failed. See messages above ***", e); //$NON-NLS-1$
        }

        return errno;

    }

    private String toString(boolean isTrue) {
        if (isTrue) {
            return STRING_TRUE;
        }
        return STRING_FALSE;
    }
}