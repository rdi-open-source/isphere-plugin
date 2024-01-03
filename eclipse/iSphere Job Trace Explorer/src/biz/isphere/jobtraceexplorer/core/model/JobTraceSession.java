/*******************************************************************************
 * Copyright (c) 2012-2021 iSphere Project Team
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.jobtraceexplorer.core.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.views.properties.IPropertySource;

import biz.isphere.core.json.JsonImporter;
import biz.isphere.core.json.JsonSerializable;

import com.google.gson.annotations.Expose;

public class JobTraceSession implements JsonSerializable, IAdaptable {

    @Expose(serialize = true, deserialize = true)
    private String connectionName;
    @Expose(serialize = true, deserialize = true)
    private String libraryName;
    @Expose(serialize = true, deserialize = true)
    private String sessionID;

    @Expose(serialize = true, deserialize = true)
    private Boolean isIBMDataExcluded;

    @Expose(serialize = true, deserialize = true)
    private JobTraceEntries jobTraceEntries;
    @Expose(serialize = true, deserialize = true)
    private String fileName;

    private transient boolean isFileSession;
    private transient JobTraceSessionPropertySource propertySource;

    /**
     * Produces a new JobTraceSession object. This constructor is exclusively
     * used by the {@link JsonImporter}.
     */
    public JobTraceSession() {
        this.isFileSession = true;
        initialize();
    }

    /**
     * Produces a new JobTraceSession object. This constructor is used when
     * loading job trace entries from a Json file.
     * 
     * @param fileName - Json file that stores a job trace session
     */
    public JobTraceSession(String fileName) {
        this.fileName = fileName;
        this.isFileSession = true;
        initialize();
    }

    /**
     * Produces a new JobTraceSession object. This constructor is used when
     * loading job trace entries from a job trace database.
     * 
     * @param connectionName - connection of the remote system
     * @param libraryName - name of the library, where the job trace session is
     *        stored
     * @param sessionID - session ID of the job trace session
     */
    public JobTraceSession(String connectionName, String libraryName, String sessionID) {
        this.connectionName = connectionName;
        this.libraryName = libraryName;
        this.sessionID = sessionID;
        this.isFileSession = false;

        initialize();
    }

    private void initialize() {

        this.isIBMDataExcluded = true;
        this.jobTraceEntries = null;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public String getSessionID() {
        return sessionID;
    }

    public String getQualifiedName() {
        if (isFileSession) {
            return fileName;
        } else {
            return libraryName + ":" + sessionID; //$NON-NLS-1$
        }
    }

    public boolean isIBMDataExcluded() {
        return isIBMDataExcluded;
    }

    public void setExcludeIBMData(boolean isExcluded) {
        this.isIBMDataExcluded = isExcluded;
    }

    public String getFileName() {
        return fileName;
    }

    public void updateFileName(String fileName) {

        if (!isFileSession) {
            throw new IllegalAccessError("Method not allowed, when session is not a file session."); //$NON-NLS-1$
        }

        this.fileName = fileName;
    }

    public JobTraceEntries getJobTraceEntries() {

        if (jobTraceEntries == null) {
            jobTraceEntries = new JobTraceEntries();
        }

        return jobTraceEntries;
    }

    public Object getAdapter(Class adapter) {
        if (adapter == IPropertySource.class) {
            if (propertySource == null) {
                propertySource = new JobTraceSessionPropertySource(this);
            }
            return propertySource;
        }
        return null;
    }

    public String getContentId() {
        if (isFileSession) {
            return "local_file:/" + fileName;
        } else {
            return "remote_session:/" + connectionName + ":" + libraryName + ":" + sessionID;
        }
    }

    @Override
    public String toString() {
        return getQualifiedName();
    }
}
