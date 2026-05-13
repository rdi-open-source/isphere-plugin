/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.xml;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import biz.isphere.core.xmlparser.internal.CallbackHandlerEntry;
import biz.isphere.core.xmlparser.internal.CallbackHandlerStack;
import biz.isphere.core.xmlparser.internal.ElementStack;
import biz.isphere.core.xmlparser.internal.ElementStackEntry;

public class XmlParser extends AbstractBaseXmlHelper {

    private CallbackHandlerStack callbackHandlerStack;
    private ElementStack elementStack;
    private StringBuilder elementData;
    private Object responseValue;

    /**
     * Produces a new XML SAX parser.
     */
    public XmlParser() {
        callbackHandlerStack = new CallbackHandlerStack();
        elementStack = new ElementStack();
        elementData = new StringBuilder();
    }

    /**
     * @param path - path to the XML stream file.
     * @param callbackHandler - callback handler that is called for start and
     *        end element events.
     * @param userData - user data that is passed to the callback handler and
     *        that is populated with the XML data.
     * @return user data populated with the XML data
     * @throws FileNotFoundException
     * @throws XMLStreamException
     */
    public Object parse(File file, ICallbackHandler callbackHandler, Object userData)
        throws FileNotFoundException, XMLStreamException, XmlParserException {

        ElementStackEntry elementStackEntry = new ElementStackEntry("", 1, null);
        callbackHandlerStack.pushEntry(new CallbackHandlerEntry(elementStackEntry, callbackHandler, userData));

        XMLEventReader eventReader = null;

        try {

            eventReader = createXMLEventReader(file);
            while (eventReader.hasNext()) {

                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    startElement(event);
                }
                if (event.isEndElement()) {
                    endElement(event);
                }
                if (event.isCharacters()) {
                    collectElementCharacters(elementData, event);
                }
            }

        } finally {
            if (eventReader != null) {
                eventReader.close();
            }
        }

        return userData;
    }

    /**
     * Pushes control to a new callback handler. Typically a new callback
     * handler is pushed if the parser comes to a complex element.
     * 
     * @param event - XML parser event.
     * @param callbackHandler - new callback handler.
     * @param userData - user data that is passed to the new callback handler.
     */
    public void pushCallbackHandler(XMLEvent event, ICallbackHandler callbackHandler, Object userData) throws XmlParserException {

        if (!event.isStartElement()) {
            throw new IllegalArgumentException("Event is not a XML start element.");
        }

        try {

            ElementStackEntry elementStackEntry = elementStack.peekEntry();
            CallbackHandlerEntry callbackHandlerEntry = new CallbackHandlerEntry(elementStackEntry, callbackHandler, userData);
            callbackHandlerStack.pushEntry(callbackHandlerEntry);

            callbackHandler.performStartEvent(event, callbackHandlerEntry.getRelativeXmlPath(elementStackEntry),
                callbackHandlerEntry.getRelativeLevel(elementStackEntry), callbackHandlerEntry.getUserData(), elementStackEntry.getAttributes(),
                this);

        } catch (Exception e) {
            throw new XmlParserException("Error in start-element callback: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Pops the current callback handler from the handler stack.
     * 
     * @param event - XML parser event.
     * @param elementStackEntry - current element stack entry
     * @param callbackHandlerEntry - current callback handler entry
     */
    private void popCallbackHandler(XMLEvent event, ElementStackEntry elementStackEntry, CallbackHandlerEntry callbackHandlerEntry)
        throws XmlParserException {

        if (!event.isEndElement()) {
            throw new IllegalArgumentException("Event is not a XML end element.");
        }

        try {

            if (callbackHandlerEntry.getLevel() == elementStackEntry.getLevel()) {
                callbackHandlerStack.popEntry(); // getElementName(event)

                callbackHandlerEntry = callbackHandlerStack.peekEntry();
                if (callbackHandlerEntry != null) {
                    ICallbackHandler callbackHandler = callbackHandlerEntry.getCallbackHandler();
                    callbackHandler.performEndEvent(event, callbackHandlerEntry.getRelativeXmlPath(elementStackEntry),
                        callbackHandlerEntry.getRelativeLevel(elementStackEntry), getElementData(event), callbackHandlerEntry.getUserData(),
                        elementStackEntry.getAttributes(), this);
                }
            }

        } catch (Exception e) {
            throw new XmlParserException("Error in start-element callback: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Called on a start element event.
     * 
     * @param event - XML parser event.
     */
    private void startElement(XMLEvent event) throws XmlParserException {

        try {

            ElementStackEntry elementStackEntry = elementStack.pushEntry(event);

            CallbackHandlerEntry callbackHandlerEntry = callbackHandlerStack.peekEntry();
            if (callbackHandlerEntry != null) {
                ICallbackHandler callbackHandler = callbackHandlerEntry.getCallbackHandler();
                callbackHandler.performStartEvent(event, callbackHandlerEntry.getRelativeXmlPath(elementStackEntry),
                    callbackHandlerEntry.getRelativeLevel(elementStackEntry), callbackHandlerEntry.getUserData(), elementStackEntry.getAttributes(),
                    this);
            }

            startElementCharacters(elementData, null);

        } catch (Exception e) {
            throw new XmlParserException("Error in start-element callback: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Called on an end element event.
     * 
     * @param event - XML parser event.
     */
    private void endElement(XMLEvent event) throws XmlParserException {

        try {

            ElementStackEntry elementStackEntry = elementStack.peekEntry();

            CallbackHandlerEntry callbackHandlerEntry = callbackHandlerStack.peekEntry();
            if (callbackHandlerEntry != null) {
                ICallbackHandler callbackHandler = callbackHandlerEntry.getCallbackHandler();
                callbackHandler.performEndEvent(event, callbackHandlerEntry.getRelativeXmlPath(elementStackEntry),
                    callbackHandlerEntry.getRelativeLevel(elementStackEntry), getElementData(event), callbackHandlerEntry.getUserData(),
                    elementStackEntry.getAttributes(), this);
            }

            popCallbackHandler(event, elementStackEntry, callbackHandlerEntry);

            elementStack.popEntry();

            clearElementCharacters(elementData);

        } catch (Exception e) {
            throw new XmlParserException("Error in start-element callback: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Returns the element data that has been collected for the event.
     * 
     * @return element data
     */
    private String getElementData(XMLEvent event) {
        if (isEndElement(event)) {
            return elementData.toString();
        } else {
            throw new IllegalArgumentException("Event is not a XML end element.");
        }
    }
}
