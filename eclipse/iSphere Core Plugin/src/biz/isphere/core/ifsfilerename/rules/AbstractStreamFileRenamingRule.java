/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifsfilerename.rules;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.IFSFile;

import biz.isphere.base.internal.IFSFileHelper;

public abstract class AbstractStreamFileRenamingRule implements IStreamFileRenamingRule, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Maximum length of an absolute IFS path name.
     */
    public static final int MAX_PATH_LENGTH = 1024;

    private transient AS400 system;
    private String label;

    private String directory;
    private String fileName;

    public AbstractStreamFileRenamingRule(String label) {
        this.label = label;
    }

    public AS400 getSystem() {
        return system;
    }

    public String getLabel() {
        return label;
    }

    public void initialize(AS400 system, String ifsFilePath) throws Exception {

        this.system = system;
        this.directory = IFSFileHelper.getPathName(ifsFilePath);
        this.fileName = IFSFileHelper.getFileName(ifsFilePath);
    }

    /**
     * @return directory of the IFS file that is renamed
     */
    public String getBaseDirectory() {
        return directory;
    }

    /**
     * @return name of the IFS file that is renamed, without the directory
     */
    public String getBaseFileName() {
        return fileName;
    }

    /**
     * Joins the directory of the IFS file that is renamed and a given file
     * name.
     * 
     * @param fileName - name of the IFS file, without the directory
     * @return absolute path of the IFS file
     */
    protected String getAbsolutePath(String fileName) {

        if (getBaseDirectory() == null || getBaseDirectory().length() == 0) {
            return fileName;
        }

        if (getBaseDirectory().endsWith("/")) { //$NON-NLS-1$
            return getBaseDirectory() + fileName;
        }

        return getBaseDirectory() + "/" + fileName; //$NON-NLS-1$
    }

    /**
     * Returns the names of the IFS files of the directory of the IFS file that
     * is renamed, that match a given filter.
     * 
     * @param fileNameFilter - generic name of the IFS files that are returned
     * @return names of the matching IFS files, without the directory
     * @throws Exception
     */
    protected String[] loadIfsFileList(String fileNameFilter) throws Exception {

        List<String> fileNames = new LinkedList<String>();

        if (system == null) {
            // system is null when running JUnit tests
            return fileNames.toArray(new String[fileNames.size()]);
        }

        IFSFile directory = new IFSFile(system, getBaseDirectory());
        String[] items = directory.list(fileNameFilter);
        if (items == null) {
            return fileNames.toArray(new String[fileNames.size()]);
        }

        for (String item : items) {
            fileNames.add(item);
        }

        return fileNames.toArray(new String[fileNames.size()]);
    }

    /**
     * Tests whether a given IFS file exists in the directory of the IFS file
     * that is renamed.
     * 
     * @param fileName - name of the IFS file, without the directory
     * @return <code>true</code>, if the IFS file exists, else
     *         <code>false</code>
     */
    protected boolean exists(String fileName) {

        if (system == null) {
            // system is null when running JUnit tests
            return false;
        }

        try {
            return new IFSFile(system, getAbsolutePath(fileName)).exists();
        } catch (Exception e) {
            return false;
        }
    }
}
