/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization;

import biz.isphere.core.Messages;
import biz.isphere.core.ifssynchronization.rse.StreamFileCompareItem;

public class TableStatistics {

    private static final String SLASH = "/"; //$NON-NLS-1$
    private static final String SPACE = " "; //$NON-NLS-1$

    private int elements;
    private int elementsSelected;
    private int identicalElements;
    private int identicalElementsSelected;
    private int differentElements;
    private int differentElementsSelected;
    private int uniqueElementsLeft;
    private int uniqueElementsLeftSelected;
    private int uniqueElementsRight;
    private int uniqueElementsRightSelected;

    private CompareOptions compareOptions;

    public TableStatistics() {
        this.compareOptions = null;
    }

    public void setCompareOptions(CompareOptions compareOptions) {
        this.compareOptions = compareOptions;
    }

    public void clearStatistics() {

        this.elements = 0;
        this.elementsSelected = 0;

        this.identicalElements = 0;
        this.identicalElementsSelected = 0;

        this.differentElements = 0;
        this.differentElementsSelected = 0;

        this.uniqueElementsLeft = 0;
        this.uniqueElementsLeftSelected = 0;

        this.uniqueElementsRight = 0;
        this.uniqueElementsRightSelected = 0;
    }

    /**
     * Adds an item to the statistics. Directories are ignored, because they are
     * not compared and would distort the number of identical elements.
     */
    public void addElement(StreamFileCompareItem compareItem, TableFilterData filterData) {

        if (compareItem.isDirectory()) {
            return;
        }

        incrementElements(compareItem);
        if (compareItem.isSelected(filterData, compareOptions)) {
            incrementSelectedElements(compareItem);
        }
    }

    /**
     * Removes an item from the statistics. Directories are ignored. See
     * {@link #addElement(StreamFileCompareItem, TableFilterData)}.
     */
    public void removeElement(StreamFileCompareItem compareItem, TableFilterData filterData) {

        if (compareItem.isDirectory()) {
            return;
        }

        decrementElements(compareItem);
        if (compareItem.isSelected(filterData, compareOptions)) {
            decrementSelectedElements(compareItem);
        }
    }

    private void incrementElements(StreamFileCompareItem compareItem) {
        countElements(compareItem, 1);
    }

    private void decrementElements(StreamFileCompareItem compareItem) {
        countElements(compareItem, -1);
    }

    private void countElements(StreamFileCompareItem compareItem, int value) {

        int compareStatus = compareItem.compareIfsFileDescriptions(compareOptions);

        countElements(value);

        if (compareStatus == StreamFileCompareItem.LEFT_EQUALS_RIGHT) {
            countIdenticalElements(value);
        } else if (compareStatus == StreamFileCompareItem.NOT_EQUAL) {
            countDifferentElements(value);
        }

        if (compareItem.isSingle()) {
            if (compareItem.getLeftIfsFileDescription() != null) {
                countUniqueElementsLeft(value);
            } else {
                countUniqueElementsRight(value);
            }
        }
    }

    private void incrementSelectedElements(StreamFileCompareItem compareItem) {
        countSelectedElements(compareItem, 1);
    }

    private void decrementSelectedElements(StreamFileCompareItem compareItem) {
        countSelectedElements(compareItem, -1);
    }

    private void countSelectedElements(StreamFileCompareItem compareItem, int value) {

        int compareStatus = compareItem.compareIfsFileDescriptions(compareOptions);

        countElementsSelected(value);

        if (compareStatus == StreamFileCompareItem.LEFT_EQUALS_RIGHT) {
            countIdenticalElementsSelected(value);
        } else if (compareStatus == StreamFileCompareItem.NOT_EQUAL) {
            countDifferentElementsSelected(value);
        }

        if (compareItem.isSingle()) {
            if (compareItem.getLeftIfsFileDescription() != null) {
                countUniqueElementsLeftSelected(value);
            } else {
                countUniqueElementsRightSelected(value);
            }
        }
    }

    private void countElements(int value) {
        elements += value;
    }

    private void countElementsSelected(int value) {
        elementsSelected += value;
    }

    private void countIdenticalElements(int value) {
        identicalElements += value;
    }

    private void countIdenticalElementsSelected(int value) {
        identicalElementsSelected += value;
    }

    private void countDifferentElements(int value) {
        differentElements += value;
    }

    private void countDifferentElementsSelected(int value) {
        differentElementsSelected += value;
    }

    private void countUniqueElementsLeft(int value) {
        uniqueElementsLeft += value;
    }

    private void countUniqueElementsLeftSelected(int value) {
        uniqueElementsLeftSelected += value;
    }

    private void countUniqueElementsRight(int value) {
        uniqueElementsRight += value;
    }

    private void countUniqueElementsRightSelected(int value) {
        uniqueElementsRightSelected += value;
    }

    public int getFilteredElements() {
        return elements - elementsSelected;
    }

    @Override
    public String toString() {

        StringBuilder statusMessage = new StringBuilder();

        statusMessage.append(Messages.Items_found_colon + SPACE + elementsSelected); // $NON-NLS-1$
        if (elementsSelected != elements) {
            statusMessage.append(SLASH + elements);
        }

        statusMessage.append(" ("); //$NON-NLS-1$
        statusMessage.append(Messages.Identical_colon + SPACE + identicalElementsSelected);
        if (identicalElementsSelected != identicalElements) {
            statusMessage.append(SLASH + identicalElements);
        }

        statusMessage.append(", " + Messages.Different_colon + SPACE + differentElementsSelected); //$NON-NLS-1$
        if (differentElementsSelected != differentElements) {
            statusMessage.append(SLASH + differentElements);
        }

        statusMessage.append(", " + Messages.Unique_left_colon + SPACE + uniqueElementsLeftSelected); //$NON-NLS-1$
        if (uniqueElementsLeftSelected != uniqueElementsLeft) {
            statusMessage.append(SLASH + uniqueElementsLeft);
        }

        statusMessage.append(", " + Messages.Unique_right_colon + SPACE + uniqueElementsRightSelected); //$NON-NLS-1$
        if (uniqueElementsRightSelected != uniqueElementsRight) {
            statusMessage.append(SLASH + uniqueElementsRight);
        }

        statusMessage.append(")"); //$NON-NLS-1$

        return statusMessage.toString();
    }
}
