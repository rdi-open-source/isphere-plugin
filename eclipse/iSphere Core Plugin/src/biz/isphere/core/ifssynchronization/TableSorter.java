/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import biz.isphere.core.ifssynchronization.jobs.SyncIfsFileMode;
import biz.isphere.core.ifssynchronization.rse.StreamFileCompareItem;

public class TableSorter extends ViewerSorter {

    private SyncIfsFileMode mode;

    public TableSorter(SyncIfsFileMode mode) {
        this.mode = mode;
    }

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {

        StreamFileCompareItem item1 = (StreamFileCompareItem)e1;
        StreamFileCompareItem item2 = (StreamFileCompareItem)e2;

        int rc;
        if (item1 == null && item2 == null) {
            rc = 0;
        } else if (item1 == null) {
            rc = -1;
        } else if (item2 == null) {
            rc = 1;
        } else {

            StreamFileDescription description1;
            StreamFileDescription description2;
            if (mode == SyncIfsFileMode.LEFT_SYSTEM) {
                description1 = getSortIfsFileDescription(item1.getLeftIfsFileDescription(), item1.getRightIfsFileDescription());
                description2 = getSortIfsFileDescription(item2.getLeftIfsFileDescription(), item2.getRightIfsFileDescription());
            } else {
                description1 = getSortIfsFileDescription(item1.getRightIfsFileDescription(), item1.getLeftIfsFileDescription());
                description2 = getSortIfsFileDescription(item2.getRightIfsFileDescription(), item2.getLeftIfsFileDescription());
            }

            /*
             * Sorting by the relative path also produces the tree order,
             * because a path always sorts in front of the paths it is a prefix
             * of: '.', './DEMO12.RPGLE', './QRPGLESRC',
             * './QRPGLESRC/DEMO11.RPGLE'.
             */
            rc = description1.getRelativePath().compareTo(description2.getRelativePath());
        }

        return rc;
    }

    private StreamFileDescription getSortIfsFileDescription(StreamFileDescription value, StreamFileDescription defaultValue) {
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }
}
