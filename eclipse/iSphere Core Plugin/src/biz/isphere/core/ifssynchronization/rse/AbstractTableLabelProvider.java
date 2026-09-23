/*******************************************************************************
 * Copyright (c) 2012-2024 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization.rse;

import java.io.File;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableItem;

import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.ifssynchronization.CompareOptions;
import biz.isphere.core.ifssynchronization.StreamFileDescription;
import biz.isphere.core.internal.DateTimeHelper;

/**
 * Class the provides the content for the cells of the table.
 */
public abstract class AbstractTableLabelProvider extends LabelProvider implements ITableLabelProvider {

    protected static final int COLUMN_DUMMY = 0;
    protected static final int COLUMN_LEFT_FILE = 1;
    protected static final int COLUMN_LEFT_LAST_CHANGES = 2;
    protected static final int COLUMN_COMPARE_RESULT = 3;
    protected static final int COLUMN_RIGHT_FILE = 4;
    protected static final int COLUMN_RIGHT_LAST_CHANGES = 5;

    protected Image copyToLeft;
    protected Image copyToRight;
    protected Image copyNotEqual;
    protected Image copyEqual;
    protected Image error;

    private CompareOptions compareOptions;

    public AbstractTableLabelProvider(TableViewer tableViewer) {

        this.compareOptions = null;

        this.copyToLeft = ISpherePlugin.getDefault().getImage(ISpherePlugin.IMAGE_COPY_LEFT);
        this.copyToRight = ISpherePlugin.getDefault().getImage(ISpherePlugin.IMAGE_COPY_RIGHT);
        this.copyEqual = ISpherePlugin.getDefault().getImage(ISpherePlugin.IMAGE_COPY_EQUAL);
        this.copyNotEqual = ISpherePlugin.getDefault().getImage(ISpherePlugin.IMAGE_COPY_NOT_EQUAL);
        this.error = ISpherePlugin.getDefault().getImage(ISpherePlugin.IMAGE_ERROR);

        if (useCompareStatusImagePainter()) {
            tableViewer.getTable().addListener(SWT.PaintItem, new CompareStatusImagePainter(COLUMN_COMPARE_RESULT));
        }
    }

    protected boolean useCompareStatusImagePainter() {
        // Must be false; otherwise image is not updated properly in TreeViewer.
        return false;
    }

    public void setCompareOptions(CompareOptions compareOptions) {
        this.compareOptions = compareOptions;
    }

    public Image getColumnImage(Object element, int columnIndex) {

        /*
         * No image for the dummy column! On Windows, the table creates its
         * image list with the size of the first image assigned to any cell and
         * scales all subsequent images to that size. A small blank image for
         * the dummy column was tried to shrink the state-icon area, but it
         * caused the 16x16 compare status images to be scaled down to an
         * invisible size.
         */
        if (columnIndex != COLUMN_COMPARE_RESULT) {
            return null;
        }

        if (useCompareStatusImagePainter()) {
            return null;
        }

        StreamFileCompareItem compareItem = (StreamFileCompareItem)element;
        Image compareStatusImage = getCompareStatusImage(compareItem);

        return compareStatusImage;
    }

    /**
     * Returns the image that visualizes the compare status of an item. It is
     * also used by the editor, which paints the directory rows itself and
     * therefore does not get the image of the table cell.
     */
    public Image getCompareStatusImage(StreamFileCompareItem compareItem) {

        if (compareItem == null) {
            return null;
        }

        int compareStatus = compareItem.getCompareStatus(compareOptions);
        if (compareStatus == StreamFileCompareItem.RIGHT_MISSING) {
            return copyToRight;
        } else if (compareStatus == StreamFileCompareItem.LEFT_MISSING) {
            return copyToLeft;
        } else if (compareStatus == StreamFileCompareItem.LEFT_EQUALS_RIGHT) {
            return copyEqual;
        } else if (compareStatus == StreamFileCompareItem.NOT_EQUAL) {
            return copyNotEqual;
        } else if (compareStatus == StreamFileCompareItem.ERROR) {
            return error;
        } else {
            return null;
        }
    }

    /**
     * Directories are not compared, hence their last changed date is left
     * blank, so that it does not contradict the compare status.
     */
    public String getColumnText(Object element, int columnIndex) {

        if (columnIndex == COLUMN_COMPARE_RESULT) {
            return null;
        }

        if (!(element instanceof StreamFileCompareItem)) {
            return ""; //$NON-NLS-1$
        }

        StreamFileCompareItem compareItem = (StreamFileCompareItem)element;

        switch (columnIndex) {
        case COLUMN_DUMMY:
            return ""; //$NON-NLS-1$

        case COLUMN_LEFT_FILE:
            if (compareItem.getLeftIfsFileDescription() != null) {
                return getFileNameUI(compareItem.getLeftIfsFileDescription());
            } else {
                return ""; //$NON-NLS-1$
            }

        case COLUMN_LEFT_LAST_CHANGES:
            if (compareItem.getLeftIfsFileDescription() != null && !compareItem.isDirectory()) {
                return DateTimeHelper.getTimestampFormatted(compareItem.getLeftIfsFileDescription().getLastChangedDate());
            } else {
                return ""; //$NON-NLS-1$
            }

        case COLUMN_RIGHT_FILE:
            if (compareItem.getRightIfsFileDescription() != null) {
                return getFileNameUI(compareItem.getRightIfsFileDescription());
            } else {
                return ""; //$NON-NLS-1$
            }

        case COLUMN_RIGHT_LAST_CHANGES:
            if (compareItem.getRightIfsFileDescription() != null && !compareItem.isDirectory()) {
                return DateTimeHelper.getTimestampFormatted(compareItem.getRightIfsFileDescription().getLastChangedDate());
            } else {
                return ""; //$NON-NLS-1$
            }

        default:
            return ""; //$NON-NLS-1$
        }
    }

    /**
     * Directories are displayed with their path relative to the root directory
     * of the compared directory tree, files with their name only. Together with
     * the sort order of the table that produces a tree like display.
     */
    protected String getFileNameUI(StreamFileDescription ifsFileDescription) {

        if (ifsFileDescription.isDirectory()) {
            return ifsFileDescription.getRelativePath();
        }

        return new File(ifsFileDescription.getRelativePath()).getName();
    }

    protected class CompareStatusImagePainter implements Listener {

        private int columnIndex;

        public CompareStatusImagePainter(int columnIndex) {
            this.columnIndex = columnIndex;
        }

        public void handleEvent(Event event) {
            TableItem tableItem = (TableItem)event.item;
            if (event.index == columnIndex) {
                Image tmpImage = getImage(tableItem);
                if (tmpImage == null) {
                    return;
                }
                int tmpWidth = tableItem.getParent().getColumn(event.index).getWidth();
                int tmpHeight = ((TableItem)event.item).getBounds().height;
                int tmpX = tmpImage.getBounds().width;
                tmpX = (tmpWidth / 2 - tmpX / 2);
                int tmpY = tmpImage.getBounds().height;
                tmpY = (tmpHeight / 2 - tmpY / 2);
                if (tmpX <= 0)
                    tmpX = event.x;
                else
                    tmpX += event.x;
                if (tmpY <= 0)
                    tmpY = event.y;
                else
                    tmpY += event.y;
                event.gc.drawImage(tmpImage, tmpX, tmpY);
            }
        }

        private Image getImage(TableItem tableItem) {

            StreamFileCompareItem compareItem = (StreamFileCompareItem)tableItem.getData();

            return getCompareStatusImage(compareItem);
        }
    }
}
