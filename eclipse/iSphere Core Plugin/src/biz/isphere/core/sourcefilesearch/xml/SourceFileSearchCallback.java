/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.sourcefilesearch.xml;

import biz.isphere.core.xml.AbstractCallbackHandler;
import biz.isphere.core.xml.XmlParser;

public class SourceFileSearchCallback extends AbstractCallbackHandler<Object> {

    public void doStartElement(Object userData, String path, int level, XmlParser xmlParser) throws Exception {

        if ("/sourceFileSearch/tabFolder".equals(path)) {
            pushCallbackHandler(new SearchResultTabFolderCallback(), getUserData());
        }
    }

    public void doEndElement(Object userData, String path, int level, String elementData, XmlParser xmlParser) throws Exception {
    }
}
