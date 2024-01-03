/*******************************************************************************
 * Copyright (c) 2012-2020 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.sourcemembercopy.rse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.QSYSObjectPathName;

import biz.isphere.core.Messages;
import biz.isphere.core.ibmi.contributions.extension.handler.IBMiHostContributionsHandler;
import biz.isphere.core.internal.ISphereHelper;
import biz.isphere.core.memberrename.RenameMemberActor;
import biz.isphere.core.memberrename.rules.IMemberRenamingRule;
import biz.isphere.core.preferences.Preferences;
import biz.isphere.core.sourcemembercopy.CopyMemberItem;
import biz.isphere.core.sourcemembercopy.ICopyMembersPostRun;

/**
 * This class copies a given list of members to another library, file or member
 * name.
 * <p>
 * Today 'fromConnection' must equal 'toConnection'.
 */
public class CopyMemberService implements CopyMemberItem.ModifiedListener, ICopyMembersPostRun {

    public static String TO_FILE_FROMFILE = "*FROMFILE";

    private String fromConnectionName;
    private String toConnectionName;
    private String toLibrary;
    private String toFile;
    private SortedSet<CopyMemberItem> members;
    private ExistingMemberAction existingMemberAction;

    private Set<String> fromLibraryNames = new HashSet<String>();
    private Set<String> fromFileNames = new HashSet<String>();

    private Shell shell;
    private List<ModifiedListener> modifiedListeners;
    private int copiedCount;
    private boolean isActive;
    private boolean isCanceled;

    private CopyMembersJob copyMembersJob;

    public CopyMemberService(Shell shell, String fromConnectionName) {
        this.shell = shell;
        this.fromConnectionName = fromConnectionName;
        this.toConnectionName = fromConnectionName;
        this.toLibrary = null;
        this.toFile = null;
        this.members = new TreeSet<CopyMemberItem>();
        this.existingMemberAction = ExistingMemberAction.ERROR;
    }

    public void setExistingMemberAction(ExistingMemberAction action) {
        this.existingMemberAction = action;
    }

    public CopyMemberItem addItem(String file, String library, String member, String srcType) {

        CopyMemberItem copyMemberItem = new CopyMemberItem(file, library, member, srcType);
        copyMemberItem.addModifiedListener(this);

        members.add(copyMemberItem);

        fromLibraryNames.add(copyMemberItem.getFromLibrary());
        fromFileNames.add(copyMemberItem.getFromFile());

        return copyMemberItem;
    }

    public int getFromConnectionCcsid() {
        return getSystemCcsid(fromConnectionName);
    }

    public int getToConnectionCcsid() {
        return getSystemCcsid(toConnectionName);
    }

    private int getSystemCcsid(String connectionName) {
        AS400 system = IBMiHostContributionsHandler.getSystem(connectionName);
        if (system != null) {
            return system.getCcsid();
        }

        return -1;
    }

    public String getFromConnectionName() {
        return fromConnectionName;
    }

    public String getToConnectionName() {
        return toConnectionName;
    }

    public int getFromLibraryNamesCount() {
        return fromLibraryNames.size();
    }

    public String[] getFromLibraryNames() {
        return fromLibraryNames.toArray(new String[fromLibraryNames.size()]);
    }

    public String getToLibrary() {
        return toLibrary;
    }

    public int getFromFileNamesCount() {
        return fromFileNames.size();
    }

    public String[] getFromFileNames() {
        return fromFileNames.toArray(new String[fromFileNames.size()]);
    }

    public String getToFile() {
        return toFile;
    }

    public CopyMemberItem[] getItems() {
        return members.toArray(new CopyMemberItem[members.size()]);
    }

    public void setToConnection(String connectionName) {
        this.toConnectionName = connectionName.trim();
    }

    public void setToLibrary(String libraryName) {
        this.toLibrary = libraryName.trim();
    }

    public void setToFile(String fileName) {
        this.toFile = fileName.trim();
    }

    public CopyMemberItem[] getCopiedItems() {

        SortedSet<CopyMemberItem> copied = new TreeSet<CopyMemberItem>();

        for (CopyMemberItem member : members) {
            if (member.isCopied()) {
                copied.add(member);
            }
        }

        return copied.toArray(new CopyMemberItem[copied.size()]);
    }

    public CopyMemberItem[] getItemsToCopy() {

        SortedSet<CopyMemberItem> toCopy = new TreeSet<CopyMemberItem>();

        for (CopyMemberItem member : members) {
            if (!member.isCopied()) {
                toCopy.add(member);
            }
        }

        return toCopy.toArray(new CopyMemberItem[toCopy.size()]);
    }

    public boolean hasItemsToCopy() {

        if (copiedCount < members.size()) {
            return true;
        }

        return false;
    }

    public int getItemsCopiedCount() {
        return copiedCount;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    /**
     * Copies the members.
     */
    public void execute() {

        isCanceled = false;

        copyMembersJob = new CopyMembersJob(fromConnectionName, toConnectionName, members, existingMemberAction, this);
        copyMembersJob.schedule();
    }

    public void updateMembersWithTargetSourceFile() {

        startProcess();

        try {

            for (CopyMemberItem member : members) {
                member.setToLibrary(this.toLibrary);
                if (TO_FILE_FROMFILE.equals(this.toFile)) {
                    member.setToFile(member.getFromFile());
                } else {
                    member.setToFile(this.toFile);
                }
                member.setErrorMessage(null);
            }

        } finally {
            endProcess();
        }

    }

    public void returnResult(boolean isError, int countMembersCopied, long averageTime) {

        this.copyMembersJob = null;

        this.copiedCount = this.copiedCount + countMembersCopied;

        if (isCanceled && !hasItemsToCopy()) {
            isCanceled = false;
        }
    }

    public void reset() {

        for (CopyMemberItem member : members) {
            member.reset();
        }

        copiedCount = 0;
        isCanceled = false;
    }

    public void cancel() {

        if (copyMembersJob != null) {
            copyMembersJob.cancelOperation();
            isCanceled = true;
        }
    }

    private void startProcess() {
        isActive = true;
        notifyModifiedListeners(null);
    }

    private void endProcess() {
        isActive = false;
        notifyModifiedListeners(null);
    }

    /**
     * Copy member item has been modified. Forward notification to listeners of
     * this service.
     */
    public void modified(CopyMemberItem item) {
        notifyModifiedListeners(item);
    }

    /**
     * Adds a modified listener to this service.
     * 
     * @param listener - modified listener that is added
     */
    public void addModifiedListener(ModifiedListener listener) {

        if (modifiedListeners == null) {
            modifiedListeners = new ArrayList<ModifiedListener>();
        }

        modifiedListeners.add(listener);
    }

    /**
     * Removes a modified listener that listens to this service.
     * 
     * @param listener - modified listener that is removed
     */
    public void removeModifiedListener(ModifiedListener listener) {

        if (modifiedListeners != null) {
            modifiedListeners.remove(listener);
        }
    }

    /**
     * Notifies modified listeners about modifications to this service.
     * 
     * @param item - copy member item that has been changed
     */
    private void notifyModifiedListeners(CopyMemberItem item) {
        if (modifiedListeners == null) {
            return;
        }

        for (int i = 0; i < modifiedListeners.size(); ++i) {
            modifiedListeners.get(i).modified(item);
        }
    }

    public interface ModifiedListener {
        public void modified(CopyMemberItem item);
    }

    private class CopyMembersJob extends Job {

        private DoCopyMembers doCopyMembers;
        private ICopyMembersPostRun postRun;

        public CopyMembersJob(String fromConnectionName, String toConnectionName, SortedSet<CopyMemberItem> members,
            ExistingMemberAction existingMemberAction, ICopyMembersPostRun postRun) {
            super(Messages.Copying_dots);
            this.doCopyMembers = new DoCopyMembers(fromConnectionName, toConnectionName, members, existingMemberAction);
            this.postRun = postRun;
        }

        @Override
        public IStatus run(IProgressMonitor monitor) {

            startProcess();

            try {

                doCopyMembers.start(monitor);

                while (doCopyMembers.isAlive()) {

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }

            } finally {
                postRun.returnResult(doCopyMembers.isError(), doCopyMembers.getMembersCopiedCount(), doCopyMembers.getAverageTime());
                endProcess();
            }

            return Status.OK_STATUS;
        }

        public void cancelOperation() {
            if (doCopyMembers != null) {
                doCopyMembers.cancel();
            }
        }
    }

    private class DoCopyMembers extends Thread {

        private String fromConnectionName;
        private String toConnectionName;
        private SortedSet<CopyMemberItem> members;
        private ExistingMemberAction existingMemberAction;

        private IProgressMonitor monitor;

        private boolean isError;
        private int copiedCount;
        private long averageTime;

        public DoCopyMembers(String fromConnectionName, String toConnectionName, SortedSet<CopyMemberItem> members,
            ExistingMemberAction existingMemberAction) {
            this.fromConnectionName = fromConnectionName;
            this.toConnectionName = toConnectionName;
            this.members = members;
            this.existingMemberAction = existingMemberAction;
        }

        public void start(IProgressMonitor monitor) {
            if (monitor != null) {
                this.monitor = monitor;
            } else {
                this.monitor = new NullProgressMonitor();
            }
            this.start();
        }

        @Override
        public void run() {

            monitor.beginTask(Messages.Copying_dots, members.size());

            AS400 toSystem = IBMiHostContributionsHandler.getSystem(toConnectionName);

            try {

                isError = false;
                copiedCount = 0;

                long startTime = System.currentTimeMillis();

                int count = 0;
                for (CopyMemberItem member : members) {

                    count++;

                    if (monitor.isCanceled()) {
                        break;
                    }

                    if (member.isCopied()) {
                        monitor.worked(count);
                        continue;
                    }

                    if (monitor != null) {
                        monitor.setTaskName(Messages.bind(Messages.Copying_A_B_of_C, new Object[] { member.getFromMember(), count, members.size() }));
                    }

                    boolean canCopy;
                    if (isMember(toSystem, member.getToLibrary(), member.getToFile(), member.getToMember())) {
                        if (ExistingMemberAction.RENAME.equals(existingMemberAction)) {
                            canCopy = performRenameMember(toSystem, member);
                        } else if (ExistingMemberAction.REPLACE.equals(existingMemberAction)) {
                            canCopy = true;
                        } else {
                            canCopy = false;
                            member.setErrorMessage(Messages.bind(Messages.Target_member_A_already_exists, member.getToMember()));
                        }
                    } else {
                        canCopy = true;
                    }

                    if (!canCopy || !member.performCopyOperation(fromConnectionName, toConnectionName)) {
                        isError = true;
                    } else {
                        copiedCount++;
                    }

                    monitor.worked(count);
                }

                if (copiedCount > 0) {
                    averageTime = (System.currentTimeMillis() - startTime) / copiedCount;
                }
                // System.out.println("\nAverage time used: " + averageTime +
                // " mSecs.");

            } finally {
                monitor.done();
            }
        }

        private boolean isMember(AS400 toSystem, String library, String file, String member) {

            boolean isMember = ISphereHelper.checkMember(toSystem, library, file, member);

            return isMember;
        }

        private boolean performRenameMember(AS400 system, CopyMemberItem copyMemberItem) {

            IMemberRenamingRule newNameRule = Preferences.getInstance().getMemberRenamingRule();
            RenameMemberActor actor = new RenameMemberActor(system, newNameRule);

            String library = copyMemberItem.getToLibrary();
            String file = copyMemberItem.getToFile();
            String member = copyMemberItem.getToMember();

            try {

                // printDebug(String.format("Renaming member: %s", member));
                // //$NON-NLS-1$

                List<AS400Message> rtnMessages = new LinkedList<AS400Message>();
                QSYSObjectPathName newMember = actor.produceNewMemberName(library, file, member);

                String command = String.format("RNMM FILE(%s/%s) MBR(%s) NEWMBR(%s)", library, file, member, newMember.getMemberName()); //$NON-NLS-1$
                String message = ISphereHelper.executeCommand(system, command, rtnMessages);

                if (message != null) {
                    // printDebug(message);
                    StringBuilder errorMessage = new StringBuilder();
                    for (AS400Message as400Message : rtnMessages) {
                        if (errorMessage.length() > 0) {
                            errorMessage.append(" :: "); //$NON-NLS-1$
                        }
                        errorMessage.append(as400Message.getText());
                    }
                    copyMemberItem.setErrorMessage(errorMessage.toString());
                    return false;
                }

                // printDebug(String.format("Renamed member: %s --> %s", member,
                // newMember.getMemberName())); //$NON-NLS-1$

                return true;

            } catch (Exception e) {
                // e.printStackTrace();
                copyMemberItem.setErrorMessage(e.getMessage());
                return false;
            }

        }

        private void printDebug(String message) {
            System.out.println(message);
        }

        public void cancel() {
            monitor.setCanceled(true);
        }

        public boolean isError() {
            return isError;
        }

        public int getMembersCopiedCount() {
            return copiedCount;
        }

        public long getAverageTime() {
            return averageTime;
        }
    }
}
