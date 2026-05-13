/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.xml;

@SuppressWarnings("serial")
public class XmlParserException extends Exception {

    public XmlParserException(String message, Exception e) {
        super(message, e);
    }

}
