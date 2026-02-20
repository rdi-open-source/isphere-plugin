/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.junit.xml.dataspaceeditordesigner;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.nio.file.Paths;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import biz.isphere.core.dataspaceeditordesigner.model.DBoolean;
import biz.isphere.core.dataspaceeditordesigner.model.DDecimal;
import biz.isphere.core.dataspaceeditordesigner.model.DEditor;
import biz.isphere.core.dataspaceeditordesigner.model.DInteger;
import biz.isphere.core.dataspaceeditordesigner.model.DLongInteger;
import biz.isphere.core.dataspaceeditordesigner.model.DReferencedObject;
import biz.isphere.core.dataspaceeditordesigner.model.DShortInteger;
import biz.isphere.core.dataspaceeditordesigner.model.DText;
import biz.isphere.core.dataspaceeditordesigner.model.DTinyInteger;
import biz.isphere.core.dataspaceeditordesigner.model.xml.DataSpaceEditorRepositoryCallback;
import biz.isphere.core.dataspaceeditordesigner.model.xml.XMLRegistryHelper;
import biz.isphere.core.internal.exception.LoadFileException;
import biz.isphere.core.xml.XmlParser;
import biz.isphere.core.xml.XmlParserException;

public class TestXmlDataSpaceEditorDesigner {

    public static void main(String[] args) throws Exception {

        TestXmlDataSpaceEditorDesigner main = new TestXmlDataSpaceEditorDesigner();
        main.testXmlParser();
    }

    @Test
    public void testXmlParser() throws Exception {

        log("Testing Data Space Editor Designer...\n");

        String path = getPathToXmlFile("biz/isphere/junit/xml/dataspaceeditordesigner", "DataSpaceEditorDesigner-v6.1.1.dtaspced");

        DEditor newObject = parseWithNewParser(path);
        DEditor oldObject = parseWithOldParser(path);

        assertEquals(newObject.toString(), oldObject.toString());

        File resourcePath = new File(path).getParentFile();
        String newObjectWritePath = new File(resourcePath, "DataSpaceEditorDesigner-v6.1.2_new-writer.xml").getAbsolutePath();
        writeWithNewWriter(newObjectWritePath, newObject);

        String oldObjectWritePath = new File(resourcePath, "DataSpaceEditorDesigner-v6.1.2_old-writer.xml").getAbsolutePath();
        writeWithOldWriter(oldObjectWritePath, newObject);

        DEditor newObjectWithOldParser = parseWithOldParser(newObjectWritePath);
        DEditor oldObjectWithNewParser = parseWithNewParser(oldObjectWritePath);

        assertEquals(oldObjectWithNewParser.toString(), newObjectWithOldParser.toString());

        System.out.println("\n*** ...Testing Data Space Editor Designer successfully finished. ***");
    }

    private DEditor parseWithNewParser(String path) throws FileNotFoundException, XMLStreamException, XmlParserException {

        log("Parsing with new parser...");

        XmlParser xmlParser = new XmlParser();
        DEditor object1 = (DEditor)xmlParser.parse(new File(path), new DataSpaceEditorRepositoryCallback(), new DEditor());
        return object1;
    }

    private DEditor parseWithOldParser(String path) throws LoadFileException {

        log("Parsing with old parser...");

        File xmlFile = new File(path);

        DEditor object2;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(xmlFile));
            String line;
            StringBuffer xml = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                xml.append(line);
            }
            reader.close();

            object2 = (DEditor)getXStream().fromXML(xml.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return object2;
    }

    private void writeWithNewWriter(String path, DEditor object1) throws Exception {

        log("Writing with new XML writer...");

        File outputFile = new File(path);
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }

        XMLRegistryHelper.saveRegistryToXML(outputFile, object1);
    }

    private void writeWithOldWriter(String path, DEditor object2) throws Exception {

        log("Writing with old XML writer...");

        DataSpaceEditorRepository repository = DataSpaceEditorRepository.getInstance(path);
        repository.updateOrAddDialog(object2);
    }

    private void log(String message) {
        System.out.println(message);
    }

    private String getPathToXmlFile(String packageName, String resourceName) throws Exception {

        String resourcePath = packageName + "/" + resourceName;

        URL res = getClass().getClassLoader().getResource(resourcePath);

        if (res != null) {
            String absolutePath = Paths.get(res.toURI()).toFile().getAbsolutePath();
            return absolutePath;
        } else {
            throw new FileNotFoundException("Resource not found: " + resourcePath);
        }
    }

    private XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.autodetectAnnotations(true);
        xstream.alias("dialog", DEditor.class);
        xstream.alias("referencedBy", DReferencedObject.class);
        xstream.alias("boolean", DBoolean.class);
        xstream.alias("longInt", DLongInteger.class);
        xstream.alias("integer", DInteger.class);
        xstream.alias("shortInt", DShortInteger.class);
        xstream.alias("tinyInt", DTinyInteger.class);
        xstream.alias("text", DText.class);
        xstream.alias("decimal", DDecimal.class);
        return xstream;
    }
}
