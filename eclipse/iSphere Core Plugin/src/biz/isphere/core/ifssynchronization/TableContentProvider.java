/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import biz.isphere.core.ifssynchronization.rse.StreamFileCompareItem;

/**
 * Class to provide the content of the table viewer.
 */
public class TableContentProvider implements IStructuredContentProvider {

    private TableStatistics tableStatistics;
    private SynchronizeStreamFilesEditorInput editorInput;
    private StreamFileCompareItem[] compareItemsArray;

    public TableContentProvider(TableStatistics tableStatistics) {

        this.tableStatistics = tableStatistics;
        this.editorInput = null;
        this.compareItemsArray = null;
    }

    public TableStatistics getTableStatistics() {
        return tableStatistics;
    }

    public Object[] getElements(Object inputElement) {

        if (compareItemsArray != null) {
            return compareItemsArray;
        }

        tableStatistics.clearStatistics();

        Map<String, StreamFileCompareItem> compareItems = new LinkedHashMap<String, StreamFileCompareItem>();

        if (editorInput != null) {

            for (StreamFileDescription leftIfsFileDescription : editorInput.getLeftIfsFileDescriptions()) {
                String key = produceKey(leftIfsFileDescription);
                compareItems.put(key, new StreamFileCompareItem(leftIfsFileDescription, null));
            }

            for (StreamFileDescription rightIfsFileDescription : editorInput.getRightIfsFileDescriptions()) {
                String key = produceKey(rightIfsFileDescription);
                StreamFileCompareItem compareItem = compareItems.get(key);
                if (compareItem != null) {
                    compareItem.setRightIfsFileDescription(rightIfsFileDescription);
                } else {
                    compareItems.put(key, new StreamFileCompareItem(null, rightIfsFileDescription));
                }
            }
        }

        compareItemsArray = compareItems.values().toArray(new StreamFileCompareItem[compareItems.size()]);
        Arrays.sort(compareItemsArray);

        return compareItemsArray;
    }

    /**
     * Removes an item from the list of compare items, for example, when both
     * the left and the right IFS file have been deleted.
     * <p>
     * The item is removed from the cached array, because that array is
     * returned by {@link #getElements(Object)} as long as the input of the
     * table viewer does not change.
     * 
     * @param compareItem - item that is removed from the table
     */
    public void removeCompareItem(StreamFileCompareItem compareItem) {

        if (compareItemsArray == null || compareItem == null) {
            return;
        }

        List<StreamFileCompareItem> remainingCompareItems = new ArrayList<StreamFileCompareItem>(Arrays.asList(compareItemsArray));
        if (!remainingCompareItems.remove(compareItem)) {
            return;
        }

        compareItemsArray = remainingCompareItems.toArray(new StreamFileCompareItem[remainingCompareItems.size()]);
    }

    /**
     * Items are identified by their path relative to the root directory of the
     * compared directory tree. Only then a left item can be matched with its
     * right counterpart, because the left and the right root directory differ.
     */
    private String produceKey(StreamFileDescription ifsFileDescription) {
        return ifsFileDescription.getRelativePath();
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

        editorInput = (SynchronizeStreamFilesEditorInput)newInput;
        compareItemsArray = null;
    }
}
