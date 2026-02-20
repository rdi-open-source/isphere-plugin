/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.search.xml;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import biz.isphere.core.search.GenericSearchOption;
import biz.isphere.core.xml.AbstractBaseXmlHelper;
import biz.isphere.core.xml.XMLPrettyPrintWriter;

public abstract class AbstractXmlSearchHelper extends AbstractBaseXmlHelper {

    private static final String GENERIC_OPTION_KEY = "biz.isphere.core.search.GenericSearchOption_-Key";
    private static final String GENERIC_OPTION = "biz.isphere.core.search.GenericSearchOption";

    protected static void createNode(XMLPrettyPrintWriter eventWriter, String name, GenericSearchOption genericSearchOption)
        throws XMLStreamException {

        eventWriter.writeStartElement(name);

        createNode(eventWriter, genericSearchOption);

        eventWriter.writeEndElement();
    }

    private static void createNode(XMLPrettyPrintWriter eventWriter, GenericSearchOption genericSearchOption) throws XMLStreamException {

        GenericSearchOption.Key key = GenericSearchOption.Key.findByKeyValue(genericSearchOption.getKey());

        createNode(eventWriter, GENERIC_OPTION_KEY, key.name());

        createNode(eventWriter, key, genericSearchOption);
    }

    private static void createNode(XMLPrettyPrintWriter eventWriter, GenericSearchOption.Key key, GenericSearchOption genericSearchOption)
        throws XMLStreamException {

        eventWriter.writeStartElement(GENERIC_OPTION);

        createNode(eventWriter, "key", key.name());

        Object value = genericSearchOption.getValue();

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("class", getXStreamClass(value));

        createNode(eventWriter, "value", objectToXml(value), attributes);

        eventWriter.writeEndElement();
    }

    private static String getXStreamClass(Object object) {

        Class<?> clazz = object.getClass();
        if (clazz.equals(String.class)) {
            return "string";
        } else if (clazz.equals(Integer.class)) {
            return "int";
        } else if (clazz.equals(Boolean.class)) {
            return "boolean";
        } else {
            throw new IllegalArgumentException("Unexpected object type of parameter 'object': " + object.getClass().getName());
        }
    }
}
