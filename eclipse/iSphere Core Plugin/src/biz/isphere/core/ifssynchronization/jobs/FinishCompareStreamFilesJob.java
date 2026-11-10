/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization.jobs;

import org.eclipse.core.runtime.SubMonitor;

import biz.isphere.core.Messages;
import biz.isphere.core.ifssynchronization.SYNCIFS_clear;

/**
 * This class cleans up the data stored in file SYNCIFSW.
 * 
 * @see {@link StartCompareStreamFilesJob}
 */
public class FinishCompareStreamFilesJob extends AbstractCompareStreamFilesJob {

    public FinishCompareStreamFilesJob(SubMonitor monitor, CompareStreamFilesSharedJobValues sharedValues) {
        super(monitor, sharedValues);
    }

    protected int getNumWorkItems() {
        return 2;
    }

    @Override
    protected void execute(SubMonitor monitor) {

        SubMonitor subMonitor = split(monitor, 2);

        try {

            CompareStreamFilesSharedJobValues sharedValues = getSharedValues();

            if (sharedValues.getLeftHandle() != ERROR_HANDLE) {
                consume(subMonitor, Messages.Task_Cleaning_up);
                cleanupCompareData(sharedValues.getLeftConnectionName(), sharedValues.getLeftHandle());
            }

            if (sharedValues.getRightHandle() != ERROR_HANDLE) {
                consume(subMonitor, Messages.Task_Cleaning_up);
                cleanupCompareData(sharedValues.getRightConnectionName(), sharedValues.getRightHandle());
            }

        } finally {
            done(subMonitor);
        }
    }

    private void cleanupCompareData(String connectionName, int handle) {

        if (!initialize(connectionName)) {
            return;
        }

        try {

            if (!setCurrentLibrary()) {
                return;
            }

            new SYNCIFS_clear().run(getSystem(), handle);

        } finally {
            restoreCurrentLibrary();
        }
    }
}
