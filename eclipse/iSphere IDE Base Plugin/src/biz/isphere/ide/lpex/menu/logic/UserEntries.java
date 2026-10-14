/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Entries of the four LPEX user parameters: user actions, user commands, user
 * key actions and user mouse actions. The order of the entries is kept and each
 * entry is contained only once.
 */
public final class UserEntries {

    private final Map<LpexParameter, List<Pair>> entries;

    public UserEntries() {

        entries = new LinkedHashMap<LpexParameter, List<Pair>>();
        for (LpexParameter parameter : LpexParameter.userParameters()) {
            entries.put(parameter, new ArrayList<Pair>());
        }
    }

    public List<Pair> get(LpexParameter parameter) {
        return Collections.unmodifiableList(getList(parameter));
    }

    public void add(LpexParameter parameter, Pair pair) {

        List<Pair> list = getList(parameter);
        if (!list.contains(pair)) {
            list.add(pair);
        }
    }

    public void add(LpexParameter parameter, String first, String second) {
        add(parameter, new Pair(first, second));
    }

    public void addAll(LpexParameter parameter, List<Pair> pairs) {
        for (Pair pair : pairs) {
            add(parameter, pair);
        }
    }

    public void addAll(UserEntries other) {
        for (LpexParameter parameter : LpexParameter.userParameters()) {
            addAll(parameter, other.get(parameter));
        }
    }

    public boolean contains(LpexParameter parameter, Pair pair) {
        return getList(parameter).contains(pair);
    }

    public boolean isEmpty() {

        for (List<Pair> list : entries.values()) {
            if (!list.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public int size() {

        int size = 0;
        for (List<Pair> list : entries.values()) {
            size = size + list.size();
        }

        return size;
    }

    /**
     * Returns the entries that are not contained in the other entries.
     */
    public UserEntries minus(UserEntries other) {

        UserEntries result = new UserEntries();
        for (LpexParameter parameter : LpexParameter.userParameters()) {
            for (Pair pair : get(parameter)) {
                if (!other.contains(parameter, pair)) {
                    result.add(parameter, pair);
                }
            }
        }

        return result;
    }

    private List<Pair> getList(LpexParameter parameter) {

        List<Pair> list = entries.get(parameter);
        if (list == null) {
            throw new IllegalArgumentException("Not a user parameter: " + parameter); //$NON-NLS-1$
        }

        return list;
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }

        if (!(object instanceof UserEntries)) {
            return false;
        }

        return entries.equals(((UserEntries)object).entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    @Override
    public String toString() {

        StringBuilder buffer = new StringBuilder();
        for (LpexParameter parameter : LpexParameter.userParameters()) {
            List<Pair> list = getList(parameter);
            if (!list.isEmpty()) {
                if (buffer.length() > 0) {
                    buffer.append(", "); //$NON-NLS-1$
                }
                buffer.append(parameter.getName());
                buffer.append("="); //$NON-NLS-1$
                buffer.append(list);
            }
        }

        return "[" + buffer.toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
