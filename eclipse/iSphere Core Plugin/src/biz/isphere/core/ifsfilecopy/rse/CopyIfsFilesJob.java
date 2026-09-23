/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifsfilecopy.rse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.IFSFile;

import biz.isphere.base.internal.ExceptionHelper;
import biz.isphere.base.internal.StringHelper;
import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.Messages;
import biz.isphere.core.ibmi.contributions.extension.handler.IBMiHostContributionsHandler;
import biz.isphere.core.ifsfilecopy.CopyStreamFileItem;
import biz.isphere.core.ifsfilecopy.ErrorContext;
import biz.isphere.core.ifsfilecopy.ICopyStreamFilesPostRun;
import biz.isphere.core.ifsfilecopy.ICopyItemMessageListener;
import biz.isphere.core.ifsfilecopy.StreamFileCopyError;
import biz.isphere.core.ifsfilecopy.SynchronizeStreamFilesAction;
import biz.isphere.core.ifsfilerename.RenameStreamFileActor;
import biz.isphere.core.ifsfilerename.rules.IStreamFileRenamingRule;
import biz.isphere.core.ifsfilerename.rules.StreamFileRenamingRuleNumber;
import biz.isphere.core.internal.MessageDialogAsync;
import biz.isphere.core.internal.RemoteStreamFile;
import biz.isphere.core.internal.StreamFile;
import biz.isphere.core.sourcemembercopy.AbstractResult;

/**
 * This job copies a list of IFS files from a source directory to a target
 * directory. It is the IFS counterpart of
 * {@link biz.isphere.core.sourcemembercopy.rse.CopyMembersJob}.
 * <p>
 * The job is also used for <i>validating</i> the IFS files that are copied. See
 * {@link #setValidationMode(boolean)} and
 * {@link biz.isphere.core.ifsfilecopy.ValidateStreamFilesJob}.
 */
public class CopyIfsFilesJob extends Job {

    /**
     * Maximum length of an absolute IFS path name.
     */
    private static final int MAX_PATH_LENGTH = 1024;

    private Set<ICopyItemMessageListener> itemMessageListeners;

    private String fromConnectionName;
    private String toConnectionName;
    private CopyStreamFileItem[] ifsFiles;
    private ExistingIfsFileAction existingIfsFileAction;
    private MissingDirectoryAction missingDirectoryAction;
    boolean isIgnoreUnsavedChangesError;
    boolean isFullErrorCheck;
    boolean isRenameIfsFileCheck;
    boolean isValidationMode;
    private ICopyStreamFilesPostRun postRun;

    private IStreamFileRenamingRule ifsFileRenamingRule;

    private AS400 fromSystem;
    private AS400 toSystem;

    private Map<String, DirectoryError> directoryValidationResult;
    private IProgressMonitor monitor;

    private CopyResult copyResult;

    public CopyIfsFilesJob(String fromConnectionName, String toConnectionName, CopyStreamFileItem[] ifsFiles, ICopyStreamFilesPostRun postRun) {
        super(Messages.Copying_dots);

        this.itemMessageListeners = new HashSet<ICopyItemMessageListener>();

        this.fromConnectionName = fromConnectionName;
        this.toConnectionName = toConnectionName;
        this.ifsFiles = ifsFiles;
        this.existingIfsFileAction = ExistingIfsFileAction.ERROR;
        this.missingDirectoryAction = MissingDirectoryAction.ASK_USER;
        this.isIgnoreUnsavedChangesError = false;
        this.isFullErrorCheck = false;
        this.isRenameIfsFileCheck = true;
        this.isValidationMode = false;
        this.postRun = postRun;

        this.directoryValidationResult = new HashMap<String, DirectoryError>();
    }

    public void setMissingDirectoryAction(MissingDirectoryAction missingDirectoryAction) {
        this.missingDirectoryAction = missingDirectoryAction;
    }

    public void setExistingIfsFileAction(ExistingIfsFileAction existingIfsFileAction) {
        this.existingIfsFileAction = existingIfsFileAction;
    }

    public void setIgnoreUnsavedChanges(boolean enabled) {
        this.isIgnoreUnsavedChangesError = enabled;
    }

    public void setFullErrorCheck(boolean enabled) {
        this.isFullErrorCheck = enabled;
    }

    public void setRenameIfsFileCheck(boolean enabled) {
        this.isRenameIfsFileCheck = enabled;
    }

    public void setValidationMode(boolean enabled) {
        this.isValidationMode = enabled;
    }

    /**
     * Sets the rule that is used for producing the backup name of an IFS file
     * that is replaced. Defaults to {@link StreamFileRenamingRuleNumber}.
     * <p>
     * TODO: load the rule from the preferences, as soon as the IFS file
     * renaming rules are configurable on a preference page.
     * 
     * @param ifsFileRenamingRule - IFS file renaming rule
     */
    public void setIfsFileRenamingRule(IStreamFileRenamingRule ifsFileRenamingRule) {
        this.ifsFileRenamingRule = ifsFileRenamingRule;
    }

    public void addItemErrorListener(ICopyItemMessageListener listener) {
        itemMessageListeners.add(listener);
    }

    @Override
    public IStatus run(IProgressMonitor monitor) {

        SubMonitor subMonitor = SubMonitor.convert(monitor, ifsFiles.length);

        try {
            runInSameThread(subMonitor);
        } finally {
            subMonitor.done();
        }

        return Status.OK_STATUS;
    }

    public void runInSameThread(SubMonitor subMonitor) {

        monitor = subMonitor;

        try {

            copyResult = new CopyResult();

            fromSystem = IBMiHostContributionsHandler.getSystem(fromConnectionName);
            toSystem = IBMiHostContributionsHandler.getSystem(toConnectionName);

            if (isAbortProcessError(fromConnectionName, toConnectionName)) {
                return;
            }

            // Validate IFS file copied to same target
            Set<String> targetIfsFiles = new HashSet<String>();

            // Validate IFS files open in editor
            Set<String> dirtyFiles;
            if (!(isIgnoreUnsavedChangesError() || isFullErrorCheck())) {
                dirtyFiles = getDirtyFiles();
            } else {
                dirtyFiles = new HashSet<String>();
            }

            for (CopyStreamFileItem ifsFile : ifsFiles) {

                if (isCanceled()) {
                    break;
                }

                copyResult.addTotal();

                subMonitor.worked(1);

                if (ifsFile.isCopied()) {
                    copyResult.addSkipped();
                    continue;
                }

                if (ifsFile.isError()) {
                    copyResult.addSkipped();
                    continue;
                }

                RemoteStreamFile fromObject = RemoteStreamFile.newFile(fromConnectionName, ifsFile.getFromIfsFile());
                RemoteStreamFile toObject = RemoteStreamFile.newFile(toConnectionName, ifsFile.getToIfsFile());

                ErrorContext errorContext = new ErrorContext();
                errorContext.setFromObject(fromObject);
                errorContext.setToObject(toObject);
                errorContext.setCopyIfsFileItem(ifsFile);

                /*
                 * -------------------------------------------------------------
                 * Ensure that the IFS file is not copied to the same name
                 * twice.
                 * -------------------------------------------------------------
                 */

                String from = ifsFile.getFromIfsFile();
                String to = ifsFile.getToIfsFile();

                boolean isTwiceError;
                if (isSameSystem() && from.equals(to)) {
                    // Local copy...
                    setItemError(StreamFileCopyError.ERROR_TO_IFS_FILE_COPY_TO_SAME_NAME, errorContext,
                        Messages.bind(Messages.Cannot_copy_A_to_the_same_name, from));
                    isTwiceError = true;
                } else {
                    if (targetIfsFiles.contains(to)) {
                        setItemError(StreamFileCopyError.ERROR_TO_IFS_FILE_COPY_TO_SAME_NAME, errorContext,
                            Messages.Can_not_copy_stream_file_twice_to_same_target_stream_file);
                        isTwiceError = true;
                    } else {
                        isTwiceError = false;
                    }
                }

                // Always add the IFS file to the set.
                // No matter whether there is an error or not.
                targetIfsFiles.add(to);

                if (isTwiceError) {
                    continue;
                }

                /*
                 * -------------------------------------------------------------
                 * Check if the IFS file is open in an editor and has unsaved
                 * changes. Does not apply to directories.
                 * -------------------------------------------------------------
                 */

                if (!ifsFile.isDirectory()) {

                    String localResourcePath = getLocalResourcePath(fromConnectionName, from);

                    boolean isDirty;
                    if (localResourcePath != null && dirtyFiles.contains(localResourcePath)) {
                        setItemError(StreamFileCopyError.ERROR_FROM_IFS_FILE_IS_DIRTY, errorContext,
                            Messages.IFS_file_is_open_in_editor_and_has_unsaved_changes);
                        isDirty = true;
                    } else {
                        isDirty = false;
                    }

                    if (isDirty) {
                        continue;
                    }
                }

                /*
                 * -------------------------------------------------------------
                 * Do pre-checks.
                 * -------------------------------------------------------------
                 */

                boolean isPreCheckValid;
                if (isFromIfsFileValid(fromConnectionName, ifsFile, errorContext)) {
                    isPreCheckValid = isToDirectoryValid(toConnectionName, ifsFile, errorContext);
                } else {
                    isPreCheckValid = false;
                }

                if (!isPreCheckValid) {
                    continue;
                }

                /*
                 * -------------------------------------------------------------
                 * Check target IFS file. Directories are simply created, when
                 * they are missing, hence there is nothing to check here.
                 * -------------------------------------------------------------
                 */

                boolean canCopy;
                if (ifsFile.isDirectory()) {
                    canCopy = true;
                } else if (isIfsFile(getToSystem(), ifsFile.getToIfsFile())) {
                    if (ExistingIfsFileAction.RENAME.equals(existingIfsFileAction)) {
                        canCopy = performRenameIfsFile(getToSystem(), ifsFile, errorContext);
                    } else if (ExistingIfsFileAction.REPLACE.equals(existingIfsFileAction)) {
                        canCopy = true;
                    } else {
                        canCopy = false;
                        setItemError(StreamFileCopyError.ERROR_TO_IFS_FILE_EXISTS, errorContext,
                            Messages.bind(Messages.Target_stream_file_A_already_exists, ifsFile.getToIfsFile()));
                    }
                } else {
                    canCopy = true;
                }

                if (!canCopy) {
                    continue;
                }

                /*
                 * -------------------------------------------------------------
                 * Copy the IFS file.
                 * -------------------------------------------------------------
                 */

                boolean isCopied;
                if (!isValidationMode()) {
                    isCopied = ifsFile.performCopyOperation(fromConnectionName, toConnectionName);
                    if (!isCopied) {
                        setItemError(StreamFileCopyError.ERROR_COPY_IFS_FILE_COMMAND, errorContext, ifsFile.getErrorMessage());
                    }
                } else {
                    isCopied = true;
                }

                if (!isCopied) {
                    continue;
                }

                reportIfsFileCopied(ifsFile);
            }

        } catch (Throwable e) {
            String message = ExceptionHelper.getLocalizedMessage(e);
            ISpherePlugin.logError("Unexpected error: " + message, e); //$NON-NLS-1$
            setAbortErrorAndCancel(StreamFileCopyError.ERROR_EXCEPTION, message);
        } finally {
            copyResult.finished();
            subMonitor.done();
            if (postRun != null) {
                postRun.returnCopyIfsFilesResult(monitor.isCanceled(), copyResult.getTotal(), copyResult.getSkipped(), copyResult.getProcessed(),
                    copyResult.getErrors(), copyResult.getAverageTime(), copyResult.getCancelErrorId(), copyResult.getCancelMessage());
            }
        }
    }

    private boolean isAbortProcessError(String fromConnectionName, String toConnectionName) {

        if (getFromSystem() == null) {
            String errorMessage = Messages.bind(Messages.Connection_A_not_found, fromConnectionName);
            setAbortErrorAndCancel(StreamFileCopyError.ERROR_FROM_CONNECTION_NOT_FOUND, errorMessage);
            return true;
        }

        if (getToSystem() == null) {
            String errorMessage = Messages.bind(Messages.Connection_A_not_found, toConnectionName);
            setAbortErrorAndCancel(StreamFileCopyError.ERROR_TO_CONNECTION_NOT_FOUND, errorMessage);
            return true;
        }

        return false;
    }

    private boolean isFromIfsFileValid(String fromConnectionName, CopyStreamFileItem copyIfsFileItem, ErrorContext errorContext) {

        String fromIfsFile = copyIfsFileItem.getFromIfsFile();

        if (!isValidPath(fromIfsFile)) {
            setItemError(StreamFileCopyError.ERROR_FROM_PATH_NAME_NOT_VALID, errorContext,
                Messages.bind(Messages.Invalid_IFS_path_name_A, fromIfsFile));
            return false;
        }

        if (!isFullErrorCheck()) {
            return true;
        }

        String fromDirectoryName = getDirectoryName(fromIfsFile);

        DirectoryError directoryError = null;

        String fromDirectoryKey = getDirectoryValidKey(fromConnectionName, fromDirectoryName);
        if (!directoryValidationResult.containsKey(fromDirectoryKey)) {

            if (!isDirectory(getFromSystem(), fromDirectoryName)) {
                String errorMessage = Messages.bind(Messages.Directory_A_not_found, fromDirectoryName);
                directoryError = new DirectoryError(StreamFileCopyError.ERROR_FROM_DIRECTORY_NOT_FOUND, errorMessage);
            }

            // Store validation result
            directoryValidationResult.put(fromDirectoryKey, directoryError);

        } else {

            // Get previous result
            directoryError = directoryValidationResult.get(fromDirectoryKey);
        }

        if (directoryError != null) {
            setItemError(directoryError.errorId, errorContext, directoryError.errorMessage);
            return false;
        }

        if (!copyIfsFileItem.isDirectory()) {
            if (!isIfsFile(getFromSystem(), fromIfsFile)) {
                setItemError(StreamFileCopyError.ERROR_FROM_IFS_FILE_NOT_FOUND, errorContext,
                    Messages.bind(Messages.IFS_file_A_not_found, fromIfsFile));
                return false;
            }
        }

        return true;
    }

    private boolean isToDirectoryValid(String toConnectionName, CopyStreamFileItem copyIfsFileItem, ErrorContext errorContext) {

        String toIfsFile = copyIfsFileItem.getToIfsFile();

        if (!isValidPath(toIfsFile)) {
            setItemError(StreamFileCopyError.ERROR_TO_PATH_NAME_NOT_VALID, errorContext, Messages.bind(Messages.Invalid_IFS_path_name_A, toIfsFile));
            return false;
        }

        String toDirectoryName = getDirectoryName(toIfsFile);

        DirectoryError directoryError = null;

        String toDirectoryKey = getDirectoryValidKey(toConnectionName, toDirectoryName);
        if (!directoryValidationResult.containsKey(toDirectoryKey)) {

            if (!isDirectory(getToSystem(), toDirectoryName)) {
                if (!askUserAndCreateDirectory(toDirectoryName)) {
                    String errorMessage = Messages.bind(Messages.Directory_A_not_found, toDirectoryName);
                    directoryError = new DirectoryError(StreamFileCopyError.ERROR_TO_DIRECTORY_NOT_FOUND, errorMessage);
                }
            }

            // Store validation result
            directoryValidationResult.put(toDirectoryKey, directoryError);

        } else {

            // Get previous result
            directoryError = directoryValidationResult.get(toDirectoryKey);
        }

        if (directoryError == null) {
            return true;
        }

        setItemError(directoryError.errorId, errorContext, directoryError.errorMessage);

        return false;
    }

    private boolean askUserAndCreateDirectory(String toDirectoryName) {

        boolean doCreateMissingDirectory;
        if (MissingDirectoryAction.ERROR.equals(missingDirectoryAction)) {
            doCreateMissingDirectory = false;
        } else if (MissingDirectoryAction.CREATE.equals(missingDirectoryAction)) {
            doCreateMissingDirectory = true;
        } else {
            String[] messages = new String[] { toDirectoryName, Messages.Create_missing_directory_question };
            String[] buttonLabels = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL };
            int result = MessageDialogAsync.displayBlockingDialog(MessageDialog.CONFIRM, buttonLabels, Messages.Confirmation, messages);
            if (result == 0) {
                doCreateMissingDirectory = true;
            } else if (result == 1) {
                doCreateMissingDirectory = false;
            } else {
                doCreateMissingDirectory = false;
                String errorMessage = Messages.bind(Messages.Directory_A_not_found, toDirectoryName);
                setAbortErrorAndCancel(StreamFileCopyError.ERROR_TO_DIRECTORY_NOT_FOUND, errorMessage);
            }
        }

        if (!doCreateMissingDirectory) {
            return false;
        }

        if (isValidationMode()) {
            // Do not create anything while validating the IFS files.
            return true;
        }

        return createDirectory(getToSystem(), toDirectoryName);
    }

    private boolean createDirectory(AS400 system, String directoryName) {

        try {

            IFSFile directory = new IFSFile(system, directoryName);
            if (directory.exists()) {
                return true;
            }

            if (!directory.mkdirs()) {
                ISpherePlugin.logError("*** Could not create directory " + directoryName + " ***", null); //$NON-NLS-1$ //$NON-NLS-2$
                return false;
            }

            return true;

        } catch (Exception e) {
            ISpherePlugin.logError("*** Could not create directory " + directoryName + " ***", e); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }
    }

    private String getDirectoryValidKey(String connectionName, String directoryName) {
        return String.format("%s:%s", connectionName, directoryName); //$NON-NLS-1$
    }

    /**
     * Returns the directory of a given IFS path name.
     */
    private String getDirectoryName(String ifsFileName) {

        int i = ifsFileName.lastIndexOf('/');
        if (i < 0) {
            return "/"; //$NON-NLS-1$
        }

        if (i == 0) {
            // The item is stored in the root directory.
            return "/"; //$NON-NLS-1$
        }

        return ifsFileName.substring(0, i);
    }

    /**
     * Tests whether a given path name is a valid absolute IFS path name.
     */
    private boolean isValidPath(String ifsFileName) {

        if (StringHelper.isNullOrEmpty(ifsFileName)) {
            return false;
        }

        if (!ifsFileName.startsWith("/")) { //$NON-NLS-1$
            return false;
        }

        if (ifsFileName.length() > MAX_PATH_LENGTH) {
            return false;
        }

        if (ifsFileName.indexOf('\0') >= 0) {
            return false;
        }

        return true;
    }

    private boolean isDirectory(AS400 system, String directoryName) {

        try {
            return new IFSFile(system, directoryName).isDirectory();
        } catch (Exception e) {
            ISpherePlugin.logError("*** Could not check directory " + directoryName + " ***", e); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }
    }

    private boolean isIfsFile(AS400 system, String ifsFileName) {

        try {
            return new IFSFile(system, ifsFileName).exists();
        } catch (Exception e) {
            ISpherePlugin.logError("*** Could not check IFS file " + ifsFileName + " ***", e); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }
    }

    /**
     * Renames the existing target IFS file, so that the source IFS file can be
     * copied to that name.
     */
    private boolean performRenameIfsFile(AS400 system, CopyStreamFileItem copyIfsFileItem, ErrorContext errorContext) {

        if (isValidationMode()) {
            if (!(isRenameIfsFileCheck() || isFullErrorCheck())) {
                return true;
            }
        }

        RenameStreamFileActor actor = new RenameStreamFileActor(system, getIfsFileRenamingRule());

        String toIfsFile = copyIfsFileItem.getToIfsFile();

        try {

            String newIfsFile = actor.produceNewIfsFileName(toIfsFile);

            if (!isValidationMode()) {
                IFSFile oldFile = new IFSFile(system, toIfsFile);
                IFSFile newFile = new IFSFile(system, newIfsFile);
                if (!oldFile.renameTo(newFile)) {
                    setItemError(StreamFileCopyError.ERROR_TO_IFS_FILE_RENAME_EXCEPTION, errorContext,
                        Messages.bind(Messages.Could_not_rename_stream_file_A_to_B, new Object[] { toIfsFile, newIfsFile }));
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            // e.g. no more names are available
            setItemError(StreamFileCopyError.ERROR_TO_IFS_FILE_RENAME_EXCEPTION, errorContext, ExceptionHelper.getLocalizedMessage(e));
            return false;
        }
    }

    /**
     * Returns the path of the local resource of a given IFS file or
     * <code>null</code>, if it cannot be determined.
     */
    private String getLocalResourcePath(String connectionName, String ifsFileName) {

        try {

            StreamFile streamFile = IBMiHostContributionsHandler.getStreamFile(connectionName, ifsFileName);
            if (streamFile == null) {
                return null;
            }

            IFile localResource = streamFile.getLocalResource();
            if (localResource == null || localResource.getLocation() == null) {
                return null;
            }

            return localResource.getLocation().makeAbsolute().toOSString();

        } catch (Exception e) {
            ISpherePlugin.logError("*** Could not resolve the local resource of IFS file " + ifsFileName + " ***", e); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
    }

    private Set<String> getDirtyFiles() throws Exception {

        Set<String> openFiles = new HashSet<String>();

        try {

            IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
            for (IWorkbenchWindow window : windows) {
                IWorkbenchPage[] pages = window.getPages();
                for (IWorkbenchPage page : pages) {
                    IEditorReference[] editors = page.getEditorReferences();
                    for (IEditorReference editorReference : editors) {
                        if (editorReference.isDirty()) {
                            IEditorInput input = editorReference.getEditorInput();
                            if (input instanceof FileEditorInput) {
                                FileEditorInput fileInput = (FileEditorInput)input;
                                openFiles.add(fileInput.getFile().getLocation().makeAbsolute().toOSString());
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            String message = "*** Failed retrieving list of open editors ***"; //$NON-NLS-1$
            ISpherePlugin.logError(message, e);
            throw new Exception(message);
        }

        return openFiles;
    }

    protected boolean isSameSystem() {

        if (getFromSystem().getSystemName().equals(getToSystem().getSystemName())) {
            return true;
        }

        return false;
    }

    private boolean isIgnoreUnsavedChangesError() {
        return isIgnoreUnsavedChangesError;
    }

    private boolean isFullErrorCheck() {
        return isFullErrorCheck;
    }

    private boolean isRenameIfsFileCheck() {
        return isRenameIfsFileCheck;
    }

    private boolean isValidationMode() {
        return isValidationMode;
    }

    private IStreamFileRenamingRule getIfsFileRenamingRule() {

        if (ifsFileRenamingRule == null) {
            ifsFileRenamingRule = new StreamFileRenamingRuleNumber();
        }

        return ifsFileRenamingRule;
    }

    private AS400 getFromSystem() {
        return fromSystem;
    }

    private AS400 getToSystem() {
        return toSystem;
    }

    private void setAbortErrorAndCancel(StreamFileCopyError errorId, String errorMessage) {

        if (itemMessageListeners != null) {
            for (ICopyItemMessageListener errorListener : itemMessageListeners) {
                errorListener.reportCopyIfsFileMessage(errorId, null, errorMessage);
                copyResult.setCancel(errorId, errorMessage);
                cancelOperation();
            }
        }
    }

    private void setItemError(StreamFileCopyError errorId, ErrorContext errorContext, String errorMessage) {

        if (errorId == StreamFileCopyError.ERROR_NONE) {
            throw new IllegalArgumentException("Update Javadoc in ICopyItemMessageListener, if you want to allow: " + errorId.name()); //$NON-NLS-1$
        }

        CopyStreamFileItem ifsFile = errorContext.getCopyIfsFileItem();

        ifsFile.setErrorMessage(errorMessage);

        if (itemMessageListeners != null) {
            for (ICopyItemMessageListener errorListener : itemMessageListeners) {
                SynchronizeStreamFilesAction response = errorListener.reportCopyIfsFileMessage(errorId, ifsFile, errorMessage);
                if (response == SynchronizeStreamFilesAction.CANCEL) {
                    ifsFile.setErrorMessage(errorMessage);
                    copyResult.addError();
                    copyResult.setCancel(errorId, errorMessage);
                    cancelOperation();
                } else if (response == SynchronizeStreamFilesAction.CONTINUE_WITH_ERROR) {
                    ifsFile.setErrorMessage(errorMessage);
                    copyResult.addError();
                } else {
                    // Continue
                }
            }
        }
    }

    private void reportIfsFileCopied(CopyStreamFileItem ifsFile) {

        StreamFileCopyError errorId = StreamFileCopyError.ERROR_NONE;

        copyResult.addProcessed();

        if (itemMessageListeners != null) {
            for (ICopyItemMessageListener errorListener : itemMessageListeners) {
                SynchronizeStreamFilesAction response = errorListener.reportCopyIfsFileMessage(StreamFileCopyError.ERROR_NONE, ifsFile, null);
                if (response == SynchronizeStreamFilesAction.CANCEL) {
                    String errorMessage = Messages.Operation_has_been_canceled_by_the_user;
                    copyResult.setCancel(errorId, errorMessage);
                    cancelOperation();
                }
            }
        }
    }

    public boolean isError() {
        return copyResult.isError();
    }

    public int getCountTotal() {
        return copyResult.getTotal();
    }

    public int getCountSkipped() {
        return copyResult.getSkipped();
    }

    public int getIfsFilesCopiedCount() {
        return copyResult.getProcessed();
    }

    public int getIfsFilesErrorCount() {
        return copyResult.getErrors();
    }

    public long getAverageTime() {
        return copyResult.getAverageTime();
    }

    public void cancelOperation() {
        monitor.setCanceled(true);
    }

    public boolean isCanceled() {
        return monitor.isCanceled();
    }

    private class DirectoryError {

        StreamFileCopyError errorId;
        String errorMessage;

        public DirectoryError(StreamFileCopyError errorId, String errorMessage) {
            this.errorId = errorId;
            this.errorMessage = errorMessage;
        }
    }

    private class CopyResult extends AbstractResult<StreamFileCopyError> {
    };
}
