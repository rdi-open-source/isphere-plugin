/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.compareeditor.filters;

import java.util.HashMap;

import org.eclipse.compare.ICompareFilter;
import org.eclipse.compare.ITypedElement;
import org.eclipse.jface.text.IRegion;

import biz.isphere.base.internal.BooleanHelper;
import biz.isphere.base.internal.FileHelper;
import biz.isphere.core.compareeditor.CompareNode;
import biz.isphere.core.preferences.Preferences;

public class IgnoreDateCompareFilter implements ICompareFilter {

    public static final String JVM_PROPERTY_IGNORE_DATE = "iSphere.IgnoreDateCompareFilter.enabled";

    private boolean isEnabled;

    public IgnoreDateCompareFilter() {
        return;
    }

    public boolean canCacheFilteredRegions() {
        return false;
    }

    public IRegion[] getFilteredRegions(HashMap arg0) {

        if (isEnabled && arg0.get(THIS_LINE) instanceof String) {
            String line = (String)arg0.get(THIS_LINE);
            if (line.length() >= 6) {
                return new IRegion[] { new IgnoredDateRegion() };
            }
        }

        return new IRegion[0];
    }

    public boolean isEnabledInitially() {
        String property = System.getProperty(JVM_PROPERTY_IGNORE_DATE);
        return BooleanHelper.tryParseBoolean(property, true);

    }

    public void setInput(Object input, Object ancestor, Object left, Object right) {

        if ((ancestor == null || isSourceMemberWithDate(ancestor)) && isSourceMemberWithDate(left) && isSourceMemberWithDate(right)) {
            isEnabled = true;
        } else {
            isEnabled = false;
        }

        return;
    }

    /**
     * The filter is registered for all text files. But only the source members
     * of the iSphere compare editor start with the date column. Other compare
     * editors, e.g. the compare editor of the Git Staging view, show stream
     * files, where the first 6 characters are part of the source code.
     */
    private boolean isSourceMemberWithDate(Object node) {

        if (!(node instanceof CompareNode)) {
            return false;
        }

        if (!((CompareNode)node).hasDate()) {
            return false;
        }

        return isSupportedFileExtension(getFileExtension(node));
    }

    private boolean isSupportedFileExtension(String fileExtension) {
        return Preferences.getInstance().supportsFileExtension(fileExtension);
    }

    private String getFileExtension(Object node) {

        if (node == null) {
            return null;
        }

        return FileHelper.getFileExtension(((ITypedElement)node).getName());
    }

    private class IgnoredDateRegion implements IRegion {

        public int getOffset() {
            return 0;
        }

        public int getLength() {
            return 6;
        }
    }
}
