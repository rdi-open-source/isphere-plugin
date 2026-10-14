/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Determines the entries of the LPEX user parameters that belong to a menu
 * extension. The entries are recognized by themselves, not by the current
 * configuration of the extension:
 * <ul>
 * <li>User actions and user commands belong to the extension, if their class
 * starts with one of its class prefixes.</li>
 * <li>The own action IDs are the names of these user actions, the action IDs of
 * the configuration and all action IDs that start with one of the action ID
 * prefixes.</li>
 * <li>User key actions and user mouse actions belong to the extension, if their
 * action is an own action ID. The key or mouse event does not matter.</li>
 * </ul>
 * Empty prefixes are ignored, because they would match the entries of all other
 * plug-ins.
 */
public class OwnedEntriesMatcher {

    private final List<String> classPrefixes;
    private final List<String> actionIdPrefixes;
    private final Set<String> configuredActionIds;

    public OwnedEntriesMatcher(String[] classPrefixes, String[] actionIdPrefixes, Collection<String> configuredActionIds) {
        this.classPrefixes = cleanPrefixes(classPrefixes);
        this.actionIdPrefixes = cleanPrefixes(actionIdPrefixes);
        this.configuredActionIds = new HashSet<String>(configuredActionIds);
    }

    /**
     * Returns the entries that belong to the menu extension. The own action IDs
     * are determined from the specified entries, before anything is removed.
     */
    public UserEntries match(UserEntries entries) {

        UserEntries owned = new UserEntries();
        Set<String> ownActionIds = new HashSet<String>(configuredActionIds);

        for (Pair userAction : entries.get(LpexParameter.USER_ACTIONS)) {
            if (isOwnedClass(userAction.getSecond())) {
                owned.add(LpexParameter.USER_ACTIONS, userAction);
                ownActionIds.add(userAction.getFirst());
            }
        }

        for (Pair userCommand : entries.get(LpexParameter.USER_COMMANDS)) {
            if (isOwnedClass(userCommand.getSecond())) {
                owned.add(LpexParameter.USER_COMMANDS, userCommand);
            }
        }

        for (Pair keyAction : entries.get(LpexParameter.USER_KEY_ACTIONS)) {
            if (isOwnedActionId(keyAction.getSecond(), ownActionIds)) {
                owned.add(LpexParameter.USER_KEY_ACTIONS, keyAction);
            }
        }

        for (Pair mouseAction : entries.get(LpexParameter.USER_MOUSE_ACTIONS)) {
            if (isOwnedActionId(mouseAction.getSecond(), ownActionIds)) {
                owned.add(LpexParameter.USER_MOUSE_ACTIONS, mouseAction);
            }
        }

        return owned;
    }

    public boolean isOwnedClass(String className) {
        return startsWithPrefix(className, classPrefixes);
    }

    private boolean isOwnedActionId(String actionId, Set<String> ownActionIds) {

        if (ownActionIds.contains(actionId)) {
            return true;
        }

        return startsWithPrefix(actionId, actionIdPrefixes);
    }

    private boolean startsWithPrefix(String value, List<String> prefixes) {

        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private static List<String> cleanPrefixes(String[] prefixes) {

        List<String> cleanPrefixes = new ArrayList<String>();
        if (prefixes == null) {
            return cleanPrefixes;
        }

        for (String prefix : prefixes) {
            if (prefix != null && prefix.trim().length() > 0) {
                cleanPrefixes.add(prefix.trim());
            }
        }

        return cleanPrefixes;
    }
}
