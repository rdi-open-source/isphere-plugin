/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Team
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.dataspaceeditordesigner.model.xml;

import java.util.HashMap;
import java.util.Map;

import biz.isphere.core.dataspaceeditordesigner.model.DBoolean;
import biz.isphere.core.dataspaceeditordesigner.model.DComment;
import biz.isphere.core.dataspaceeditordesigner.model.DDecimal;
import biz.isphere.core.dataspaceeditordesigner.model.DEditor;
import biz.isphere.core.dataspaceeditordesigner.model.DInteger;
import biz.isphere.core.dataspaceeditordesigner.model.DLongInteger;
import biz.isphere.core.dataspaceeditordesigner.model.DReferencedObject;
import biz.isphere.core.dataspaceeditordesigner.model.DShortInteger;
import biz.isphere.core.dataspaceeditordesigner.model.DText;
import biz.isphere.core.dataspaceeditordesigner.model.DTinyInteger;
import biz.isphere.core.xml.AbstractCallbackHandler;
import biz.isphere.core.xml.XmlParser;

public class DEditorCallback extends AbstractCallbackHandler<DEditor> {

    private Map<String, String> referencedByMap;

    public void doStartElement(Object userData, String path, int level, XmlParser xmlParser) throws Exception {

        if ("/widgets/entry/boolean".equals(path)) {
            DBoolean dBoolean = new DBoolean();
            getUserData().addWidget(dBoolean);
            pushCallbackHandler(new DBooleanCallback(), dBoolean);
        } else if ("/widgets/entry/longInt".equals(path)) {
            DLongInteger dLongInteger = new DLongInteger();
            getUserData().addWidget(dLongInteger);
            pushCallbackHandler(new DLongIntegerCallback(), dLongInteger);
        } else if ("/widgets/entry/integer".equals(path)) {
            DInteger dInteger = new DInteger();
            getUserData().addWidget(dInteger);
            pushCallbackHandler(new DIntegerCallback(), dInteger);
        } else if ("/widgets/entry/shortInt".equals(path)) {
            DShortInteger dShortInteger = new DShortInteger();
            getUserData().addWidget(dShortInteger);
            pushCallbackHandler(new DShortIntegerCallback(), dShortInteger);
        } else if ("/widgets/entry/tinyInt".equals(path)) {
            DTinyInteger dShortInteger = new DTinyInteger();
            getUserData().addWidget(dShortInteger);
            pushCallbackHandler(new DTinyIntegerCallback(), dShortInteger);
        } else if ("/widgets/entry/text".equals(path)) {
            DText dText = new DText();
            getUserData().addWidget(dText);
            pushCallbackHandler(new DTextCallback(), dText);
        } else if ("/widgets/entry/decimal".equals(path)) {
            DDecimal dDecimal = new DDecimal();
            getUserData().addWidget(dDecimal);
            pushCallbackHandler(new DDecimalCallback(), dDecimal);
        } else if ("/widgets/entry/biz.isphere.core.dataspaceeditordesigner.model.DComment".equals(path)) {
            DComment dComment = new DComment();
            getUserData().addWidget(dComment);
            pushCallbackHandler(new DCommentCallback(), dComment);
        } else if ("/referencedBy/entry".equals(path)) {
            referencedByMap = new HashMap<String, String>();
            pushCallbackHandler(new DReferencedObjectCallback(), referencedByMap);
        }
    }

    public void doEndElement(Object userData, String path, int level, String elementData, XmlParser xmlParser) throws Exception {

        if ("/name".equals(path)) {
            getUserData().setName(xmlToString(elementData));
        } else if ("/description".equals(path)) {
            getUserData().setDescription(xmlToString(elementData));
        } else if ("/columns".equals(path)) {
            getUserData().setColumns(xmlToInteger(elementData));
        } else if ("/key".equals(path)) {
            getUserData().setKey(xmlToString(elementData));
        } else if ("/columnsEqualWidth".equals(path)) {
            getUserData().setColumnsEqualWidth(xmlToBoolean(elementData));
        } else if ("/referencedBy/entry".equals(path)) {
            String name = referencedByMap.get("name");
            String library = referencedByMap.get("library");
            String type = referencedByMap.get("type");
            String isDefault = referencedByMap.get("isDefault");
            DReferencedObject dReferencedObject = new DReferencedObject();
            dReferencedObject.setName(xmlToString(name));
            dReferencedObject.setLibrary(xmlToString(library));
            dReferencedObject.setType(xmlToString(type));
            dReferencedObject.setIsDefault(xmlToBoolean(isDefault));
            getUserData().addReferencedByObject(dReferencedObject);
        }
    }
}
