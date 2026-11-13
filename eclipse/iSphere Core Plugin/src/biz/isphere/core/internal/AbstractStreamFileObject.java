/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.internal;

/**
 * This class is the common base class of the stream files and directories the
 * <i>iSphere Synchronize Stream Files</i> editor compares, for example a stream
 * file or directory of the IFS of an IBM i, see {@link RemoteStreamFile}.
 */
public abstract class AbstractStreamFileObject {

    private String name;

    protected AbstractStreamFileObject(String name) {
        this.name = name;
    }

    /**
     * @return absolute path of the stream file or directory
     */
    public String getName() {
        return name;
    }

    /**
     * @return name that identifies the stream file or directory, for example
     *         including the connection of an IFS directory
     */
    public abstract String getAbsoluteName();

    public abstract String getToolTipText();

    @Override
    public String toString() {
        return getAbsoluteName();
    }
}
