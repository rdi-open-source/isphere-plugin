/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.swt.widgets.objectselector.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.IFSFileFilter;

import biz.isphere.base.internal.ExceptionHelper;

/**
 * This class represents a directory of the IFS of an IBM i in the tree of the
 * {@link biz.isphere.core.swt.widgets.objectselector.SelectIFSDirectoryDialog}.
 */
public class DirectoryItem extends AbstractListItem {

    public static final String ROOT_DIRECTORY = "/"; //$NON-NLS-1$

    private String path;

    public static DirectoryItem createRootDirectory(AS400 system) {
        return new DirectoryItem(system, ROOT_DIRECTORY, ROOT_DIRECTORY);
    }

    private DirectoryItem(AS400 system, String name, String path) {
        super(system, PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER), name, null);

        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String getLabel() {
        return super.getName();
    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    public AbstractListItem[] resolveChildren() {

        List<AbstractListItem> children = new LinkedList<AbstractListItem>();

        try {

            IFSFile[] directories = new IFSFile(getSystem(), path).listFiles(new IFSFileFilter() {
                public boolean accept(IFSFile file) {
                    try {
                        return file.isDirectory();
                    } catch (Exception e) {
                        return false;
                    }
                }
            });

            if (directories != null) {
                Arrays.sort(directories, new Comparator<IFSFile>() {
                    public int compare(IFSFile me, IFSFile other) {
                        return me.getName().compareToIgnoreCase(other.getName());
                    }
                });

                for (IFSFile directory : directories) {
                    children.add(new DirectoryItem(getSystem(), directory.getName(), directory.getAbsolutePath()));
                }
            }

        } catch (Exception e) {
            children.add(new ErrorItem(ExceptionHelper.getLocalizedMessage(e)));
        }

        return children.toArray(new AbstractListItem[children.size()]);
    }
}
