/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifsfilerename;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.IFSFile;

import biz.isphere.core.Messages;
import biz.isphere.core.ifsfilerename.exceptions.InvalidStreamFileNameException;
import biz.isphere.core.ifsfilerename.rules.AbstractStreamFileRenamingRule;
import biz.isphere.core.ifsfilerename.rules.IStreamFileRenamingRule;
import biz.isphere.core.memberrename.exceptions.NoMoreNamesAvailableException;

/**
 * This class produces the backup name of an IFS file that is about to be
 * replaced. It is the IFS counterpart of
 * {@link biz.isphere.core.memberrename.RenameMemberActor}.
 */
public class RenameStreamFileActor {

    private AS400 system;
    private IStreamFileRenamingRule ifsFileRenamingRule;

    public RenameStreamFileActor(AS400 system, IStreamFileRenamingRule backupNameRule) {
        this.system = system;
        this.ifsFileRenamingRule = backupNameRule;
    }

    /**
     * Produces a new IFS file name based of a given IFS file and renaming rule.
     * 
     * @param ifsFilePath - absolute path of the IFS file that is renamed
     * @return absolute path of the new IFS file
     * @throws NoMoreNamesAvailableException
     * @throws InvalidStreamFileNameException
     * @throws Exception
     */
    public String produceNewIfsFileName(String ifsFilePath) throws NoMoreNamesAvailableException, InvalidStreamFileNameException, Exception {

        if (ifsFilePath.length() >= AbstractStreamFileRenamingRule.MAX_PATH_LENGTH) {
            // Too long, because we cannot add the numerical extension
            throw new InvalidStreamFileNameException(Messages.bind(Messages.Error_Invalid_stream_file_name_Name_is_too_long_A, ifsFilePath));
        }

        ifsFileRenamingRule.initialize(system, ifsFilePath);

        String nextIfsFilePath = null;
        while (nextIfsFilePath == null) {

            nextIfsFilePath = ifsFileRenamingRule.getNextName();
            if (nextIfsFilePath.length() > AbstractStreamFileRenamingRule.MAX_PATH_LENGTH) {
                throw new InvalidStreamFileNameException(Messages.bind(Messages.Error_Invalid_stream_file_name_Name_is_too_long_A, nextIfsFilePath));
            }

            if (exists(system, nextIfsFilePath)) {
                // May happen, when fill gaps is enabled.
                nextIfsFilePath = null;
            }
        }

        return nextIfsFilePath;
    }

    protected boolean exists(AS400 system, String ifsFilePath) {

        try {
            return new IFSFile(system, ifsFilePath).exists();
        } catch (Exception e) {
            return false;
        }
    }
}
