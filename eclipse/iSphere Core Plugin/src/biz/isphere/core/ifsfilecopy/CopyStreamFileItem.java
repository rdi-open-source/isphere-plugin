/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifsfilecopy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.IFSFileInputStream;
import com.ibm.as400.access.IFSFileOutputStream;

import biz.isphere.base.internal.Buffer;
import biz.isphere.base.internal.ExceptionHelper;
import biz.isphere.base.internal.StringHelper;
import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.Messages;
import biz.isphere.core.ibmi.contributions.extension.handler.IBMiHostContributionsHandler;
import biz.isphere.core.internal.ISphereHelper;

/**
 * This class represents an IFS file that is copied from a source directory to a
 * target directory. It is the IFS counterpart of
 * {@link biz.isphere.core.sourcemembercopy.CopyMemberItem}.
 */
public class CopyStreamFileItem implements Comparable<CopyStreamFileItem> {

    private static final boolean debug = false;

    private String fromIfsFile;
    private String toIfsFile;
    private boolean isDirectory;

    private Object data;

    private String errorMessage;
    private boolean copied;

    private List<ModifiedListener> modifiedListeners;

    /**
     * Produces a new copy item for an IFS <i>file</i>.
     * 
     * @param fromIfsFile - absolute path of the source IFS file
     */
    public static CopyStreamFileItem newFile(String fromIfsFile) {
        return new CopyStreamFileItem(fromIfsFile, false);
    }

    /**
     * Produces a new copy item for an IFS <i>directory</i>.
     * 
     * @param fromIfsFile - absolute path of the source directory
     */
    public static CopyStreamFileItem newDirectory(String fromIfsFile) {
        return new CopyStreamFileItem(fromIfsFile, true);
    }

    public CopyStreamFileItem(String fromIfsFile, boolean isDirectory) {
        this.fromIfsFile = fromIfsFile.trim();
        this.toIfsFile = fromIfsFile.trim();
        this.isDirectory = isDirectory;
        this.errorMessage = null;
        this.copied = false;
        this.data = null;
    }

    public String getFromIfsFile() {
        return fromIfsFile;
    }

    public String getToIfsFile() {
        return toIfsFile;
    }

    public void setToIfsFile(String toIfsFile) {
        if (hasChanged(this.toIfsFile, toIfsFile)) {
            this.toIfsFile = toIfsFile;
            notifyModifiedListeners();
        }
    }

    /**
     * @return <code>true</code>, if this item refers to a directory, else
     *         <code>false</code>
     */
    public boolean isDirectory() {
        return isDirectory;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public boolean isError() {
        if (!StringHelper.isNullOrEmpty(getErrorMessage())) {
            return true;
        }
        return false;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String message) {
        if (hasChanged(this.errorMessage, message)) {
            this.errorMessage = message;
            notifyModifiedListeners();
        }
    }

    public boolean isCopied() {
        return copied;
    }

    private void setCopyStatus(boolean copied) {
        if (hasChanged(this.copied, copied)) {
            this.copied = copied;
            this.errorMessage = null;
            notifyModifiedListeners();
        }
    }

    private boolean hasChanged(String currentValue, String newValue) {

        if (currentValue == null && newValue == null) {
            return false;
        } else if (currentValue != null && currentValue.equals(newValue)) {
            return false;
        }

        return true;
    }

    private boolean hasChanged(boolean currentValue, boolean newValue) {

        if (currentValue == newValue) {
            return false;
        }

        return true;
    }

    public int compareTo(CopyStreamFileItem item) {

        if (this.equals(item)) {
            return 0;
        }

        int result;

        result = getFromIfsFile().compareTo(item.getFromIfsFile());
        if (result != 0) {
            return result;
        }

        if (getToIfsFile() == null) {
            return -1;
        } else if (item.getToIfsFile() == null) {
            return 1;
        } else {
            result = getToIfsFile().compareTo(item.getToIfsFile());
            if (result != 0) {
                return result;
            }
        }

        return 0;
    }

    @Override
    public String toString() {

        StringBuilder buffer = new StringBuilder();

        buffer.append(fromIfsFile);
        buffer.append(" -> "); //$NON-NLS-1$
        buffer.append(toIfsFile);

        return buffer.toString();
    }

    /**
     * Copies the source IFS file to the target IFS file.
     * 
     * @param fromConnectionName - name of the source connection
     * @param toConnectionName - name of the target connection
     * @return <code>true</code>, if the item has been copied, else
     *         <code>false</code>
     */
    public boolean performCopyOperation(String fromConnectionName, String toConnectionName) {

        String message;

        if (isDirectory()) {
            message = performCreateDirectory(fromConnectionName, toConnectionName);
        } else if (fromConnectionName.equalsIgnoreCase(toConnectionName)) {
            message = performLocalCopy(fromConnectionName);
        } else {
            message = performCopyBetweenConnections(fromConnectionName, toConnectionName);
        }

        if (message != null) {
            setErrorMessage(message);
            return false;
        }

        setCopyStatus(true);

        return true;
    }

    public void reset() {

        setErrorMessage(null);
        setCopyStatus(false);
    }

    /**
     * Creates the target directory. The <code>mkdirs()</code> method also
     * creates the missing parent directories.
     */
    private String performCreateDirectory(String fromConnectionName, String toConnectionName) {

        try {

            AS400 fromSystem = IBMiHostContributionsHandler.getSystem(fromConnectionName);
            AS400 toSystem = IBMiHostContributionsHandler.getSystem(toConnectionName);

            IFSFile toDirectory = new IFSFile(toSystem, getToIfsFile());
            if (!toDirectory.exists()) {
                if (!toDirectory.mkdirs()) {
                    return Messages.bind(Messages.Could_not_create_directory_A, getToIfsFile());
                }
            }

            copyLastModified(fromSystem, toSystem);

        } catch (Throwable e) {
            ISpherePlugin.logError("*** Unexpected error when creating IFS directory ***", e); //$NON-NLS-1$
            return ExceptionHelper.getLocalizedMessage(e);
        }

        return null;
    }

    /**
     * Copies the IFS file on the same system using the <code>CPY</code>
     * command.
     */
    private String performLocalCopy(String connectionName) {

        try {

            AS400 system = IBMiHostContributionsHandler.getSystem(connectionName);

            IFSFile fromIfsFile = new IFSFile(system, getFromIfsFile());
            if (!fromIfsFile.exists()) {
                return Messages.bind(Messages.IFS_file_A_not_found, getFromIfsFile());
            }

            List<AS400Message> rtnMessages = new ArrayList<AS400Message>();

            StringBuilder command = new StringBuilder();
            command.append("CPY"); //$NON-NLS-1$
            command.append(" OBJ('"); //$NON-NLS-1$
            command.append(quote(getFromIfsFile()));
            command.append("')"); //$NON-NLS-1$
            command.append(" TOOBJ('"); //$NON-NLS-1$
            command.append(quote(getToIfsFile()));
            command.append("')"); //$NON-NLS-1$
            command.append(" REPLACE(*YES)"); //$NON-NLS-1$

            String message = ISphereHelper.executeCommand(system, command.toString(), rtnMessages);
            if (message != null) {
                return buildMessageString(rtnMessages);
            }

            copyLastModified(system, system);

        } catch (Throwable e) {
            ISpherePlugin.logError("*** Unexpected error when copying IFS file ***", e); //$NON-NLS-1$
            return ExceptionHelper.getLocalizedMessage(e);
        }

        return null;
    }

    /**
     * Copies the IFS file between two systems by streaming the bytes of the
     * source file to the target file.
     */
    private String performCopyBetweenConnections(String fromConnectionName, String toConnectionName) {

        IFSFileInputStream in = null;
        IFSFileOutputStream out = null;

        try {

            debugPrint("\nProcessing IFS file: " + getFromIfsFile()); //$NON-NLS-1$

            long startTime = System.currentTimeMillis();

            AS400 fromSystem = IBMiHostContributionsHandler.getSystem(fromConnectionName);
            AS400 toSystem = IBMiHostContributionsHandler.getSystem(toConnectionName);

            IFSFile fromIfsFile = new IFSFile(fromSystem, getFromIfsFile());
            if (!fromIfsFile.exists()) {
                return Messages.bind(Messages.IFS_file_A_not_found, getFromIfsFile());
            }

            /*
             * Delete the target file and create it anew, because the CCSID of a
             * file can only be changed as long as the file is empty.
             */
            IFSFile toIfsFile = new IFSFile(toSystem, getToIfsFile());
            if (toIfsFile.exists()) {
                toIfsFile.delete();
            }
            toIfsFile.createNewFile();

            copyCcsid(fromIfsFile, toIfsFile);

            in = new IFSFileInputStream(fromSystem, getFromIfsFile());
            out = new IFSFileOutputStream(toSystem, getToIfsFile());

            byte[] buffer = new byte[Buffer.size("8k")]; //$NON-NLS-1$
            int count = 0;
            do {
                count = in.read(buffer, 0, buffer.length);
                if (count > 0) {
                    out.write(buffer, 0, count);
                }
            } while (count != -1);

            out.close();
            out = null;
            in.close();
            in = null;

            copyLastModified(fromSystem, toSystem);

            debugPrint("Copying the IFS file took " + (System.currentTimeMillis() - startTime) + " mSecs."); //$NON-NLS-1$ //$NON-NLS-2$

        } catch (Throwable e) {
            ISpherePlugin.logError("*** Unexpected error when copying IFS file ***", e); //$NON-NLS-1$
            return ExceptionHelper.getLocalizedMessage(e);
        } finally {

            if (out != null) {
                try {
                    out.close();
                } catch (Throwable e) {
                    ISpherePlugin.logError("*** Could not close IFS output stream ***", e); //$NON-NLS-1$
                }
            }

            if (in != null) {
                try {
                    in.close();
                } catch (Throwable e) {
                    ISpherePlugin.logError("*** Could not close IFS input stream ***", e); //$NON-NLS-1$
                }
            }
        }

        return null;
    }

    /**
     * Assigns the CCSID of the source file to the target file. Must be called
     * before the data is written to the target file.
     * <p>
     * Best effort. The synchronization is not canceled, when the CCSID cannot
     * be changed.
     */
    private void copyCcsid(IFSFile fromIfsFile, IFSFile toIfsFile) {

        try {
            if (!toIfsFile.setCCSID(fromIfsFile.getCCSID())) {
                ISpherePlugin.logError("*** Could not set the CCSID of IFS file " + getToIfsFile() + " ***", null); //$NON-NLS-1$ //$NON-NLS-2$
            }
        } catch (Throwable e) {
            ISpherePlugin.logError("*** Could not set the CCSID of IFS file " + getToIfsFile() + " ***", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Assigns the last modification time of the source item to the target item.
     * That is important, because the <i>Synchronize IFS files</i> editor
     * compares the items by their last modification time.
     * <p>
     * Best effort. The synchronization is not canceled, when the time stamp
     * cannot be changed.
     */
    private void copyLastModified(AS400 fromSystem, AS400 toSystem) {

        try {
            long lastModified = new IFSFile(fromSystem, getFromIfsFile()).lastModified();
            if (lastModified != 0) {
                new IFSFile(toSystem, getToIfsFile()).setLastModified(lastModified);
            }
        } catch (Throwable e) {
            ISpherePlugin.logError("*** Could not set the last modification time of IFS file " + getToIfsFile() + " ***", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Doubles the apostrophes of a path name, so that it can safely be passed
     * to a CL command.
     */
    private String quote(String path) {
        return path.replaceAll("'", "''"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String buildMessageString(List<AS400Message> rtnMessages) {

        StringBuilder message = new StringBuilder();

        List<AS400Message> messages = new LinkedList<AS400Message>(rtnMessages);
        for (AS400Message as400Message : messages) {
            if (message.length() > 0) {
                message.append(" :: "); //$NON-NLS-1$
            }
            message.append(as400Message.getText());
        }

        return message.toString();
    }

    private void debugPrint(String message) {
        if (debug) {
            System.out.println(message);
        }
    }

    public void addModifiedListener(ModifiedListener listener) {

        if (modifiedListeners == null) {
            modifiedListeners = new ArrayList<ModifiedListener>();
        }

        modifiedListeners.add(listener);
    }

    public void removeModifiedListener(ModifiedListener listener) {

        if (modifiedListeners != null) {
            modifiedListeners.remove(listener);
        }
    }

    private void notifyModifiedListeners() {

        if (modifiedListeners == null) {
            return;
        }

        for (int i = 0; i < modifiedListeners.size(); ++i) {
            modifiedListeners.get(i).modified(this);
        }
    }

    public interface ModifiedListener {
        public void modified(CopyStreamFileItem item);
    }
}
