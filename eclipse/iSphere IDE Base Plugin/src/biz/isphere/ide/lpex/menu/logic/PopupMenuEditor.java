/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.logic;

import java.util.List;

import biz.isphere.ide.lpex.menu.model.MenuMarkers;

/**
 * Creates, inserts and removes the block of menu items of a menu extension in
 * the text of an LPEX popup menu. The block is enclosed by two markers. They
 * make inserting repeatable: a popup menu that already contains the start
 * marker is not changed.
 */
public class PopupMenuEditor {

    public static final int TOP = 1;
    public static final int BOTTOM = 2;

    private static final String BEGIN_SUB_MENU = "beginSubmenu"; //$NON-NLS-1$
    private static final String END_SUB_MENU = "endSubmenu"; //$NON-NLS-1$
    private static final String SEPARATOR = "separator"; //$NON-NLS-1$
    private static final String DOUBLE_QUOTES = "\""; //$NON-NLS-1$
    private static final String DELIMITER = " "; //$NON-NLS-1$

    private final MenuMarkers markers;

    public PopupMenuEditor(MenuMarkers markers) {
        this.markers = markers;
    }

    /**
     * Returns <code>true</code>, if the popup menu contains the start marker of
     * the block.
     */
    public boolean containsBlock(String popupMenu) {

        if (popupMenu == null) {
            return false;
        }

        return indexOfToken(popupMenu, markers.getStart().trim(), 0) >= 0;
    }

    /**
     * Creates the block of menu items.
     *
     * @param menuName - name of the sub-menu, used if the block is not inserted
     *        into an existing menu
     * @param menuActions - menu items, <code>null</code> stands for a separator
     * @param intoExistingMenu - <code>true</code>, if the block is inserted
     *        into an existing menu, e.g. the source menu
     * @return block of menu items, including the markers
     */
    public String createBlock(String menuName, List<String> menuActions, boolean intoExistingMenu) {

        StringBuilder block = new StringBuilder();
        append(block, markers.getStart());

        if (intoExistingMenu) {
            append(block, SEPARATOR);
            appendMenuItems(block, menuActions);
        } else {
            append(block, BEGIN_SUB_MENU);
            append(block, DOUBLE_QUOTES + menuName + DOUBLE_QUOTES);
            appendMenuItems(block, menuActions);
            append(block, END_SUB_MENU);
            append(block, SEPARATOR);
        }

        append(block, markers.getEnd());

        return block.toString();
    }

    /**
     * Inserts the block into the popup menu.
     *
     * @param popupMenu - popup menu, may be <code>null</code>
     * @param block - block of menu items, see
     *        {@link #createBlock(String, List, boolean)}
     * @param location - index in the popup menu where the block is inserted, or
     *        a negative value
     * @param position - {@link #TOP} or {@link #BOTTOM}, used if the location
     *        is negative
     * @return popup menu with the block, unchanged if it already contains the
     *         block
     */
    public String insertBlock(String popupMenu, String block, int location, int position) {

        if (popupMenu == null || popupMenu.trim().length() == 0) {
            return block;
        }

        if (containsBlock(popupMenu)) {
            return popupMenu;
        }

        if (location >= 0) {
            int index = Math.min(location, popupMenu.length());
            StringBuilder newPopupMenu = new StringBuilder(popupMenu);
            newPopupMenu.insert(index, DELIMITER + block);
            return newPopupMenu.toString();
        }

        if (position == TOP) {
            return block + DELIMITER + popupMenu;
        }

        return popupMenu + DELIMITER + block;
    }

    /**
     * Removes all blocks that are enclosed by the specified markers. A block
     * without end marker is left unchanged and reported. A marker may consist
     * of several tokens, e.g. <code>endSubmenu separator</code>. White space
     * around a marker is ignored.
     *
     * @param popupMenu - popup menu
     * @param markers - markers of the blocks
     * @param warnings - receives a message for each block without end marker
     * @return popup menu without the blocks
     */
    public static String removeBlocks(String popupMenu, MenuMarkers markers, List<String> warnings) {

        if (popupMenu == null) {
            return null;
        }

        String startMarker = markers.getStart().trim();
        String endMarker = markers.getEnd().trim();

        String result = popupMenu;
        int from = 0;

        while (true) {

            int start = indexOfToken(result, startMarker, from);
            if (start < 0) {
                break;
            }

            int end = indexOfToken(result, endMarker, start + startMarker.length());
            if (end < 0) {
                warnings.add("End marker of menu block not found: " + endMarker); //$NON-NLS-1$
                break;
            }

            String before = trimRight(result.substring(0, start));
            String after = trimLeft(result.substring(end + endMarker.length()));

            if (before.length() > 0 && after.length() > 0) {
                result = before + DELIMITER + after;
            } else {
                result = before + after;
            }

            from = before.length();
        }

        return result;
    }

    /**
     * Returns <code>true</code>, if both menus are equal, apart from white
     * space between the tokens.
     */
    public static boolean isSameMenu(String menu1, String menu2) {

        if (menu1 == null || menu2 == null) {
            return menu1 == menu2;
        }

        return normalize(menu1).equals(normalize(menu2));
    }

    private void appendMenuItems(StringBuilder block, List<String> menuActions) {

        if (menuActions == null) {
            return;
        }

        for (String action : menuActions) {
            if (action == null) {
                append(block, SEPARATOR);
            } else {
                append(block, action);
            }
        }
    }

    private void append(StringBuilder buffer, String token) {

        if (buffer.length() > 0) {
            buffer.append(DELIMITER);
        }

        buffer.append(token);
    }

    private static int indexOfToken(String text, String token, int from) {

        int index = text.indexOf(token, from);
        while (index >= 0) {

            int end = index + token.length();
            boolean startsToken = index == 0 || Character.isWhitespace(text.charAt(index - 1));
            boolean endsToken = end == text.length() || Character.isWhitespace(text.charAt(end));
            if (startsToken && endsToken) {
                return index;
            }

            index = text.indexOf(token, index + 1);
        }

        return -1;
    }

    private static String normalize(String menu) {
        return menu.trim().replaceAll("\\s+", DELIMITER); //$NON-NLS-1$
    }

    private static String trimLeft(String text) {

        int i = 0;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }

        return text.substring(i);
    }

    private static String trimRight(String text) {

        int i = text.length();
        while (i > 0 && Character.isWhitespace(text.charAt(i - 1))) {
            i--;
        }

        return text.substring(0, i);
    }
}
