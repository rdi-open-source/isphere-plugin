/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Team
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.rse.resourcemanagement;

import java.util.StringTokenizer;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import biz.isphere.core.resourcemanagement.XmlVersion;
import biz.isphere.core.xml.AbstractBaseXmlHelper;
import biz.isphere.core.xml.XMLPrettyPrintWriter;

public abstract class AbstractXmlHelper extends AbstractBaseXmlHelper {

    protected static final String CONTAINER = "container";

    private static final String ARRAY_DEFAULT_DELIMITER = ";";
    private static final String ARRAY_DELIMITERS = " ,\t\n\r\f" + ARRAY_DEFAULT_DELIMITER;

    protected static boolean isContainerStartElement(XMLEvent event) {

        if (event.isStartElement()) {
            if (event.asStartElement().getName().getLocalPart().equals(CONTAINER)) {
                return true;
            }
        }

        return false;
    }

    protected static String getVersionNumber(XMLEvent event) throws Exception {

        StartElement startElement = event.asStartElement();
        Attribute versionAttribute = startElement.getAttributeByName(new QName("version"));
        if (versionAttribute != null) {
            String currentVersionNumber = versionAttribute.getValue();
            return currentVersionNumber;
        }

        return null;
    }

    protected static boolean validateVersionNumber(XMLEvent event, String minVersionNumber) throws Exception {

        StartElement startElement = event.asStartElement();
        Attribute versionAttribute = startElement.getAttributeByName(new QName("version"));
        if (versionAttribute != null) {
            String currentVersionNumber = versionAttribute.getValue();
            XmlVersion currentVersion = new XmlVersion(currentVersionNumber);
            XmlVersion minVersion = new XmlVersion(minVersionNumber);
            if (currentVersion.compareTo(minVersion) < 0) {
                return false;
            }
        }

        return true;
    }

    protected static void startContainer(XMLPrettyPrintWriter eventWriter, String version) throws XMLStreamException {

        eventWriter.writeStartElement(CONTAINER);
        eventWriter.writeAttribute("version", version);
    }

    protected static void endContainer(XMLPrettyPrintWriter eventWriter) throws XMLStreamException {

        eventWriter.writeEndElement();
    }

    protected static String arrayToXml(String[] fileTypes) {

        StringBuilder buffer = new StringBuilder();

        for (String fileType : fileTypes) {
            if (buffer.length() > 0) {
                buffer.append(ARRAY_DEFAULT_DELIMITER);
            }
            buffer.append(fileType);
        }

        return buffer.toString();
    }

    protected static String[] xmlToArray(String xml) {

        StringTokenizer st = new StringTokenizer(xml, ARRAY_DELIMITERS);

        int n = st.countTokens();
        String[] fileTypes = new String[n];
        for (int i = 0; i < n; i++) {
            fileTypes[i] = st.nextToken();
        }

        return fileTypes;
    }
}
