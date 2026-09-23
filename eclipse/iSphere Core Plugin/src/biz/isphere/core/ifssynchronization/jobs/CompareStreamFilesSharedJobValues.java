/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization.jobs;

import java.io.Serializable;

import biz.isphere.core.ifssynchronization.CompareOptions;

/**
 * This class is used for storing the compare options and status values when
 * comparing IFS files in the <i>iSphere Synchronize IFS Files</i> editor. It is
 * used for sharing these values between the jobs involved.
 */
@SuppressWarnings("serial")
public class CompareStreamFilesSharedJobValues implements Serializable {

    private boolean isSameSystem;

    private int leftHandle;
    private int rightHandle;

    private String leftConnectionName;
    private String rightConnectionName;

    private CompareOptions compareOptions;

    public CompareStreamFilesSharedJobValues() {
        this(new CompareOptions());
    }

    public CompareStreamFilesSharedJobValues(CompareOptions compareOptions) {
        this.compareOptions = compareOptions;
    }

    public boolean isSameSystem() {
        return isSameSystem;
    }

    public void setSameSystem(boolean isSameSystem) {
        this.isSameSystem = isSameSystem;
    }

    public CompareOptions getCompareOptions() {
        return compareOptions;
    }

    public int getLeftHandle() {
        return leftHandle;
    }

    public String getLeftConnectionName() {
        return leftConnectionName;
    }

    public void setLeftHandle(String connectionName, int leftHandle) {
        this.leftHandle = leftHandle;
        this.leftConnectionName = connectionName;
    }

    public int getRightHandle() {
        return rightHandle;
    }

    public String getRightConnectionName() {
        return rightConnectionName;
    }

    public void setRightHandle(String connectionName, int rightHandle) {
        this.rightHandle = rightHandle;
        this.rightConnectionName = connectionName;
    }
}
