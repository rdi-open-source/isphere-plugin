/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.logic;

/**
 * Steps of starting and stopping a menu extension, that depend on LPEX or on
 * the workbench. {@link MenuExtensionLifecycle} calls them in the right order
 * and catches their exceptions one by one.
 */
public interface IExtensionCallback {

    /**
     * Removes the own entries that were found in the LPEX settings at start:
     * entries of earlier versions and entries left by a crash.
     *
     * @param ownedEntries - own entries, determined before anything was removed
     */
    public void removeLegacyEntries(UserEntries ownedEntries);

    /**
     * Registers the extension with its plug-in, so that the plug-in stops it.
     */
    public void registerExtension();

    /**
     * Starts extending the LPEX views.
     *
     * @param restoredEntries - user entries that were restored in the LPEX
     *        settings at start. Views that are already open applied the LPEX
     *        settings before. The entries must be set in these views directly.
     */
    public void startViewExtension(UserEntries restoredEntries);

    /**
     * Registers the listener on the LPEX settings, if the extension has one.
     */
    public void addPreferencesListener();

    /**
     * Stops extending the LPEX views.
     */
    public void stopViewExtension();

    /**
     * Removes the listener on the LPEX settings.
     */
    public void removePreferencesListener();
}
