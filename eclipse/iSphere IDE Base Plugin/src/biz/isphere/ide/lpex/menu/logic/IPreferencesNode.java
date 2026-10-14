/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.logic;

/**
 * Preference node that stores the entries of a {@link UserEntriesStore}.
 */
public interface IPreferencesNode {

    /**
     * Returns the value of a key or <code>null</code>, if the key does not
     * exist.
     */
    public String get(String key);

    public void put(String key, String value);

    public void remove(String key);

    /**
     * Saves the node.
     *
     * @throws Exception if the node could not be saved
     */
    public void flush() throws Exception;
}
