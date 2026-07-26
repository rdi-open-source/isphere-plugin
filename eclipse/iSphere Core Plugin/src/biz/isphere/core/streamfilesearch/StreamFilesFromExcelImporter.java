/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Team
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.streamfilesearch;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
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
import jxl.DateCell;
import jxl.NumberCell;
import jxl.Sheet;
import jxl.Workbook;

public class StreamFilesFromExcelImporter {

    private static final int COLUMN_DIRECTORY = 0;
    private static final int COLUMN_STREAM_FILE = 1;
    private static final int COLUMN_SOURCE_TYPE = 2;
    private static final int COLUMN_LAST_CHANGED = 3;
    private static final int COLUMN_STMTS_COUNT = 4;
    private static final int COLUMN_LINE = 5;
    private static final int COLUMN_STATEMENT = 6;

    private static final int EXPECTED_COLUMNS = 7;

    private Shell shell;

    public StreamFilesFromExcelImporter(Shell shell) {
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
        dialog.setFilterPath(Preferences.getInstance().getStreamFileSearchExportDirectory());

        String selected = dialog.open();
        if (selected == null) {
            return null;
        }

        Preferences.getInstance().setStreamFileSearchExportDirectory(dialog.getFilterPath());

        String connectionName = askForConnection();
        if (connectionName == null) {
            return null;
        }

        return importTab(new File(selected), connectionName);
    }

    /**
     * Reads the given workbook file and returns the reconstructed tab, tagged
     * with the given RSE connection name. On failure a non-blocking error
     * dialog is shown and <code>null</code> is returned. The sheet "Streams
     * files" is skipped (subset of "Stream files with statements"); the sheet
     * "Search arguments" is ignored by design (no round-trip serialization).
     */
    public SearchResultTab importTab(File file, String connectionName) {

        Workbook workbook = null;
        try {

            workbook = Workbook.getWorkbook(file);

            Sheet sheet = findStreamFilesWithStatementsSheet(workbook);
            if (sheet == null) {
                MessageDialogAsync.displayNonBlockingError(shell,
                    "Workbook does not contain a '" + Messages.Stream_files_with_statements + "' sheet.");
                return null;
            }

            SearchResult[] results = readSearchResults(sheet);

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
     * Locates the "Stream files with statements" sheet. Preferred lookup is by
     * the current-locale sheet name; falls back to structural detection so that
     * a workbook exported from a different locale is still importable. The
     * "Stream files" sheet is explicitly rejected.
     */
    private Sheet findStreamFilesWithStatementsSheet(Workbook workbook) {

        Sheet sheet = workbook.getSheet(Messages.Stream_files_with_statements);
        if (sheet != null) {
            return sheet;
        }

        String streamFilesSheetName = Messages.Stream_files;

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet candidate = workbook.getSheet(i);
            if (candidate == null) {
                continue;
            }
            if (streamFilesSheetName.equals(candidate.getName())) {
                continue;
            }
            if (hasStreamFilesWithStatementsShape(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Structural fingerprint of the "Stream files with statements" sheet: at
     * least 7 columns, one label header row, and at least one data row whose
     * columns 4 and 5 are numeric while column 6 is a label. This distinguishes
     * it from the narrower "Stream files" sheet and from "Search arguments".
     */
    private boolean hasStreamFilesWithStatementsShape(Sheet sheet) {

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

        Cell stmtsCount = sheet.getCell(COLUMN_STMTS_COUNT, 1);
        Cell line = sheet.getCell(COLUMN_LINE, 1);
        Cell statement = sheet.getCell(COLUMN_STATEMENT, 1);

        return stmtsCount.getType() == CellType.NUMBER && line.getType() == CellType.NUMBER && statement.getType() == CellType.LABEL;
    }

    private SearchResult[] readSearchResults(Sheet sheet) {

        List<SearchResult> results = new ArrayList<SearchResult>();

        SearchResult current = null;
        List<SearchResultStatement> currentStatements = null;

        int rows = sheet.getRows();
        for (int row = 1; row < rows; row++) {

            String directory = getStringCell(sheet, COLUMN_DIRECTORY, row);
            if (directory.length() == 0) {
                current = finishGroup(results, current, currentStatements);
                currentStatements = null;
                continue;
            }

            String streamFileName = getStringCell(sheet, COLUMN_STREAM_FILE, row);

            if (current == null || !isStreamFile(current, directory, streamFileName)) {
                current = finishGroup(results, current, currentStatements);

                current = new SearchResult();
                current.setDirectory(directory);
                current.setStreamFile(streamFileName);
                current.setType(getStringCell(sheet, COLUMN_SOURCE_TYPE, row));
                current.setLastChangedDate(getTimestampCell(sheet, COLUMN_LAST_CHANGED, row));
                currentStatements = new ArrayList<SearchResultStatement>();
            }

            SearchResultStatement stmt = new SearchResultStatement();
            stmt.setStatement((int)getNumberCell(sheet, COLUMN_LINE, row));
            stmt.setLine(getStringCell(sheet, COLUMN_STATEMENT, row));
            currentStatements.add(stmt);
        }

        finishGroup(results, current, currentStatements);

        return results.toArray(new SearchResult[results.size()]);
    }

    private SearchResult finishGroup(List<SearchResult> results, SearchResult current, List<SearchResultStatement> statements) {

        if (current == null) {
            return null;
        }
        if (statements == null) {
            statements = new ArrayList<SearchResultStatement>();
        }
        current.setStatements(statements.toArray(new SearchResultStatement[statements.size()]));
        results.add(current);
        return null;
    }

    private boolean isStreamFile(SearchResult r, String directory, String streamFile) {
        return directory.equals(r.getDirectory()) && streamFile.equals(r.getStreamFile());
    }

    private String getStringCell(Sheet sheet, int col, int row) {
        Cell cell = sheet.getCell(col, row);
        if (cell.getType() == CellType.EMPTY) {
            return ""; //$NON-NLS-1$
        }
        String contents = cell.getContents();
        return contents == null ? "" : contents; //$NON-NLS-1$
    }

    private double getNumberCell(Sheet sheet, int col, int row) {
        Cell cell = sheet.getCell(col, row);
        if (cell instanceof NumberCell) {
            return ((NumberCell)cell).getValue();
        }
        return 0;
    }

    private Timestamp getTimestampCell(Sheet sheet, int col, int row) {
        Cell cell = sheet.getCell(col, row);
        if (cell instanceof DateCell) {
            Date date = ((DateCell)cell).getDate();
            if (date != null) {
                return new Timestamp(date.getTime());
            }
        }
        return null;
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
