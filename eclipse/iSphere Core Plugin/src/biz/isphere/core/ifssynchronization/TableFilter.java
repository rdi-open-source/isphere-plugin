/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import biz.isphere.core.ifssynchronization.rse.StreamFileCompareItem;

/**
 * Class to filter the content of the table according to the selection settings
 * that can be changed with the buttons above the table.
 * <p>
 * Directories are not filtered by their compare status, because they are not
 * compared. They are pure structure and keep the directory rows available for
 * the automatic selection of the items of a directory. A directory is
 * displayed, when there is at least one stream file below it.
 * <p>
 * A directory whose subtree is free of stream files is an <i>empty</i>
 * directory. It is displayed only, when
 * {@link CompareOptions#isIncludeEmptyDirectories()} is enabled. Because the
 * synchronization processes the displayed rows, an empty directory that is not
 * displayed is not synchronized either.
 * <p>
 * The root directory is special: it is displayed only, when it <i>directly</i>
 * contains stream files. It does not display the compared directory tree, it
 * merely offers the context menu that sets the compare status of the items
 * below it.
 */
public class TableFilter extends ViewerFilter {

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
     * Resets the table statistics, before the elements are passed to
     * {@link #select(Viewer, Object, Object)}, which collects them again.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public Object[] filter(Viewer viewer, Object parent, Object[] elements) {

        if (filterData == null) {
            return elements;
        }

        tableStatistics.clearStatistics();

        return super.filter(viewer, parent, elements);
    }

    /**
     * Decides whether an element is displayed and adds it to the table
     * statistics.
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

        StreamFileCompareItem compareItem = (StreamFileCompareItem)element;

        tableStatistics.addElement(compareItem, filterData);

        if (compareItem.isDirectory()) {
            if (compareItem.isRootDirectory()) {
                return compareItem.haveFiles();
            } else {
                return isIncludeEmptyDirectories() || compareItem.haveFilesInSubtree();
            }
        } else {
            return compareItem.isSelected(filterData, compareOptions);
        }
    }
}
