/*******************************************************************************
 * Copyright (c) 2012-2020 iSphere Project Team
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.journalexplorer.core.handlers;

import biz.isphere.journalexplorer.core.externalapi.IJournaledObject;

public interface ISelectedObject extends IJournaledObject {

    /**
     * Returns the name of the host connection.
     * 
     * @return connection name
     */
    public String getConnectionName();

    /**
     * Returns the library where the object is stored.
     * 
     * @return library name
     */
    public String getLibrary();

    /**
     * Returns the name of the object.
     * 
     * @return object name
     */
    public String getName();

}
