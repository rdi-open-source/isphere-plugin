/*******************************************************************************
 * Copyright (c) 2012-2024 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.swt.widgets.objectselector.model;

import org.eclipse.swt.graphics.Image;

import com.ibm.as400.access.AS400;

import biz.isphere.core.internal.ISeries;

public class FileItem extends ObjectItem {

    public FileItem(AS400 system, String library, String object, Image image, String objectTypeFilter) {
        super(system, library, object, ISeries.FILE, image, objectTypeFilter);
    }

    @Override
    public String getObjectType() {
        return ISeries.FILE;
    }

    @Override
    public AbstractListItem[] resolveChildren() {
        return new AbstractListItem[0];
    }
}
