/*******************************************************************************
 * Copyright (c) 2012-2024 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.internal;

import com.ibm.as400.access.AS400;

import biz.isphere.core.ibmi.contributions.extension.handler.IBMiHostContributionsHandler;

/**
 * The RemoteIfsFile class is used by the RDI plug-in as an adapter to pass
 * IFSRemoteFile (RDi) to the core plug-in.
 */
public class RemoteStreamFile {

    private String connectionName;
    private String name;

    private AS400 system;

    public static RemoteStreamFile newFile(String connectionName, String fileName) {
        return new RemoteStreamFile(connectionName, fileName);
    }

    public RemoteStreamFile(String connectionName, String name) {
        this.connectionName = connectionName;
        this.name = name;
    }

    public void setConnectionName(String connectionName) {
        if (this.connectionName != null) {
            throw new IllegalAccessError("Attribute 'connectionName' cannot be changed.");
        }
        this.connectionName = connectionName;
    }

    public AS400 getSystem() {
        if (system == null) {
            system = IBMiHostContributionsHandler.getSystem(connectionName);
        }
        return system;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public String getName() {
        return name;
    }

    public String getIFSName() {

        StringBuilder buffer = new StringBuilder();

        buffer.append(name);

        return buffer.toString();
    }

    public String getQualifiedObject() {
        return name;
    }

    public String getAbsoluteName() {
        return connectionName + ":" + name;
    }

    public String getObjectPathName() {
        return name;
    }

    public String getToolTipText() {
        return "\\\\" + connectionName + "\\" + name;
    }

    @Override
    public String toString() {
        return getAbsoluteName();
    }
}
