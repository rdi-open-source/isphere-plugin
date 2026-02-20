/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.streamfilesearch.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import biz.isphere.core.search.GenericSearchOption;
import biz.isphere.core.search.SearchArgument;
import biz.isphere.core.search.SearchOptions;
import biz.isphere.core.search.xml.AbstractXmlSearchHelper;
import biz.isphere.core.streamfilesearch.SearchResult;
import biz.isphere.core.streamfilesearch.SearchResultStatement;
import biz.isphere.core.streamfilesearch.SearchResultTab;
import biz.isphere.core.streamfilesearch.SearchResultTabFolder;
import biz.isphere.core.xml.XMLPrettyPrintWriter;

public class XMLSearchHelper extends AbstractXmlSearchHelper {

    private static final String STREAM_FILE_SEARCH = "streamFileSearch";
    private static final String TAB_FOLDER = "tabFolder";
    private static final String TAB_ITEM = "tabItem";
    private static final String SEARCH_RESULT = "searchResult";
    private static final String MEMBER = "member";
    private static final String STATEMENTS = "statements";
    private static final String STATEMENT = "statement";

    private static final String SEARCH_OPTIONS = "searchOptions";
    private static final String SEARCH_ARGUMENTS = "searchArguments";
    private static final String SEARCH_ARGUMENT = "biz.isphere.core.search.SearchArgument";
    private static final String GENERIC_OPTIONS = "genericOptions";
    private static final String GENERIC_OPTION_ENTRY = "entry";

    public static void saveSearchResultToXML(File toFile, SearchResultTabFolder searchResultTabFolder) throws Exception {

        FileOutputStream fileOutputStream = new FileOutputStream(toFile);

        XMLPrettyPrintWriter streamWriter = createXMLStreamWriter(fileOutputStream);

        streamWriter.writeStartDocument();

        streamWriter.writeStartElement(STREAM_FILE_SEARCH);

        writeTabFolder(streamWriter, searchResultTabFolder);

        streamWriter.writeEndElement();

        streamWriter.writeEndDocument();

        streamWriter.flush();
        streamWriter.close();

        fileOutputStream.close();
    }

    private static void writeTabFolder(XMLPrettyPrintWriter streamWriter, SearchResultTabFolder searchResultTabFolder) throws XMLStreamException {

        streamWriter.writeStartElement(TAB_FOLDER);

        for (SearchResultTab searchResultTab : searchResultTabFolder.getTabs()) {
            writeTab(streamWriter, searchResultTab);
        }

        streamWriter.writeEndElement();
    }

    private static void writeTab(XMLPrettyPrintWriter streamWriter, SearchResultTab searchResultTab) throws XMLStreamException {

        streamWriter.writeStartElement(TAB_ITEM);

        createNode(streamWriter, "connectionName", searchResultTab.getConnectionName());
        createNode(streamWriter, "searchString", searchResultTab.getSearchString());

        writeSearchResultArray(streamWriter, searchResultTab.getSearchResult());
        writeSearchOptions(streamWriter, searchResultTab.getSearchOptions());

        streamWriter.writeEndElement();
    }

    private static void writeSearchResultArray(XMLPrettyPrintWriter streamWriter, SearchResult[] searchResult) throws XMLStreamException {

        streamWriter.writeStartElement(SEARCH_RESULT);

        for (SearchResult searchResultItem : searchResult) {
            writeSearchResult(streamWriter, searchResultItem);
        }

        streamWriter.writeEndElement();

    }

    private static void writeSearchOptions(XMLPrettyPrintWriter streamWriter, SearchOptions searchOptions) throws XMLStreamException {

        streamWriter.writeStartElement(SEARCH_OPTIONS);

        createNode(streamWriter, "matchOption", searchOptions.getMatchOption());
        createNode(streamWriter, "showAllItems", searchOptions.isShowAllItems());

        writeSearchArguments(streamWriter, searchOptions.getSearchArguments());
        writeGenericSearchOptions(streamWriter, searchOptions.getGenericOptions());

        streamWriter.writeEndElement();
    }

    private static void writeSearchArguments(XMLPrettyPrintWriter streamWriter, List<SearchArgument> searchArguments) throws XMLStreamException {

        streamWriter.writeStartElement(SEARCH_ARGUMENTS);

        for (SearchArgument searchArgument : searchArguments) {
            writeSearchArgument(streamWriter, searchArgument);
        }

        streamWriter.writeEndElement();
    }

    private static void writeSearchArgument(XMLPrettyPrintWriter streamWriter, SearchArgument searchArgument) throws XMLStreamException {

        streamWriter.writeStartElement(SEARCH_ARGUMENT);

        createNode(streamWriter, "operator", searchArgument.getOperator());
        createNode(streamWriter, "string", searchArgument.getString());
        createNode(streamWriter, "fromColumn", searchArgument.getFromColumn());
        createNode(streamWriter, "toColumn", searchArgument.getToColumn());
        createNode(streamWriter, "caseSensitive", searchArgument.getCaseSensitive());
        createNode(streamWriter, "regularExpression", searchArgument.getRegularExpression());

        streamWriter.writeEndElement();
    }

    private static void writeGenericSearchOptions(XMLPrettyPrintWriter streamWriter, GenericSearchOption[] genericSearchOptions)
        throws XMLStreamException {

        streamWriter.writeStartElement(GENERIC_OPTIONS);

        for (GenericSearchOption genericSearchOption : genericSearchOptions) {
            createNode(streamWriter, GENERIC_OPTION_ENTRY, genericSearchOption);
        }

        streamWriter.writeEndElement();
    }

    private static void writeSearchResult(XMLPrettyPrintWriter streamWriter, SearchResult searchResult) throws XMLStreamException {

        streamWriter.writeStartElement(MEMBER);

        createNode(streamWriter, "directory", searchResult.getDirectory());
        createNode(streamWriter, "streamFile", searchResult.getStreamFile());
        createNode(streamWriter, "type", searchResult.getType());
        createNode(streamWriter, "lastChangedDate", searchResult.getLastChangedDate());

        writeSearchResultStatements(streamWriter, searchResult.getStatements());

        streamWriter.writeEndElement();

    }

    private static void writeSearchResultStatements(XMLPrettyPrintWriter streamWriter, SearchResultStatement[] statements) throws XMLStreamException {

        streamWriter.writeStartElement(STATEMENTS);

        for (SearchResultStatement searchResultStatement : statements) {
            writeSearchResultStatement(streamWriter, searchResultStatement);
        }

        streamWriter.writeEndElement();
    }

    private static void writeSearchResultStatement(XMLPrettyPrintWriter streamWriter, SearchResultStatement searchResultStatement)
        throws XMLStreamException {

        streamWriter.writeStartElement(STATEMENT);

        createNode(streamWriter, "statement", searchResultStatement.getStatement());
        createNode(streamWriter, "line", searchResultStatement.getLine());

        streamWriter.writeEndElement();
    }
}
