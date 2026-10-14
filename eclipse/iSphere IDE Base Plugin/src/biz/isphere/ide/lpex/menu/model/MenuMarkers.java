/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.model;

/**
 * The two markers that enclose a block of menu items in the popup menu of the
 * LPEX editor. LPEX ignores popup tokens that start with <code>MARK-</code>.
 */
public class MenuMarkers {

    private String start;
    private String end;

    public MenuMarkers(String start, String end) {
        this.start = start;
        this.end = end;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return start + " ... " + end; //$NON-NLS-1$
    }
}
