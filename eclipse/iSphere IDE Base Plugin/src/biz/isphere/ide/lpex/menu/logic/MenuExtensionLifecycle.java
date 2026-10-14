/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import biz.isphere.ide.lpex.menu.model.MenuMarkers;

/**
 * Starts and stops a menu extension. The extension itself is applied to each
 * LPEX view and never stored in the LPEX settings. What this class maintains in
 * the LPEX settings are the entries that the user created on actions or classes
 * of the extension:
 * <ul>
 * <li><b>Start:</b> Own entries found in the LPEX settings are entries of
 * earlier versions or entries left by a crash. Entries that are not part of the
 * configuration are added to the list of remembered user entries. Then all own
 * entries and menu blocks are removed. Afterwards the remembered user entries
 * are restored, if they are still valid.</li>
 * <li><b>Stop:</b> The own entries in the LPEX settings, minus the
 * configuration, replace the list of remembered user entries. Then all own
 * entries are removed and the LPEX settings are saved. Nothing of the extension
 * is left in the LPEX settings, if the plug-in is uninstalled afterwards.</li>
 * </ul>
 * Each step catches its exceptions, so that an error in one step does not
 * prevent the other steps.
 */
public class MenuExtensionLifecycle {

    private final IMenuExtensionConfiguration configuration;
    private final ILpexProfile profile;
    private final IUserEntriesStore store;
    private final IClassChecker classChecker;
    private final ILogger logger;

    private boolean restoreCompleted;
    private boolean stopped;
    private UserEntries restoredEntries;

    public MenuExtensionLifecycle(IMenuExtensionConfiguration configuration, ILpexProfile profile, IUserEntriesStore store,
        IClassChecker classChecker, ILogger logger) {
        this.configuration = configuration;
        this.profile = profile;
        this.store = store;
        this.classChecker = classChecker;
        this.logger = logger;
    }

    /**
     * Starts the menu extension.
     */
    public void start(IExtensionCallback callback) {

        restoreCompleted = false;
        stopped = false;
        restoredEntries = new UserEntries();

        if (!isProfileAvailable()) {
            logger.warning("LPEX is not available. Menu extension is not started: " + getName()); //$NON-NLS-1$
            return;
        }

        boolean isSaved = false;
        UserEntries removableEntries = null;
        try {

            UserEntries ownedEntries = findOwnedEntries();
            UserEntries userEntries = ownedEntries.minus(configuration.getConfiguredEntries());

            try {
                store.merge(userEntries);
                isSaved = true;
            } catch (Throwable e) {
                logger.error("Could not remember the user entries found in LPEX: " + getName(), e); //$NON-NLS-1$
            }

            if (isSaved) {
                removableEntries = ownedEntries;
            } else {
                // Keep the user entries in LPEX, they are not saved.
                removableEntries = ownedEntries.minus(userEntries);
            }

            trace("start: own entries in LPEX=" + ownedEntries + ", remembered=" + userEntries); //$NON-NLS-1$ //$NON-NLS-2$

        } catch (Throwable e) {
            logger.error("Could not determine the own entries in LPEX: " + getName(), e); //$NON-NLS-1$
        }

        if (removableEntries != null) {
            try {
                callback.removeLegacyEntries(removableEntries);
            } catch (Throwable e) {
                logger.error("Could not remove the own entries from LPEX: " + getName(), e); //$NON-NLS-1$
            }
        }

        try {
            callback.registerExtension();
        } catch (Throwable e) {
            logger.error("Could not register LPEX menu extension: " + getName(), e); //$NON-NLS-1$
        }

        if (isSaved) {
            try {
                restoreUserEntries();
                restoreCompleted = true;
            } catch (Throwable e) {
                logger.error("Could not restore the user entries: " + getName(), e); //$NON-NLS-1$
            }
        }

        try {
            callback.startViewExtension(getRestoredEntries());
        } catch (Throwable e) {
            logger.error("Could not start extending the LPEX views: " + getName(), e); //$NON-NLS-1$
        }

        try {
            callback.addPreferencesListener();
        } catch (Throwable e) {
            logger.error("Could not add the listener on the LPEX settings: " + getName(), e); //$NON-NLS-1$
        }
    }

    /**
     * Stops the menu extension. The extension is stopped when the workbench has
     * shut down and again, when the plug-in stops. Only the first call does
     * something, until the extension is started again.
     */
    public void stop(IExtensionCallback callback) {

        if (stopped) {
            trace("stop: already stopped"); //$NON-NLS-1$
            return;
        }

        try {
            callback.stopViewExtension();
        } catch (Throwable e) {
            logger.error("Could not stop extending the LPEX views: " + getName(), e); //$NON-NLS-1$
        }

        try {
            callback.removePreferencesListener();
        } catch (Throwable e) {
            logger.error("Could not remove the listener on the LPEX settings: " + getName(), e); //$NON-NLS-1$
        }

        if (!isProfileAvailable()) {
            logger.warning("LPEX is not available. Entries of the menu extension are not removed: " + getName()); //$NON-NLS-1$
            return;
        }

        UserEntries ownedEntries = null;
        try {
            ownedEntries = findOwnedEntries();
        } catch (Throwable e) {
            logger.error("Could not determine the own entries in LPEX: " + getName(), e); //$NON-NLS-1$
        }

        if (ownedEntries != null) {

            // Remember the user entries before they are removed.
            try {
                UserEntries userEntries = ownedEntries.minus(configuration.getConfiguredEntries());
                if (restoreCompleted) {
                    store.replace(userEntries);
                } else {
                    // Keep entries that were not restored in this session.
                    store.merge(userEntries);
                }
                trace("stop: remembered=" + userEntries + ", replaced=" + restoreCompleted); //$NON-NLS-1$ //$NON-NLS-2$
            } catch (Throwable e) {
                // Nothing of the extension must be left in LPEX. Therefore the
                // entries are removed nevertheless.
                logger.error("Could not remember the user entries: " + getName(), e); //$NON-NLS-1$
            }

            for (LpexParameter parameter : LpexParameter.userParameters()) {
                try {
                    removeEntries(parameter, ownedEntries.get(parameter));
                } catch (Throwable e) {
                    logger.error("Could not remove the own entries from LPEX parameter " + parameter.getName() + ": " + getName(), e); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }

        try {
            removeMenuBlocks();
        } catch (Throwable e) {
            logger.error("Could not remove the menu blocks from LPEX: " + getName(), e); //$NON-NLS-1$
        }

        try {
            profile.flush();
        } catch (Throwable e) {
            logger.error("Could not save the LPEX settings: " + getName(), e); //$NON-NLS-1$
        }

        restoreCompleted = false;
        stopped = true;
    }

    /**
     * Returns the own entries in the LPEX settings.
     */
    public UserEntries findOwnedEntries() {
        return createMatcher().match(readUserEntries());
    }

    /**
     * Removes the specified entries from an LPEX user parameter. Resets the
     * parameter to its install value, if only the install value is left.
     */
    public void removeEntries(LpexParameter parameter, List<Pair> entries) {

        if (entries == null || entries.isEmpty()) {
            return;
        }

        PairList currentValue = PairList.parse(profile.getCurrent(parameter));
        PairList newValue = currentValue.remove(entries);
        if (newValue.getPairs().size() == currentValue.getPairs().size()) {
            return;
        }

        PairList installValue = PairList.parse(profile.getInstall(parameter));
        if (newValue.format().equals(installValue.format())) {
            profile.resetToInstall(parameter);
        } else {
            profile.setDefault(parameter, newValue.format());
        }

        trace("removed from " + parameter.getName() + ": " + currentValue.remove(newValue.getPairs()).getPairs()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Removes all menu blocks with the current and the former markers from the
     * popup menu. Resets the popup menu to its install value, if only the
     * install value is left.
     */
    public void removeMenuBlocks() {

        String popupMenu = profile.getCurrent(LpexParameter.POPUP);
        if (popupMenu == null) {
            return;
        }

        List<String> warnings = new ArrayList<String>();
        String newPopupMenu = popupMenu;
        for (MenuMarkers markers : configuration.getMenuMarkers()) {
            newPopupMenu = PopupMenuEditor.removeBlocks(newPopupMenu, markers, warnings);
        }

        for (String warning : warnings) {
            logger.warning(warning + " (" + getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (PopupMenuEditor.isSameMenu(newPopupMenu, popupMenu)) {
            return;
        }

        String installPopupMenu = profile.getInstall(LpexParameter.POPUP);
        if (installPopupMenu != null && PopupMenuEditor.isSameMenu(newPopupMenu, installPopupMenu)) {
            profile.resetToInstall(LpexParameter.POPUP);
        } else {
            profile.setDefault(LpexParameter.POPUP, newPopupMenu);
        }

        trace("removed menu blocks from " + LpexParameter.POPUP.getName()); //$NON-NLS-1$
    }

    /**
     * Restores the remembered user entries in the LPEX settings, if they pass
     * the checks:
     * <ul>
     * <li>User actions and user commands: the class is an LPEX action or
     * command, the name is not used.</li>
     * <li>Key actions and mouse actions: the action is defined by the extension
     * or by a restored user action, the key or mouse event is not used.</li>
     * </ul>
     * Entries that fail the checks are skipped. They drop out of the list at
     * the next stop.
     */
    public void restoreUserEntries() {

        UserEntries rememberedEntries = store.load();
        if (rememberedEntries.isEmpty()) {
            return;
        }

        UserEntries currentEntries = readUserEntries();
        UserEntries configuredEntries = configuration.getConfiguredEntries();
        restoredEntries = new UserEntries();
        List<String> skippedEntries = new ArrayList<String>();

        Set<String> validActionIds = new HashSet<String>(configuration.getEnabledActionIds());

        Set<String> actionNames = getNames(currentEntries, configuredEntries, LpexParameter.USER_ACTIONS);
        actionNames.addAll(validActionIds);
        for (Pair userAction : rememberedEntries.get(LpexParameter.USER_ACTIONS)) {
            if (actionNames.contains(userAction.getFirst())) {
                skippedEntries.add(userAction + " (name is used)"); //$NON-NLS-1$
            } else if (!classChecker.isAction(userAction.getSecond())) {
                skippedEntries.add(userAction + " (not an LPEX action)"); //$NON-NLS-1$
            } else {
                restoredEntries.add(LpexParameter.USER_ACTIONS, userAction);
                actionNames.add(userAction.getFirst());
                validActionIds.add(userAction.getFirst());
            }
        }

        Set<String> commandNames = getNames(currentEntries, configuredEntries, LpexParameter.USER_COMMANDS);
        for (Pair userCommand : rememberedEntries.get(LpexParameter.USER_COMMANDS)) {
            if (commandNames.contains(userCommand.getFirst())) {
                skippedEntries.add(userCommand + " (name is used)"); //$NON-NLS-1$
            } else if (!classChecker.isCommand(userCommand.getSecond())) {
                skippedEntries.add(userCommand + " (not an LPEX command)"); //$NON-NLS-1$
            } else {
                restoredEntries.add(LpexParameter.USER_COMMANDS, userCommand);
                commandNames.add(userCommand.getFirst());
            }
        }

        List<Pair> keyActions = new ArrayList<Pair>(currentEntries.get(LpexParameter.USER_KEY_ACTIONS));
        for (Pair keyAction : rememberedEntries.get(LpexParameter.USER_KEY_ACTIONS)) {
            if (!validActionIds.contains(keyAction.getSecond())) {
                skippedEntries.add(keyAction + " (action is not defined)"); //$NON-NLS-1$
            } else if (KeyNames.isAssigned(keyAction.getFirst(), keyActions)) {
                skippedEntries.add(keyAction + " (key is used)"); //$NON-NLS-1$
            } else {
                restoredEntries.add(LpexParameter.USER_KEY_ACTIONS, keyAction);
                keyActions.add(keyAction);
            }
        }

        List<Pair> mouseActions = new ArrayList<Pair>(currentEntries.get(LpexParameter.USER_MOUSE_ACTIONS));
        for (Pair mouseAction : rememberedEntries.get(LpexParameter.USER_MOUSE_ACTIONS)) {
            if (!validActionIds.contains(mouseAction.getSecond())) {
                skippedEntries.add(mouseAction + " (action is not defined)"); //$NON-NLS-1$
            } else if (isMouseEventAssigned(mouseAction.getFirst(), mouseActions)) {
                skippedEntries.add(mouseAction + " (mouse event is used)"); //$NON-NLS-1$
            } else {
                restoredEntries.add(LpexParameter.USER_MOUSE_ACTIONS, mouseAction);
                mouseActions.add(mouseAction);
            }
        }

        for (LpexParameter parameter : LpexParameter.userParameters()) {
            List<Pair> restoredPairs = restoredEntries.get(parameter);
            if (!restoredPairs.isEmpty()) {
                PairList currentValue = PairList.parse(profile.getCurrent(parameter));
                profile.setDefault(parameter, currentValue.append(restoredPairs).format());
            }
        }

        trace("restored=" + restoredEntries + ", skipped=" + skippedEntries); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Returns <code>true</code>, if the menu extension completed restoring the
     * user entries at start. Only then the list of remembered user entries is
     * replaced at stop. Otherwise it is extended.
     */
    public boolean isRestoreCompleted() {
        return restoreCompleted;
    }

    /**
     * Returns the user entries that were restored in the LPEX settings at
     * start.
     */
    public UserEntries getRestoredEntries() {

        UserEntries entries = new UserEntries();
        if (restoredEntries != null) {
            entries.addAll(restoredEntries);
        }

        return entries;
    }

    private UserEntries readUserEntries() {

        UserEntries entries = new UserEntries();

        for (LpexParameter parameter : LpexParameter.userParameters()) {
            PairList value = PairList.parse(profile.getCurrent(parameter));
            if (value.hasRemainder()) {
                logger.warning("Incomplete entry in LPEX parameter " + parameter.getName() + ": " + value.getRemainder()); //$NON-NLS-1$ //$NON-NLS-2$
            }
            entries.addAll(parameter, value.getPairs());
        }

        return entries;
    }

    private OwnedEntriesMatcher createMatcher() {

        Set<String> configuredActionIds = getNames(configuration.getConfiguredEntries(), new UserEntries(), LpexParameter.USER_ACTIONS);
        configuredActionIds.addAll(configuration.getEnabledActionIds());

        return new OwnedEntriesMatcher(configuration.getOwnedClassPrefixes(), configuration.getOwnedActionIdPrefixes(), configuredActionIds);
    }

    private Set<String> getNames(UserEntries entries1, UserEntries entries2, LpexParameter parameter) {

        Set<String> names = new HashSet<String>();
        for (Pair pair : entries1.get(parameter)) {
            names.add(pair.getFirst());
        }

        for (Pair pair : entries2.get(parameter)) {
            names.add(pair.getFirst());
        }

        return names;
    }

    private boolean isMouseEventAssigned(String mouseEvent, List<Pair> mouseActions) {

        for (Pair mouseAction : mouseActions) {
            if (mouseAction.getFirst().equalsIgnoreCase(mouseEvent)) {
                return true;
            }
        }

        return false;
    }

    private boolean isProfileAvailable() {

        try {
            return profile.isAvailable();
        } catch (Throwable e) {
            logger.error("Could not check whether LPEX is available: " + getName(), e); //$NON-NLS-1$
            return false;
        }
    }

    private String getName() {
        return configuration.getName();
    }

    private void trace(String message) {
        logger.trace(getName() + ": " + message); //$NON-NLS-1$
    }
}
