/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.view;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.lpex.core.LpexAction;
import com.ibm.lpex.core.LpexCommand;
import com.ibm.lpex.core.LpexView;
import com.ibm.lpex.core.LpexViewAdapter;

import biz.isphere.ide.lpex.helper.LogHelper;
import biz.isphere.ide.lpex.menu.logic.ILpexProfile;
import biz.isphere.ide.lpex.menu.logic.KeyNames;
import biz.isphere.ide.lpex.menu.logic.LpexParameter;
import biz.isphere.ide.lpex.menu.logic.Pair;
import biz.isphere.ide.lpex.menu.logic.PairList;
import biz.isphere.ide.lpex.menu.logic.UserEntries;
import biz.isphere.ide.lpex.menu.model.UserAction;
import biz.isphere.ide.lpex.menu.model.UserCommand;
import biz.isphere.ide.lpex.menu.model.UserKeyAction;
import biz.isphere.ide.lpex.menu.model.UserMouseAction;

/**
 * Applies an {@link IViewExtension} to LPEX views. The extension is applied
 * when a view is attached and again after each profile update of the view,
 * because LPEX then drops all actions, commands, key actions, mouse actions and
 * the popup menu of the view. Nothing is stored in the LPEX preferences.
 * <p>
 * Applying the extension is repeatable: actions, commands, key actions, mouse
 * actions and menu items that are already present are left unchanged. Key and
 * mouse actions that the user stored in the LPEX preferences take precedence.
 */
public class ViewExtender extends LpexViewAdapter {

    private static final String KEY_ACTION = "keyAction."; //$NON-NLS-1$
    private static final String MOUSE_ACTION = "mouseAction."; //$NON-NLS-1$
    private static final String POPUP = "current.popup"; //$NON-NLS-1$
    private static final String SET = "set "; //$NON-NLS-1$
    private static final String SET_POPUP = "set popup "; //$NON-NLS-1$
    private static final String UPDATE_PROFILE = "updateProfile"; //$NON-NLS-1$

    private IViewExtension extension;
    private ILpexProfile profile;
    private Map<LpexView, Boolean> views;
    private Set<String> reportedMessages;

    public ViewExtender(IViewExtension extension, ILpexProfile profile) {
        this.extension = extension;
        this.profile = profile;
        this.views = new IdentityHashMap<LpexView, Boolean>();
        this.reportedMessages = new HashSet<String>();
    }

    /**
     * Attaches the extender to a view and applies the extension at once. Does
     * nothing, if the view is already attached.
     *
     * @param view - view that is extended
     * @param userEntries - user entries that were restored in the LPEX settings
     *        after the view applied them, or <code>null</code>. They are set in
     *        the view directly. A profile update of the view would set them as
     *        well, but it has side effects in the editors of RDi.
     */
    public void attach(LpexView view, UserEntries userEntries) {

        if (view == null || views.containsKey(view)) {
            return;
        }

        views.put(view, Boolean.TRUE);
        view.addLpexViewListener(this);

        trace("attached", view); //$NON-NLS-1$

        if (userEntries != null && !userEntries.isEmpty()) {
            try {
                applyUserEntries(view, userEntries);
            } catch (Throwable e) {
                LogHelper.logError("Could not set the restored user entries in LPEX view: " + extension.getName(), e); //$NON-NLS-1$
            }
        }

        apply(view, "attach"); //$NON-NLS-1$
    }

    /**
     * Detaches the extender from all views.
     *
     * @param removeExtensions - <code>true</code>, to remove the extension from
     *        the views. That is done with a profile update, which drops
     *        everything that is not stored in the LPEX preferences.
     */
    public void detachAll(boolean removeExtensions) {

        for (LpexView view : new ArrayList<LpexView>(views.keySet())) {
            try {
                view.removeLpexViewListener(this);
                if (removeExtensions) {
                    view.doCommand(UPDATE_PROFILE);
                    trace("extension removed", view); //$NON-NLS-1$
                }
            } catch (Throwable e) {
                LogHelper.logError("Could not detach LPEX view: " + extension.getName(), e); //$NON-NLS-1$
            }
        }

        views.clear();
    }

    public int getViewCount() {
        return views.size();
    }

    @Override
    public void updateProfile(LpexView view) {
        apply(view, UPDATE_PROFILE);
    }

    @Override
    public void disposed(LpexView view) {

        // LPEX drops its listeners on its own.
        views.remove(view);

        trace("disposed", view); //$NON-NLS-1$
    }

    private void apply(LpexView view, String reason) {

        int actions = 0;
        try {
            actions = defineActions(view);
        } catch (Throwable e) {
            LogHelper.logError("Could not define the actions of LPEX view: " + extension.getName(), e); //$NON-NLS-1$
        }

        int commands = 0;
        try {
            commands = defineCommands(view);
        } catch (Throwable e) {
            LogHelper.logError("Could not define the commands of LPEX view: " + extension.getName(), e); //$NON-NLS-1$
        }

        int keyActions = 0;
        try {
            keyActions = defineKeyActions(view);
        } catch (Throwable e) {
            LogHelper.logError("Could not define the key actions of LPEX view: " + extension.getName(), e); //$NON-NLS-1$
        }

        int mouseActions = 0;
        try {
            mouseActions = defineMouseActions(view);
        } catch (Throwable e) {
            LogHelper.logError("Could not define the mouse actions of LPEX view: " + extension.getName(), e); //$NON-NLS-1$
        }

        boolean popupMenu = false;
        try {
            popupMenu = addPopupMenuItems(view);
        } catch (Throwable e) {
            LogHelper.logError("Could not extend the popup menu of LPEX view: " + extension.getName(), e); //$NON-NLS-1$
        }

        if (LogHelper.isTraceEnabled()) {
            trace("applied (" + reason + "): actions=" + actions + ", commands=" + commands + ", keyActions=" + keyActions //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                + ", mouseActions=" + mouseActions + ", popupMenu=" + popupMenu, view); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /*
     * Sets user entries in the view, as a profile update would do it.
     */
    private void applyUserEntries(LpexView view, UserEntries userEntries) {

        int count = 0;

        for (Pair userAction : userEntries.get(LpexParameter.USER_ACTIONS)) {
            if (view.action(userAction.getFirst()) == null) {
                LpexAction action = (LpexAction)createInstance(userAction.getSecond(), LpexAction.class);
                if (action != null) {
                    view.defineAction(userAction.getFirst(), action);
                    count++;
                }
            }
        }

        for (Pair userCommand : userEntries.get(LpexParameter.USER_COMMANDS)) {
            if (view.command(userCommand.getFirst()) == null) {
                LpexCommand command = (LpexCommand)createInstance(userCommand.getSecond(), LpexCommand.class);
                if (command != null) {
                    view.defineCommand(userCommand.getFirst(), command);
                    count++;
                }
            }
        }

        for (Pair keyAction : userEntries.get(LpexParameter.USER_KEY_ACTIONS)) {
            if (!keyAction.getSecond().equals(view.query(KEY_ACTION + keyAction.getFirst()))) {
                view.doCommand(SET + KEY_ACTION + keyAction.getFirst() + " " + keyAction.getSecond()); //$NON-NLS-1$
                count++;
            }
        }

        for (Pair mouseAction : userEntries.get(LpexParameter.USER_MOUSE_ACTIONS)) {
            if (!mouseAction.getSecond().equals(view.query(MOUSE_ACTION + mouseAction.getFirst()))) {
                view.doCommand(SET + MOUSE_ACTION + mouseAction.getFirst() + " " + mouseAction.getSecond()); //$NON-NLS-1$
                count++;
            }
        }

        trace("restored user entries set: " + count, view); //$NON-NLS-1$
    }

    private int defineActions(LpexView view) {

        int count = 0;

        for (UserAction userAction : nonNull(extension.getViewUserActions(), new UserAction[0])) {

            String name = userAction.getActionId();

            LpexAction existingAction = view.action(name);
            if (existingAction != null) {
                if (!existingAction.getClass().getName().equals(userAction.getClassName())) {
                    reportOnce("LPEX action exists and is left unchanged: " + name + " (" + getOwner() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }
                continue;
            }

            LpexAction action = (LpexAction)createInstance(userAction.getClassName(), LpexAction.class);
            if (action != null) {
                view.defineAction(name, action);
                count++;
            }
        }

        return count;
    }

    private int defineCommands(LpexView view) {

        int count = 0;

        for (UserCommand userCommand : nonNull(extension.getViewUserCommands(), new UserCommand[0])) {

            String name = userCommand.getCommandName();

            LpexCommand existingCommand = view.command(name);
            if (existingCommand != null) {
                if (!existingCommand.getClass().getName().equals(userCommand.getClassName())) {
                    reportOnce("LPEX command exists and is left unchanged: " + name + " (" + getOwner() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }
                continue;
            }

            LpexCommand command = (LpexCommand)createInstance(userCommand.getClassName(), LpexCommand.class);
            if (command != null) {
                view.defineCommand(name, command);
                count++;
            }
        }

        return count;
    }

    private Object createInstance(String className, Class<?> type) {

        try {

            Class<?> clazz = Class.forName(className, true, extension.getClassLoader());
            if (!type.isAssignableFrom(clazz)) {
                reportOnce("Class does not implement " + type.getName() + ": " + className + " (" + getOwner() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                return null;
            }

            return clazz.getConstructor().newInstance();

        } catch (Throwable e) {
            if (reportedMessages.add("create:" + className)) { //$NON-NLS-1$
                LogHelper.logError("Could not create instance of class: " + className + " (" + getOwner() + ")", e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            return null;
        }
    }

    private int defineKeyActions(LpexView view) {

        int count = 0;

        List<Pair> userKeyActions = PairList.parse(profile.getCurrent(LpexParameter.USER_KEY_ACTIONS)).getPairs();

        for (UserKeyAction keyAction : nonNull(extension.getViewUserKeyActions(), new UserKeyAction[0])) {

            String key = keyAction.getKeyStrokes();
            String actionId = keyAction.getActionId();

            Pair userKeyAction = findKeyAction(key, userKeyActions);
            if (userKeyAction != null) {
                // The key actions of the user take precedence.
                if (!userKeyAction.getSecond().equals(actionId)) {
                    reportOnce("Key is used by a key action of the user: " + key + " " + actionId + " (" + getOwner() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                }
                continue;
            }

            if (actionId.equals(view.query(KEY_ACTION + key))) {
                continue;
            }

            view.doCommand(SET + KEY_ACTION + key + " " + actionId); //$NON-NLS-1$
            count++;
        }

        return count;
    }

    private Pair findKeyAction(String key, List<Pair> keyActions) {

        for (Pair keyAction : keyActions) {
            if (KeyNames.isSameKey(key, keyAction.getFirst())) {
                return keyAction;
            }
        }

        return null;
    }

    private int defineMouseActions(LpexView view) {

        int count = 0;

        List<Pair> userMouseActions = PairList.parse(profile.getCurrent(LpexParameter.USER_MOUSE_ACTIONS)).getPairs();

        for (UserMouseAction mouseAction : nonNull(extension.getViewUserMouseActions(), new UserMouseAction[0])) {

            String mouseEvent = mouseAction.getMouseEvent();
            String actionId = mouseAction.getActionId();

            Pair userMouseAction = findMouseAction(mouseEvent, userMouseActions);
            if (userMouseAction != null) {
                // The mouse actions of the user take precedence.
                if (!userMouseAction.getSecond().equals(actionId)) {
                    reportOnce("Mouse event is used by a mouse action of the user: " + mouseEvent + " " + actionId + " (" + getOwner() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                }
                continue;
            }

            if (actionId.equals(view.query(MOUSE_ACTION + mouseEvent))) {
                continue;
            }

            view.doCommand(SET + MOUSE_ACTION + mouseEvent + " " + actionId); //$NON-NLS-1$
            count++;
        }

        return count;
    }

    private Pair findMouseAction(String mouseEvent, List<Pair> mouseActions) {

        for (Pair mouseAction : mouseActions) {
            if (mouseAction.getFirst().equalsIgnoreCase(mouseEvent)) {
                return mouseAction;
            }
        }

        return null;
    }

    private boolean addPopupMenuItems(LpexView view) {

        String popupMenu = view.query(POPUP);
        if (popupMenu == null) {
            // Never replace the popup menu of the view by the menu items of
            // the extension alone.
            trace("popup menu not available", view); //$NON-NLS-1$
            return false;
        }

        String newPopupMenu = extension.addViewPopupMenuItems(popupMenu);
        if (newPopupMenu == null || newPopupMenu.equals(popupMenu)) {
            return false;
        }

        view.doCommand(SET_POPUP + newPopupMenu);

        return true;
    }

    private <T> T[] nonNull(T[] items, T[] empty) {

        if (items == null) {
            return empty;
        }

        return items;
    }

    private String getOwner() {
        return extension.getName() + ", " + extension.getBundleName(); //$NON-NLS-1$
    }

    private void reportOnce(String message) {
        if (reportedMessages.add(message)) {
            LogHelper.logWarning(message);
        }
    }

    private void trace(String message, LpexView view) {

        if (!LogHelper.isTraceEnabled()) {
            return;
        }

        String name;
        try {
            name = view.query("name"); //$NON-NLS-1$
        } catch (Throwable e) {
            name = "?"; //$NON-NLS-1$
        }

        LogHelper.trace(extension.getName() + ": " + message + " [" + name + ", view@" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + Integer.toHexString(System.identityHashCode(view)) + ", views=" + views.size() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
