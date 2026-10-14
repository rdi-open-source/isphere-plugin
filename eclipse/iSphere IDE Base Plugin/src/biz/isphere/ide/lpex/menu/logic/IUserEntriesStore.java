/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.logic;

/**
 * List of the entries that the user created on actions or classes of a menu
 * extension. The entries are removed from LPEX, when the extension stops, and
 * restored, when it starts again. Each change is saved at once.
 */
public interface IUserEntriesStore {

    /**
     * Returns the remembered entries.
     */
    public UserEntries load();

    /**
     * Replaces the remembered entries.
     */
    public void replace(UserEntries entries);

    /**
     * Adds the specified entries to the remembered entries.
     */
    public void merge(UserEntries entries);
}
