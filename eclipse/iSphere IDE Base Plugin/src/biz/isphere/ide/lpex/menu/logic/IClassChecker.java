/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.logic;

/**
 * Checks the classes of remembered user actions and user commands, before they
 * are restored.
 */
public interface IClassChecker {

    /**
     * Returns <code>true</code>, if the class can be loaded, implements
     * <code>LpexAction</code> and has a public constructor without parameters.
     */
    public boolean isAction(String className);

    /**
     * Returns <code>true</code>, if the class can be loaded, implements
     * <code>LpexCommand</code> and has a public constructor without parameters.
     */
    public boolean isCommand(String className);
}
