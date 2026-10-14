/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.ibm.lpex.alef.LpexPlugin;

import biz.isphere.ide.lpex.helper.LogHelper;
import biz.isphere.ide.lpex.menu.logic.IExtensionCallback;
import biz.isphere.ide.lpex.menu.logic.IMenuExtensionConfiguration;
import biz.isphere.ide.lpex.menu.logic.LpexParameter;
import biz.isphere.ide.lpex.menu.logic.MenuExtensionLifecycle;
import biz.isphere.ide.lpex.menu.logic.Pair;
import biz.isphere.ide.lpex.menu.logic.PairList;
import biz.isphere.ide.lpex.menu.logic.PopupMenuEditor;
import biz.isphere.ide.lpex.menu.logic.UserEntries;
import biz.isphere.ide.lpex.menu.logic.UserEntriesStore;
import biz.isphere.ide.lpex.menu.model.AbstractLpexAction;
import biz.isphere.ide.lpex.menu.model.MenuMarkers;
import biz.isphere.ide.lpex.menu.model.UserAction;
import biz.isphere.ide.lpex.menu.model.UserCommand;
import biz.isphere.ide.lpex.menu.model.UserKeyAction;
import biz.isphere.ide.lpex.menu.model.UserMouseAction;
import biz.isphere.ide.lpex.menu.platform.EclipsePreferencesNode;
import biz.isphere.ide.lpex.menu.platform.LpexClassChecker;
import biz.isphere.ide.lpex.menu.platform.LpexProfile;
import biz.isphere.ide.lpex.menu.platform.PlatformLogger;
import biz.isphere.ide.lpex.menu.view.IViewExtension;
import biz.isphere.ide.lpex.menu.view.ViewExtender;
import biz.isphere.ide.lpex.menu.view.ViewExtensionTracker;

// MARK-Source.Start
// MARK-Source.End
//
// beginSubmenu "STRPREPRC"
// endSubmenu
//
// beginSubmenu "STRPREPRC"
// endSubmenu
//
// MARK-biz.Source.Start
// MARK-biz.Source.End
//
// MARK-biz.Quelle.Start
// MARK-biz.Quelle.End
//
// MARK-biz.iSphere.LPEX.Start
// MARK-biz.iSphere.LPEX.End
//
// MARK-biz.iSphere.STRPREPRC.Start
// MARK-biz.iSphere.STRPREPRC.End

/**
 * Extends the LPEX editor with actions, commands, key actions, mouse actions
 * and menu items of a plug-in.
 * <p>
 * The extensions are applied to each LPEX view of the LPEX editors and are
 * never stored in the LPEX settings. Nothing of the plug-in is left in the LPEX
 * settings after it has been uninstalled, even if RDi crashed before.
 * <p>
 * Entries that the user creates in the LPEX settings on actions or classes of
 * the plug-in, e.g. a key action, are removed when the plug-in stops. They are
 * remembered in the preferences of the plug-in and restored at the next start.
 * <p>
 * Usage: Call {@link #initializeLpexEditor(LpexMenuExtensionPlugin)} from the
 * LPEX preload of the plug-in and {@link #uninstall()} from the
 * <code>stop()</code> method of the plug-in. The menu extension also stops
 * itself, after the workbench has shut down. The call of {@link #uninstall()}
 * from the plug-in then does nothing.
 */
public abstract class AbstractLpexMenuExtension implements ILpexMenuExtension {

    protected static final String BEGIN_SUB_MENU = "beginSubmenu"; //$NON-NLS-1$
    protected static final String END_SUB_MENU = "endSubmenu"; //$NON-NLS-1$
    protected static final String SEPARATOR = "separator"; //$NON-NLS-1$
    protected static final String DOUBLE_QUOTES = "\""; //$NON-NLS-1$
    protected static final String ACTION_DELIMITER = " "; //$NON-NLS-1$

    protected static final int TOP = 1;
    protected static final int BOTTOM = 2;

    private static final String MARK_PREFIX = "MARK-"; //$NON-NLS-1$

    private int position; // Menu position
    IPropertyChangeListener listener;

    private LpexMenuExtensionPlugin plugin;
    private MenuExtensionLifecycle lifecycle;
    private ViewExtensionTracker viewExtensionTracker;
    private UserEntries legacyEntries;

    public AbstractLpexMenuExtension(int position) {
        this.position = position;
    }

    protected abstract String getMenuName();

    protected abstract String getMarkId();

    protected abstract IPropertyChangeListener getPreferencesChangeListener();

    /**
     * Starts the menu extension. Must be called from the LPEX preload of the
     * plug-in. Exceptions are logged and never thrown, because LPEX would
     * cancel the preloads of all other plug-ins.
     *
     * @param plugin - plug-in that owns the menu extension. It stops the menu
     *        extension with {@link #uninstall()}. The remembered user entries
     *        are stored in its preferences.
     */
    public void initializeLpexEditor(LpexMenuExtensionPlugin plugin) {

        try {

            if (lifecycle != null) {
                // Started once more: stop the views of the first start.
                Callback callback = new Callback();
                callback.stopViewExtension();
                callback.removePreferencesListener();
            }

            this.plugin = plugin;

            String markId = getMarkId();
            lifecycle = new MenuExtensionLifecycle(new Configuration(), new LpexProfile(),
                new UserEntriesStore(new EclipsePreferencesNode(getBundleName()), markId), new LpexClassChecker(getExtensionClassLoader()),
                new PlatformLogger());

            lifecycle.start(new Callback());

        } catch (Throwable e) {
            LogHelper.logError("Could not initialize LPEX menu extension: " + getClass().getName(), e); //$NON-NLS-1$
        }
    }

    /**
     * Stops the menu extension. Must be called from the <code>stop()</code>
     * method of the plug-in. Exceptions are logged and never thrown.
     */
    public void uninstall() {

        try {

            if (lifecycle == null) {
                return;
            }

            lifecycle.stop(new Callback());

        } catch (Throwable e) {
            LogHelper.logError("Could not uninstall LPEX menu extension: " + getClass().getName(), e); //$NON-NLS-1$
        }
    }

    protected UserAction[] parseUserActions(String actions) {

        List<UserAction> actionsList = new ArrayList<UserAction>();
        for (Pair pair : PairList.parse(actions).getPairs()) {
            actionsList.add(new UserAction(pair.getFirst(), pair.getSecond()));
        }

        return actionsList.toArray(new UserAction[actionsList.size()]);
    }

    protected UserKeyAction[] parseUserKeyActions(String actions) {

        List<UserKeyAction> actionsList = new ArrayList<UserKeyAction>();
        for (Pair pair : PairList.parse(actions).getPairs()) {
            actionsList.add(new UserKeyAction(pair.getFirst(), pair.getSecond()));
        }

        return actionsList.toArray(new UserKeyAction[actionsList.size()]);
    }

    protected String getUserKeyActionShortcut(String availableAtions, String actionId) {

        String userKeyActionsString = availableAtions;
        UserKeyAction[] userKeyActions = parseUserKeyActions(userKeyActionsString);

        Map<String, UserKeyAction> userKeyActionsMap = new LinkedHashMap<String, UserKeyAction>();
        for (UserKeyAction userKeyAction : userKeyActions) {
            userKeyActionsMap.put(userKeyAction.getActionId(), userKeyAction);
        }

        UserKeyAction userKeyAction = userKeyActionsMap.get(actionId);
        if (userKeyAction == null) {
            return null;
        }

        return userKeyAction.getKeyStrokes();
    }

    /**
     * Removes the own user actions from the LPEX settings. Called at start, to
     * remove the entries of earlier versions and the entries left by a crash.
     */
    protected void removeUserActions() {
        removeOwnedEntries(LpexParameter.USER_ACTIONS);
    }

    /**
     * Removes the own user commands from the LPEX settings. Called at start.
     */
    protected void removeUserCommands() {
        removeOwnedEntries(LpexParameter.USER_COMMANDS);
    }

    /**
     * Removes the own user key actions from the LPEX settings. Called at start.
     * Own key actions are recognized by their action, not by their key.
     */
    protected void removeUserKeyActions() {
        removeOwnedEntries(LpexParameter.USER_KEY_ACTIONS);
    }

    /**
     * Removes the own user mouse actions from the LPEX settings. Called at
     * start.
     */
    protected void removeUserMouseActions() {
        removeOwnedEntries(LpexParameter.USER_MOUSE_ACTIONS);
    }

    /**
     * Removes the own menu blocks from the popup menu in the LPEX settings,
     * including the blocks with the markers of {@link #getLegacyMarkers()}.
     * Called at start.
     */
    protected void removePopupMenu() {

        if (lifecycle == null) {
            return;
        }

        lifecycle.removeMenuBlocks();
    }

    protected UserAction[] getEnabledUserActions() {
        return getUserActionsInternal(true);
    }

    protected UserAction[] getAllUserActions() {
        return getUserActionsInternal(false);
    }

    /**
     * Returns the user actions of the menu extension.
     * <p>
     * Do not call this method, use {@link #getEnabledUserActions()} or
     * {@link #getAllUserActions()} instead. The meaning of the parameter has
     * been mixed up by subclasses in the past.
     *
     * @param enabled - <code>true</code> to return only the actions of enabled
     *        functions, <code>false</code> to return all actions
     * @return user actions
     * @deprecated The parameter is easily misread. Subclasses still implement
     *             this method, because it is the only way to configure the
     *             actions.
     */
    @Deprecated
    protected abstract UserAction[] getUserActionsInternal(boolean enabled);

    protected void checkAndAddUserAction(List<UserAction> actions, String actionId, String className) {

        actions.add(new UserAction(actionId, className));
    }

    protected static void appendActionToBuffer(StringBuilder buffer, AbstractLpexAction<? extends AbstractLpexAction<?>> action) {

        if (buffer.length() > 0) {
            buffer.append(ACTION_DELIMITER);
        }

        buffer.append(action.toString());
    }

    protected abstract UserKeyAction[] getUserKeyActions();

    /**
     * Returns the user commands that are defined in each LPEX view. Override
     * this method to provide commands.
     *
     * @return user commands, empty by default
     */
    protected UserCommand[] getUserCommands() {
        return new UserCommand[0];
    }

    /**
     * Returns the mouse actions that are defined in each LPEX view. Override
     * this method to provide mouse actions.
     *
     * @return mouse actions, empty by default
     */
    protected UserMouseAction[] getUserMouseActions() {
        return new UserMouseAction[0];
    }

    /**
     * Returns the class prefixes of the own user actions and user commands.
     * User actions and user commands in the LPEX settings, whose class starts
     * with one of the prefixes, belong to the menu extension.
     * <p>
     * The prefixes of different plug-ins must not overlap. Otherwise a plug-in
     * would remove the entries of another plug-in.
     *
     * @return class prefixes, by default the packages of the classes of all
     *         user actions and user commands, e.g.
     *         <code>de.tools400.lpex.irpgformatter.actions.</code>
     */
    protected String[] getOwnedClassPrefixes() {

        Set<String> prefixes = new LinkedHashSet<String>();

        for (UserAction action : nonNull(getEnabledUserActions(), new UserAction[0])) {
            addPackagePrefix(prefixes, action.getClassName());
        }

        for (UserAction action : nonNull(getAllUserActions(), new UserAction[0])) {
            addPackagePrefix(prefixes, action.getClassName());
        }

        for (UserCommand command : nonNull(getUserCommands(), new UserCommand[0])) {
            addPackagePrefix(prefixes, command.getClassName());
        }

        return prefixes.toArray(new String[prefixes.size()]);
    }

    /**
     * Returns the prefixes of the own action IDs. Key actions and mouse actions
     * in the LPEX settings, whose action starts with one of the prefixes,
     * belong to the menu extension. That also finds key actions on actions of
     * earlier versions.
     *
     * @return action ID prefixes, empty by default
     */
    protected String[] getOwnedActionIdPrefixes() {
        return new String[0];
    }

    /**
     * Returns the markers of menu blocks of earlier versions, which are removed
     * from the popup menu in the LPEX settings at start.
     *
     * @return markers of earlier versions, empty by default
     */
    protected MenuMarkers[] getLegacyMarkers() {
        return new MenuMarkers[0];
    }

    public static String createShortcut(String... modifiers) {

        StringBuilder buffer = new StringBuilder();
        for (String modifier : modifiers) {
            if (buffer.length() > 0) {
                buffer.append("-"); //$NON-NLS-1$
            }
            buffer.append(modifier);
        }

        return buffer.toString();
    }

    protected static void checkAndAddUserKeyAction(List<UserKeyAction> actions, String shortcut, String actionId) {
        if (shortcut == null) {
            return;
        }
        actions.add(new UserKeyAction(shortcut, actionId));
    }

    public static String getInitialUserKeyActions() {
        return ""; //$NON-NLS-1$
    }

    /*
     * The start marker ends with ".End" and the end marker with ".Start". The
     * markers must stay like that, otherwise the blocks of earlier versions are
     * not found.
     */
    private String getMarkStart() {
        return MARK_PREFIX + getMarkId() + ".End"; //$NON-NLS-1$
    }

    private String getMarkEnd() {
        return MARK_PREFIX + getMarkId() + ".Start"; //$NON-NLS-1$
    }

    protected abstract List<String> getMenuActions();

    protected abstract int findStartOfLpexSubMenu(String menu);

    /**
     * Removes all blocks that are enclosed by the specified markers from the
     * menu. A block without end marker is left unchanged and reported in the
     * log.
     */
    protected String removeMenuItems(String menu, String startMark, String endMark) {

        if (menu == null) {
            return null;
        }

        List<String> warnings = new ArrayList<String>();
        String newMenu = PopupMenuEditor.removeBlocks(menu, new MenuMarkers(startMark, endMark), warnings);

        for (String warning : warnings) {
            LogHelper.logWarning(warning + " (" + getMarkId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return newMenu;
    }

    private void removeOwnedEntries(LpexParameter parameter) {

        if (lifecycle == null) {
            return;
        }

        UserEntries ownedEntries = legacyEntries;
        if (ownedEntries == null) {
            // Called outside of the start: remove only the entries of the
            // configuration. User entries are only removed, after they have
            // been remembered.
            UserEntries foundEntries = lifecycle.findOwnedEntries();
            UserEntries userEntries = foundEntries.minus(new Configuration().getConfiguredEntries());
            ownedEntries = foundEntries.minus(userEntries);
        }

        lifecycle.removeEntries(parameter, ownedEntries.get(parameter));
    }

    private String getBundleName() {

        Class<?> pluginClass = getPluginClass();
        Bundle bundle = FrameworkUtil.getBundle(pluginClass);
        if (bundle != null) {
            return bundle.getSymbolicName();
        }

        return pluginClass.getName();
    }

    private Class<?> getPluginClass() {

        if (plugin != null) {
            return plugin.getClass();
        }

        return getClass();
    }

    /*
     * The subclass configures the classes of the actions. Its class loader
     * therefore sees them.
     */
    private ClassLoader getExtensionClassLoader() {
        return getClass().getClassLoader();
    }

    private void addPackagePrefix(Set<String> prefixes, String className) {

        if (className == null) {
            return;
        }

        int i = className.lastIndexOf('.');
        if (i > 0) {
            prefixes.add(className.substring(0, i + 1));
        }
    }

    private static <T> T[] nonNull(T[] items, T[] empty) {

        if (items == null) {
            return empty;
        }

        return items;
    }

    /**
     * Configuration of this menu extension for the lifecycle.
     */
    private class Configuration implements IMenuExtensionConfiguration {

        public String getName() {
            return getMarkId();
        }

        public String[] getOwnedClassPrefixes() {
            return AbstractLpexMenuExtension.this.getOwnedClassPrefixes();
        }

        public String[] getOwnedActionIdPrefixes() {
            return AbstractLpexMenuExtension.this.getOwnedActionIdPrefixes();
        }

        public UserEntries getConfiguredEntries() {

            UserEntries entries = new UserEntries();

            for (UserAction action : nonNull(getEnabledUserActions(), new UserAction[0])) {
                add(entries, LpexParameter.USER_ACTIONS, action.getActionId(), action.getClassName());
            }

            for (UserAction action : nonNull(getAllUserActions(), new UserAction[0])) {
                add(entries, LpexParameter.USER_ACTIONS, action.getActionId(), action.getClassName());
            }

            for (UserCommand command : nonNull(getUserCommands(), new UserCommand[0])) {
                add(entries, LpexParameter.USER_COMMANDS, command.getCommandName(), command.getClassName());
            }

            for (UserKeyAction keyAction : nonNull(getUserKeyActions(), new UserKeyAction[0])) {
                add(entries, LpexParameter.USER_KEY_ACTIONS, keyAction.getKeyStrokes(), keyAction.getActionId());
            }

            for (UserMouseAction mouseAction : nonNull(getUserMouseActions(), new UserMouseAction[0])) {
                add(entries, LpexParameter.USER_MOUSE_ACTIONS, mouseAction.getMouseEvent(), mouseAction.getActionId());
            }

            return entries;
        }

        public Set<String> getEnabledActionIds() {

            Set<String> actionIds = new HashSet<String>();
            for (UserAction action : nonNull(getEnabledUserActions(), new UserAction[0])) {
                if (action.getActionId() != null) {
                    actionIds.add(action.getActionId());
                }
            }

            return actionIds;
        }

        public MenuMarkers[] getMenuMarkers() {

            List<MenuMarkers> markers = new ArrayList<MenuMarkers>();
            markers.add(new MenuMarkers(getMarkStart(), getMarkEnd()));

            for (MenuMarkers legacyMarkers : nonNull(getLegacyMarkers(), new MenuMarkers[0])) {
                if (legacyMarkers != null && legacyMarkers.getStart() != null && legacyMarkers.getEnd() != null) {
                    markers.add(legacyMarkers);
                }
            }

            return markers.toArray(new MenuMarkers[markers.size()]);
        }

        private void add(UserEntries entries, LpexParameter parameter, String first, String second) {
            if (first != null && second != null) {
                entries.add(parameter, first, second);
            }
        }
    }

    /**
     * Steps of the lifecycle that depend on LPEX and the workbench.
     */
    private class Callback implements IExtensionCallback {

        public void removeLegacyEntries(UserEntries ownedEntries) {

            legacyEntries = ownedEntries;

            try {

                try {
                    removeUserActions();
                } catch (Throwable e) {
                    LogHelper.logError("Could not remove user actions: " + getMarkId(), e); //$NON-NLS-1$
                }

                try {
                    removeUserCommands();
                } catch (Throwable e) {
                    LogHelper.logError("Could not remove user commands: " + getMarkId(), e); //$NON-NLS-1$
                }

                try {
                    removeUserKeyActions();
                } catch (Throwable e) {
                    LogHelper.logError("Could not remove user key actions: " + getMarkId(), e); //$NON-NLS-1$
                }

                try {
                    removeUserMouseActions();
                } catch (Throwable e) {
                    LogHelper.logError("Could not remove user mouse actions: " + getMarkId(), e); //$NON-NLS-1$
                }

                try {
                    removePopupMenu();
                } catch (Throwable e) {
                    LogHelper.logError("Could not remove menu blocks: " + getMarkId(), e); //$NON-NLS-1$
                }

            } finally {
                legacyEntries = null;
            }
        }

        public void registerExtension() {
            plugin.setLpexMenuExtension(AbstractLpexMenuExtension.this);
        }

        public void startViewExtension(UserEntries restoredEntries) {

            // Stops the menu extension after the workbench has shut down,
            // even if the plug-in does not call uninstall(), e.g. because
            // registering the menu extension failed.
            Runnable shutdownHandler = new Runnable() {
                public void run() {
                    uninstall();
                }
            };

            viewExtensionTracker = new ViewExtensionTracker(new ViewExtender(new ViewExtension(), new LpexProfile()), shutdownHandler);
            viewExtensionTracker.start(restoredEntries);
        }

        public void addPreferencesListener() {
            listener = getPreferencesChangeListener();
            if (listener != null) {
                LpexPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(listener);
            }
        }

        public void stopViewExtension() {
            if (viewExtensionTracker != null) {
                viewExtensionTracker.stop();
                viewExtensionTracker = null;
            }
        }

        public void removePreferencesListener() {
            if (listener != null) {
                LpexPlugin lpexPlugin = LpexPlugin.getDefault();
                if (lpexPlugin != null) {
                    lpexPlugin.getPreferenceStore().removePropertyChangeListener(listener);
                }
                listener = null;
            }
        }
    }

    /**
     * Supplies the extensions of this menu extension to the
     * {@link ViewExtender}.
     */
    private class ViewExtension implements IViewExtension {

        public String getName() {
            return getMarkId();
        }

        public String getBundleName() {
            return AbstractLpexMenuExtension.this.getBundleName();
        }

        public ClassLoader getClassLoader() {
            return getExtensionClassLoader();
        }

        public UserAction[] getViewUserActions() {
            return getEnabledUserActions();
        }

        public UserCommand[] getViewUserCommands() {
            return getUserCommands();
        }

        public UserKeyAction[] getViewUserKeyActions() {
            return getUserKeyActions();
        }

        public UserMouseAction[] getViewUserMouseActions() {
            return getUserMouseActions();
        }

        public String addViewPopupMenuItems(String popupMenu) {

            PopupMenuEditor editor = new PopupMenuEditor(new MenuMarkers(getMarkStart(), getMarkEnd()));
            if (editor.containsBlock(popupMenu)) {
                return popupMenu;
            }

            int location = -1;
            if (popupMenu != null) {
                location = findStartOfLpexSubMenu(popupMenu);
            }

            String block = editor.createBlock(getMenuName(), getMenuActions(), location >= 0);

            return editor.insertBlock(popupMenu, block, location, position);
        }
    }
}
