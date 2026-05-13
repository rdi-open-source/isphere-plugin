/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.lpex.comments.lpex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.ibm.lpex.alef.LpexPlugin;

import biz.isphere.ide.lpex.helper.LpexHelper;
import biz.isphere.ide.lpex.menu.AbstractLpexMenuExtension;
import biz.isphere.ide.lpex.menu.LpexMenuExtensionPlugin;
import biz.isphere.ide.lpex.menu.model.UserAction;
import biz.isphere.ide.lpex.menu.model.UserKeyAction;
import biz.isphere.lpex.comments.lpex.action.AbstractLpexAction;
import biz.isphere.lpex.comments.lpex.action.CommentAction;
import biz.isphere.lpex.comments.lpex.action.IndentAction;
import biz.isphere.lpex.comments.lpex.action.RemoveColorCodesAction;
import biz.isphere.lpex.comments.lpex.action.ToggleCommentAction;
import biz.isphere.lpex.comments.lpex.action.UnCommentAction;
import biz.isphere.lpex.comments.lpex.action.UnIndentAction;
import biz.isphere.lpex.comments.preferences.Preferences;

/**
 * This class extends the popup menue of the Lpex editor. It adds the following
 * options:
 * <ul>
 * <li>Edit STRPREPRC header</li>
 * <li>Remove STRPREPRC header</li>
 * </ul>
 */
public class MenuExtension extends AbstractLpexMenuExtension implements IPropertyChangeListener {

    private static final String PROPERTY_LPEX_USER_KEY_ACTIONS = "default.updateProfile.userKeyActions"; //$NON-NLS-1$
    private static final String MENU_NAME = LpexPlugin.getResourceLpexString(LpexMenu.SOURCE);
    private static final String MARK_ID = "biz.iSphere.LPEX"; //$NON-NLS-1$

    public MenuExtension() {
        super(BOTTOM);
    }

    @Override
    public void initializeLpexEditor(LpexMenuExtensionPlugin plugin) {

        removeOldPopupMenu();
        super.initializeLpexEditor(plugin);
    }

    @Override
    protected UserAction[] getUserActionsInternal(boolean allActions) {

        List<UserAction> actions = new LinkedList<UserAction>();

        if (allActions || isCommentsEnabled()) {
            checkAndAddUserAction(actions, CommentAction.ID, CommentAction.class.getName());
            checkAndAddUserAction(actions, UnCommentAction.ID, UnCommentAction.class.getName());
            checkAndAddUserAction(actions, ToggleCommentAction.ID, ToggleCommentAction.class.getName());
        }

        if (allActions || isIndentingEnabled()) {
            checkAndAddUserAction(actions, IndentAction.ID, IndentAction.class.getName());
            checkAndAddUserAction(actions, UnIndentAction.ID, UnIndentAction.class.getName());
        }

        checkAndAddUserAction(actions, RemoveColorCodesAction.ID, RemoveColorCodesAction.class.getName());

        return actions.toArray(new UserAction[actions.size()]);
    }

    @Override
    protected String getMenuName() {
        return MENU_NAME;
    }

    @Override
    protected String getMarkId() {
        return MARK_ID;
    }

    @Override
    protected UserKeyAction[] getUserKeyActions() {

        List<UserKeyAction> actions = getUserKeyActionsList(false);

        return actions.toArray(new UserKeyAction[actions.size()]);
    }

    @Override
    protected List<String> getMenuActions() {

        List<String> menuActions = new ArrayList<String>();

        if (isCommentsEnabled()) {
            menuActions.add(CommentAction.getLPEXMenuAction());
            menuActions.add(UnCommentAction.getLPEXMenuAction());
            menuActions.add(ToggleCommentAction.getLPEXMenuAction());
        }

        if (isCommentsEnabled() && isIndentingEnabled()) {
            menuActions.add(null); // Add separator
        }

        if (isIndentingEnabled()) {
            menuActions.add(IndentAction.getLPEXMenuAction());
            menuActions.add(UnIndentAction.getLPEXMenuAction());
        }

        menuActions.add(RemoveColorCodesAction.getLPEXMenuAction());

        return menuActions;
    }

    @Override
    protected int findStartOfLpexSubMenu(String menu) {

        int i = menu.indexOf(LpexMenu.SOURCE);
        if (i >= 0) {
            i = i + LpexMenu.SOURCE.length();
        }

        return i;
    }

    @Override
    protected IPropertyChangeListener getPreferencesChangeListener() {
        return this;
    }

    public static String getInitialUserKeyActions() {

        List<UserKeyAction> actions = getUserKeyActionsList(false);

        StringBuilder buffer = new StringBuilder();
        for (UserKeyAction action : actions) {
            appendActionToBuffer(buffer, action);
        }

        return buffer.toString();

    }

    private static List<UserKeyAction> getUserKeyActionsList(boolean allActions) {

        List<UserKeyAction> actions = new LinkedList<UserKeyAction>();

        if (allActions || isCommentsEnabled()) {
            checkAndAddUserKeyAction(actions, getShortcut(CommentAction.class), CommentAction.ID);
            checkAndAddUserKeyAction(actions, getShortcut(UnCommentAction.class), UnCommentAction.ID);
            checkAndAddUserKeyAction(actions, getShortcut(ToggleCommentAction.class), ToggleCommentAction.ID);
        }

        if (allActions || isIndentingEnabled()) {
            checkAndAddUserKeyAction(actions, getShortcut(IndentAction.class), IndentAction.ID);
            checkAndAddUserKeyAction(actions, getShortcut(UnIndentAction.class), UnIndentAction.ID);
        }

        checkAndAddUserKeyAction(actions, getShortcut(RemoveColorCodesAction.class), RemoveColorCodesAction.ID);

        return actions;
    }

    private static String getShortcut(Class<? extends AbstractLpexAction> clazz) {

        if (clazz.equals(CommentAction.class)) {
            return createShortcut(LpexKey.CTRL, LpexKey.SHIFT, LpexKey.ADD);
        } else if (clazz.equals(UnCommentAction.class)) {
            return createShortcut(LpexKey.CTRL, LpexKey.SHIFT, LpexKey.SUBSTRACT);
        } else if (clazz.equals(ToggleCommentAction.class)) {
            return createShortcut(LpexKey.CTRL, LpexKey.SHIFT, LpexKey.MULTIPLY);
        } else if (clazz.equals(IndentAction.class)) {
            return createShortcut(LpexKey.CTRL, LpexKey.TAB);
        } else if (clazz.equals(UnIndentAction.class)) {
            return createShortcut(LpexKey.CTRL, LpexKey.SHIFT, LpexKey.TAB);
        } else if (clazz.equals(RemoveColorCodesAction.class)) {
            return createShortcut(LpexKey.CTRL, LpexKey.ALT, LpexKey.NUMPAD_0);
        } else {
            throw new IllegalArgumentException("Unknown action: " + clazz.getName());
        }
    }

    private static boolean isCommentsEnabled() {
        return Preferences.getInstance().isCommentsEnabled();
    }

    private static boolean isIndentingEnabled() {
        return Preferences.getInstance().isIndentionEnabled();
    }

    public void propertyChange(PropertyChangeEvent event) {

        if (!PROPERTY_LPEX_USER_KEY_ACTIONS.equals(event.getProperty())) {
            return;
        }

        UserKeyAction[] newUserKeyActions = parseUserKeyActions((String)event.getNewValue());

        UserAction[] userActionsList = getEnabledUserActions();
        Set<String> knownActionClasses = new HashSet<String>();
        for (UserAction action : userActionsList) {
            knownActionClasses.add(action.getActionId());
        }

        StringBuilder buffer = new StringBuilder();
        for (UserKeyAction action : newUserKeyActions) {
            if (knownActionClasses.contains(action.getActionId())) {
                appendActionToBuffer(buffer, action);
            }
        }

        Preferences.getInstance().setUserKeyActions(buffer.toString());
    }

    /*
     * TODO: remove start of start of 2019
     */
    private void removeOldPopupMenu() {

        String popupMenu = LpexHelper.getLpexPopupMenu();

        // MARK-Source.Start / MARK-Source.End
        popupMenu = removeMenuItems(popupMenu, "MARK-Source.Start", "MARK-Source.End"); //$NON-NLS-1$ //$NON-NLS-2$

        // MARK-Quelle.Start / MARK-Quelle.End
        popupMenu = removeMenuItems(popupMenu, "MARK-Quelle.Start", "MARK-Quelle.End"); //$NON-NLS-1$ //$NON-NLS-2$

        LpexHelper.setLpexViewPopup(popupMenu);
    }

    @Override
    protected void removeUserActions() {
        super.removeUserActions();
    }

    @Override
    protected void removeUserKeyActions() {
        super.removeUserKeyActions();
    }

    @Override
    protected void removePopupMenu() {
        super.removePopupMenu();
    }
}
