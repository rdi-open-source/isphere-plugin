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

import biz.isphere.base.internal.IFSFileHelper;
import biz.isphere.core.ifssynchronization.jobs.SyncIfsFileMode;
import biz.isphere.core.ifssynchronization.rse.StreamFileCompareItem;

public class TableSorter extends ViewerSorter {

    private static final String SLASH = "/"; //$NON-NLS-1$

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

            rc = compareTreeOrder(description1, description2);
        }

        return rc;
    }

    /**
     * Compares two items in the order of the directory tree: each directory
     * row is followed by the files of that directory and then by its
     * subdirectories.
     * <p>
     * Sorting by the plain relative path does not produce that order: it puts
     * the files of a directory behind its subdirectories, when the name of a
     * subdirectory sorts in front of the name of a file, for example
     * <code>./d1/d2/f2.txt</code> in front of <code>./d1/f1.txt</code>. The
     * table then shows <code>f1.txt</code> below the row of directory
     * <code>./d1/d2</code>.
     */
    private int compareTreeOrder(StreamFileDescription description1, StreamFileDescription description2) {

        int rc = compareSegments(getDirectorySegments(description1), getDirectorySegments(description2));
        if (rc != 0) {
            return rc;
        }

        // Same directory: the directory row in front of its files.
        if (description1.isDirectory() != description2.isDirectory()) {
            if (description1.isDirectory()) {
                return -1;
            } else {
                return 1;
            }
        }

        return description1.getRelativePath().compareTo(description2.getRelativePath());
    }

    /**
     * Returns the segments of the directory of an item: the path of a directory
     * itself, the path of the parent directory of a file. Example:
     * <code>./d1/f1.txt</code> and <code>./d1</code> both return
     * <code>[".", "d1"]</code>.
     */
    private String[] getDirectorySegments(StreamFileDescription description) {

        String path = description.getRelativePath();

        if (description.isFile()) {
            int index = path.lastIndexOf(SLASH);
            if (index > 0) {
                path = path.substring(0, index);
            } else {
                path = IFSFileHelper.RELATIVE_ROOT;
            }
        }

        return path.split(SLASH);
    }

    /**
     * Compares two directories segment by segment, so that a directory sorts
     * in front of its subdirectories and the subdirectories in front of the
     * next directory of the same level: <code>./d1</code>,
     * <code>./d1/d2</code>, <code>./d1-x</code>. A plain string compare would
     * sort <code>./d1-x</code> in front of <code>./d1/d2</code>, because '-'
     * sorts in front of '/'.
     */
    private int compareSegments(String[] segments1, String[] segments2) {

        int count = Math.min(segments1.length, segments2.length);
        for (int i = 0; i < count; i++) {
            int rc = segments1[i].compareTo(segments2[i]);
            if (rc != 0) {
                return rc;
            }
        }

        return segments1.length - segments2.length;
    }

    private StreamFileDescription getSortIfsFileDescription(StreamFileDescription value, StreamFileDescription defaultValue) {
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }
}
