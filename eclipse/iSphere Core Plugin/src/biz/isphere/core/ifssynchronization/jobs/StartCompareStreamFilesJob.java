/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization.jobs;

import java.sql.SQLException;
import java.sql.Statement;

import org.eclipse.core.runtime.SubMonitor;

import biz.isphere.base.internal.IBMiHelper;
import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.Messages;
import biz.isphere.core.ifssynchronization.SYNCIFS_getHandle;
import biz.isphere.core.internal.RemoteStreamFile;

/**
 * This class is the first job of the synchronize IFS files task. It stores the
 * objects (file or library) that are compared in file SYNCIFSW.
 * <p>
 * The compare IFS files jobs are executed in the following sequence:
 * <ol>
 * <li>{@link StartCompareStreamFilesJob}
 * <li>{@link ResolveGenericFilesJob}
 * <li>{@link ResolveGenericIfsFilesJob}
 * <li>{@link LoadCompareStreamFilesJob}
 * <li>{@link FinishCompareStreamFilesJob}
 * </ol>
 */
public class StartCompareStreamFilesJob extends AbstractCompareStreamFilesJob {

    private static final int NULL_PARENT = -1;

    private RemoteStreamFile leftObject;
    private RemoteStreamFile rightObject;

    public StartCompareStreamFilesJob(SubMonitor monitor, CompareStreamFilesSharedJobValues sharedValues, RemoteStreamFile leftObject,
        RemoteStreamFile rightObject) {
        super(monitor, sharedValues);

        this.leftObject = leftObject;
        this.rightObject = rightObject;
    }

    protected int getNumWorkItems() {
        return 2;
    }

    protected void execute(SubMonitor monitor) {

        SubMonitor subMonitor = split(monitor, 2);

        try {

            CompareStreamFilesSharedJobValues sharedValues = getSharedValues();

            sharedValues.setLeftHandle(null, ERROR_HANDLE);
            sharedValues.setRightHandle(null, ERROR_HANDLE);

            consume(subMonitor, Messages.Task_Preparing);
            int handle = doSystem(subMonitor, leftObject.getConnectionName(), SyncIfsFileMode.LEFT_SYSTEM);

            if (handle != ERROR_HANDLE) {
                sharedValues.setLeftHandle(leftObject.getConnectionName(), handle);

                consume(subMonitor, Messages.Task_Preparing);
                handle = doSystem(subMonitor, rightObject.getConnectionName(), SyncIfsFileMode.RIGHT_SYSTEM);

                if (handle != ERROR_HANDLE) {
                    sharedValues.setRightHandle(rightObject.getConnectionName(), handle);
                } else {
                    ISpherePlugin.logError("*** Could not get SYNCIFS handle for *RIGHT system ***", null); //$NON-NLS-1$
                }
            } else {
                ISpherePlugin.logError("*** Could not get SYNCIFS handle for *LEFT system ***", null); //$NON-NLS-1$
            }

        } finally {
            done(subMonitor);
        }
    }

    private int doSystem(SubMonitor subMonitor, String connectionName, SyncIfsFileMode mode) {

        if (!initialize(connectionName)) {
            return ERROR_HANDLE;
        }

        try {

            if (subMonitor.isCanceled()) {
                return ERROR_HANDLE;
            }

            if (!setCurrentLibrary()) {
                return ERROR_HANDLE;
            }

            int handle = new SYNCIFS_getHandle().run(getSystem());
            if (handle <= 0) {
                return ERROR_HANDLE;
            }

            String sqlInsert;
            if (SyncIfsFileMode.LEFT_SYSTEM.equals(mode)) {
                sqlInsert = getSqlInsertLeftSystems(handle);
            } else if (SyncIfsFileMode.RIGHT_SYSTEM.equals(mode)) {
                sqlInsert = getSqlInsertRightSystems(handle);
            } else {
                throw new IllegalArgumentException("Unsupported mode value: " + mode.mode()); //$NON-NLS-1$
            }

            Statement statementInsert = null;

            try {
                statementInsert = getJdbcConnection().createStatement();
                statementInsert.executeUpdate(sqlInsert);
            } catch (SQLException e) {
                ISpherePlugin.logError("*** Could not insert compare elements into SYNCIFSW ***", e); //$NON-NLS-1$
            }

            if (statementInsert != null) {
                try {
                    statementInsert.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            return handle;

        } finally {
            restoreCurrentLibrary();
        }
    }

    private String getSqlInsertLeftSystems(int handle) {

        StringBuffer sqlInsert = new StringBuffer();
        sqlInsert.append(
            "INSERT INTO " + getSqlHelper().getObjectName(getISphereLibrary(), "SYNCIFSW") + " (XWHDL, XWID, XWPARENT, XWTYPE, XWITEM) VALUES"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        sqlInsert.append("("); //$NON-NLS-1$
        sqlInsert.append(handle);
        sqlInsert.append(", "); //$NON-NLS-1$
        sqlInsert.append("1");
        sqlInsert.append(", "); //$NON-NLS-1$
        sqlInsert.append(NULL_PARENT);
        sqlInsert.append(", '"); //$NON-NLS-1$
        sqlInsert.append("F");
        sqlInsert.append("', '"); //$NON-NLS-1$
        sqlInsert.append(leftObject.getName()); // All IFS files.
        sqlInsert.append("/*");

        sqlInsert.append("')"); //$NON-NLS-1$

        return sqlInsert.toString();
    }

    private String getSqlInsertRightSystems(int handle) {

        StringBuffer sqlInsert = new StringBuffer();
        sqlInsert.append(
            "INSERT INTO " + getSqlHelper().getObjectName(getISphereLibrary(), "SYNCIFSW") + " (XWHDL, XWID, XWPARENT, XWTYPE, XWITEM) VALUES"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        sqlInsert.append("("); //$NON-NLS-1$
        sqlInsert.append(handle);
        sqlInsert.append(", "); //$NON-NLS-1$
        sqlInsert.append("1");
        sqlInsert.append(", "); //$NON-NLS-1$
        sqlInsert.append(NULL_PARENT);
        sqlInsert.append(", '"); //$NON-NLS-1$
        sqlInsert.append("F");
        sqlInsert.append("', '"); //$NON-NLS-1$
        sqlInsert.append(rightObject.getName()); // All IFS files.
        sqlInsert.append("/*");

        sqlInsert.append("')"); //$NON-NLS-1$

        return sqlInsert.toString();
    }

    @Override
    protected boolean isSameSystem() {

        boolean isSameSystem;
        if (IBMiHelper.isSameSystem(leftObject.getSystem(), rightObject.getSystem())) {
            isSameSystem = true;
        } else {
            isSameSystem = false;
        }

        getSharedValues().setSameSystem(isSameSystem);

        return isSameSystem;
    }
}