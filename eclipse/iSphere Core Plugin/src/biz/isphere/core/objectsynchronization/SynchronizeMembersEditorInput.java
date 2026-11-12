/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.objectsynchronization;

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
import biz.isphere.core.externalapi.ISynchronizeMembersEditorConfiguration;
import biz.isphere.core.internal.ISeries;
import biz.isphere.core.internal.RemoteObject;
import biz.isphere.core.objectsynchronization.rse.SynchronizeMembersEditorConfiguration;

public class SynchronizeMembersEditorInput implements IEditorInput {

    private RemoteObject leftRemoteObject;
    private RemoteObject rightRemoteObject;
    private Image titleImage;

    private ISynchronizeMembersEditorConfiguration configuration;
    private MemberDescription[] leftMemberDescriptions;
    private MemberDescription[] rightMemberDescriptions;

    public SynchronizeMembersEditorInput(RemoteObject leftRemoteObject, RemoteObject rightRemoteObject) {
        this(leftRemoteObject, rightRemoteObject, new SynchronizeMembersEditorConfiguration());
    }

    public SynchronizeMembersEditorInput(RemoteObject leftRemoteObject, RemoteObject rightRemoteObject,
        ISynchronizeMembersEditorConfiguration configuration) {

        this.configuration = configuration;
        this.leftRemoteObject = leftRemoteObject;
        this.rightRemoteObject = rightRemoteObject;
        this.titleImage = ISpherePlugin.getDefault().getImageRegistry().get(ISpherePlugin.IMAGE_SYNCHRONIZE_MEMBERS);
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

        return leftRemoteObject.getObjectPathName().getPath().equals(rightRemoteObject.getObjectPathName().getPath());
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

    public ISynchronizeMembersEditorConfiguration getConfiguration() {
        return configuration;
    }

    public boolean isFileSynchronization() {
        if (ISeries.FILE.equals(leftRemoteObject.getObjectType())) {
            return true;
        }
        return false;
    }

    public boolean isLibrarySynchronization() {
        return !isFileSynchronization();
    }

    public RemoteObject getLeftObject() {
        return leftRemoteObject;
    }

    public void setLeftObject(RemoteObject object) {
        this.leftRemoteObject = object;
    }

    public RemoteObject getRightObject() {
        return rightRemoteObject;
    }

    public void setRightObject(RemoteObject object) {
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

    public MemberDescription[] getLeftMemberDescriptions() {
        return this.leftMemberDescriptions;
    }

    public void setLeftMemberDescriptions(MemberDescription[] leftMemberDescriptions) {
        this.leftMemberDescriptions = leftMemberDescriptions;
    }

    public MemberDescription[] getRightMemberDescriptions() {
        return this.rightMemberDescriptions;
    }

    public void setRightMemberDescriptions(MemberDescription[] rightMemberDescriptions) {
        this.rightMemberDescriptions = rightMemberDescriptions;
    }

    /**
     * Removes the description of a deleted member from the left side of the
     * comparison.
     * 
     * @param memberDescription - description of the deleted member
     */
    public void removeLeftMemberDescription(MemberDescription memberDescription) {
        leftMemberDescriptions = removeMemberDescription(leftMemberDescriptions, memberDescription);
    }

    /**
     * Removes the description of a deleted member from the right side of the
     * comparison.
     * 
     * @param memberDescription - description of the deleted member
     */
    public void removeRightMemberDescription(MemberDescription memberDescription) {
        rightMemberDescriptions = removeMemberDescription(rightMemberDescriptions, memberDescription);
    }

    private MemberDescription[] removeMemberDescription(MemberDescription[] memberDescriptions, MemberDescription memberDescription) {

        if (memberDescriptions == null || memberDescription == null) {
            return memberDescriptions;
        }

        List<MemberDescription> remainingMemberDescriptions = new ArrayList<MemberDescription>(Arrays.asList(memberDescriptions));
        if (!remainingMemberDescriptions.remove(memberDescription)) {
            return memberDescriptions;
        }

        return remainingMemberDescriptions.toArray(new MemberDescription[remainingMemberDescriptions.size()]);
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

    public SynchronizeMembersEditorInput clearAll() {

        leftMemberDescriptions = new MemberDescription[0];
        rightMemberDescriptions = new MemberDescription[0];

        return this;
    }

    /**
     * Returns the hash code of the editor input. It is built from the same
     * attributes as {@link #equals(Object)} and allows a side without an
     * object, because the editor can be opened with one object only.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + hashCode(leftRemoteObject);
        result = prime * result + hashCode(rightRemoteObject);
        return result;
    }

    private int hashCode(RemoteObject remoteObject) {

        if (remoteObject == null) {
            return 0;
        }

        final int prime = 31;
        int result = 1;
        result = prime * result + hashCode(remoteObject.getConnectionName());
        result = prime * result + hashCode(remoteObject.getLibrary());
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

        SynchronizeMembersEditorInput other = (SynchronizeMembersEditorInput)obj;

        return isEqual(leftRemoteObject, other.leftRemoteObject) && isEqual(rightRemoteObject, other.rightRemoteObject);
    }

    /**
     * Returns <code>true</code>, if both objects are missing or if they refer
     * to the same object of the same connection.
     */
    private boolean isEqual(RemoteObject remoteObject, RemoteObject otherRemoteObject) {

        if (remoteObject == null || otherRemoteObject == null) {
            return remoteObject == otherRemoteObject;
        }

        return isEqual(remoteObject.getConnectionName(), otherRemoteObject.getConnectionName())
            && isEqual(remoteObject.getLibrary(), otherRemoteObject.getLibrary()) && isEqual(remoteObject.getName(), otherRemoteObject.getName());
    }

    private boolean isEqual(String value, String otherValue) {

        if (value == null) {
            return otherValue == null;
        }

        return value.equals(otherValue);
    }
}
