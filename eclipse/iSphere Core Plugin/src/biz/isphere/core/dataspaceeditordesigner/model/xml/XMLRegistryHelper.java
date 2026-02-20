/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Team
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.dataspaceeditordesigner.model.xml;

import java.io.File;
import java.io.FileOutputStream;

import javax.xml.stream.XMLStreamException;

import biz.isphere.core.dataspaceeditordesigner.model.AbstractDWidget;
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
import biz.isphere.core.search.xml.AbstractXmlSearchHelper;
import biz.isphere.core.xml.XMLPrettyPrintWriter;

public class XMLRegistryHelper extends AbstractXmlSearchHelper {

    private static final String DIALOG = "dialog";
    private static final String WIDGETS = "widgets";
    private static final String WIDGET = "entry";
    private static final String REFERENCED_BY_LIST = "referencedBy";
    private static final String REFERENCED_BY = "entry";
    private static final String REFERENCED_BY_ITEM = "referencedBy";

    public static void saveRegistryToXML(File toFile, DEditor dEditor) throws Exception {

        FileOutputStream fileOutputStream = new FileOutputStream(toFile);

        XMLPrettyPrintWriter eventWriter = createXMLStreamWriter(fileOutputStream);

        eventWriter.writeStartDocument();

        writeDEditor(eventWriter, dEditor);

        eventWriter.writeEndDocument();

        eventWriter.flush();
        eventWriter.close();

        fileOutputStream.close();
    }

    private static void writeDEditor(XMLPrettyPrintWriter eventWriter, DEditor dEditor) throws XMLStreamException {

        eventWriter.writeStartElement(DIALOG);

        createNode(eventWriter, "name", dEditor.getName());
        createNode(eventWriter, "description", dEditor.getDescription());

        writeDWidgets(eventWriter, dEditor.getWidgets());
        writeDReferencedBy(eventWriter, dEditor.getReferencedObjects());

        createNode(eventWriter, "columns", dEditor.getColumns());
        createNode(eventWriter, "key", dEditor.getKey());
        createNode(eventWriter, "columnsEqualWidth", dEditor.isColumnsEqualWidth());

        eventWriter.writeEndElement();
    }

    private static void writeDWidgets(XMLPrettyPrintWriter eventWriter, AbstractDWidget[] widgets) throws XMLStreamException {

        eventWriter.writeStartElement(WIDGETS);

        for (AbstractDWidget dWidget : widgets) {
            writeDWidget(eventWriter, dWidget);
        }

        eventWriter.writeEndElement();
    }

    private static void writeDReferencedBy(XMLPrettyPrintWriter eventWriter, DReferencedObject[] dReferencedObjects) throws XMLStreamException {

        eventWriter.writeStartElement(REFERENCED_BY_LIST);

        for (DReferencedObject dReferencedObject : dReferencedObjects) {
            writeDReferencedBy(eventWriter, dReferencedObject);
        }

        eventWriter.writeEndElement();
    }

    private static void writeDWidget(XMLPrettyPrintWriter eventWriter, AbstractDWidget abstractDWidget) throws XMLStreamException {

        eventWriter.writeStartElement(WIDGET);

        if (abstractDWidget instanceof DBoolean) {
            writeAbstractWidget(eventWriter, abstractDWidget, "boolean");
        } else if (abstractDWidget instanceof DComment) {
            writeAbstractWidget(eventWriter, abstractDWidget, "biz.isphere.core.dataspaceeditordesigner.model.DComment");
        } else if (abstractDWidget instanceof DDecimal) {
            writeAbstractWidget(eventWriter, abstractDWidget, "decimal");
        } else if (abstractDWidget instanceof DText) {
            writeAbstractWidget(eventWriter, abstractDWidget, "text");
        } else if (abstractDWidget instanceof DTinyInteger) {
            writeAbstractWidget(eventWriter, abstractDWidget, "tinyInt");
        } else if (abstractDWidget instanceof DShortInteger) {
            writeAbstractWidget(eventWriter, abstractDWidget, "shortInt");
        } else if (abstractDWidget instanceof DInteger) {
            writeAbstractWidget(eventWriter, abstractDWidget, "integer");
        } else if (abstractDWidget instanceof DLongInteger) {
            writeAbstractWidget(eventWriter, abstractDWidget, "longInt");
        }

        eventWriter.writeEndElement();
    }

    private static void writeAbstractWidget(XMLPrettyPrintWriter eventWriter, AbstractDWidget abstractDWidget, String elementName)
        throws XMLStreamException {

        createNode(eventWriter, "string", abstractDWidget.getKey());

        eventWriter.writeStartElement(elementName);

        createNode(eventWriter, "label", abstractDWidget.getLabel());
        createNode(eventWriter, "offset", abstractDWidget.getOffset());
        createNode(eventWriter, "length", abstractDWidget.getLength());
        createNode(eventWriter, "sequence", abstractDWidget.getSequence());
        createNode(eventWriter, "horizontalSpan", abstractDWidget.getHorizontalSpan());

        if (abstractDWidget instanceof DDecimal) {
            DDecimal dDecimal = (DDecimal)abstractDWidget;
            createNode(eventWriter, "fraction", dDecimal.getFraction());
        }

        eventWriter.writeEndElement();

    }

    private static void writeDReferencedBy(XMLPrettyPrintWriter eventWriter, DReferencedObject dReferencedObject) throws XMLStreamException {

        eventWriter.writeStartElement(REFERENCED_BY);

        createNode(eventWriter, "string", dReferencedObject.getKey());

        eventWriter.writeStartElement(REFERENCED_BY_ITEM);

        createNode(eventWriter, "name", dReferencedObject.getName());
        createNode(eventWriter, "library", dReferencedObject.getLibrary());
        createNode(eventWriter, "type", dReferencedObject.getType());
        createNode(eventWriter, "isDefault", dReferencedObject.isDefault());

        eventWriter.writeEndElement();

        eventWriter.writeEndElement();
    }
}
