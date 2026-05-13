/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Team
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.dataspaceeditordesigner.model.xml;

import java.util.Map;

import biz.isphere.core.xml.AbstractCallbackHandler;
import biz.isphere.core.xml.XmlParser;

public class DReferencedObjectCallback<M> extends AbstractCallbackHandler<Map<String, String>> {

    public void doStartElement(Object userData, String path, int level, XmlParser xmlParser) throws Exception {
    }

    public void doEndElement(Object userData, String path, int level, String elementData, XmlParser xmlParser) throws Exception {

        if ("/referencedBy/name".equals(path)) {
            getUserData().put("name", xmlToString(elementData));
        } else if ("/referencedBy/library".equals(path)) {
            getUserData().put("library", xmlToString(elementData));
        } else if ("/referencedBy/type".equals(path)) {
            getUserData().put("type", xmlToString(elementData));
        } else if ("/referencedBy/isDefault".equals(path)) {
            getUserData().put("isDefault", xmlToString(elementData));
        }
    }
}
