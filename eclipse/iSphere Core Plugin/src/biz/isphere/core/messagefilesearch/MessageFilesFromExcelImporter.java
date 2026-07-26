/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.messagefilesearch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import biz.isphere.base.internal.FileHelper;
import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.Messages;
import biz.isphere.core.ibmi.contributions.extension.handler.IBMiHostContributionsHandler;
import biz.isphere.core.internal.MessageDialogAsync;
import biz.isphere.core.preferences.Preferences;
import biz.isphere.core.swt.widgets.extension.handler.WidgetFactoryContributionsHandler;
import biz.isphere.core.swt.widgets.extension.point.IFileDialog;
import jxl.Cell;
import jxl.CellType;
import jxl.Sheet;
import jxl.Workbook;

public class MessageFilesFromExcelImporter {

    private static final int COLUMN_LIBRARY = 0;
    private static final int COLUMN_MESSAGE_FILE = 1;
    private static final int COLUMN_MESSAGE_FILE_DESCRIPTION = 2;
    private static final int COLUMN_MESSAGE_ID = 3;
    private static final int COLUMN_MESSAGE_TEXT = 4;

    private static final int EXPECTED_COLUMNS = 5;

    private Shell shell;

    public MessageFilesFromExcelImporter(Shell shell) {
        this.shell = shell;
    }

    /**
     * Opens a file dialog, then asks the user to select an RSE connection, and
     * finally imports the workbook. Returns <code>null</code> when the user
     * cancels any of the two dialogs or an error is displayed to the user.
     */
    public SearchResultTab importTab() {

        WidgetFactoryContributionsHandler factory = new WidgetFactoryContributionsHandler();
        IFileDialog dialog = factory.getFileDialog(shell, SWT.OPEN);

        dialog.setFilterNames(new String[] { "Excel Files", FileHelper.getAllFilesText() }); //$NON-NLS-1$
        dialog.setFilterExtensions(new String[] { "*.xls", FileHelper.getAllFilesFilter() }); //$NON-NLS-1$
        dialog.setFilterPath(Preferences.getInstance().getMessageFileSearchExportDirectory());

        String selected = dialog.open();
        if (selected == null) {
            return null;
        }

        Preferences.getInstance().setMessageFileSearchExportDirectory(dialog.getFilterPath());

        String connectionName = askForConnection();
        if (connectionName == null) {
            return null;
        }

        return importTab(new File(selected), connectionName);
    }

    /**
     * Reads the given workbook file and returns the reconstructed tab, tagged
     * with the given RSE connection name. On failure a non-blocking error
     * dialog is shown and <code>null</code> is returned. The sheet "Files" is
     * skipped (subset of "Files with IDs"); the sheet "Search arguments" is
     * ignored by design (no round-trip serialization).
     */
    public SearchResultTab importTab(File file, String connectionName) {

        Workbook workbook = null;
        try {

            workbook = Workbook.getWorkbook(file);

            Sheet sheet = findMessageFilesWithStatementsSheet(workbook);
            if (sheet == null) {
                MessageDialogAsync.displayNonBlockingError(shell, "Workbook does not contain a '" + Messages.Files_with_Id_s + "' sheet.");
                return null;
            }

            SearchResult[] results = readSearchResults(sheet, connectionName);

            String searchString = getFileNameWithoutExtension(file);

            return new SearchResultTab(connectionName, searchString, results, null);

        } catch (Exception e) {
            ISpherePlugin.logError(e.getMessage(), e);
            MessageDialogAsync.displayNonBlockingError(shell, e.getLocalizedMessage());
            return null;
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
    }

    /**
     * Prompts the user to pick an RSE connection via the connection-selection
     * dialog contributed by the RSE adapter plug-in. Returns the qualified
     * connection name or <code>null</code> when the user cancels or when no RSE
     * contribution is registered.
     */
    private String askForConnection() {

        if (!IBMiHostContributionsHandler.hasContribution()) {
            MessageDialogAsync.displayNonBlockingError(shell, Messages.No_RSE_contribution_available_for_connection_selection);
            return null;
        }

        return IBMiHostContributionsHandler.selectConnection(shell);
    }

    /**
     * Locates the "Files with IDs" sheet. Preferred lookup is by the
     * current-locale sheet name; falls back to structural detection so that a
     * workbook exported from a different locale is still importable. The
     * "Files" sheet is explicitly rejected.
     */
    private Sheet findMessageFilesWithStatementsSheet(Workbook workbook) {

        Sheet sheet = workbook.getSheet(Messages.Files_with_Id_s);
        if (sheet != null) {
            return sheet;
        }

        String filesSheetName = Messages.Files;

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet candidate = workbook.getSheet(i);
            if (candidate == null) {
                continue;
            }
            if (filesSheetName.equals(candidate.getName())) {
                continue;
            }
            if (hasMessageFilesWithIDsShape(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Structural fingerprint of the "Files with IDs" sheet: at least 5 columns,
     * one label header row, and at least one data row whose columns are a
     * label. This distinguishes it from the narrower "Files" sheet and from
     * "Search arguments".
     */
    private boolean hasMessageFilesWithIDsShape(Sheet sheet) {

        if (sheet.getColumns() < EXPECTED_COLUMNS) {
            return false;
        }
        if (sheet.getRows() < 2) {
            return false;
        }

        for (int col = 0; col < EXPECTED_COLUMNS; col++) {
            Cell headerCell = sheet.getCell(col, 0);
            if (headerCell.getType() != CellType.LABEL) {
                return false;
            }
        }

        Cell library = sheet.getCell(COLUMN_LIBRARY, 1);
        Cell messageFile = sheet.getCell(COLUMN_MESSAGE_FILE, 1);
        Cell messageFileDescription = sheet.getCell(COLUMN_MESSAGE_FILE_DESCRIPTION, 1);
        Cell messageID = sheet.getCell(COLUMN_MESSAGE_ID, 1);
        Cell messageText = sheet.getCell(COLUMN_MESSAGE_TEXT, 1);

        return library.getType() == CellType.LABEL && messageFile.getType() == CellType.LABEL && messageFileDescription.getType() == CellType.LABEL
            && messageID.getType() == CellType.LABEL && messageText.getType() == CellType.LABEL;
    }

    private SearchResult[] readSearchResults(Sheet sheet, String connectionName) {

        List<SearchResult> results = new ArrayList<SearchResult>();

        SearchResult current = null;
        List<SearchResultMessageId> currentMessageIDs = null;

        int rows = sheet.getRows();
        for (int row = 1; row < rows; row++) {

            String library = getStringCell(sheet, COLUMN_LIBRARY, row);
            if (library.length() == 0) {
                current = finishGroup(results, current, currentMessageIDs);
                currentMessageIDs = null;
                continue;
            }

            String fileName = getStringCell(sheet, COLUMN_MESSAGE_FILE, row);

            if (current == null || !isSameMessageFile(current, library, fileName)) {
                current = finishGroup(results, current, currentMessageIDs);

                current = new SearchResult();
                current.setConnectionName(connectionName);
                current.setLibrary(library);
                current.setMessageFile(fileName);
                current.setDescription(getStringCell(sheet, COLUMN_MESSAGE_FILE_DESCRIPTION, row));
                currentMessageIDs = new ArrayList<SearchResultMessageId>();
            }

            SearchResultMessageId message = new SearchResultMessageId();
            message.setMessageId(getStringCell(sheet, COLUMN_MESSAGE_ID, row));
            message.setMessage(getStringCell(sheet, COLUMN_MESSAGE_TEXT, row));
            currentMessageIDs.add(message);
        }

        finishGroup(results, current, currentMessageIDs);

        return results.toArray(new SearchResult[results.size()]);
    }

    private SearchResult finishGroup(List<SearchResult> results, SearchResult current, List<SearchResultMessageId> messageIDs) {

        if (current == null) {
            return null;
        }
        if (messageIDs == null) {
            messageIDs = new ArrayList<SearchResultMessageId>();
        }
        current.setMessageIds(messageIDs.toArray(new SearchResultMessageId[messageIDs.size()]));
        results.add(current);
        return null;
    }

    private boolean isSameMessageFile(SearchResult r, String library, String file) {
        return library.equals(r.getLibrary()) && file.equals(r.getMessageFile());
    }

    private String getStringCell(Sheet sheet, int col, int row) {
        Cell cell = sheet.getCell(col, row);
        if (cell.getType() == CellType.EMPTY) {
            return ""; //$NON-NLS-1$
        }
        String contents = cell.getContents();
        return contents == null ? "" : contents; //$NON-NLS-1$
    }

    private String getFileNameWithoutExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            return name.substring(0, dot);
        }
        return name;
    }
}
