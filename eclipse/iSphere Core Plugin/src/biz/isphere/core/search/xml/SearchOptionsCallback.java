/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.search.xml;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import biz.isphere.core.search.GenericSearchOption;
import biz.isphere.core.search.MatchOption;
import biz.isphere.core.search.SearchArgument;
import biz.isphere.core.search.SearchOptions;
import biz.isphere.core.xml.AbstractCallbackHandler;
import biz.isphere.core.xml.XmlParser;

public class SearchOptionsCallback extends AbstractCallbackHandler<SearchOptions> {

    private List<SearchArgument> searchArguments;
    private Map<String, String> genericSearchOptionMap;

    public void doStartElement(Object userData, String path, int level, XmlParser xmlParser) throws Exception {

        if ("/searchArguments".equals(path)) {
            searchArguments = new LinkedList<SearchArgument>();
        } else if ("/searchArguments/biz.isphere.core.search.SearchArgument".equals(path)) {
            SearchArgument searchArgument = new SearchArgument();
            searchArguments.add(searchArgument);
            pushCallbackHandler(new SearchArgumentCallback(), searchArgument);
        } else if ("/genericOptions/entry".equals(path)) {
            genericSearchOptionMap = new HashMap<String, String>();
            pushCallbackHandler(new GenericSearchOptionCallback(), genericSearchOptionMap);
        }
    }

    public void doEndElement(Object userData, String path, int level, String elementData, XmlParser xmlParser) throws Exception {

        if ("/matchOption".equals(path)) {
            MatchOption matchOption = xmlToMatchOption(elementData);
            getUserData().setMatchOption(matchOption);
        } else if ("/showAllItems".equals(path)) {
            boolean showAllItems = xmlToBoolean(elementData);
            getUserData().setShowAllItems(showAllItems);
        } else if ("/searchArguments".equals(path)) {
            getUserData().setSearchArguments(searchArguments);
        } else if ("/genericOptions/entry".equals(path)) {
            GenericSearchOption.Key key = GenericSearchOption.Key.valueOf((String)genericSearchOptionMap.get("key"));
            String clazz = genericSearchOptionMap.get("attr_class");
            String value = genericSearchOptionMap.get("value");
            if ("string".equals(clazz)) {
                getUserData().setGenericOption(key, xmlToString(value));
            } else if ("int".equals(clazz)) {
                getUserData().setGenericOption(key, xmlToInteger(value));
            } else if ("boolean".equals(clazz)) {
                getUserData().setGenericOption(key, xmlToBoolean(value));
            } else {
                throw new IllegalArgumentException("Unexpected object type: " + clazz);
            }
        }
    }
}
