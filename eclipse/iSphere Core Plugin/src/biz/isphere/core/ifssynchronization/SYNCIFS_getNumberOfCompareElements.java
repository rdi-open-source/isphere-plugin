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

public class SYNCIFS_getNumberOfCompareElements {

    public int run(AS400 _as400, int handle, String mode) {

        int numberOfSearchElements = 0;

        try {

            ProgramCallDocument pcml = new ProgramCallDocument(_as400, "biz.isphere.core.ifssynchronization.SYNCIFS_getNumberOfCompareElements", //$NON-NLS-1$
                this.getClass().getClassLoader());

            pcml.setIntValue("SYNCIFS_getNumberOfCompareElements.handle", handle); //$NON-NLS-1$
            pcml.setStringValue("SYNCIFS_getNumberOfCompareElements.mode", mode); //$NON-NLS-1$

            boolean rc = pcml.callProgram("SYNCIFS_getNumberOfCompareElements"); //$NON-NLS-1$

            if (rc == false) {

                AS400Message[] msgs = pcml.getMessageList("SYNCIFS_getNumberOfCompareElements"); //$NON-NLS-1$
                for (int idx = 0; idx < msgs.length; idx++) {
                    ISpherePlugin.logError(msgs[idx].getID() + " - " + msgs[idx].getText(), null); //$NON-NLS-1$
                }
                ISpherePlugin.logError("*** Call to SYNCIFS_getNumberOfCompareElements failed. See messages above ***", null); //$NON-NLS-1$

                numberOfSearchElements = -1;

            } else {

                numberOfSearchElements = pcml.getIntValue("SYNCIFS_getNumberOfCompareElements.numberOfCompareElements"); //$NON-NLS-1$

            }

        } catch (PcmlException e) {

            numberOfSearchElements = -1;
            ISpherePlugin.logError("*** Call to SYNCIFS_getNumberOfCompareElements failed. See messages above ***", e); //$NON-NLS-1$
        }

        return numberOfSearchElements;

    }

}