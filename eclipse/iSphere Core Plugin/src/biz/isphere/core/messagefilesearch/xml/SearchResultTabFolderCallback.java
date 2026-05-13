/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.messagefilesearch.xml;

import biz.isphere.core.messagefilesearch.SearchResultTab;
import biz.isphere.core.messagefilesearch.SearchResultTabFolder;
import biz.isphere.core.xml.AbstractCallbackHandler;
import biz.isphere.core.xml.XmlParser;

public class SearchResultTabFolderCallback extends AbstractCallbackHandler<SearchResultTabFolder> {

    private SearchResultTab searchResultTab;

    public void doStartElement(Object userData, String path, int level, XmlParser xmlParser) throws Exception {

        if ("/tabItem".equals(path)) {
            searchResultTab = new SearchResultTab();
            pushCallbackHandler(new SearchResultTabCallback(), searchResultTab);
        }
    }

    public void doEndElement(Object userData, String path, int level, String elementData, XmlParser xmlParser) throws Exception {

        if ("/tabItem".equals(path)) {
            SearchResultTabFolder searchResultTabFolder = (SearchResultTabFolder)getUserData();
            searchResultTabFolder.addTab(searchResultTab);
        }
    }
}
