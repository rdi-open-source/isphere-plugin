/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.messagefilesearch;

import java.io.Serializable;

import biz.isphere.core.search.ISearchResultTab;
import biz.isphere.core.search.SearchArgument;
import biz.isphere.core.search.SearchOptions;

@SuppressWarnings("serial")
public class SearchResultTab implements ISearchResultTab, Serializable {

    private static final String LF = "\n"; //$NON-NLS-1$

    private String connectionName;
    private String searchString;
    private SearchResult[] searchResult;
    private SearchOptions searchOptions;

    public SearchResultTab() {
    }

    public SearchResultTab(String connectionName, String searchString, SearchResult[] searchResult, SearchOptions searchOptions) {

        this.connectionName = connectionName;
        this.searchString = searchString;
        this.searchResult = searchResult;
        this.searchOptions = searchOptions;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public SearchResult[] getSearchResult() {
        return searchResult;
    }

    public void setSearchResult(SearchResult[] searchResult) {
        this.searchResult = searchResult;
    }

    public SearchOptions getSearchOptions() {
        return searchOptions;
    }

    public void setSearchOptions(SearchOptions searchOptions) {
        this.searchOptions = searchOptions;
    }

    public boolean hasSearchOptions() {

        if (searchOptions != null) {
            return true;
        }

        return false;
    }

    public String toText() {

        StringBuilder buffer = new StringBuilder();

        buffer.append("Connection: " + connectionName);
        buffer.append("\n");
        if (searchOptions != null) {
            buffer.append(searchOptions.toText());
        } else {
            buffer.append(searchString);
            buffer.append("\n");
        }

        return buffer.toString();
    }

    @Override
    public String toString() {

        StringBuilder buffer = new StringBuilder();

        buffer.append("connectionName: " + connectionName + LF);
        buffer.append("searchString:   " + searchString + LF);

        buffer.append("search Options:" + LF);
        buffer.append("  matchOption:  " + searchOptions.getMatchOption() + LF);
        buffer.append("  showAllItems: " + searchOptions.isShowAllItems() + LF);
        buffer.append("  searchArguments: " + LF);
        for (SearchArgument searchArgument : searchOptions.getSearchArguments()) {
            buffer.append("    ");
            buffer.append(searchArgument.toString());
        }

        buffer.append(LF + "  search Results: " + LF);
        for (SearchResult searchResultItem : searchResult) {
            buffer.append("    ");
            buffer.append(searchResultItem.toString());
        }

        return buffer.toString();
    }
}
