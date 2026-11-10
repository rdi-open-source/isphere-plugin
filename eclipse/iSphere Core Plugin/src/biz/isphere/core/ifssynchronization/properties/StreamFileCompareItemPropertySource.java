/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization.properties;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import biz.isphere.core.Messages;
import biz.isphere.core.ifssynchronization.CompareOptions;
import biz.isphere.core.ifssynchronization.StreamFileDescription;
import biz.isphere.core.ifssynchronization.rse.StreamFileCompareItem;

public class StreamFileCompareItemPropertySource implements IPropertySource {

    private static final String PROPERTY_LEFT_FILE = "biz.isphere.core.ifssynchronization.leftFile"; //$NON-NLS-1$
    private static final String PROPERTY_RIGHT_FILE = "biz.isphere.core.ifssynchronization.rightFile"; //$NON-NLS-1$
    private static final String PROPERTY_SELECTION = "biz.isphere.core.ifssynchronization.selection"; //$NON-NLS-1$
    private static final String PROPERTY_ERROR_MESSAGE = "biz.isphere.core.ifssynchronization.errorMessage"; //$NON-NLS-1$

    private StreamFileCompareItem ifsFileCompareItem;
    private CompareOptions compareOptions;

    private IPropertyDescriptor[] propertyDescriptors;

    public StreamFileCompareItemPropertySource(StreamFileCompareItem ifsFileCompareItem) {
        this.ifsFileCompareItem = ifsFileCompareItem;
        this.compareOptions = null;
    }

    public void setCompareOptions(CompareOptions compareOptions) {
        this.compareOptions = compareOptions;
    }

    public Object getEditableValue() {
        return null;
    }

    public IPropertyDescriptor[] getPropertyDescriptors() {

        if (propertyDescriptors == null) {

            PropertyDescriptor leftFile = createPropertyDescriptor(PROPERTY_LEFT_FILE, Messages.File, Messages.Left);

            PropertyDescriptor rightFile = createPropertyDescriptor(PROPERTY_RIGHT_FILE, Messages.File, Messages.Right);

            PropertyDescriptor selection = createPropertyDescriptor(PROPERTY_SELECTION, Messages.Selection, Messages.Other);
            PropertyDescriptor errorMessage = createPropertyDescriptor(PROPERTY_ERROR_MESSAGE, Messages.Message, Messages.Other);

            propertyDescriptors = new IPropertyDescriptor[] { leftFile, rightFile, selection, errorMessage };
        }
        return propertyDescriptors;
    }

    private PropertyDescriptor createPropertyDescriptor(Object id, String displayName, String category) {

        PropertyDescriptor descriptor = new PropertyDescriptor(id, displayName);
        descriptor.setCategory(category);

        return descriptor;
    }

    public Object getPropertyValue(Object propertyName) {

        if (ifsFileCompareItem == null) {
            return null;
        }

        if (PROPERTY_LEFT_FILE.equals(propertyName)) {
            return getFileUI(ifsFileCompareItem.getLeftIfsFileDescription());
        } else if (PROPERTY_RIGHT_FILE.equals(propertyName)) {
            return getFileUI(ifsFileCompareItem.getRightIfsFileDescription());
        } else if (PROPERTY_SELECTION.equals(propertyName)) {
            if (compareOptions != null) {
                return getSelectionUI();
            }
        } else if (PROPERTY_ERROR_MESSAGE.equals(propertyName)) {
            return getErrorMessage();
        }
        ;

        return null;
    }

    public void resetPropertyValue(Object propertyName) {
        return;
    }

    public void setPropertyValue(Object propertyName, Object value) {
        return;
    }

    public boolean isPropertySet(Object propertyName) {
        return false;
    }

    private String getFileUI(StreamFileDescription ifsFileDescription) {
        if (ifsFileDescription != null) {
            return ifsFileDescription.getAbsolutePath();
        }
        return Messages.EMPTY;
    }

    private String getSelectionUI() {
        int compareStatus = ifsFileCompareItem.getCompareStatus(compareOptions);
        switch (compareStatus) {
        case StreamFileCompareItem.NO_ACTION:
            return Messages.Property_No_action;
        case StreamFileCompareItem.LEFT_MISSING:
            return Messages.Property_Copy_to_left;
        case StreamFileCompareItem.RIGHT_MISSING:
            return Messages.Property_Copy_to_right;
        case StreamFileCompareItem.LEFT_EQUALS_RIGHT:
            return Messages.Property_Left_equals_right;
        case StreamFileCompareItem.NOT_EQUAL:
            return Messages.Property_Left_unequal_right;
        case StreamFileCompareItem.ERROR:
            int tCompareStatus = ifsFileCompareItem.getOriginalCompareStatus(compareOptions);
            if (tCompareStatus == StreamFileCompareItem.LEFT_MISSING) {
                return Messages.Property_Copy_to_left;
            } else if (tCompareStatus == StreamFileCompareItem.RIGHT_MISSING) {
                return Messages.Property_Copy_to_right;
            }
        default:
            return Messages.EMPTY;
        }
    }

    private String getErrorMessage() {
        String message = ifsFileCompareItem.getErrorMessage();
        if (message != null) {
            return message;
        }
        return Messages.EMPTY;
    }
}
