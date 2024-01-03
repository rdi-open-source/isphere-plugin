/*******************************************************************************
 * Copyright (c) 2012-2021 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.journalexplorer.core.ui.popupmenus;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.wb.swt.ResourceManager;

import biz.isphere.journalexplorer.core.model.JournalEntry;
import biz.isphere.journalexplorer.core.model.adapters.JournalProperties;
import biz.isphere.journalexplorer.core.ui.actions.CompareJournalPropertiesAction;
import biz.isphere.journalexplorer.core.ui.actions.CompareSideBySideAction;
import biz.isphere.journalexplorer.core.ui.widgets.actions.CopyJournalPropertyToClipboardAction;
import biz.isphere.journalexplorer.core.ui.widgets.actions.ToggleTrimPropertyValuesAction;

public class JournalPropertiesMenuAdapter extends MenuAdapter {

    private TreeViewer treeViewer;
    private Menu menuTableMembers;
    private Shell shell;
    private MenuItem compareJournalPropertiesMenuItem;
    private MenuItem compareSideBySideMenuItem;
    private MenuItem separator;
    private MenuItem copyAllToClipboardMenuItem;
    private MenuItem copyValuesToClipboardMenuItem;
    private MenuItem toggleTrimPropertyValuesMenuItem;

    public JournalPropertiesMenuAdapter(Menu menuTableMembers, TreeViewer treeViewer) {
        this.treeViewer = treeViewer;
        this.shell = treeViewer.getControl().getShell();
        this.menuTableMembers = menuTableMembers;
    }

    @Override
    public void menuShown(MenuEvent event) {
        destroyMenuItems();
        createMenuItems();
    }

    public void destroyMenuItems() {
        dispose(compareJournalPropertiesMenuItem);
        dispose(compareSideBySideMenuItem);
        dispose(separator);
        dispose(copyAllToClipboardMenuItem);
        dispose(copyValuesToClipboardMenuItem);
        dispose(toggleTrimPropertyValuesMenuItem);
    }

    private int selectedItemsCount() {
        return getSelectedItems().size();
    }

    private int selectedJournalEntriesCount() {
        return getSelectedJournalEntries().size();
    }

    private StructuredSelection getSelectedJournalProperties() {

        List<JournalProperties> journalProperties = new LinkedList<JournalProperties>();
        if (treeViewer != null && (treeViewer.getControl() instanceof Tree)) {
            Tree tree = (Tree)treeViewer.getControl();
            for (TreeItem treeItem : tree.getSelection()) {
                Object data = treeItem.getData();
                if (data instanceof JournalProperties) {
                    journalProperties.add((JournalProperties)data);
                }
            }
        }

        return new StructuredSelection(journalProperties.toArray(new JournalProperties[journalProperties.size()]));
    }

    private StructuredSelection getSelectedItems() {

        if (treeViewer != null && (treeViewer.getControl() instanceof Tree)) {
            Tree tree = (Tree)treeViewer.getControl();
            List<Object> items = new LinkedList<Object>();
            for (TreeItem treeItem : tree.getSelection()) {
                Object journalProperties = treeItem.getData();
                items.add(journalProperties);
            }

            return new StructuredSelection(items.toArray(new Object[items.size()]));
        }

        return new StructuredSelection(new Object[0]);
    }

    private StructuredSelection getSelectedJournalEntries() {

        if (treeViewer != null && (treeViewer.getControl() instanceof Tree)) {
            Tree tree = (Tree)treeViewer.getControl();
            List<JournalEntry> journalEntries = new LinkedList<JournalEntry>();
            for (TreeItem treeItem : tree.getSelection()) {
                Object data = treeItem.getData();
                if (data instanceof JournalProperties) {
                    JournalProperties journalProperties = (JournalProperties)treeItem.getData();
                    journalEntries.add(journalProperties.getJournalEntry());
                }
            }

            return new StructuredSelection(journalEntries.toArray(new JournalEntry[journalEntries.size()]));
        }

        return new StructuredSelection(new Object[0]);
    }

    private void dispose(MenuItem menuItem) {

        if (!((menuItem == null) || (menuItem.isDisposed()))) {
            menuItem.dispose();
        }
    }

    public void createMenuItems() {

        if (selectedItemsCount() > 0) {

            copyAllToClipboardMenuItem = new MenuItem(menuTableMembers, SWT.NONE);
            final CopyJournalPropertyToClipboardAction copyAllToClipboardAction = new CopyJournalPropertyToClipboardAction(true);
            copyAllToClipboardMenuItem.setText(copyAllToClipboardAction.getText());
            copyAllToClipboardMenuItem.setImage(ResourceManager.getImage(copyAllToClipboardAction.getImageDescriptor()));
            copyAllToClipboardMenuItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    copyAllToClipboardAction.setSelectedItems(getSelectedItems());
                    copyAllToClipboardAction.run();
                }
            });

            copyValuesToClipboardMenuItem = new MenuItem(menuTableMembers, SWT.NONE);
            final CopyJournalPropertyToClipboardAction copyValueToClipboardAction = new CopyJournalPropertyToClipboardAction(false);
            copyValuesToClipboardMenuItem.setText(copyValueToClipboardAction.getText());
            copyValuesToClipboardMenuItem.setImage(ResourceManager.getImage(copyValueToClipboardAction.getImageDescriptor()));
            copyValuesToClipboardMenuItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    copyValueToClipboardAction.setSelectedItems(getSelectedItems());
                    copyValueToClipboardAction.run();
                }
            });

            toggleTrimPropertyValuesMenuItem = new MenuItem(menuTableMembers, SWT.CHECK);
            final ToggleTrimPropertyValuesAction toggleTrimPropertyValuesAction = new ToggleTrimPropertyValuesAction();
            toggleTrimPropertyValuesMenuItem.setText(toggleTrimPropertyValuesAction.getText());
            toggleTrimPropertyValuesMenuItem.setImage(ResourceManager.getImage(toggleTrimPropertyValuesAction.getImageDescriptor()));
            toggleTrimPropertyValuesMenuItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    toggleTrimPropertyValuesAction.setChecked(!toggleTrimPropertyValuesAction.isChecked());
                    toggleTrimPropertyValuesAction.run();
                }
            });

            if (selectedJournalEntriesCount() == 2) {
                separator = new MenuItem(menuTableMembers, SWT.SEPARATOR);

                compareJournalPropertiesMenuItem = new MenuItem(menuTableMembers, SWT.NONE);
                final CompareJournalPropertiesAction compareJournalPropertiesAction = new CompareJournalPropertiesAction(treeViewer);
                compareJournalPropertiesMenuItem.setText(compareJournalPropertiesAction.getText());
                compareJournalPropertiesMenuItem.setImage(compareJournalPropertiesAction.getImage());
                compareJournalPropertiesMenuItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        compareJournalPropertiesAction.setSelectedItems(getSelectedJournalProperties());
                        compareJournalPropertiesAction.run();
                    }
                });

                compareSideBySideMenuItem = new MenuItem(menuTableMembers, SWT.NONE);
                final CompareSideBySideAction compareSideBySideAction = new CompareSideBySideAction(shell);
                compareSideBySideMenuItem.setText(compareSideBySideAction.getText());
                compareSideBySideMenuItem.setImage(compareSideBySideAction.getImage());
                compareSideBySideMenuItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        compareSideBySideAction.setSelectedItems(getSelectedJournalEntries());
                        compareSideBySideAction.run();
                    }
                });
            }
        }
    }
}
