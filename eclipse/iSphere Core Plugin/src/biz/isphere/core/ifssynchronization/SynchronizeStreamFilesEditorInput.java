/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization;

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((leftRemoteObject == null) ? 0 : leftRemoteObject.hashCode());
        result = prime * result + ((leftRemoteObject.getConnectionName() == null) ? 0 : leftRemoteObject.getConnectionName().hashCode());
        result = prime * result + ((leftRemoteObject.getName() == null) ? 0 : leftRemoteObject.getName().hashCode());
        result = prime * result + ((rightRemoteObject == null) ? 0 : rightRemoteObject.hashCode());
        result = prime * result + ((rightRemoteObject.getConnectionName() == null) ? 0 : rightRemoteObject.getConnectionName().hashCode());
        result = prime * result + ((rightRemoteObject.getName() == null) ? 0 : rightRemoteObject.getName().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        SynchronizeStreamFilesEditorInput other = (SynchronizeStreamFilesEditorInput)obj;

        if (leftRemoteObject == null) {
            if (other.leftRemoteObject != null) return false;
        } else if (other.leftRemoteObject != null) {
            if (leftRemoteObject.getConnectionName() == null) {
                if (other.leftRemoteObject.getConnectionName() != null) return false;
            } else if (!leftRemoteObject.getConnectionName().equals(other.leftRemoteObject.getConnectionName())) return false;

            if (leftRemoteObject.getName() == null) {
                if (other.leftRemoteObject.getName() != null) return false;
            } else if (!leftRemoteObject.getName().equals(other.leftRemoteObject.getName())) return false;
        }

        if (rightRemoteObject == null) {
            if (other.rightRemoteObject != null) return false;
        } else if (other.rightRemoteObject != null) {
            if (rightRemoteObject.getConnectionName() == null) {
                if (other.rightRemoteObject.getConnectionName() != null) return false;
            } else if (!rightRemoteObject.getConnectionName().equals(other.rightRemoteObject.getConnectionName())) return false;

            if (rightRemoteObject.getName() == null) {
                if (other.rightRemoteObject.getName() != null) return false;
            } else if (!rightRemoteObject.getName().equals(other.rightRemoteObject.getName())) return false;
        }

        return true;
    }
}
