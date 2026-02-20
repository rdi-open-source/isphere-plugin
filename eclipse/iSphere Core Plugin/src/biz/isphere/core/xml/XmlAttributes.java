/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.xml;

import java.util.HashMap;
import java.util.Map;

public class XmlAttributes extends AbstractBaseXmlHelper {

    private Map<String, XmlAttribute> attributes;

    public XmlAttributes() {
        this.attributes = new HashMap<String, XmlAttribute>();
    }

    public XmlAttributes(Map<String, XmlAttribute> attributes) {
        this.attributes = attributes;
    }

    public void putAttribute(XmlAttribute attribute) {
        attributes.put(attribute.getKey(), attribute);
    }

    public void putAttribute(String key, Object value) {
        attributes.put(key, new XmlAttribute(key, objectToXml(value)));
    }

    public XmlAttribute getAttribute(String name) {

        if (attributes == null) {
            return null;
        } else {
            return attributes.get(name);
        }
    }

    public void setAttributes(Map<String, XmlAttribute> attributes) {
        this.attributes = attributes;
    }

    public Map<String, String> getAttributes() {

        Map<String, String> attributesMap = new HashMap<String, String>();

        for (XmlAttribute attribute : attributes.values()) {
            attributesMap.put(attribute.getKey(), attribute.getValue());
        }

        return attributesMap;
    }

    @Override
    public String toString() {
        return getAttributes().toString();
    }
}
