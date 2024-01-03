/*******************************************************************************
 * Copyright (c) 2012-2021 iSphere Project Team
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.journalexplorer.core.model.sqljep;

import java.util.Map.Entry;

public class NullValueVariable implements Entry<String, Comparable<?>> {

    private String key;

    public NullValueVariable(String name) {
        this.key = name;
    }

    public java.lang.Comparable<?> getValue() {
        return null;
    }

    public String getKey() {
        throw new IllegalAccessError();
    }

    public Comparable<?> setValue(Comparable<?> value) {
        throw new IllegalAccessError();
    };

    @Override
    public String toString() {

        StringBuilder buffer = new StringBuilder();

        buffer.append(key);
        buffer.append("=[null]"); //$NON-NLS-1$

        return buffer.toString();
    }
}
