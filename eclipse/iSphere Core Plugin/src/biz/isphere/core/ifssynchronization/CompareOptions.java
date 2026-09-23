/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Team
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization;

import java.io.Serializable;

/**
 * This class is used for storing the compare options when comparing IFS files
 * in the <i>iSphere Synchronize IFS Files</i> editor.
 */
@SuppressWarnings("serial")
public class CompareOptions implements Serializable {

    private boolean isIgnoreDate;
    private boolean isIncludeEmptyDirectories;
    private String ifsFileFilter;
    private boolean isRegEx;
    private int maxDepth;

    public CompareOptions() {
        this(false, "*"); //$NON-NLS-1$
    }

    public CompareOptions(boolean ignoreDate, String ifsFileFilter) {
        this.isIgnoreDate = ignoreDate;
        this.isIncludeEmptyDirectories = false;
        this.ifsFileFilter = ifsFileFilter;
        this.maxDepth = -1;
    }

    public boolean isIgnoreDate() {
        return isIgnoreDate;
    }

    public void setIgnoreDate(boolean isIgnoreDate) {
        this.isIgnoreDate = isIgnoreDate;
    }

    /**
     * Specifies whether empty directories are part of the comparison. An empty
     * directory is a directory that does not contain any file that matches the
     * IFS file filter, not even in one of its sub directories.
     * <p>
     * Empty directories that are not part of the comparison are neither
     * displayed nor synchronized.
     * 
     * @return <code>true</code>, if empty directories are included, else
     *         <code>false</code>
     */
    public boolean isIncludeEmptyDirectories() {
        return isIncludeEmptyDirectories;
    }

    public void setIncludeEmptyDirectories(boolean isIncludeEmptyDirectories) {
        this.isIncludeEmptyDirectories = isIncludeEmptyDirectories;
    }

    /**
     * Returns the filter that selects the IFS <i>files</i> of the comparison.
     * Directories are not filtered by it, they are always included.
     */
    public String getIfsFileFilter() {
        return ifsFileFilter;
    }

    public void setIfsFileFilter(String ifsFileFilter) {
        this.ifsFileFilter = ifsFileFilter;
    }

    public void setIsRegEx(boolean isRegEx) {
        this.isRegEx = isRegEx;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getMaxDepth() {
        return maxDepth;
    }
}
