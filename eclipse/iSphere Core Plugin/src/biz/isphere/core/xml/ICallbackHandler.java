/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/
package biz.isphere.core.xml;

import javax.xml.stream.events.XMLEvent;

public interface ICallbackHandler {

    /**
     * Called for XML <start-element> events.
     * 
     * @param event - XML event.
     * @param xmlPath - Relative XML path based on the element the callback
     *        handler was registered for.
     * @param level - Relative element level based on the element the callback
     *        handler was registered for.
     * @param userData - User data passed to the callback handler on registering
     *        the handler.
     * @param attributes - XML attributes of the current XML element.
     * @param xmlParser - The XML parser processing the XML stream file.
     */
    public void performStartEvent(XMLEvent event, String xmlPath, int level, Object userData, XmlAttributes attributes, XmlParser xmlParser)
        throws Exception;

    /**
     * Called for XML <end-element> events.
     * 
     * @param event - XML event.
     * @param xmlPath - Relative XML path based on the element the callback
     *        handler was registered for.
     * @param level - Relative element level based on the element the callback
     *        handler was registered for.
     * @param elementData - String value of the current XML element.
     * @param userData - User data passed to the callback handler on registering
     *        the handler.
     * @param attributes - XML attributes of the current XML element.
     * @param xmlParser - The XML parser processing the XML stream file.
     */
    public void performEndEvent(XMLEvent event, String xmlPath, int level, String elementData, Object userData, XmlAttributes attributes,
        XmlParser xmlParser) throws Exception;
}
