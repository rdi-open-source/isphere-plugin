/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization.jobs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.SubMonitor;

import com.ibm.as400.access.AS400;

import biz.isphere.base.internal.IFSFileHelper;
import biz.isphere.base.internal.SqlHelper;
import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.Messages;
import biz.isphere.core.ibmi.contributions.extension.handler.IBMiHostContributionsHandler;
import biz.isphere.core.ifssynchronization.StreamFileDescription;
import biz.isphere.core.ifssynchronization.SYNCIFS_getNumberOfCompareElements;

/**
 * This class loads the IFS files that are compared from file SYNCIFSW.
 * 
 * @see {@link StartCompareStreamFilesJob}
 */
public class LoadCompareStreamFilesJob extends AbstractCompareStreamFilesJob {

    private static final StreamFileDescription[] EMPTY_RESULT = new StreamFileDescription[0];

    private StreamFileDescription[] leftIfsFiles;
    private StreamFileDescription[] rightIfsFiles;

    public LoadCompareStreamFilesJob(SubMonitor monitor, CompareStreamFilesSharedJobValues sharedValues) {
        super(monitor, sharedValues);
    }

    protected int getNumWorkItems() {
        return 2;
    }

    @Override
    protected void execute(SubMonitor monitor) {

        SubMonitor subMonitor = split(monitor, 2);

        try {

            CompareStreamFilesSharedJobValues sharedValues = getSharedValues();

            int numLeftIfsFiles = getNumIfsFiles(sharedValues.getLeftConnectionName(), sharedValues.getLeftHandle(), SyncIfsFileMode.LEFT_SYSTEM);
            int numRightIfsFiles = getNumIfsFiles(sharedValues.getRightConnectionName(), sharedValues.getRightHandle(), SyncIfsFileMode.RIGHT_SYSTEM);
            subMonitor.setWorkRemaining(numLeftIfsFiles + numRightIfsFiles);

            leftIfsFiles = loadIfsFileDescriptions(subMonitor, sharedValues.getLeftConnectionName(), sharedValues.getLeftHandle(),
                SyncIfsFileMode.LEFT_SYSTEM);

            rightIfsFiles = loadIfsFileDescriptions(subMonitor, sharedValues.getRightConnectionName(), sharedValues.getRightHandle(),
                SyncIfsFileMode.RIGHT_SYSTEM);

        } finally {
            done(subMonitor);
        }
    }

    public StreamFileDescription[] getLeftIfsFiles() {
        return leftIfsFiles;
    }

    public StreamFileDescription[] getRightIfsFiles() {
        return rightIfsFiles;
    }

    private StreamFileDescription[] loadIfsFileDescriptions(SubMonitor subMonitor, String connectionName, int handle, SyncIfsFileMode mode) {

        if (!initialize(connectionName)) {
            return EMPTY_RESULT;
        }

        try {

            if (subMonitor.isCanceled()) {
                cancelHostJob(handle);
                return EMPTY_RESULT;
            }

            if (!setCurrentLibrary()) {
                return EMPTY_RESULT;
            }

            return doLoadIfsFileDescriptions(subMonitor, connectionName, handle, mode);

        } finally {
            restoreCurrentLibrary();
        }
    }

    private StreamFileDescription[] doLoadIfsFileDescriptions(SubMonitor subMonitor, String connectionName, int handle, SyncIfsFileMode mode) {

        ArrayList<StreamFileDescription> arrayListSearchResults = new ArrayList<StreamFileDescription>();

        Connection jdbcConnection = IBMiHostContributionsHandler.getJdbcConnection(connectionName);
        SqlHelper sqlHelper = new SqlHelper(jdbcConnection);

        PreparedStatement preparedStatementSelect = null;
        ResultSet resultSet = null;

        try {

            if (handle == ERROR_HANDLE) {
                return EMPTY_RESULT;
            }

            final int ID = 1;
            final int PARENT = 2;
            final int TYPE = 3;
            final int ITEM = 4;
            final int LAST_CHANGED = 5;
            final int CHECKSUM = 6;

            if (SyncIfsFileMode.LEFT_SYSTEM.equals(mode)) {
                preparedStatementSelect = jdbcConnection.prepareStatement("SELECT XWID, XWPARENT, XWTYPE, XWITEM, XWLEFTLCHG, XWLEFTCRC FROM " //$NON-NLS-1$
                    + sqlHelper.getObjectName(getISphereLibrary(), "SYNCIFSW") //$NON-NLS-1$
                    + " WHERE XWHDL = ? ORDER BY XWTYPE, XWHDL, XWID, XWITEM", //$NON-NLS-1$
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            } else if (SyncIfsFileMode.RIGHT_SYSTEM.equals(mode)) {
                preparedStatementSelect = jdbcConnection.prepareStatement("SELECT XWID, XWPARENT, XWTYPE, XWITEM, XWRGHTLCHG, XWRGHTCRC FROM " //$NON-NLS-1$
                    + sqlHelper.getObjectName(getISphereLibrary(), "SYNCIFSW") //$NON-NLS-1$
                    + " WHERE XWHDL = ? ORDER BY XWTYPE, XWHDL, XWID, XWITEM", //$NON-NLS-1$
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            } else {
                throw new IllegalArgumentException("Incorrect mode: " + mode.mode()); //$NON-NLS-1$
            }

            preparedStatementSelect.setString(1, Integer.toString(handle));
            resultSet = preparedStatementSelect.executeQuery();

            int id;
            int parent;
            String type;
            String item;
            Timestamp lastChanged;
            long checksum;

            String relativePath;

            /*
             * Absolute path of the root directory of the compared directory
             * tree. It is taken from the one and only root row, which is the
             * row the host program stored the resolved directory path in. All
             * other items are identified by their path relative to it, because
             * that is the only way a left item can be matched with its right
             * counterpart.
             */
            String rootDirectory = null;

            Map<Integer, StreamFileDescription> parents = new HashMap<Integer, StreamFileDescription>();

            while (resultSet.next()) {

                if (subMonitor.isCanceled()) {
                    cancelHostJob(handle);
                    return EMPTY_RESULT;
                }

                consume(subMonitor, Messages.Task_Loading_compare_data);

                id = resultSet.getInt(ID);
                parent = resultSet.getInt(PARENT);
                type = resultSet.getString(TYPE).trim();
                item = resultSet.getString(ITEM).trim();
                lastChanged = resultSet.getTimestamp(LAST_CHANGED);
                checksum = resultSet.getLong(CHECKSUM);

                StreamFileDescription ifsFileDescription;
                StreamFileDescription parentDescription = parent == -1 ? null : parents.get(parent);

                if ("D".equals(type)) {
                    if (parentDescription == null) {
                        if (rootDirectory != null) {
                            ISpherePlugin.logError("*** Unexpected second root directory: " + item + " ***", null); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                        rootDirectory = item;
                        relativePath = IFSFileHelper.RELATIVE_ROOT;
                    } else {
                        relativePath = IFSFileHelper.toRelativePath(parentDescription.getRelativePath(), item);
                    }
                    ifsFileDescription = StreamFileDescription.newDirectoryDescription();
                    parents.put(id, ifsFileDescription);
                } else {
                    relativePath = IFSFileHelper.toRelativePath(parentDescription.getRelativePath(), item);
                    ifsFileDescription = StreamFileDescription.newFileDescription();
                }

                ifsFileDescription.setParentDirectory(parentDescription);

                ifsFileDescription.setConnectionName(connectionName);

                ifsFileDescription.setRootDirectory(rootDirectory);
                ifsFileDescription.setRelativePath(relativePath);

                ifsFileDescription.setLastChangedDate(lastChanged);
                ifsFileDescription.setChecksum(checksum);
                arrayListSearchResults.add(ifsFileDescription);

            }

        } catch (SQLException e) {
            ISpherePlugin.logError("*** Could not download IFS files (" + mode.mode() + ") ***", e); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (preparedStatementSelect != null) {
            try {
                preparedStatementSelect.close();
            } catch (SQLException e) {
                ISpherePlugin.logError("*** Could close prepared statement (" + mode.mode() + ") ***", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return arrayListSearchResults.toArray(new StreamFileDescription[arrayListSearchResults.size()]);
    }

    private int getNumIfsFiles(String connectionName, int handle, SyncIfsFileMode mode) {

        int numIfsFiles = 0;

        try {

            if (!initialize(connectionName)) {
                ISpherePlugin.logError("*** LoadCompareIfsFilesJob.getNumIfsFiles(): Could not initialize job ***", null);
                return 0;
            }

            if (!setCurrentLibrary()) {
                ISpherePlugin.logError("*** LoadCompareIfsFilesJob.getNumIfsFiles(): Could not set current library ***", null);
                return 0;
            }

            AS400 system = IBMiHostContributionsHandler.getSystem(connectionName);
            numIfsFiles = new SYNCIFS_getNumberOfCompareElements().run(system, handle, mode.mode());

        } finally {
            restoreCurrentLibrary();
        }

        return numIfsFiles;
    }

    private void debug(String message) {
        System.out.println(message);
    }
}
