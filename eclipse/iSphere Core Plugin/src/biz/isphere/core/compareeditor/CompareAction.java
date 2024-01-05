/*******************************************************************************
 * Copyright (c) 2012-2024 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.compareeditor;

import java.util.ArrayList;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

import biz.isphere.base.internal.ExceptionHelper;
import biz.isphere.base.internal.UIHelper;
import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.Messages;
import biz.isphere.core.annotations.CMOne;
import biz.isphere.core.compareeditor.filters.IgnoreDateCompareFilter;
import biz.isphere.core.internal.Member;

public class CompareAction {

    private ArrayList<CleanupListener> cleanupListener = new ArrayList<CleanupListener>();
    private CompareEditorConfiguration cc;
    private Member ancestorMember;
    private Member leftMember;
    private Member rightMember;
    private String editorTitle;
    private boolean onPage;
    private CompareInput fInput;

    @CMOne(info = "Don`t change/remove this method due to CMOne compatibility reasons.")
    public CompareAction(CompareEditorConfiguration compareConfiguration, Member ancestorMember, Member leftMember, Member rightMember,
        String editorTitle) {
        this.cc = compareConfiguration;
        this.ancestorMember = ancestorMember;
        this.leftMember = leftMember;
        this.rightMember = rightMember;
        this.editorTitle = editorTitle;
        this.onPage = cc.isOpenInEditor();
    }

    public void run() {
        BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
            public void run() {

                if (cc.isLeftEditable()) {
                    try {
                        if (leftMember.isLocked()) {
                            throw new Exception(leftMember.getMemberLockedMessages());
                        }
                    } catch (Exception e) {
                        MessageDialog.openError(getShell(), Messages.Compare_source_members, ExceptionHelper.getLocalizedMessage(e));
                        return;
                    }
                }

                if (cc.isThreeWay() && (ancestorMember == null || !ancestorMember.exists())) {
                    MessageDialog.openError(getShell(), Messages.Compare_source_members, Messages.Member_not_found_colon_ANCESTOR);
                    return;
                }

                if (leftMember == null) {
                    MessageDialog.openError(getShell(), Messages.Compare_source_members, Messages.Member_not_found_colon_LEFT);
                    return;
                } else {
                    // Retrieve name parts before exist(), because the file name
                    // is lost if the member does not exist.
                    String library = leftMember.getLibrary();
                    String file = leftMember.getSourceFile();
                    String member = leftMember.getMember();
                    if (!leftMember.exists()) {
                        displayMemberNotFoundMessage(library, file, member);
                        return;
                    }
                }

                if (rightMember == null) {
                    MessageDialog.openError(getShell(), Messages.Compare_source_members, Messages.Member_not_found_colon_RIGHT);
                    return;
                } else {
                    // Retrieve name parts before exist(), because the file name
                    // is lost if the member does not exist.
                    String library = rightMember.getLibrary();
                    String file = rightMember.getSourceFile();
                    String member = rightMember.getMember();
                    if (!rightMember.exists()) {
                        displayMemberNotFoundMessage(library, file, member);
                        return;
                    }
                }

                if (cc.isLeftEditable()) {
                    IEditorPart editor = findMemberInEditor(leftMember);
                    if (editor != null) {
                        MessageDialog.openError(getShell(), Messages.Compare_source_members,
                            Messages.bind(Messages.Member_is_already_open_in_an_editor, leftMember.getMember()));
                        return;
                    }
                }

                UIHelper.getActivePage().addPartListener(new IPartListener() {
                    public void partClosed(IWorkbenchPart part) {
                        if (part instanceof EditorPart) {
                            EditorPart editorPart = (EditorPart)part;
                            if (editorPart.getEditorInput() == fInput) {
                                fInput.cleanup();
                                IWorkbenchPage workbenchPage = UIHelper.getActivePage();
                                if (workbenchPage != null) {
                                    workbenchPage.removePartListener(this);
                                }
                            }
                        }
                    }

                    public void partActivated(IWorkbenchPart part) {
                    }

                    public void partBroughtToTop(IWorkbenchPart part) {
                    }

                    public void partDeactivated(IWorkbenchPart part) {
                    }

                    public void partOpened(IWorkbenchPart part) {
                    }
                });

                if (cc.isThreeWay()) {
                    cc.setAncestorLabel(createLabel(ancestorMember));
                }

                cc.setLeftLabel(createLabel(leftMember));
                cc.setRightLabel(createLabel(rightMember));

                if (ISpherePlugin.isSaveNeededHandling()) {
                    // executed when WDSCi is the host application
                    fInput = new CompareInputWithSaveNeededHandling(cc, ancestorMember, leftMember, rightMember);
                } else {
                    // executed when RDi is the host application
                    fInput = new CompareInput(cc, ancestorMember, leftMember, rightMember);
                }

                // Set editor title displayed at the top of the Eclipse window.
                if (editorTitle != null) {
                    fInput.setTitle(editorTitle);
                } else {
                    String title;
                    if (cc.isLeftEditable()) {
                        title = Messages.bind(Messages.CompareEditor_Compare_Edit,
                            new String[] { getQualifiedMemberName(leftMember), getQualifiedMemberName(rightMember) });
                    } else {
                        title = Messages.bind(Messages.CompareEditor_Compare,
                            new String[] { getQualifiedMemberName(leftMember), getQualifiedMemberName(rightMember) });
                    }
                    fInput.setTitle(title);
                }

                /*
                 * Using a JVM property for changing the 'isEnabledInitially'
                 * property of class 'IgnoreDateCompareFilter' is ugly. But I
                 * could not figure out how to do it right. The
                 * 'IgnoreDateCompareFilter' is created in the constructor of
                 * 'ChangeCompareFilterPropertyAction'. The 'enabled' status of
                 * the compare filter can be changed with method setProperty(),
                 * which is called by the run() method. But how can we trigger
                 * the run() method?
                 */

                // See:
                // org.eclipse.compare.internal.ChangeCompareFilterPropertyAction

                boolean compareFilterEnabled = !cc.isConsiderDate();
                System.setProperty(IgnoreDateCompareFilter.JVM_PROPERTY_IGNORE_DATE, Boolean.toString(compareFilterEnabled));

                if (onPage) {
                    openCompareEditorOnPage();
                } else {
                    openCompareEditorDialog();
                }

                for (int index = 0; index < cleanupListener.size(); index++) {
                    (cleanupListener.get(index)).cleanup();
                }

            }

            private void openCompareEditorOnPage() {

                IEditorReference editorReference = findCompareEditor(leftMember, rightMember);
                if (editorReference != null) {

                    // TODO: make a decision, what is better: closing the editor
                    // or restoring it. Now the part is brought to front
                    // IEditorPart editorPart =
                    // editorReference.getEditor(false);
                    // editorPart.getEditorSite().getPage().closeEditor(editorPart,
                    // false);

                    UIHelper.getActivePage().activate(editorReference.getPart(false));
                    return;
                }
                CompareUI.openCompareEditorOnPage(fInput, UIHelper.getActivePage());
            }

            private void openCompareEditorDialog() {
                CompareUI.openCompareDialog(fInput);
            }

            private String createLabel(Member member) {

                if (member.getLabel() != null) {
                    return member.getLabel();
                } else {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append(member.getConnection());
                    buffer.append(": "); //$NON-NLS-1$
                    buffer.append(member.getLibrary());
                    buffer.append("/"); //$NON-NLS-1$
                    buffer.append(member.getSourceFile());
                    buffer.append("("); //$NON-NLS-1$
                    buffer.append(member.getMember());
                    buffer.append(")"); //$NON-NLS-1$
                    return buffer.toString();
                }
            }

            private Shell getShell() {
                return Display.getCurrent().getActiveShell();
            }

            private String getQualifiedMemberName(Member member) {

                StringBuilder buffer = new StringBuilder();

                buffer.append(member.getLibrary());
                buffer.append("/");
                buffer.append(member.getSourceFile());
                buffer.append("(");
                buffer.append(member.getMember());
                buffer.append(")");

                return buffer.toString();
            }

            private void displayMemberNotFoundMessage(String library, String file, String member) {
                String message = biz.isphere.core.Messages.bind(biz.isphere.core.Messages.Member_2_of_file_1_in_library_0_not_found,
                    new Object[] { library, file, member });
                MessageDialog.openError(getShell(), Messages.Compare_source_members, message);
            }
        });
    }

    protected static IEditorReference findCompareEditor(Member left, Member right) {
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorReference editorReference : page.getEditorReferences()) {
                    try {
                        IEditorInput editorInput = editorReference.getEditorInput();
                        if (editorInput instanceof CompareInput) {
                            CompareInput compareInput = (CompareInput)editorInput;
                            IFile leftMember = compareInput.getLeft().getLocalResource();
                            IFile rightMember = compareInput.getRight().getLocalResource();
                            if (leftMember.equals(left.getLocalResource()) && rightMember.equals(right.getLocalResource())
                                || leftMember.equals(right.getLocalResource()) && rightMember.equals(left.getLocalResource())) {
                                return editorReference;
                            }
                        }
                    } catch (Exception e) {
                        ISpherePlugin.logError("*** Could not find the compare editor ***", e);
                    }
                }
            }
        }
        return null;
    }

    protected static IEditorPart findMemberInEditor(Member left) {
        IFile member = left.getLocalResource();
        IWorkbench workbench = ISpherePlugin.getDefault().getWorkbench();
        IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
        IWorkbenchPage[] pages;
        IEditorReference[] editorRefs;
        IEditorPart editor;
        IEditorInput editorInput;
        for (int i = 0; i < windows.length; i++) {
            pages = windows[i].getPages();
            for (int x = 0; x < pages.length; x++) {
                editorRefs = pages[x].getEditorReferences();
                for (int refsIdx = 0; refsIdx < editorRefs.length; refsIdx++) {
                    editor = editorRefs[refsIdx].getEditor(false);
                    if (editor != null) {
                        editorInput = editor.getEditorInput();
                        if (editorInput instanceof FileEditorInput) {
                            if (((FileEditorInput)editorInput).getFile().equals(member)) {
                                return editor;
                            }
                        }
                        if (editorInput instanceof CompareInput) {
                            Member editorMember = ((CompareInput)editorInput).getLeft();
                            if (editorMember.getLocalResource().equals(left.getLocalResource())) {
                                return editor;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @CMOne(info = "Don`t change/remove this method due to CMOne compatibility reasons.")
    public void addCleanupListener(CleanupListener cleanupListener) {
        this.cleanupListener.add(cleanupListener);
    }

    @CMOne(info = "Don`t change/remove this method due to CMOne compatibility reasons.")
    public void removeCleanupListener(CleanupListener cleanupListener) {
        this.cleanupListener.remove(cleanupListener);
    }

}
