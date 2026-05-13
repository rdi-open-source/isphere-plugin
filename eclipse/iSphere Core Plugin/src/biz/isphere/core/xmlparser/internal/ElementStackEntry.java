/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.xmlparser.internal;

import biz.isphere.core.xml.XmlAttributes;

public class ElementStackEntry {

    private String xmlPath;
    private int level;
    private XmlAttributes attributes;

    public ElementStackEntry(String xmlPath, int level, XmlAttributes attributes) {
        this.xmlPath = xmlPath;
        this.level = level;
        this.attributes = attributes;
    }

    public String getXmlPath() {
        return xmlPath;
    }

    public int getLevel() {
        return level;
    }

    public XmlAttributes getAttributes() {
        return attributes;
    }
}
