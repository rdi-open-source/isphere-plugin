/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.view;

import biz.isphere.ide.lpex.menu.model.UserAction;
import biz.isphere.ide.lpex.menu.model.UserCommand;
import biz.isphere.ide.lpex.menu.model.UserKeyAction;
import biz.isphere.ide.lpex.menu.model.UserMouseAction;

/**
 * Supplies the extensions that a {@link ViewExtender} applies to each LPEX
 * view. Nothing of it is stored in the LPEX preferences.
 */
public interface IViewExtension {

    /**
     * Returns a name that identifies the extension in log messages.
     */
    public String getName();

    /**
     * Returns the symbolic name of the bundle that owns the extension, for log
     * messages.
     */
    public String getBundleName();

    /**
     * Returns the class loader that loads the classes of the actions and
     * commands.
     */
    public ClassLoader getClassLoader();

    /**
     * Returns the actions that are defined in each view.
     */
    public UserAction[] getViewUserActions();

    /**
     * Returns the commands that are defined in each view.
     */
    public UserCommand[] getViewUserCommands();

    /**
     * Returns the key actions that are defined in each view.
     */
    public UserKeyAction[] getViewUserKeyActions();

    /**
     * Returns the mouse actions that are defined in each view.
     */
    public UserMouseAction[] getViewUserMouseActions();

    /**
     * Returns the popup menu with the menu items of the extension added.
     * Returns the specified popup menu unchanged, if it already contains the
     * menu items.
     */
    public String addViewPopupMenuItems(String popupMenu);
}
