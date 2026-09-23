/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization;

import java.sql.Timestamp;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.data.PcmlException;
import com.ibm.as400.data.ProgramCallDocument;

import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.internal.PcmlProgramCallDocument;

public class SYNCIFS_retrieveItemAttributes {

    public IfsFileAttributes run(AS400 _as400, String path) {

        IfsFileAttributes ifsFileAttributes = null;

        try {

            ProgramCallDocument pcml = new PcmlProgramCallDocument(_as400, "biz.isphere.core.ifssynchronization.SYNCIFS_retrieveItemAttributes", //$NON-NLS-1$
                this.getClass().getClassLoader());

            /*
             * A varying length field is passed as its 2 byte length prefix plus
             * the character data. See the PCML document.
             */
            pcml.setValue("SYNCIFS_retrieveItemAttributes.path.length", Integer.valueOf(path.length())); //$NON-NLS-1$
            pcml.setStringValue("SYNCIFS_retrieveItemAttributes.path.value", path); //$NON-NLS-1$

            boolean rc = pcml.callProgram("SYNCIFS_retrieveItemAttributes");

            if (rc == false) {

                AS400Message[] msgs = pcml.getMessageList("SYNCIFS_retrieveItemAttributes");
                for (int idx = 0; idx < msgs.length; idx++) {
                    ISpherePlugin.logError(msgs[idx].getID() + " - " + msgs[idx].getText(), null); //$NON-NLS-1$
                }
                ISpherePlugin.logError("*** Call to SYNCIFS_retrieveItemAttributes failed. See messages above ***", null); //$NON-NLS-1$

                ifsFileAttributes = null;

            } else {

                ifsFileAttributes = new IfsFileAttributes();
                ifsFileAttributes.setName(getVaryingLengthValue(pcml, "SYNCIFS_retrieveItemAttributes.itemAttrs.name")); //$NON-NLS-1$
                ifsFileAttributes.setType(pcml.getStringValue("SYNCIFS_retrieveItemAttributes.itemAttrs.type")); //$NON-NLS-1$
                ifsFileAttributes.setLastChanged((Timestamp)pcml.getValue("SYNCIFS_retrieveItemAttributes.itemAttrs.lastChanged")); //$NON-NLS-1$
                ifsFileAttributes.setCheckSum((Long)pcml.getValue("SYNCIFS_retrieveItemAttributes.itemAttrs.checkSum")); //$NON-NLS-1$

            }

        } catch (PcmlException e) {

            ifsFileAttributes = null;
            ISpherePlugin.logError("*** Call to SYNCIFS_retrieveItemAttributes failed. See messages above ***", e); //$NON-NLS-1$
        }

        return ifsFileAttributes;

    }

    /**
     * Returns the value of a varying length field, which is stored as its 2
     * byte length prefix plus the character data.
     */
    private String getVaryingLengthValue(ProgramCallDocument pcml, String name) throws PcmlException {

        String value = pcml.getStringValue(name + ".value"); //$NON-NLS-1$
        if (value == null) {
            return null;
        }

        int length = pcml.getIntValue(name + ".length"); //$NON-NLS-1$
        if (length < 0 || length > value.length()) {
            return value;
        }

        return value.substring(0, length);
    }

    /**
     * Attributes of an IFS item. The fields match struct <i>itemAttrs_t</i> of
     * the PCML document.
     */
    public class IfsFileAttributes {

        private String name;
        private String type;
        private Timestamp lastChanged;
        private long checkSum;

        /**
         * @return absolute path of the item
         */
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return <code>D</code> for a directory, else <code>F</code>
         */
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isDirectory() {
            return "D".equals(type); //$NON-NLS-1$
        }

        public Timestamp getLastChanged() {
            return lastChanged;
        }

        public void setLastChanged(Timestamp lastChanged) {
            this.lastChanged = lastChanged;
        }

        public long getCheckSum() {
            return checkSum;
        }

        public void setCheckSum(long checkSum) {
            this.checkSum = checkSum;
        }
    }
}