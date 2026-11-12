/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import biz.isphere.base.internal.IBMiHelper;
import biz.isphere.base.internal.StringHelper;
import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.externalapi.ISynchronizeStreamFilesEditorConfiguration;
import biz.isphere.core.ifssynchronization.rse.SynchronizeStreamFilesEditorConfiguration;
import biz.isphere.core.internal.RemoteStreamFile;

public class SynchronizeStreamFilesEditorInput implements IEditorInput {

    private RemoteStreamFile leftRemoteObject;
    private RemoteStreamFile rightRemoteObject;
    private Image titleImage;

    private ISynchronizeStreamFilesEditorConfiguration configuration;
    private StreamFileDescription[] leftIfsFileDescriptions;
    private StreamFileDescription[] rightIfsFileDescriptions;

    public SynchronizeStreamFilesEditorInput(RemoteStreamFile leftRemoteObject, RemoteStreamFile rightRemoteObject) {
        this(leftRemoteObject, rightRemoteObject, new SynchronizeStreamFilesEditorConfiguration());
    }

    public SynchronizeStreamFilesEditorInput(RemoteStreamFile leftRemoteObject, RemoteStreamFile rightRemoteObject,
        ISynchronizeStreamFilesEditorConfiguration configuration) {

        this.configuration = configuration;
        this.leftRemoteObject = leftRemoteObject;
        this.rightRemoteObject = rightRemoteObject;
        this.titleImage = ISpherePlugin.getDefault().getImageRegistry().get(ISpherePlugin.IMAGE_SYNCHRONIZE_STREAM_FILES);
    }

    public boolean exists() {
        return false;
    }

    public boolean areSameObjects() {

        if (leftRemoteObject == null && rightRemoteObject == null) {
            return true;
        }

        if (leftRemoteObject != null && rightRemoteObject == null) {
            return false;
        }

        if (!IBMiHelper.isSameSystem(leftRemoteObject.getSystem(), rightRemoteObject.getSystem())) {
            return false;
        }

        return leftRemoteObject.getObjectPathName().equals(rightRemoteObject.getObjectPathName());
    }

    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    public IPersistableElement getPersistable() {
        return null;
    }

    public Object getAdapter(Class adapter) {
        return null;
    }

    public ISynchronizeStreamFilesEditorConfiguration getConfiguration() {
        return configuration;
    }

    // TODO: synchronize IFS files
    // public boolean isFileSynchronization() {
    // if (ISeries.FILE.equals(leftRemoteObject.getObjectType())) {
    // return true;
    // }
    // return false;
    // }

    // public boolean isLibrarySynchronization() {
    // return !isFileSynchronization();
    // }

    public RemoteStreamFile getLeftObject() {
        return leftRemoteObject;
    }

    public void setLeftObject(RemoteStreamFile object) {
        this.leftRemoteObject = object;
    }

    public RemoteStreamFile getRightObject() {
        return rightRemoteObject;
    }

    public void setRightObject(RemoteStreamFile object) {
        this.rightRemoteObject = object;
    }

    public String getLeftObjectName() {
        if (leftRemoteObject == null) {
            return ""; //$NON-NLS-1$
        }
        return leftRemoteObject.getAbsoluteName();
    }

    public String getRightObjectName() {
        if (rightRemoteObject == null) {
            return ""; //$NON-NLS-1$
        }
        return rightRemoteObject.getAbsoluteName();
    }

    public StreamFileDescription[] getLeftIfsFileDescriptions() {
        return this.leftIfsFileDescriptions;
    }

    public void setLeftIfsFileDescriptions(StreamFileDescription[] leftIfsFileDescriptions) {
        this.leftIfsFileDescriptions = leftIfsFileDescriptions;
    }

    public StreamFileDescription[] getRightIfsFileDescriptions() {
        return this.rightIfsFileDescriptions;
    }

    public void setRightIfsFileDescriptions(StreamFileDescription[] rightIfsFileDescriptions) {
        this.rightIfsFileDescriptions = rightIfsFileDescriptions;
    }

    /**
     * Removes the description of a deleted IFS file from the left side of the
     * comparison.
     * 
     * @param ifsFileDescription - description of the deleted IFS file
     */
    public void removeLeftIfsFileDescription(StreamFileDescription ifsFileDescription) {
        leftIfsFileDescriptions = removeIfsFileDescription(leftIfsFileDescriptions, ifsFileDescription);
    }

    /**
     * Removes the description of a deleted IFS file from the right side of the
     * comparison.
     * 
     * @param ifsFileDescription - description of the deleted IFS file
     */
    public void removeRightIfsFileDescription(StreamFileDescription ifsFileDescription) {
        rightIfsFileDescriptions = removeIfsFileDescription(rightIfsFileDescriptions, ifsFileDescription);
    }

    private StreamFileDescription[] removeIfsFileDescription(StreamFileDescription[] ifsFileDescriptions,
        StreamFileDescription ifsFileDescription) {

        if (ifsFileDescriptions == null || ifsFileDescription == null) {
            return ifsFileDescriptions;
        }

        List<StreamFileDescription> remainingIfsFileDescriptions = new ArrayList<StreamFileDescription>(Arrays.asList(ifsFileDescriptions));
        if (!remainingIfsFileDescriptions.remove(ifsFileDescription)) {
            return ifsFileDescriptions;
        }

        return remainingIfsFileDescriptions.toArray(new StreamFileDescription[remainingIfsFileDescriptions.size()]);
    }

    public String getName() {

        if (StringHelper.isNullOrEmpty(getLeftObjectName())) {
            return getRightObjectName();
        } else if (StringHelper.isNullOrEmpty(getRightObjectName())) {
            return getLeftObjectName();
        } else {
            return getLeftObjectName() + " - " + getRightObjectName(); //$NON-NLS-1$
        }
    }

    public String getToolTipText() {
        return getName();
    }

    public Image getTitleImage() {
        return titleImage;
    }

    public SynchronizeStreamFilesEditorInput clearAll() {

        leftIfsFileDescriptions = new StreamFileDescription[0];
        rightIfsFileDescriptions = new StreamFileDescription[0];

        return this;
    }

    /**
     * Returns the hash code of the editor input. It is built from the same
     * attributes as {@link #equals(Object)} and allows a side without a
     * directory, because the editor can be opened with one directory only.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + hashCode(leftRemoteObject);
        result = prime * result + hashCode(rightRemoteObject);
        return result;
    }

    private int hashCode(RemoteStreamFile remoteObject) {

        if (remoteObject == null) {
            return 0;
        }

        final int prime = 31;
        int result = 1;
        result = prime * result + hashCode(remoteObject.getConnectionName());
        result = prime * result + hashCode(remoteObject.getName());
        return result;
    }

    private int hashCode(String value) {
        return (value == null) ? 0 : value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        SynchronizeStreamFilesEditorInput other = (SynchronizeStreamFilesEditorInput)obj;

        return isEqual(leftRemoteObject, other.leftRemoteObject) && isEqual(rightRemoteObject, other.rightRemoteObject);
    }

    /**
     * Returns <code>true</code>, if both directories are missing or if they
     * refer to the same directory of the same connection.
     */
    private boolean isEqual(RemoteStreamFile remoteObject, RemoteStreamFile otherRemoteObject) {

        if (remoteObject == null || otherRemoteObject == null) {
            return remoteObject == otherRemoteObject;
        }

        return isEqual(remoteObject.getConnectionName(), otherRemoteObject.getConnectionName())
            && isEqual(remoteObject.getName(), otherRemoteObject.getName());
    }

    private boolean isEqual(String value, String otherValue) {

        if (value == null) {
            return otherValue == null;
        }

        return value.equals(otherValue);
    }
}
