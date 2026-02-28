/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.helper;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.UIJob;

import com.ibm.lpex.core.LpexView;
import com.ibm.lpex.core.LpexWindow;

public final class LpexHelper {

    private LpexHelper() {
    }

    /**
     * Returns the current Lpex user actions.
     * 
     * @return Current Lpex user actions.
     */
    public static String getUserActions() {

        String lpexUserActions = LpexView.globalQuery("current.updateProfile.userActions");
        if (lpexUserActions == null) {
            lpexUserActions = "";
        }

        return lpexUserActions;
    }

    /**
     * Returns the current popup menu of the Lpex view.
     * 
     * @return Current Lpex popup menu.
     */
    public static String getPopMenu() {

        String lpexPopupMenu = LpexView.globalQuery("current.popup");
        if (lpexPopupMenu == null) {
            lpexPopupMenu = "";
        }

        return lpexPopupMenu;
    }

    /**
     * Sets the Lpex user actions.
     * 
     * @param userActions - String defining the Lpex user actions.
     */
    public static void setUserActions(String userActions) {
        LpexView.doGlobalCommand("set default.updateProfile.userActions " + userActions);
    }

    /**
     * Sets the popup menu of the Lpex view.
     * 
     * @param popupMenu - String defining the popup menu of the Lpex view.
     */
    public static void setPopupMenu(String popupMenu) {
        LpexView.doGlobalCommand("set default.popup " + popupMenu);
    }

    /**
     * Returns the file name that is loaded into the view.
     * 
     * @param aLpexView - LpexView that contains the source code.
     * @return Name of the file loaded into the view.
     */
    public static String getFileName(LpexView aLpexView) {

        String fileName = aLpexView.query("name");

        return fileName;
    }

    /**
     * Returns the current mode of the view.
     * 
     * @param aLpexView - LpexView that contains the source code.
     * @return <code>true</code> if the view is in read-only mode, otherwise
     *         <code>false</code>.
     */
    public static boolean isReadOnly(LpexView aLpexView) {

        boolean isReadOnly = aLpexView.queryOn("readonly");

        return isReadOnly;
    }

    /**
     * Displays a message at the bottom of the LpexView. The message must be
     * placed in a UI job, because otherwise it is not displayed, if the below
     * conditions are met:
     * <ul>
     * <li>The member has been loaded into the editor.</li>
     * <li>Option "Remove Color Codes" is executed without a left-click in the
     * editor before.</li>
     * </ul>
     * In this case message <i>(Main Procedure)</i> is not displayed and the
     * status line is empty. The procedure name is displayed on the first
     * left-click in the editor.
     * <p>
     * 
     * @param aLpexView - LpexView that contains the source code.
     * @param message - Message that is displayed.
     */
    public static void displayMessage(final LpexView aLpexView, final String message) {

        UIJob job = new UIJob("") {
            @Override
            public IStatus runInUIThread(IProgressMonitor arg0) {
                aLpexView.doCommand("set messageText " + message);
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    /**
     * Returns the current cursor position within the source line.
     * 
     * @param aLpexView - LpexView that contains the source code.
     * @return Cursor column.
     */
    public static int getCursorPosition(LpexView aLpexView) {

        int position = aLpexView.queryInt("displayPosition");

        return position;
    }

    /**
     * Returns the line number the current cursor position.
     * 
     * @param aLpexView - LpexView that contains the source code.
     * @return Cursor row.
     */
    public static int getCursorLineNumber(LpexView aLpexView) {

        int lineNumber = aLpexView.currentElement() + 1;

        return lineNumber;
    }

    /**
     * Returns the total number of source lines.
     * 
     * @param aLpexView - LpexView that contains the source code.
     * @return total number of source lines.
     */
    public static int getLinesCount(LpexView aLpexView) {

        int numLines = aLpexView.elements();

        return numLines;
    }

    /**
     * Positions the cursor on a given source line and position.
     * 
     * @param aLpexView - LpexView that contains the source code.
     * @param lineIndex - Number of the source line the cursor is positioned to.
     * @param linePosition - Position within the source line the cursor is
     *        placed on.
     */
    public static void setCursorPosition(LpexView aLpexView, int lineIndex, int linePosition) {
        aLpexView.jump(lineIndex, linePosition);
    }

    /**
     * Returns the source line text of the source line identified by the
     * specified line number.
     * 
     * @param aLpexView - LpexView that contains the source code.
     * @param aLineNumber - Number of the source line whose text is returned..
     * @return Source line text.
     */
    public static String getSourceLineText(LpexView aLpexView, int aLineNumber) {

        String text = aLpexView.elementText(aLineNumber);

        return text;
    }

    /**
     * Sets the source line text of the source line identified by the specified
     * line number.
     * 
     * @param aLpexView - LpexView that contains the source code.
     * @param aLineNumber - Number of the source line that is changed.
     * @param aText - Source line text.
     */
    public static void setSourceLineText(LpexView aLpexView, int aLineNumber, String aText) {

        aLpexView.setElementText(aLineNumber, aText);
    }

    /**
     * Returns <code>true</code> if the source line identified by the specified
     * line number is a source line and not a compiler message.
     * 
     * @param aLpexView - LpexView that contains the source code.
     * @param aLineNumber - Number of the source line that is checked.
     * @return <code>true</code> for a source line, otherwise
     *         <code>false</code>.
     */
    public static boolean isSourceLine(LpexView aLpexView, int aLineNumber) {

        boolean isSourceLine = !aLpexView.show(aLineNumber);

        return isSourceLine;
    }

    /**
     * Returns the shell of a given Lpex view.
     * 
     * @param aLpexView - View whose shell is returned.
     * @return Shell of the view.
     */
    public static Shell getShell(LpexView aLpexView) {
        LpexWindow lpexWindow = aLpexView.window();

        if (lpexWindow != null && !lpexWindow.isDisposed()) {
            Shell shell = lpexWindow.getShell();
            return shell;
        }

        return null;
    }
}
