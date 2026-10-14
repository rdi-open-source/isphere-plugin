/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.logic;

import java.util.Set;

import biz.isphere.ide.lpex.menu.model.MenuMarkers;

/**
 * Configuration of a menu extension, as needed by
 * {@link MenuExtensionLifecycle}.
 */
public interface IMenuExtensionConfiguration {

    /**
     * Returns the name of the extension (the mark ID) for messages.
     */
    public String getName();

    /**
     * Returns the class prefixes that identify own user actions and user
     * commands.
     */
    public String[] getOwnedClassPrefixes();

    /**
     * Returns the action ID prefixes that identify own key and mouse actions.
     */
    public String[] getOwnedActionIdPrefixes();

    /**
     * Returns all entries of the configuration, including the entries of
     * disabled functions.
     */
    public UserEntries getConfiguredEntries();

    /**
     * Returns the IDs of the actions that the extension defines in each view.
     */
    public Set<String> getEnabledActionIds();

    /**
     * Returns the current and the former markers of the menu blocks.
     */
    public MenuMarkers[] getMenuMarkers();
}
