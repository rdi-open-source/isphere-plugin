/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.xmlparser.internal;

import biz.isphere.core.xml.ICallbackHandler;

public class CallbackHandlerEntry {

    private ElementStackEntry elementStackEntry;
    private ICallbackHandler callbackHandler;
    private Object userData;

    public CallbackHandlerEntry(ElementStackEntry elementStackEntry, ICallbackHandler callbackHandler, Object userData) {
        this.elementStackEntry = elementStackEntry;
        this.callbackHandler = callbackHandler;
        this.userData = userData;
    }

    public int getLevel() {
        return elementStackEntry.getLevel();
    }

    public int getRelativeLevel(ElementStackEntry elementStackEntry) {
        return elementStackEntry.getLevel() - this.elementStackEntry.getLevel();
    }

    public String getRelativeXmlPath(ElementStackEntry elementStackEntry) {
        int offset = this.elementStackEntry.getXmlPath().length();
        String relativeXmlPath = elementStackEntry.getXmlPath().substring(offset);
        if (relativeXmlPath.length() == 0) {
            return ElementStack.XML_PATH_DELIMITER;
        }
        return relativeXmlPath;
    }

    public ICallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    public Object getUserData() {
        return userData;
    }
}
