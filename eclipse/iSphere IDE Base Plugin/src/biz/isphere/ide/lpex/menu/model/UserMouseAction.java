/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.model;

/**
 * User mouse action of the LPEX editor: the mouse event, e.g.
 * <code>1-pressed</code>, and the action that is run.
 */
public class UserMouseAction extends AbstractLpexAction<UserMouseAction> implements Comparable<UserMouseAction> {

    private String mouseEvent;

    public UserMouseAction(String mouseEvent, String actionId) {
        super(actionId);
        this.mouseEvent = mouseEvent;
    }

    public String getMouseEvent() {
        return mouseEvent;
    }

    @Override
    public String toString() {
        return mouseEvent + ACTION_DELIMITER + getActionId();
    }

    @Override
    public int compareTo(UserMouseAction action) {

        if (action == null || action.getMouseEvent() == null) {
            return 1;
        }

        return getMouseEvent().compareTo(action.getMouseEvent());
    }
}
