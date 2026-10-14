/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.platform;

import com.ibm.lpex.alef.LpexPlugin;
import com.ibm.lpex.core.LpexView;

import biz.isphere.ide.lpex.menu.logic.ILpexProfile;
import biz.isphere.ide.lpex.menu.logic.LpexParameter;

/**
 * Access to the persistent LPEX settings with
 * <code>LpexView.globalQuery()</code> and
 * <code>LpexView.doGlobalCommand()</code>.
 */
public class LpexProfile implements ILpexProfile {

    private static final String CURRENT = "current."; //$NON-NLS-1$
    private static final String INSTALL = "install."; //$NON-NLS-1$
    private static final String SET_DEFAULT = "set default."; //$NON-NLS-1$
    private static final String INSTALL_VALUE = "install"; //$NON-NLS-1$
    private static final String PROFILE_FLUSH = "profile flush"; //$NON-NLS-1$

    public boolean isAvailable() {
        // LpexPlugin.stop() resets the instance.
        return LpexPlugin.getDefault() != null;
    }

    public String getCurrent(LpexParameter parameter) {
        return LpexView.globalQuery(CURRENT + parameter.getName());
    }

    public String getInstall(LpexParameter parameter) {
        return LpexView.globalQuery(INSTALL + parameter.getName());
    }

    public void setDefault(LpexParameter parameter, String value) {
        LpexView.doGlobalCommand(SET_DEFAULT + parameter.getName() + " " + value); //$NON-NLS-1$
    }

    public void resetToInstall(LpexParameter parameter) {
        LpexView.doGlobalCommand(SET_DEFAULT + parameter.getName() + " " + INSTALL_VALUE); //$NON-NLS-1$
    }

    public void flush() {
        LpexView.doGlobalCommand(PROFILE_FLUSH);
    }
}
