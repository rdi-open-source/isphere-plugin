/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.base.internal;

/**
 * This class provides the path arithmetic of the IFS.
 * <p>
 * IFS path names always use a slash as the path separator, no matter which
 * operating system the workbench is running on. Hence {@link java.io.File} must
 * not be used for producing them, because it uses the separator of the local
 * file system.
 * <p>
 * When two directory trees are compared, the items are identified by their path
 * <i>relative</i> to the root directory of the tree they were found in. Given a
 * left root of <code>/home/raddatz/isphere</code> and a right root of
 * <code>/home/raddatz/isphere-dvp</code>, both items
 * <code>/home/raddatz/isphere/QRPGLESRC/DEMO12.RPGLE</code> and
 * <code>/home/raddatz/isphere-dvp/QRPGLESRC/DEMO12.RPGLE</code> share the
 * relative path <code>./QRPGLESRC/DEMO12.RPGLE</code>.
 */
public final class IFSFileHelper {

    /**
     * Relative path of the root directory of a compared directory tree.
     */
    public static final String RELATIVE_ROOT = "."; //$NON-NLS-1$

    private static final String SLASH = "/"; //$NON-NLS-1$
    private static final String BACKSLASH = "\\"; //$NON-NLS-1$

    private IFSFileHelper() {
    }

    /**
     * Returns the name of a given IFS path name, without the directory, for
     * example <code>DEMO12.RPGLE</code>.
     */
    public static String getFileName(String ifsFilePath) {

        String path = normalizeDirectory(ifsFilePath);
        if (path == null) {
            return null;
        }

        int i = path.lastIndexOf(SLASH);
        if (i < 0) {
            return path;
        }

        return path.substring(i + 1);
    }

    /**
     * Returns the directory of a given IFS path name, without a trailing slash,
     * for example <code>/home/raddatz/isphere/QRPGLESRC</code>. Returns
     * <code>null</code>, if the path name does not contain a directory.
     */
    public static String getPathName(String ifsFilePath) {

        String path = normalizeDirectory(ifsFilePath);
        if (path == null) {
            return null;
        }

        int i = path.lastIndexOf(SLASH);
        if (i < 0) {
            return null;
        }

        if (i == 0) {
            // The item is stored in the root directory of the file system.
            return SLASH;
        }

        return path.substring(0, i);
    }

    /**
     * Produces the relative path of an item of a given directory.
     * 
     * @param parentRelativePath - relative path of the directory the item is
     *        stored in, for example <code>./QRPGLESRC</code>
     * @param itemName - name of the item, without a path
     * @return relative path of the item, for example
     *         <code>./QRPGLESRC/DEMO12.RPGLE</code>
     */
    public static String toRelativePath(String parentRelativePath, String itemName) {

        String parentPath = normalizeRelativePath(parentRelativePath);
        String name = itemName.replace(BACKSLASH, SLASH);

        while (name.startsWith(SLASH)) {
            name = name.substring(1);
        }

        if (parentPath == null || RELATIVE_ROOT.equals(parentPath)) {
            return RELATIVE_ROOT + SLASH + name;
        }

        return parentPath + SLASH + name;
    }

    /**
     * Produces the absolute path of an item of a given root directory.
     * 
     * @param rootDirectory - absolute path of the root directory, for example
     *        <code>/home/raddatz/isphere</code>
     * @param relativePath - relative path of the item, for example
     *        <code>./QRPGLESRC/DEMO12.RPGLE</code>
     * @return absolute path of the item, for example
     *         <code>/home/raddatz/isphere/QRPGLESRC/DEMO12.RPGLE</code>
     */
    public static String toAbsolutePath(String rootDirectory, String relativePath) {

        String root = normalizeDirectory(rootDirectory);
        String path = normalizeRelativePath(relativePath);

        if (root == null) {
            return null;
        }

        if (path == null || RELATIVE_ROOT.equals(path)) {
            return root;
        }

        // Drop the leading '.', which leaves the path starting with a slash.
        String tail = path.substring(RELATIVE_ROOT.length());

        if (SLASH.equals(root)) {
            return tail;
        }

        return root + tail;
    }

    /**
     * Removes the trailing slashes of a directory name, except for the root
     * directory of the file system.
     */
    public static String normalizeDirectory(String directory) {

        if (directory == null) {
            return null;
        }

        String normalized = directory.replace(BACKSLASH, SLASH);

        while (normalized.length() > 1 && normalized.endsWith(SLASH)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    /**
     * Ensures that a relative path starts with {@link #RELATIVE_ROOT} and does
     * not end with a slash.
     */
    public static String normalizeRelativePath(String path) {

        if (path == null) {
            return null;
        }

        String normalized = path.replace(BACKSLASH, SLASH);

        while (normalized.length() > 1 && normalized.endsWith(SLASH)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (RELATIVE_ROOT.equals(normalized)) {
            return normalized;
        }

        if (normalized.startsWith(RELATIVE_ROOT + SLASH)) {
            return normalized;
        }

        while (normalized.startsWith(SLASH)) {
            normalized = normalized.substring(1);
        }

        if (normalized.length() == 0) {
            return RELATIVE_ROOT;
        }

        return RELATIVE_ROOT + SLASH + normalized;
    }
}
