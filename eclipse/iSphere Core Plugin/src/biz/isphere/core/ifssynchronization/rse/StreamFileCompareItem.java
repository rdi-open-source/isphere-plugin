/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization.rse;


import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.views.properties.IPropertySource;

import biz.isphere.base.internal.StringHelper;
import biz.isphere.core.ifssynchronization.CompareOptions;
import biz.isphere.core.ifssynchronization.StreamFileDescription;
import biz.isphere.core.ifssynchronization.TableFilterData;
import biz.isphere.core.ifssynchronization.properties.StreamFileCompareItemPropertySource;

public class StreamFileCompareItem implements Comparable<StreamFileCompareItem>, IAdaptable {

    private static final int OVERRIDE_STATUS_NULL = -1;

    public static final int NO_ACTION = 1;
    public static final int LEFT_MISSING = 2;
    public static final int RIGHT_MISSING = 3;
    public static final int LEFT_EQUALS_RIGHT = 4;
    public static final int NOT_EQUAL = 5;
    public static final int ERROR = 6;

    private StreamFileDescription leftIfsFileDescription;
    private StreamFileDescription rightIfsFileDescription;

    private CompareOptions compareOptions;
    private int overridenCompareStatus;
    private int oldOverridenCompareStatus;
    private String ifsFileName;
    private String errorMessage;

    private StreamFileCompareItemPropertySource propertySource;

    public StreamFileCompareItem(StreamFileDescription leftIfsFileDescription, StreamFileDescription rightIfsFileDescription) {

        setLeftIfsFileDescription(leftIfsFileDescription);
        setRightIfsFileDescription(rightIfsFileDescription);

        clearCompareStatus();
        checkMessageDescriptions();

        if (getLeftIfsFileDescription() != null) {
            this.ifsFileName = getLeftIfsFileDescription().getRelativePath();
        } else {
            this.ifsFileName = getRightIfsFileDescription().getRelativePath();
        }

    }

    public String getIfsFileName() {

        return ifsFileName;
    }

    /**
     * @return <code>true</code>, if this item refers to a directory, else
     *         <code>false</code>
     */
    public boolean isDirectory() {

        StreamFileDescription ifsFileDescription = getLeftIfsFileDescription();
        if (ifsFileDescription == null) {
            ifsFileDescription = getRightIfsFileDescription();
        }

        return ifsFileDescription != null && ifsFileDescription.isDirectory();
    }

    public StreamFileDescription getLeftIfsFileDescription() {
        return leftIfsFileDescription;
    }

    public void setLeftIfsFileDescription(StreamFileDescription ifsFileDescription) {

        if (ifsFileDescription != null) {
            String newIfsFileName = ifsFileDescription.getRelativePath();
            if (ifsFileName != null && !ifsFileName.equals(newIfsFileName)) {
                throw new IllegalArgumentException("New left IFS file name '" + newIfsFileName + "' does not match IFS file name: " + ifsFileName); //$NON-NLS-1$
            }
        }

        this.leftIfsFileDescription = ifsFileDescription;
    }

    public StreamFileDescription getRightIfsFileDescription() {
        return rightIfsFileDescription;
    }

    public void setRightIfsFileDescription(StreamFileDescription ifsFileDescription) {

        if (ifsFileDescription != null) {
            String newIfsFileName = ifsFileDescription.getRelativePath();
            if (ifsFileName != null && !ifsFileName.equals(newIfsFileName)) {
                throw new IllegalArgumentException("New right IFS file name '" + newIfsFileName + "' does not match IFS file name: " + ifsFileName); //$NON-NLS-1$
            }
        }

        this.rightIfsFileDescription = ifsFileDescription;
    }

    public int getCompareStatus(CompareOptions compareOptions) {

        setCompareOptions(compareOptions);

        if (overridenCompareStatus != OVERRIDE_STATUS_NULL) {
            return overridenCompareStatus;
        }

        return compareIfsFileDescriptions(compareOptions);
    }

    public int getOriginalCompareStatus(CompareOptions compareOptions) {
        if (oldOverridenCompareStatus != OVERRIDE_STATUS_NULL) {
            return oldOverridenCompareStatus;
        }
        return getCompareStatus(compareOptions);
    }

    public int compareIfsFileDescriptions(CompareOptions compareOptions) {

        setCompareOptions(compareOptions);

        if (getLeftIfsFileDescription() == null && getRightIfsFileDescription() == null) {
            return LEFT_EQUALS_RIGHT;
        } else if (getLeftIfsFileDescription() == null) {
            return LEFT_MISSING;
        } else if (getRightIfsFileDescription() == null) {
            return RIGHT_MISSING;
        } else if (isDirectory()) {
            /*
             * Directories serve as the structure of the compared directory
             * trees, they are not compared. A directory that is present on both
             * sides is always considered equal, because its last changed date
             * changes with every change of its content.
             */
            return LEFT_EQUALS_RIGHT;
        } else if (leftEqualsRight(getLeftIfsFileDescription(), getRightIfsFileDescription(), compareOptions)) {
            return LEFT_EQUALS_RIGHT;
        } else {
            return NOT_EQUAL;
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void resetErrorStatus() {
        clearErrorStatus();
    }

    public void setErrorStatus(String errorMessage, CompareOptions compareOptions) {

        if (this.oldOverridenCompareStatus != OVERRIDE_STATUS_NULL) {
            throw new IllegalArgumentException("Error status not set: overridenCompareStatus <> -1");
        }

        if (StringHelper.isNullOrEmpty(errorMessage)) {
            clearErrorStatus();
        } else {
            if (overridenCompareStatus != OVERRIDE_STATUS_NULL) {
                this.oldOverridenCompareStatus = overridenCompareStatus;
            } else {
                this.oldOverridenCompareStatus = getCompareStatus(compareOptions);
            }
            this.overridenCompareStatus = ERROR;
            this.errorMessage = errorMessage;
        }
    }

    public boolean isError() {

        if (overridenCompareStatus == ERROR) {
            return true;
        }

        return false;
    }

    private void clearErrorStatus() {

        if (overridenCompareStatus != ERROR) {
            return; // there is no error
        }

        this.errorMessage = null;
        this.overridenCompareStatus = oldOverridenCompareStatus;
        this.oldOverridenCompareStatus = OVERRIDE_STATUS_NULL;
    }

    public void setCompareStatus(int status, CompareOptions compareOptions) {

        if (status != LEFT_EQUALS_RIGHT && status != LEFT_MISSING && status != RIGHT_MISSING && status != NO_ACTION) {
            throw new IllegalArgumentException("Illegal status value: " + status); //$NON-NLS-1$
        }

        clearErrorStatus();

        setCompareOptions(compareOptions);

        this.overridenCompareStatus = checkCompareStatus(status, compareOptions);
    }

    public void clearCompareStatus() {
        overridenCompareStatus = OVERRIDE_STATUS_NULL;
        oldOverridenCompareStatus = OVERRIDE_STATUS_NULL;
    }

    public boolean isSingle() {

        if (getLeftIfsFileDescription() == null || getRightIfsFileDescription() == null) {
            return true;
        }

        return false;
    }

    public boolean isDuplicate() {

        return !isSingle();
    }

    public boolean isSelected(TableFilterData filterData, CompareOptions compareOptions) {

        setCompareOptions(compareOptions);

        int compareStatus = getCompareStatus(compareOptions);

        if (compareStatus == StreamFileCompareItem.ERROR) {
            return true;
        }

        if (filterData.isErrorsOnly()) {
            return false;
        }

        if (isDuplicate() && !filterData.isDuplicates()) {
            return false;
        }

        if (isSingle() && !filterData.isSingles()) {
            return false;
        }

        if (compareStatus == StreamFileCompareItem.NO_ACTION) {
            return true;
        }

        if (compareStatus == StreamFileCompareItem.LEFT_MISSING && filterData.isCopyLeft()) {
            return true;
        }

        if (compareStatus == StreamFileCompareItem.RIGHT_MISSING && filterData.isCopyRight()) {
            return true;
        }

        if (compareStatus == StreamFileCompareItem.NOT_EQUAL && filterData.isCopyNotEqual()) {
            return true;
        }

        if (compareStatus == StreamFileCompareItem.LEFT_EQUALS_RIGHT && filterData.isEqual()) {
            return true;
        }

        return false;
    }

    private int checkCompareStatus(int status, CompareOptions compareOptions) {

        setCompareOptions(compareOptions);

        if (compareOptions == null) {
            throw new IllegalArgumentException("Parameter 'compareOptions' is [null]."); //$NON-NLS-1$
        }

        if (status == NO_ACTION && isDuplicate()) {
            status = compareIfsFileDescriptions(compareOptions);
        }

        return status;
    }

    private boolean leftEqualsRight(StreamFileDescription left, StreamFileDescription right, CompareOptions compareOptions) {

        if (compareOptions == null) {
            throw new IllegalArgumentException("Parameter 'compareOptions' is [null]."); //$NON-NLS-1$
        }

        setCompareOptions(compareOptions);

        int rc = left.getRelativePath().compareTo(right.getRelativePath());
        if (rc == 0) {
            rc = left.getChecksum().compareTo(right.getChecksum());
            if (rc == 0 && !compareOptions.isIgnoreDate()) {
                rc = left.getLastChangedDate().compareTo(right.getLastChangedDate());
            }
        }

        if (rc == 0) {
            return true;
        }

        return false;
    }

    private void checkMessageDescriptions() {

        if (leftIfsFileDescription == null && rightIfsFileDescription == null) {
            throw new RuntimeException("At least one message description must not be null."); //$NON-NLS-1$
        }

        if (leftIfsFileDescription != null && rightIfsFileDescription != null) {
            if (!leftIfsFileDescription.getRelativePath().equals(rightIfsFileDescription.getRelativePath())) {
                throw new RuntimeException("Message IDs do not match."); //$NON-NLS-1$
            }
        }
    }

    public int compareTo(StreamFileCompareItem o) {
        return ifsFileName.compareTo(o.getIfsFileName());
    }

    private void setCompareOptions(CompareOptions compareOptions) {
        this.compareOptions = compareOptions;
    }

    public Object getAdapter(Class adapter) {
        if (adapter == IPropertySource.class) {
            if (propertySource == null) {
                propertySource = new StreamFileCompareItemPropertySource(this);
                propertySource.setCompareOptions(compareOptions);
            }
            return propertySource;
        }
        return null;
    }

    private String getLeftFileName() {
        if (getLeftIfsFileDescription() == null) {
            return "null"; //$NON-NLS-1$
        }
        return getLeftIfsFileDescription().getAbsolutePath();
    }

    private String getRightFileName() {
        if (getRightIfsFileDescription() == null) {
            return "null"; //$NON-NLS-1$
        }
        return getRightIfsFileDescription().getAbsolutePath();
    }

    @Override
    public String toString() {
        return getIfsFileName() + "[" + getLeftFileName() + ", " + getRightFileName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
