/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.platform;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import biz.isphere.ide.lpex.menu.logic.IPreferencesNode;

/**
 * Preference node of a bundle in the instance scope (workspace).
 */
public class EclipsePreferencesNode implements IPreferencesNode {

    private final IEclipsePreferences node;

    public EclipsePreferencesNode(String bundleSymbolicName) {
        this.node = InstanceScope.INSTANCE.getNode(bundleSymbolicName);
    }

    public String get(String key) {
        return node.get(key, null);
    }

    public void put(String key, String value) {
        node.put(key, value);
    }

    public void remove(String key) {
        node.remove(key);
    }

    public void flush() throws Exception {
        node.flush();
    }
}
