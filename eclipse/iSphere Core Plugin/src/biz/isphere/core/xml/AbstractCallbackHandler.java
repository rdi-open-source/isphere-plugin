/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.xml;

import javax.xml.stream.events.XMLEvent;

public abstract class AbstractCallbackHandler<M> extends AbstractBaseXmlHelper implements ICallbackHandler {

    private Object userData;
    private XMLEvent event;
    private XmlParser xmlParser;
    private XmlAttributes attributes;

    protected void printLog(String prefix, Object userData, String path, int level, XMLEvent event) {
        System.out
            .println(getClass().getSimpleName() + " ==> " + prefix + "-Element: " + getElementName(event) + ", Path: " + path + ", Level: " + level);
    }

    public XmlAttribute getXmlAttribute(String name) {
        return attributes.getAttribute(name);
    }

    public void performStartEvent(XMLEvent event, String xmlPath, int level, Object userData, XmlAttributes attributes, XmlParser xmlParser)
        throws Exception {

        this.userData = userData;
        this.event = event;
        this.xmlParser = xmlParser;
        this.attributes = attributes;

        doStartElement(userData, xmlPath, level, xmlParser);
    }

    public void performEndEvent(XMLEvent event, String xmlPath, int level, String elementData, Object userData, XmlAttributes attributes,
        XmlParser xmlParser) throws Exception {

        this.userData = userData;
        this.event = event;
        this.xmlParser = xmlParser;
        this.attributes = attributes;

        doEndElement(userData, xmlPath, level, elementData, xmlParser);
    }

    protected void doStartElement(Object userData, String xmlPath, int level, XmlParser xmlParser) throws Exception {
        printLog("Start", event, xmlPath, level, event);
    }

    protected void doEndElement(Object userData, String xmlPath, int level, String elementData, XmlParser xmlParser) throws Exception {
        printLog("End", event, xmlPath, level, event);
    }

    @SuppressWarnings("unchecked")
    protected M getUserData() {
        return (M)userData;
    }

    protected void pushCallbackHandler(ICallbackHandler callbackHandler, Object userData) throws XmlParserException {
        xmlParser.pushCallbackHandler(event, callbackHandler, userData);
    }
}
