/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import biz.isphere.base.internal.BooleanHelper;
import biz.isphere.base.internal.IntHelper;
import biz.isphere.core.search.GenericSearchOption;
import biz.isphere.core.search.MatchOption;

public abstract class AbstractBaseXmlHelper {

    private static final String CUSTOM_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss.S";

    private static SimpleDateFormat timestampFormat = new SimpleDateFormat(CUSTOM_FORMAT_STRING);

    protected static XMLEventReader createXMLEventReader(File file) throws FileNotFoundException, XMLStreamException {

        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = inputFactory.createXMLEventReader(new FileInputStream(file));

        return eventReader;
    }

    public static boolean isStartElement(XMLEvent event) {
        return event.isStartElement();
    }

    public static boolean isEndElement(XMLEvent event) {
        return event.isEndElement();
    }

    protected static String getElementName(XMLEvent event) {

        if (isStartElement(event)) {
            return event.asStartElement().getName().getLocalPart();
        } else if (isEndElement(event)) {
            return event.asEndElement().getName().getLocalPart();
        } else {
            throw new IllegalArgumentException("XML event is not an end element event.");
        }
    }

    public static XmlAttributes getElementAttributes(XMLEvent event) {

        if (isStartElement(event)) {

            HashMap<String, XmlAttribute> attributes = new HashMap<String, XmlAttribute>();

            @SuppressWarnings("unchecked")
            Iterator<Attribute> it = event.asStartElement().getAttributes();
            while (it.hasNext()) {
                Attribute attribute = it.next();
                String name = attribute.getName().getLocalPart();
                String value = attribute.getValue();
                attributes.put(name, new XmlAttribute(name, value));
            }

            XmlAttributes xmlAttributes = new XmlAttributes(attributes);

            return xmlAttributes;
        } else {
            throw new IllegalArgumentException("XML event is not a start element event.");
        }
    }

    /*
     * XML parser helper procedures
     */

    protected static String xmlToString(String xml) {
        return xml;
    }

    protected static int xmlToInteger(String xml) {
        return IntHelper.tryParseInt(xml, 0);
    }

    protected static boolean xmlToBoolean(String value) {
        return xmlToBoolean(value, false);
    }

    protected static boolean xmlToBoolean(String value, boolean defaultValue) {
        return BooleanHelper.tryParseBoolean(value, defaultValue);
    }

    public static Timestamp xmlToTimestamp(String timestamp) throws ParseException {
        return new Timestamp(timestampFormat.parse(timestamp).getTime());
    }

    public static MatchOption xmlToMatchOption(String matchOption) throws ParseException {
        return MatchOption.valueOf(matchOption);
    }

    public static GenericSearchOption.Key xmlToGenericSearchOption_Key(String key) throws ParseException {
        return GenericSearchOption.Key.valueOf(key);
    }

    protected static void startElementCharacters(StringBuilder elementData, XMLEvent event) {
        clearElementCharacters(elementData);
    }

    protected static void collectElementCharacters(StringBuilder elementData, XMLEvent event) {

        if (event.isCharacters()) {
            elementData.append(event.asCharacters().getData());
        }
    }

    protected static void clearElementCharacters(StringBuilder elementData) {
        elementData.replace(0, elementData.length(), ""); //$NON-NLS-1$
    }

    /*
     * XML writer helper procedures
     */

    protected static String objectToXml(Object value) {

        if (value instanceof String) {
            return stringToXml((String)value);
        } else if (value instanceof Integer) {
            return integerToXml((Integer)value);
        } else if (value instanceof Boolean) {
            return booleanToXml((Boolean)value);
        } else if (value instanceof Timestamp) {
            return timestampToXml((Timestamp)value);
        } else if (value instanceof MatchOption) {
            return matchOptionToXml((MatchOption)value);
        } else {
            throw new IllegalArgumentException("Unexpected value of parameter 'value': " + value.getClass().getName());
        }
    }

    protected static String stringToXml(String value) {
        return value;
    }

    protected static String integerToXml(int value) {
        return Integer.toString(value);
    }

    protected static String booleanToXml(boolean value) {
        return Boolean.toString(value);
    }

    public static String timestampToXml(Timestamp timestamp) {
        return timestampFormat.format(timestamp);
    }

    public static String matchOptionToXml(MatchOption matchOption) {
        return matchOption.name();
    }

    protected static XMLPrettyPrintWriter createXMLStreamWriter(FileOutputStream fileOutputStream) throws FileNotFoundException, XMLStreamException {

        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(fileOutputStream);

        return new XMLPrettyPrintWriter(streamWriter);
    }

    protected static void createNode(XMLPrettyPrintWriter eventWriter, String name, int value) throws XMLStreamException {
        createNode(eventWriter, name, integerToXml(value));
    }

    protected static void createNode(XMLPrettyPrintWriter eventWriter, String name, boolean value) throws XMLStreamException {
        createNode(eventWriter, name, booleanToXml(value));
    }

    protected static void createNode(XMLPrettyPrintWriter eventWriter, String name, String value) throws XMLStreamException {

        eventWriter.writeStartElement(name);
        eventWriter.writeCharacters(stringToXml(value));
        eventWriter.writeEndElement();
    }

    protected static void createNode(XMLPrettyPrintWriter eventWriter, String name, String value, Map<String, String> attributes)
        throws XMLStreamException {

        eventWriter.writeStartElement(name);
        eventWriter.writeElementAttributes(attributes);
        eventWriter.writeCharacters(stringToXml(value));
        eventWriter.writeEndElement();
    }

    protected static void createNode(XMLPrettyPrintWriter eventWriter, String name, Timestamp value) throws XMLStreamException {

        eventWriter.writeStartElement(name);
        eventWriter.writeCharacters(timestampToXml(value));
        eventWriter.writeEndElement();
    }

    protected static void createNode(XMLPrettyPrintWriter eventWriter, String name, MatchOption value) throws XMLStreamException {

        eventWriter.writeStartElement(name);
        eventWriter.writeCharacters(matchOptionToXml(value));
        eventWriter.writeEndElement();
    }
}
