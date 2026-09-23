/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import biz.isphere.base.internal.IFSFileHelper;
import biz.isphere.core.ifssynchronization.rse.StreamFileCompareItem;

/**
 * Class to filter the content of the table according to the selection settings
 * that can be changed with the buttons above the table.
 * <p>
 * Directories are not filtered by their compare status, because they are not
 * compared. A directory is displayed, when at least one item below it is
 * displayed. That keeps the directory rows available for the automatic
 * selection of the items of a directory, without displaying empty directories.
 * <p>
 * An empty directory has no item below it, hence it is displayed only, when
 * {@link CompareOptions#isIncludeEmptyDirectories()} is enabled and its own
 * compare status passes the filter. Because the synchronization processes the
 * displayed rows, an empty directory that is not displayed is not synchronized
 * either.
 */
public class TableFilter extends ViewerFilter {

    private static final String SLASH = "/"; //$NON-NLS-1$

    private TableFilterData filterData;
    private TableStatistics tableStatistics;

    private CompareOptions compareOptions;

    public TableFilter(TableStatistics tableStatistics) {
        this.tableStatistics = tableStatistics;
        this.compareOptions = null;
    }

    public void setCompareOptions(CompareOptions compareOptions) {
        this.compareOptions = compareOptions;
        tableStatistics.setCompareOptions(compareOptions);
    }

    public void setFilterData(TableFilterData filterData) {
        this.filterData = filterData;
    }

    /**
     * Filters the elements of the table in two passes, because whether a
     * directory is displayed depends on the other elements.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public Object[] filter(Viewer viewer, Object parent, Object[] elements) {

        if (filterData == null) {
            return elements;
        }

        /*
         * Pass 1: count the elements and collect the relative paths of the
         * items that are displayed. The relative paths are unique, because the
         * table content provider identifies the compare items by them.
         */
        Set<String> visibleItems = new HashSet<String>();
        Set<String> visibleDirectories = new HashSet<String>();

        for (Object element : elements) {

            if (!(element instanceof StreamFileCompareItem)) {
                continue;
            }

            StreamFileCompareItem compareItem = (StreamFileCompareItem)element;

            tableStatistics.addElement(compareItem, filterData);

            boolean isSelected = compareItem.isSelected(filterData, compareOptions);

            if (compareItem.isDirectory()) {
                /*
                 * A directory is displayed on its own merit only, when empty
                 * directories are part of the comparison. Otherwise a directory
                 * is pure structure and is displayed only, when at least one
                 * item below it is displayed, which is what the parent
                 * directories collected below take care of.
                 */
                if (isSelected && isIncludeEmptyDirectories()) {
                    visibleDirectories.add(compareItem.getIfsFileName());
                    addParentDirectories(visibleDirectories, compareItem.getIfsFileName());
                }
            } else if (isSelected) {
                visibleItems.add(compareItem.getIfsFileName());
                addParentDirectories(visibleDirectories, compareItem.getIfsFileName());
            }
        }

        /*
         * Pass 2: produce the result in the original order.
         */
        List<Object> filteredElements = new ArrayList<Object>(elements.length);

        for (Object element : elements) {

            if (!(element instanceof StreamFileCompareItem)) {
                continue;
            }

            StreamFileCompareItem compareItem = (StreamFileCompareItem)element;

            if (compareItem.isDirectory()) {
                if (visibleDirectories.contains(compareItem.getIfsFileName())) {
                    filteredElements.add(compareItem);
                }
            } else {
                if (visibleItems.contains(compareItem.getIfsFileName())) {
                    filteredElements.add(compareItem);
                }
            }
        }

        return filteredElements.toArray();
    }

    /**
     * @return <code>true</code>, if empty directories are part of the
     *         comparison, else <code>false</code>
     */
    private boolean isIncludeEmptyDirectories() {

        if (compareOptions == null) {
            return false;
        }

        return compareOptions.isIncludeEmptyDirectories();
    }

    /**
     * Marks all directories above a given item as visible, so that a displayed
     * item is never orphaned. Walking up the relative path is enough, because
     * the items are identified by it:
     * <code>./QRPGLESRC/copybooks/H_SPEC.RPGLE</code> adds
     * <code>./QRPGLESRC/copybooks</code>, <code>./QRPGLESRC</code> and finally
     * the root directory <code>.</code>.
     * 
     * @param visibleDirectories - relative paths of the directories that are
     *        displayed
     * @param relativePath - relative path of the item that is displayed
     */
    private void addParentDirectories(Set<String> visibleDirectories, String relativePath) {

        if (relativePath == null) {
            return;
        }

        String path = relativePath;

        while (true) {

            int i = path.lastIndexOf(SLASH);
            if (i < 0) {
                break;
            }

            path = path.substring(0, i);
            if (path.length() == 0) {
                break;
            }

            if (!visibleDirectories.add(path)) {
                // The remaining parent directories have been added before.
                break;
            }
        }

        visibleDirectories.add(IFSFileHelper.RELATIVE_ROOT);
    }

    /**
     * Not used, because the elements are filtered by
     * {@link #filter(Viewer, Object, Object[])}. Kept for the contract of
     * {@link ViewerFilter}.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {

        if (filterData == null) {
            return true;
        }

        if (!(element instanceof StreamFileCompareItem)) {
            return false;
        }

        return ((StreamFileCompareItem)element).isSelected(filterData, compareOptions);
    }
}
