/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.search.xml;

import java.util.Map;

import biz.isphere.core.search.GenericSearchOption;
import biz.isphere.core.xml.AbstractCallbackHandler;
import biz.isphere.core.xml.XmlParser;

public class GenericSearchOptionCallback extends AbstractCallbackHandler<Map<String, String>> {

    public void doStartElement(Object userData, String path, int level, XmlParser xmlParser) throws Exception {
    }

    public void doEndElement(Object userData, String path, int level, String elementData, XmlParser xmlParser) throws Exception {

        if ("/biz.isphere.core.search.GenericSearchOption/key".equals(path)) {
            GenericSearchOption.Key key = GenericSearchOption.Key.valueOf(xmlToString(elementData));
            getUserData().put("key", xmlToString(elementData));
        } else if ("/biz.isphere.core.search.GenericSearchOption/value".equals(path)) {
            String clazz = getXmlAttribute("class").getValue();
            getUserData().put("attr_class", clazz);
            getUserData().put("value", elementData);
        }
    }
}
