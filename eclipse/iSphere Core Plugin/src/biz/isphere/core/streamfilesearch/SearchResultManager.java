/*******************************************************************************
 * Copyright (c) 2012-2017 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.streamfilesearch;

import java.io.File;

import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.internal.exception.LoadFileException;
import biz.isphere.core.internal.exception.SaveFileException;
import biz.isphere.core.streamfilesearch.xml.StreamFileSearchCallback;
import biz.isphere.core.streamfilesearch.xml.XMLSearchHelper;
import biz.isphere.core.xml.XmlParser;

public class SearchResultManager {

    public static final String FILE_EXTENSION = "stmfsr"; //$NON-NLS-1$

    public void saveToXml(String fileName, SearchResultTabFolder searchResultTabFolder) throws SaveFileException {

        File xmlFile = null;

        try {
            xmlFile = new File(fileName);
            XMLSearchHelper.saveSearchResultToXML(xmlFile, searchResultTabFolder);
        } catch (Exception e) {
            ISpherePlugin.logError(e.getMessage(), e);
            throw new SaveFileException(xmlFile);
        }
    }

    public SearchResultTabFolder loadFromXml(String fileName) throws LoadFileException {

        File xmlFile = new File(fileName);

        try {
            XmlParser xmlParser = new XmlParser();
            SearchResultTabFolder searchResultTabFolder = (SearchResultTabFolder)xmlParser.parse(xmlFile, new StreamFileSearchCallback(),
                new SearchResultTabFolder());

            return searchResultTabFolder;

        } catch (Throwable e) {
            ISpherePlugin.logError(e.getMessage(), e);
            throw new LoadFileException(xmlFile);
        }
    }
}
