/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
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
public class RemoteStreamFile extends AbstractStreamFileObject {

    private String connectionName;

    private AS400 system;

    public static RemoteStreamFile newFile(String connectionName, String fileName) {
        return new RemoteStreamFile(connectionName, fileName);
    }

    public RemoteStreamFile(String connectionName, String name) {
        super(name);
        this.connectionName = connectionName;
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

    public String getIFSName() {

        StringBuilder buffer = new StringBuilder();

        buffer.append(getName());

        return buffer.toString();
    }

    public String getQualifiedObject() {
        return getName();
    }

    @Override
    public String getAbsoluteName() {
        return connectionName + ":" + getName();
    }

    public String getObjectPathName() {
        return getName();
    }

    @Override
    public String getToolTipText() {
        return "\\\\" + connectionName + "\\" + getName();
    }
}
