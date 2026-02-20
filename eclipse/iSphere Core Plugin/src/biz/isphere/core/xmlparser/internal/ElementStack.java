/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.xmlparser.internal;

import java.util.Stack;

import javax.xml.stream.events.XMLEvent;

import biz.isphere.core.xml.XmlAttributes;
import biz.isphere.core.xml.AbstractBaseXmlHelper;

public class ElementStack extends AbstractBaseXmlHelper {

    public static final String XML_PATH_DELIMITER = "/";
    private Stack<ElementStackEntry> elementStack;

    public ElementStack() {
        this.elementStack = new Stack<ElementStackEntry>();
    }

    public ElementStackEntry pushEntry(XMLEvent event) {

        String newXmlPath = getXmlPath() + XML_PATH_DELIMITER + getElementName(event);
        int newLevel = getLevel() + 1;
        XmlAttributes attributes = getElementAttributes(event);

        ElementStackEntry elementStackEntry = new ElementStackEntry(newXmlPath, newLevel, attributes);
        elementStack.push(elementStackEntry);

        return elementStackEntry;
    }

    public void popEntry() {
        elementStack.pop();
    }

    public ElementStackEntry peekEntry() {
        return elementStack.peek();
    }

    public String getXmlPath() {
        if (elementStack.isEmpty()) {
            return "";
        }
        return elementStack.peek().getXmlPath();
    }

    public int getLevel() {
        if (elementStack.isEmpty()) {
            return 0;
        }
        return elementStack.peek().getLevel();
    }
}
