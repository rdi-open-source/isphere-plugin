/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.junit.xml.messagefilesearch;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Paths;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;

import biz.isphere.core.internal.exception.LoadFileException;
import biz.isphere.core.messagefilesearch.SearchResultTabFolder;
import biz.isphere.core.messagefilesearch.xml.MessageFileSearchCallback;
import biz.isphere.core.messagefilesearch.xml.XMLSearchHelper;
import biz.isphere.core.xml.XmlParser;
import biz.isphere.core.xml.XmlParserException;

public class TestXmlParserMessageFileSearch {

    public static void main(String[] args) throws Exception {

        TestXmlParserMessageFileSearch main = new TestXmlParserMessageFileSearch();
        main.testXmlParser();
    }

    @Test
    public void testXmlParser() throws Exception {

        log("Testing Message File Search...\n");

        String path = getPathToXmlFile("biz/isphere/junit/xml/messagefilesearch", "MessageFileSearchResult-v6.1.1.msgfsr");

        SearchResultTabFolder newObject = parseWithNewParser(path);
        SearchResultTabFolder oldObject = parseWithOldParser(path);

        assertEquals(newObject.toString(), oldObject.toString());

        File resourcePath = new File(path).getParentFile();
        String newObjectWritePath = new File(resourcePath, "SourceFileSearchResult-v6.1.2_new-writer.xml").getAbsolutePath();
        writeWithNewWriter(newObjectWritePath, newObject);

        String oldObjectWritePath = new File(resourcePath, "SourceFileSearchResult-v6.1.2_old-writer.xml").getAbsolutePath();
        writeWithOldWriter(oldObjectWritePath, newObject);

        SearchResultTabFolder newObjectWithOldParser = parseWithOldParser(newObjectWritePath);
        SearchResultTabFolder oldObjectWithNewParser = parseWithNewParser(newObjectWritePath);

        assertEquals(oldObjectWithNewParser.toString(), newObjectWithOldParser.toString());

        System.out.println("\n*** ...Message File Search successfully finished. ***");
    }

    private SearchResultTabFolder parseWithNewParser(String path) throws FileNotFoundException, XMLStreamException, XmlParserException {

        log("Parsing with new parser...");

        XmlParser xmlParser = new XmlParser();
        SearchResultTabFolder object1 = (SearchResultTabFolder)xmlParser.parse(new File(path), new MessageFileSearchCallback(),
            new SearchResultTabFolder());
        return object1;
    }

    private SearchResultTabFolder parseWithOldParser(String path) throws LoadFileException {

        log("Parsing with old parser...");

        SearchResultManagerXStream manager = new SearchResultManagerXStream();
        SearchResultTabFolder object2 = manager.loadFromXml(path);

        return object2;
    }

    private void writeWithNewWriter(String path, SearchResultTabFolder object1) throws Exception {

        log("Writing with new XML writer...");

        File outputFile = new File(path);
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }

        XMLSearchHelper.saveSearchResultToXML(outputFile, object1);
    }

    private void writeWithOldWriter(String path, SearchResultTabFolder object2) throws Exception {

        log("Writing with old XML writer...");

        SearchResultManagerXStream manager = new SearchResultManagerXStream();
        manager.saveToXml(path, object2);
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
}
