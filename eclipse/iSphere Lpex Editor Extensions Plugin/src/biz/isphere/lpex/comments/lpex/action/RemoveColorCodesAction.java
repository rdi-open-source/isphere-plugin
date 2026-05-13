package biz.isphere.lpex.comments.lpex.action;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;

import com.ibm.lpex.core.LpexView;

import biz.isphere.base.internal.ExceptionHelper;
import biz.isphere.base.internal.StringHelper;
import biz.isphere.ide.lpex.helper.LpexHelper;
import biz.isphere.lpex.comments.ISphereLpexEditorExtensionsPlugin;
import biz.isphere.lpex.comments.Messages;
import biz.isphere.lpex.comments.lpex.internal.Position;

public class RemoveColorCodesAction extends AbstractLpexAction {

    public static final String ID = "iSphere.Lpex.RemoveColorCodes";

    /**
     * Identifies the line with the color codes from x'20' to x'3F' for
     * debugging purposes. The line with color codes in the source member looks
     * like that:
     * <p>
     * 
     * <pre>
     * ..... *. 1 ...+... 2 ...+... 3 ...+... 4 ...+... 5 ...+... 6 ...+... 7 ...+... 8 ...+... 9 ...+... 0
     *       *  Alle Farbattribute: X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X X
     * </pre>
     */
    private static final String COLOR_CODES_LINE_INDICATOR = "Alle Farbattribute:";

    /**
     * Color codes (x'20' to x'3F') as they are translated from EBDIC to UTF-8.
     * Some EBCDIC codes are translated to the same value in UTF-8. Duplicates
     * are resolved by variable 'colorCodesSet'.
     */
    private static final int X20 = 128;
    private static final int X21 = 129;
    private static final int X22 = 130;
    private static final int X23 = 131;
    private static final int X24 = 132;
    private static final int X25 = 9226;
    private static final int X26 = 23;
    private static final int X27 = 27;
    private static final int X28 = 136;
    private static final int X29 = 137;
    private static final int X2A = 129;
    private static final int X2B = 139;
    private static final int X2C = 140;
    private static final int X2D = 5;
    private static final int X2E = 6;
    private static final int X2F = 7;

    private static final int X30 = 144;
    private static final int X31 = 145;
    private static final int X32 = 22;
    private static final int X33 = 147;
    private static final int X34 = 148;
    private static final int X35 = 149;
    private static final int X36 = 150;
    private static final int X37 = 4;
    private static final int X38 = 152;
    private static final int X39 = 153;
    private static final int X3A = 154;
    private static final int X3B = 155;
    private static final int X3C = 20;
    private static final int X3D = 21;
    private static final int X3E = 158;
    private static final int X3F = 26;

    private static final int[] COLOR_CODES_LIST = { X20, X21, X22, X23, X24, X25, X26, X27, X28, X29, X2A, X2B, X2C, X2D, X2E, X2F, X30, X31, X32,
        X33, X34, X35, X36, X37, X38, X39, X3A, X3B, X3C, X3D, X3E, X3F };

    // EBCDIC color codes
    // Hex, Dec
    // x20, 32 = green
    // x21, 33 = green/inverse
    // x22, 34 = white
    // x23, 35 = white/inverse
    // x24, 36 = green/underline
    // x25, 37 = green/inverse/underline
    // x26, 38 = white/underline
    // x27, 39 = non-display 1
    // x28, 40 = red
    // x29, 41 = red/underline
    // x2A, 42 = red/blink
    // x2B, 43 = red/inverse/blink
    // x2C, 44 = red/underline
    // x2D, 45 = red/inverse/underline
    // x2E, 46 = red/blink/underline
    // x2F, 47 = non-display 2
    // x30, 48 = turquoise
    // x31, 49 = turquoise/inverse/colum separator
    // x32, 50 = yellow
    // x33, 51 = yellow/inverse/colum separator
    // x34, 52 = turquoise/underline
    // x35, 53 = turquoise/inverse/underline
    // x36, 54 = yellow/underline
    // x37, 54 = non-display 3
    // x38, 56 = pink
    // x39, 57 = pink/inverse
    // x3A, 58 = blue
    // x3B, 59 = blue/inverse
    // x3C, 60 = pink/underline
    // x3D, 61 = pink/inverse/underline
    // x3E, 62 = blue/underline
    // x3F, 63 = non-display 4

    private static final String SPACE = " ";

    private int countReplaced;
    private Set<Integer> colorCodesSet;

    public RemoveColorCodesAction() {

        colorCodesSet = new HashSet<Integer>();
        for (int i : COLOR_CODES_LIST) {
            colorCodesSet.add(i);
        }
    }

    public static String getLPEXMenuAction() {
        return getLPEXMenuAction(Messages.Menue_RemoveColorCodes, RemoveColorCodesAction.ID);
    }

    /**
     * Executor method of the action. This method is executed, when the user
     * clicks the menu option "Remove Color Codes".
     */
    public void doAction(LpexView view) {

        try {
            saveCursorPosition(view);

            Position start;
            Position end;
            if (anythingSelected(view)) {
                start = new Position(getBlockTopElement(view), getBlockTopPosition(view));
                end = new Position(getBlockBottomElement(view), getBLockBottomPosition(view));
                if (end.getColumn() == 1) {
                    end.setLine(end.getLine() - 1);
                }
            } else {
                start = new Position(1, -1);
                end = new Position(LpexHelper.getLinesCount(view), -1);
            }

            // Range of lines
            if (start.getLine() < end.getLine()) {
                doLines(view, start.getLine(), end.getLine());
            } else if (start.getLine() == end.getLine()) {
                // Single line
                if (start.getColumn() == end.getColumn()) {
                    doLines(view, start.getLine(), end.getLine());
                } else if (start.getColumn() < end.getColumn()) {
                    // Selection
                    doSelection(view, start.getLine(), start.getColumn(), end.getColumn());
                }
            }

        } finally {
            restoreCursorPosition(view);
        }
    }

    @Override
    protected void doLines(LpexView view, int firstLine, int lastLine) {

        if (view != null) {
            try {
                doRemoveColorCodes(view, firstLine, lastLine);
            } catch (Exception e) {
                String localizedMessage = ExceptionHelper.getLocalizedMessage(e);
                LpexHelper.displayMessage(view, localizedMessage);
                MessageDialog.openError(LpexHelper.getShell(view), Messages.E_R_R_O_R, localizedMessage);
            }
        } else {
            String tErrorMsg = "*** Lpex view is null. Can not remove color codes from file: " + LpexHelper.getFileName(view) + " ***";
            ISphereLpexEditorExtensionsPlugin.logError(tErrorMsg, null);
        }
    }

    @Override
    protected void doSelection(LpexView view, int line, int startColumn, int endColumn) {
        doLines(view, line, startColumn);
    }

    /**
     * Indicates whether or not the action is available (= active). The action
     * considered to be available, if the view is not in read-only mode.
     * 
     * @param view - View the action is executed for.
     */
    public boolean available(LpexView view) {
        return !LpexHelper.isReadOnly(view);
    }

    /**
     * Internal executor of the action.
     * 
     * @param view - View the action is executed for.
     */
    private void doRemoveColorCodes(LpexView view, int firstLine, int lastLine) {

        countReplaced = 0;

        for (int tCurrentLineNumber = firstLine; tCurrentLineNumber <= lastLine; tCurrentLineNumber++) {
            if (LpexHelper.isSourceLine(view, tCurrentLineNumber)) {
                removeColorCodes(view, tCurrentLineNumber);
            }
        }

        LpexHelper.displayMessage(view, Messages.bind(Messages.Message_Replaced_A_color_codes, countReplaced));
    }

    /**
     * Removes the color codes of a given source line.
     * 
     * @param view - View the action is executed for.
     * @param aLineNumber - Line number of the source from which the color codes
     *        are removed.
     * @return
     */
    private boolean removeColorCodes(LpexView view, int aLineNumber) {

        String tSourceLineText = LpexHelper.getSourceLineText(view, aLineNumber);
        if (StringHelper.isNullOrEmpty(tSourceLineText) || tSourceLineText.trim().length() == 0) {
            return false;
        }

        boolean isChanged = false;
        StringBuilder tLine = null;

        for (int charOffset = 0; charOffset < tSourceLineText.length(); charOffset++) {
            if (isColorCode(tSourceLineText, aLineNumber, charOffset)) {
                if (tLine == null) {
                    tLine = new StringBuilder(tSourceLineText);
                }
                tLine.replace(charOffset, charOffset + 1, SPACE);
                isChanged = true;
                countReplaced++;
            }
        }

        if (isChanged) {
            LpexHelper.setSourceLineText(view, aLineNumber, tLine.toString());
        }

        return isChanged;
    }

    /**
     * Returns <code>true</code>, if the specified byte value is a color code,
     * otherwise returns <code>false</code>.
     * 
     * @param aSourceLineText - Source line text from which the color codes are
     *        removed.
     * @param charOffset - Byte offset of the current byte.
     * @return <code>true</code>, if the byte is a color code, otherwise
     *         <code>false</code>.
     */
    private boolean isColorCode(String aSourceLineText, int aLineNumber, int charOffset) {

        int charAt = aSourceLineText.charAt(charOffset);
        int type = Character.getType(aSourceLineText.charAt(charOffset));

        if (colorCodesSet.contains(charAt)) {
            // debugLogIdentifiedColorCode(aSourceLineText, aLineNumber,
            // charOffset + 1, charAt, type);
            return true;
        } else {
            debugLogUnidentifiedColorCode(aSourceLineText, aLineNumber, charOffset + 1, charAt, type);
        }

        return false;
    }

    private boolean isLineWithColorCodes(String aSourceLineText) {
        return aSourceLineText.contains(COLOR_CODES_LINE_INDICATOR);
    }

    private boolean mustLogColorCode(String aSourceLineText, int aCharOffset) {

        int colorCodesOffset = aSourceLineText.indexOf(COLOR_CODES_LINE_INDICATOR) + COLOR_CODES_LINE_INDICATOR.length();
        if (aCharOffset > colorCodesOffset) {
            return true;
        }

        return false;
    }

    private void debugLogIdentifiedColorCode(String aSourceLineText, int aLineNumber, int aCharPos, int aCharAt, int aCharType) {

        if (isLineWithColorCodes(aSourceLineText)) {
            if (mustLogColorCode(aSourceLineText, aCharPos)) {
                System.out
                    .println("Identified color code: " + aCharAt + " (Line: " + aLineNumber + ", Index: " + aCharPos + ", Type: " + aCharType + ")");
            }
        }
    }

    private void debugLogUnidentifiedColorCode(String aSourceLineText, int aLineNumber, int aCharPos, int aCharAt, int aCharType) {

        if (isLineWithColorCodes(aSourceLineText)) {
            if (mustLogColorCode(aSourceLineText, aCharPos)) {
                if (aCharType != Character.SPACE_SEPARATOR) {
                    String message = "*** Unidentified color code: " + aCharAt + " (Line: " + aLineNumber + ", Index: " + aCharPos + ", Type: "
                        + aCharType + ") ***";
                    System.out.println(message);
                    ISphereLpexEditorExtensionsPlugin.logError(message, null);
                }
            }
        }
    }
}