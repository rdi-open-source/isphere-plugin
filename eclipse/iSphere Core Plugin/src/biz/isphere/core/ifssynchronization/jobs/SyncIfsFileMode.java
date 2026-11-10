/*******************************************************************************
 * Copyright (c) 2012-2024 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization.jobs;

/**
 * This class defines the possible mode values of the SYNCIFS service program.
 */
public enum SyncIfsFileMode {
    LEFT_SYSTEM ("*LEFT"), //$NON-NLS-1$
    RIGHT_SYSTEM ("*RIGHT"); //$NON-NLS-1$

    private String mode;

    private SyncIfsFileMode(String mode) {
        this.mode = mode;
    }

    public String mode() {
        return mode;
    }
}
