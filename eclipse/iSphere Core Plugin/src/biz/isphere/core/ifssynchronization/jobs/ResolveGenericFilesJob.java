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
import biz.isphere.core.ifssynchronization.CompareOptions;
import biz.isphere.core.ifssynchronization.SYNCIFS_resolveGenericFiles;

/**
 * This class resolves generic files stored in file SYNCIFSW.
 * 
 * @see {@link StartCompareStreamFilesJob}
 */
public class ResolveGenericFilesJob extends AbstractCompareStreamFilesJob {

    public ResolveGenericFilesJob(SubMonitor monitor, CompareStreamFilesSharedJobValues sharedValues) {
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
            CompareOptions compareOptions = getSharedValues().getCompareOptions();

            consume(subMonitor, Messages.Task_Resolving_generic_items);
            resolveGenericFiles(subMonitor, sharedValues.getLeftHandle(), sharedValues.getLeftConnectionName(), SyncIfsFileMode.LEFT_SYSTEM,
                compareOptions.getIfsFileFilter(), compareOptions.isRegEx(), compareOptions.getMaxDepth());

            consume(subMonitor, Messages.Task_Resolving_generic_items);
            resolveGenericFiles(subMonitor, sharedValues.getRightHandle(), sharedValues.getRightConnectionName(), SyncIfsFileMode.RIGHT_SYSTEM,
                compareOptions.getIfsFileFilter(), compareOptions.isRegEx(), compareOptions.getMaxDepth());

        } finally {
            done(subMonitor);
        }
    }

    private void resolveGenericFiles(SubMonitor subMonitor, int handle, String connectionName, SyncIfsFileMode mode, String itemFilter,
        boolean isRegEx, int maxDepth) {

        if (!initialize(connectionName)) {
            return;
        }

        try {

            if (subMonitor.isCanceled()) {
                return;
            }

            if (handle == ERROR_HANDLE) {
                return;
            }

            if (!setCurrentLibrary()) {
                return;
            }

            new SYNCIFS_resolveGenericFiles().run(getSystem(), handle, mode.mode(), itemFilter, isRegEx, maxDepth);

        } finally {
            restoreCurrentLibrary();
        }

    }
}
