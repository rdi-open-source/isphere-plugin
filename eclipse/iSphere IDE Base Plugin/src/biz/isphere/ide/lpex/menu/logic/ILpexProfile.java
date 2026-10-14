/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.logic;

/**
 * Access to the persistent LPEX settings (the LPEX profile).
 */
public interface ILpexProfile {

    /**
     * Returns <code>true</code>, if LPEX is active. Otherwise LPEX would write
     * changes into a substitute file in the home directory of the user.
     */
    public boolean isAvailable();

    /**
     * Returns the current value (<code>current.X</code>): the default value, if
     * set, otherwise the install value. May be <code>null</code>.
     */
    public String getCurrent(LpexParameter parameter);

    /**
     * Returns the install value (<code>install.X</code>). May be
     * <code>null</code>.
     */
    public String getInstall(LpexParameter parameter);

    /**
     * Sets the default value (<code>set default.X value</code>). LPEX stores it
     * in its preference store.
     */
    public void setDefault(LpexParameter parameter, String value);

    /**
     * Resets the default value to the install value
     * (<code>set default.X install</code>).
     */
    public void resetToInstall(LpexParameter parameter);

    /**
     * Saves the preference store of LPEX (<code>profile flush</code>).
     */
    public void flush();
}
