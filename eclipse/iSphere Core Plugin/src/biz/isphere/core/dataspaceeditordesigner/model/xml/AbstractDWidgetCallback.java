/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Team
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.dataspaceeditordesigner.model.xml;

import biz.isphere.core.dataspaceeditordesigner.model.AbstractDWidget;
import biz.isphere.core.xml.AbstractCallbackHandler;
import biz.isphere.core.xml.XmlParser;

public abstract class AbstractDWidgetCallback<M> extends AbstractCallbackHandler<AbstractDWidget> {

    public void doStartElement(Object userData, String path, int level, XmlParser xmlParser) throws Exception {
    }

    public void doEndElement(Object userData, String path, int level, String elementData, XmlParser xmlParser) throws Exception {

        if ("/label".equals(path)) {
            getUserData().setLabel(xmlToString(elementData));
        } else if ("/offset".equals(path)) {
            getUserData().setOffset(xmlToInteger(elementData));
        } else if ("/length".equals(path)) {
            getUserData().setLength(xmlToInteger(elementData));
        } else if ("/sequence".equals(path)) {
            getUserData().setSequence(xmlToInteger(elementData));
        } else if ("/horizontalSpan".equals(path)) {
            getUserData().setHorizontalSpan(xmlToInteger(elementData));
        }
    }
}
