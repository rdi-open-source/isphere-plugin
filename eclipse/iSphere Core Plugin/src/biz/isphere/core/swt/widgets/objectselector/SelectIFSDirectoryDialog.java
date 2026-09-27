/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.swt.widgets.objectselector;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.IFSFile;

import biz.isphere.base.internal.StringHelper;
import biz.isphere.base.jface.dialogs.XDialog;
import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.Messages;
import biz.isphere.core.ibmi.contributions.extension.handler.IBMiHostContributionsHandler;
import biz.isphere.core.swt.widgets.WidgetFactory;
import biz.isphere.core.swt.widgets.objectselector.model.AbstractListItem;
import biz.isphere.core.swt.widgets.objectselector.model.DirectoryItem;

/**
 * This class produces a tree browser for selecting a directory of the IFS of an
 * IBM i.
 */
public class SelectIFSDirectoryDialog extends XDialog {

    private String connectionName;
    private String initialDirectory;

    private Label toDoLabel;
    private Composite frame;
    private Label directoryPath;
    private TreeViewer viewer;

    private DirectoryItem selectedDirectory;

    public SelectIFSDirectoryDialog(Shell parentShell, String connectionName) {
        super(parentShell);

        this.connectionName = connectionName;
    }

    /**
     * Sets the directory that is expanded and selected, when the dialog is
     * opened.
     *
     * @param directory - absolute path of the directory
     */
    public void setDirectory(String directory) {
        this.initialDirectory = directory;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);

        newShell.setText(Messages.bind(Messages.Browse_For_A, Messages.Directory));
    }

    @Override
    protected Control createDialogArea(Composite parent) {

        Composite container = (Composite)super.createDialogArea(parent);
        GridLayout containerLayout = new GridLayout();
        containerLayout.marginWidth = 10;
        container.setLayout(containerLayout);

        toDoLabel = new Label(container, SWT.NONE);
        toDoLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toDoLabel.setText(Messages.bind(Messages.Select_A, Messages.Directory));

        WidgetFactory.createLineFiller(container);

        frame = new Composite(container, SWT.BORDER);
        GridLayout frameLayout = new GridLayout();
        frameLayout.marginWidth = 2;
        frameLayout.marginHeight = 2;
        frame.setLayout(frameLayout);
        frame.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        directoryPath = new Label(frame, SWT.NONE);
        directoryPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        viewer = new TreeViewer(container);
        viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        viewer.setContentProvider(new TreeContentProvider());
        viewer.setLabelProvider(new TreeLabelProvider());
        viewer.getTree().setHeaderVisible(false);
        viewer.getTree().setLinesVisible(false);

        TreeViewerColumn viewerColumn = new TreeViewerColumn(viewer, SWT.NONE);
        viewerColumn.getColumn().setWidth(300);
        viewerColumn.setLabelProvider(new TreeLabelProvider());

        setListItems();

        configureControls();

        return dialogArea;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);

        isValidated();
    }

    private void setListItems() {

        DirectoryItem rootDirectory = DirectoryItem.createRootDirectory(getSystem());

        viewer.setInput(new DirectoryItem[] { rootDirectory });

        viewer.setExpandedState(rootDirectory, true);

        expandAndSelectDirectory(rootDirectory, initialDirectory);
    }

    /**
     * Expands the tree along the path of a given directory and selects that
     * directory. Selects the root directory, if the directory cannot be found.
     */
    private void expandAndSelectDirectory(DirectoryItem rootDirectory, String directory) {

        List<DirectoryItem> directoryPath = findDirectoryPath(rootDirectory, directory);

        DirectoryItem selectedItem;
        if (directoryPath == null) {
            selectedItem = rootDirectory;
        } else {
            for (DirectoryItem directoryItem : directoryPath) {
                viewer.setExpandedState(directoryItem, true);
            }
            selectedItem = directoryPath.get(directoryPath.size() - 1);
        }

        viewer.setSelection(new StructuredSelection(selectedItem), true);
        showSelectedDirectory(selectedItem);
    }

    /**
     * Returns the directory items along the path of a given directory,
     * beginning with the root directory, or <code>null</code>, if the directory
     * cannot be found.
     */
    private List<DirectoryItem> findDirectoryPath(DirectoryItem rootDirectory, String directory) {

        if (StringHelper.isNullOrEmpty(directory)) {
            return null;
        }

        List<DirectoryItem> directoryPath = new LinkedList<DirectoryItem>();
        directoryPath.add(rootDirectory);

        DirectoryItem currentDirectory = rootDirectory;

        String[] names = directory.trim().split(IFSFile.separator);
        for (String name : names) {
            if (StringHelper.isNullOrEmpty(name)) {
                continue;
            }

            currentDirectory = findChildDirectory(currentDirectory, name);
            if (currentDirectory == null) {
                return null;
            }

            directoryPath.add(currentDirectory);
        }

        return directoryPath;
    }

    /**
     * Returns the child directory of a given name. Prefers an exact match,
     * because some file systems, such as /QOpenSys, are case-sensitive.
     */
    private DirectoryItem findChildDirectory(DirectoryItem parent, String name) {

        DirectoryItem caseInsensitiveMatch = null;

        for (AbstractListItem child : parent.getChildren()) {
            if (child instanceof DirectoryItem) {
                if (child.getName().equals(name)) {
                    return (DirectoryItem)child;
                } else if (caseInsensitiveMatch == null && child.getName().equalsIgnoreCase(name)) {
                    caseInsensitiveMatch = (DirectoryItem)child;
                }
            }
        }

        return caseInsensitiveMatch;
    }

    private void configureControls() {

        viewer.addSelectionChangedListener(new TreeSelectionChangedListener());
        viewer.addDoubleClickListener(new TreeDoubleClickListener());
    }

    private void showSelectedDirectory(Object element) {

        if (element instanceof DirectoryItem) {
            selectedDirectory = (DirectoryItem)element;
            directoryPath.setText(selectedDirectory.getPath());
        } else {
            selectedDirectory = null;
            directoryPath.setText(""); //$NON-NLS-1$
        }

        isValidated(); // Validate selected directory
    }

    @Override
    protected void okPressed() {

        if (!isValidated()) {
            return;
        }

        super.okPressed();
    }

    private boolean isValidated() {

        if (getButton(Dialog.OK) == null) {
            return selectedDirectory != null;
        }

        if (selectedDirectory == null) {
            getButton(Dialog.OK).setEnabled(false);
            return false;
        }

        getButton(Dialog.OK).setEnabled(true);

        return true;
    }

    /**
     * Returns the absolute path of the selected directory.
     *
     * @return path of the selected directory
     */
    public String getSelectedDirectory() {

        if (selectedDirectory == null) {
            return null;
        }

        return selectedDirectory.getPath();
    }

    public AS400 getSystem() {
        return IBMiHostContributionsHandler.getSystem(connectionName);
    }

    /**
     * Overridden make this dialog resizable {@link XDialog}.
     */
    @Override
    protected boolean isResizable() {
        return true;
    }

    /**
     * Overridden to return a default size.
     */
    @Override
    protected Point getDefaultSize() {
        return new Point(300, 500);
    }

    /**
     * Overridden to let {@link XDialog} store the state of this dialog in a
     * separate section of the dialog settings file.
     */
    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return super.getDialogBoundsSettings(ISpherePlugin.getDefault().getDialogSettings());
    }

    private class TreeLabelProvider extends CellLabelProvider {

        @Override
        public void update(ViewerCell cell) {
            Object element = cell.getElement();
            if (element instanceof AbstractListItem) {
                AbstractListItem listItem = (AbstractListItem)element;
                cell.setText(listItem.getLabel());
                cell.setImage(listItem.getImage());
            }
        }
    }

    private class TreeContentProvider implements ITreeContentProvider {

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }

        public boolean hasChildren(Object element) {

            if (element instanceof AbstractListItem) {
                return ((AbstractListItem)element).hasChildren();
            }

            return false;
        }

        public Object getParent(Object element) {
            return null;
        }

        public Object[] getElements(Object inputElement) {
            return ArrayContentProvider.getInstance().getElements(inputElement);
        }

        public AbstractListItem[] getChildren(Object element) {

            if (element instanceof AbstractListItem) {
                AbstractListItem abstractListItem = (AbstractListItem)element;
                return abstractListItem.getChildren();
            }

            return null;
        }
    }

    private class TreeSelectionChangedListener implements ISelectionChangedListener {
        public void selectionChanged(SelectionChangedEvent event) {
            ISelection selection = event.getSelection();
            if (selection instanceof ITreeSelection) {
                ITreeSelection treeSelection = (ITreeSelection)selection;
                showSelectedDirectory(treeSelection.getFirstElement());
            }
        }
    }

    private class TreeDoubleClickListener implements IDoubleClickListener {
        public void doubleClick(DoubleClickEvent event) {
            ISelection selection = event.getSelection();
            if (selection instanceof ITreeSelection) {
                ITreeSelection treeSelection = (ITreeSelection)selection;
                Object firstElement = treeSelection.getFirstElement();
                TreeViewer treeViewer = (TreeViewer)event.getViewer();
                treeViewer.setExpandedState(firstElement, !treeViewer.getExpandedState(firstElement));
            }
        }
    }
}
