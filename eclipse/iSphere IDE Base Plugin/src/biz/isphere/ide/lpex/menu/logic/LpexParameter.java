/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.logic;

/**
 * LPEX parameters that a menu extension reads and changes. The name is used
 * with the prefixes <code>current.</code>, <code>default.</code> and
 * <code>install.</code>.
 */
public enum LpexParameter {

    POPUP ("popup"), //$NON-NLS-1$
    USER_ACTIONS ("updateProfile.userActions"), //$NON-NLS-1$
    USER_COMMANDS ("updateProfile.userCommands"), //$NON-NLS-1$
    USER_KEY_ACTIONS ("updateProfile.userKeyActions"), //$NON-NLS-1$
    USER_MOUSE_ACTIONS ("updateProfile.userMouseActions"); //$NON-NLS-1$

    private static final LpexParameter[] USER_PARAMETERS = { USER_ACTIONS, USER_COMMANDS, USER_KEY_ACTIONS, USER_MOUSE_ACTIONS };

    private String name;

    private LpexParameter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns <code>true</code> for the parameters that hold pairs, such as
     * <code>Name Class</code> or <code>Key Action</code>.
     */
    public boolean isUserParameter() {
        return this != POPUP;
    }

    /**
     * Returns the parameters that hold pairs, in the order in which they are
     * processed.
     */
    public static LpexParameter[] userParameters() {

        LpexParameter[] parameters = new LpexParameter[USER_PARAMETERS.length];
        System.arraycopy(USER_PARAMETERS, 0, parameters, 0, USER_PARAMETERS.length);

        return parameters;
    }
}
