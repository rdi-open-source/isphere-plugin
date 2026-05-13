/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Team
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.dataspaceeditordesigner.model.xml;

import biz.isphere.core.dataspaceeditordesigner.model.DInteger;
import biz.isphere.core.xml.XmlParser;

public class DIntegerCallback extends AbstractDIntegerCallback<DInteger> {

    public void doStartElement(Object userData, String path, int level, XmlParser xmlParser) throws Exception {
        super.doStartElement(userData, path, level, xmlParser);
    }

    public void doEndElement(Object userData, String path, int level, String elementData, XmlParser xmlParser) throws Exception {
        super.doEndElement(userData, path, level, elementData, xmlParser);
    }
}
