/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.sourcefilesearch.xml;

import java.util.LinkedList;
import java.util.List;

import biz.isphere.core.sourcefilesearch.SearchResult;
import biz.isphere.core.sourcefilesearch.SearchResultStatement;
import biz.isphere.core.xml.AbstractCallbackHandler;
import biz.isphere.core.xml.XmlParser;

public class SearchResultCallback extends AbstractCallbackHandler<SearchResult> {

    private List<SearchResultStatement> statements;

    public void doStartElement(Object userData, String path, int level, XmlParser xmlParser) throws Exception {

        if ("/statements".equals(path)) {
            statements = new LinkedList<SearchResultStatement>();
        } else if ("/statements/statement".equals(path)) {
            SearchResultStatement statement = new SearchResultStatement();
            statements.add(statement);
            pushCallbackHandler(new SearchResultStatementCallback(), statement);
        }
    }

    public void doEndElement(Object userData, String path, int level, String elementData, XmlParser xmlParser) throws Exception {

        if ("/library".equals(path)) {
            getUserData().setLibrary(xmlToString(elementData));
        } else if ("/file".equals(path)) {
            getUserData().setFile(xmlToString(elementData));
        } else if ("/member".equals(path)) {
            getUserData().setMember(xmlToString(elementData));
        } else if ("/description".equals(path)) {
            getUserData().setDescription(xmlToString(elementData));
        } else if ("/srcType".equals(path)) {
            getUserData().setSrcType(xmlToString(elementData));
        } else if ("/lastChangedDate".equals(path)) {
            getUserData().setLastChangedDate(xmlToTimestamp(elementData));
        } else if ("/statements".equals(path)) {
            getUserData().setStatements(statements.toArray(new SearchResultStatement[statements.size()]));
        }
    }
}
