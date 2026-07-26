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
import biz.isphere.core.messagefilesearch.SearchResultMessageId;
import biz.isphere.core.xml.AbstractCallbackHandler;
import biz.isphere.core.xml.XmlParser;

public class SearchResultCallback extends AbstractCallbackHandler<SearchResult> {

    private List<SearchResultMessageId> messageIds;

    public void doStartElement(Object userData, String path, int level, XmlParser xmlParser) throws Exception {

        if ("/messageIds".equals(path)) {
            messageIds = new LinkedList<SearchResultMessageId>();
        } else if ("/messageIds/messageId".equals(path)) {
            SearchResultMessageId searchResultMessageId = new SearchResultMessageId();
            messageIds.add(searchResultMessageId);
            pushCallbackHandler(new SearchResultMessageIdCallback(), searchResultMessageId);
        }
    }

    public void doEndElement(Object userData, String path, int level, String elementData, XmlParser xmlParser) throws Exception {

        if ("/connectionName".equals(path)) {
            getUserData().setConnectionName(xmlToString(elementData));
        } else if ("/library".equals(path)) {
            getUserData().setLibrary(xmlToString(elementData));
        } else if ("/messageFile".equals(path)) {
            getUserData().setMessageFile(xmlToString(elementData));
        } else if ("/description".equals(path)) {
            getUserData().setDescription(xmlToString(elementData));
        } else if ("/messageIds".equals(path)) {
            getUserData().setMessageIds(messageIds.toArray(new SearchResultMessageId[messageIds.size()]));
        }
    }
}
