/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.messagefilesearch.xml;

import java.util.LinkedList;
import java.util.List;

import biz.isphere.core.messagefilesearch.SearchResult;
import biz.isphere.core.messagefilesearch.SearchResultTab;
import biz.isphere.core.search.SearchOptions;
import biz.isphere.core.search.xml.SearchOptionsCallback;
import biz.isphere.core.xml.AbstractCallbackHandler;
import biz.isphere.core.xml.XmlParser;

public class SearchResultTabCallback extends AbstractCallbackHandler<SearchResultTab> {

    private List<SearchResult> searchResultList;
    private SearchOptions searchOptions;

    public void doStartElement(Object userData, String path, int level, XmlParser xmlParser) throws Exception {

        if ("/searchResult".equals(path)) {
            searchResultList = new LinkedList<SearchResult>();
        } else if ("/searchResult/messageFile".equals(path)) {
            SearchResult searchResult = new SearchResult();
            searchResultList.add(searchResult);
            pushCallbackHandler(new SearchResultCallback(), searchResult);
        } else if ("/searchOptions".equals(path)) {
            searchOptions = new SearchOptions();
            pushCallbackHandler(new SearchOptionsCallback(), searchOptions);
        }
    }

    public void doEndElement(Object userData, String path, int level, String elementData, XmlParser xmlParser) throws Exception {

        if ("/connectionName".equals(path)) {
            getUserData().setConnectionName(xmlToString(elementData));
        } else if ("/searchString".equals(path)) {
            getUserData().setSearchString(xmlToString(elementData));
        } else if ("/searchResult".equals(path)) {
            getUserData().setSearchResult(searchResultList.toArray(new SearchResult[searchResultList.size()]));
        }

        if ("/".equals(path)) {
            SearchResultTab searchResultTab = (SearchResultTab)getUserData();
            searchResultTab.setSearchResult(searchResultList.toArray(new SearchResult[searchResultList.size()]));
            searchResultTab.setSearchOptions(searchOptions);
        }
    }
}
