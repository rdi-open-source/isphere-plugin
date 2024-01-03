/*******************************************************************************
 * Copyright (c) 2012-2022 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ibmi.contributions.extension.point;

import java.sql.Connection;
import java.util.List;

import org.eclipse.core.resources.IFile;

import com.ibm.as400.access.AS400;

import biz.isphere.core.clcommands.ICLPrompter;
import biz.isphere.core.internal.Member;
import biz.isphere.core.internal.StreamFile;

public interface IIBMiHostContributions {

    /**
     * Returns <i>true</i> if the user wants to see qualified connections names
     * in the UI.
     * 
     * @return <i>true</i>, if property <i>Qualify Connection Names</i> of the
     *         <i>Remote Systems</i> view is selected, else <i>false</i>
     */
    public boolean isShowQualifyConnectionNames();

    /**
     * Returns <i>true</i> when the RSE sub-system has been initialized.
     * 
     * @return <i>true</i>, if RSE sub-system has been initialized, else
     *         <i>false</i>
     */
    public boolean isRseSubsystemInitialized();

    /**
     * Returns <i>true</i> when Kerberos authentication is enabled on the
     * "Remote Systems - IBM i - Authentication" preference page for RDi 9.5+.
     * 
     * @return <i>true</i>, if Kerberos authentication is selected, else
     *         <i>false</i>
     */
    public boolean isKerberosAuthentication();

    /**
     * Returns <i>true</i> when the specified connection is known to the
     * application.
     * 
     * @param qualifiedConnectionName - name that uniquely identifies the
     *        connection
     * @return <i>true</i>, when known, else <i>false</i>
     */
    public boolean isAvailable(String qualifiedConnectionName);

    /**
     * Returns <i>true</i> when the specified connection is in offline mode.
     * 
     * @param qualifiedConnectionName - name that uniquely identifies the
     *        connection
     * @return <i>true</i>, when offline, else <i>false</i>
     */
    public boolean isOffline(String qualifiedConnectionName);

    /**
     * Returns <i>true</i> when specified connection is connected.
     * 
     * @param qualifiedConnectionName - name that uniquely identifies the
     *        connection
     * @return <i>true</i>, when connected, else <i>false</i>
     */
    public boolean isConnected(String qualifiedConnectionName);

    /**
     * Connects the connection identified by a given connection name.
     * 
     * @param qualifiedConnectionName - name that uniquely identifies the
     *        connection
     * @return <i>true</i>, when successfully connected, else <i>false</i>
     * @throws Exception
     */
    public boolean connect(String qualifiedConnectionName) throws Exception;

    /**
     * Changes the <i>offline</i> status of the specified connection.
     * 
     * @param qualifiedConnectionName - name that uniquely identifies the
     *        connection
     */
    public void setOffline(String qualifiedConnectionName, boolean offline);

    /**
     * Returns the name of the iSphere library that is associated to a given
     * connection.
     * 
     * @param qualifiedConnectionName - name that uniquely identifies the
     *        connection library is returned for
     * @return name of the iSphere library
     */
    public String getISphereLibrary(String qualifiedConnectionName);

    /**
     * Returns the system (AS400) identified by a given connection name.
     * 
     * @param qualifiedConnectionName - name that uniquely identifies the
     *        connection
     * @return AS400
     */
    public AS400 getSystem(String qualifiedConnectionName);

    /**
     * Returns the connection name of a given editor.
     * 
     * @param file - remote file downloaded to the workspace
     * @return name of the connection the file has been loaded from
     */
    public String getConnectionName(IFile file);

    /**
     * Returns the connection name of a given i Project.
     * 
     * @param projectName - name of an i Project
     * @return name of the connection the file has been loaded from
     */
    public String getConnectionNameOfIProject(String projectName);

    /**
     * Returns the name of the associated library of a given i Project.
     * 
     * @param projectName - name of an i Project
     * @return name of the associated library
     */
    public String getLibraryNameOfIProject(String projectName);

    /**
     * Returns the qualified connection name of a given TCP/IP Address.
     * 
     * @param projectName - TCP/IP address
     * @param isConnected - specifies whether the connection must be connected
     * @return name of the connection
     */
    public String getConnectionNameByIPAddr(String tcpIpAddr, boolean isConnected);

    /**
     * Returns a list of configured connections.
     * 
     * @return names of configured connections
     */
    public String[] getConnectionNames();

    /**
     * Returns a JDBC connection for a given connection name.
     * 
     * @param qualifiedConnectionName - name that uniquely identifies the
     *        connection
     * @return Connection
     */
    public Connection getJdbcConnection(String qualifiedConnectionName);

    /**
     * Returns an ICLPrompter for a given connection name.
     * 
     * @param qualifiedConnectionName - connection name to identify the
     *        connection
     * @return ICLPrompter
     */
    public ICLPrompter getCLPrompter(String qualifiedConnectionName);

    /**
     * Returns the file member identified by library, file and member name.
     * 
     * @param qualifiedConnectionName - name that uniquely identifies the
     *        connection
     * @param libraryName - name of the library where the file is stored
     * @param fileName - name of the file that contains the member
     * @param memberName - name that identifies the member
     * @return Member
     * @throws Exception
     */
    public Member getMember(String qualifiedConnectionName, String libraryName, String fileName, String memberName) throws Exception;

    /**
     * Opens the iSphere compare editor for the given members.
     * <p>
     * The available options are:
     * <p>
     * <b>Empty member list</b> <br>
     * Opens the compare dialog to let the user specify the members that are
     * compares.
     * <p>
     * <b>One member</b> <br>
     * Opens the compare dialog with that member set as the left (editable)
     * member. The right member is initialized with the properties of the left
     * member.
     * <p>
     * <b>Two members</b> <br>
     * Opens the compare dialog with the first member set as the left (editable)
     * and the second member set as the right member.
     * <p>
     * <b>More than 2 members</b> <br>
     * Opens the compare dialog to let the user specify the source file that
     * contains the members, which are compared one by one with the selected
     * members.
     * 
     * @param qualifiedConnectionName - name that uniquely identifies the
     *        connection
     * @param members - members that are compared
     * @param enableEditMode - specifies whether edit mode is enabled
     * @throws Exception
     */
    public void compareSourceMembers(String qualifiedConnectionName, List<Member> members, boolean enableEditMode) throws Exception;

    /**
     * Returns the stream file identified by a given path name.
     * 
     * @param qualifiedConnectionName - name that uniquely identifies the
     *        connection
     * @param streamFileName - path name of the stream file
     * @return stream file
     * @throws Exception
     */
    public StreamFile getStreamFile(String qualifiedConnectionName, String streamFileName) throws Exception;

    /**
     * Opens the iSphere compare editor for the given stream files.
     * <p>
     * The available options are:
     * <p>
     * <b>Empty stream file list</b> <br>
     * Opens the compare dialog to let the user specify the stream files that
     * are compared.
     * <p>
     * <b>One stream file</b> <br>
     * Opens the compare dialog with that stream file set as the left (editable)
     * stream file. The right stream file is initialized with the properties of
     * the left stream file.
     * <p>
     * <b>Two stream files</b> <br>
     * Opens the compare dialog with the first stream file set as the left
     * (editable) and the second stream file set as the right stream file.
     * <p>
     * <b>More than 2 stream files</b> <br>
     * Opens the compare dialog to let the user specify the source path where
     * the stream files are stored, which are compared one by one with the
     * selected stream files.
     * 
     * @param qualifiedConnectionName - name that uniquely identifies the
     *        connection
     * @param streamFiles - stream files that are compared
     * @param enableEditMode - specifies whether edit mode is enabled
     * @throws Exception
     */
    public void compareStreamFiles(String qualifiedConnectionName, List<StreamFile> streamFiles, boolean enableEditMode) throws Exception;

    /**
     * Returns the local resource of a given remote member.
     * 
     * @param qualifiedConnectionName - name that uniquely identifies the
     *        connection
     * @param libraryName - name of the library where the file is stored
     * @param fileName - name of the file that contains the member
     * @param memberName - name that identifies the member
     * @param srcType - type of the member
     * @return local member resource
     */
    public IFile getLocalResource(String qualifiedConnectionName, String libraryName, String fileName, String memberName, String srcType)
        throws Exception;
}
