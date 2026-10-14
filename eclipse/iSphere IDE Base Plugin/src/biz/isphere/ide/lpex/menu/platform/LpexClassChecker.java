/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.platform;

import com.ibm.lpex.core.LpexAction;
import com.ibm.lpex.core.LpexCommand;

import biz.isphere.ide.lpex.menu.logic.IClassChecker;

/**
 * Checks classes with the class loader of the plug-in that owns the menu
 * extension.
 */
public class LpexClassChecker implements IClassChecker {

    private final ClassLoader classLoader;

    public LpexClassChecker(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public boolean isAction(String className) {
        return isInstantiable(className, LpexAction.class);
    }

    public boolean isCommand(String className) {
        return isInstantiable(className, LpexCommand.class);
    }

    private boolean isInstantiable(String className, Class<?> type) {

        try {

            Class<?> clazz = Class.forName(className, false, classLoader);
            if (!type.isAssignableFrom(clazz)) {
                return false;
            }

            clazz.getConstructor();

            return true;

        } catch (Throwable e) {
            return false;
        }
    }
}
