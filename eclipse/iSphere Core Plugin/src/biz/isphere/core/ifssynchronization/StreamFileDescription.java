/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifssynchronization;

import java.io.Serializable;
import java.sql.Timestamp;

import biz.isphere.base.internal.IFSFileHelper;

/**
 * This class defines the attributes of a comparable IFS file description used
 * by the iSphere Synchronize IFS files editor.
 * <p>
 * An item is identified by its path <i>relative</i> to the root directory of
 * the compared directory tree, because that is the only way a left item can be
 * matched with its right counterpart. Given a left root of
 * <code>/home/raddatz/isphere</code> and a right root of
 * <code>/home/raddatz/isphere-dvp</code>, both items
 * <code>/home/raddatz/isphere/QRPGLESRC/DEMO12.RPGLE</code> and
 * <code>/home/raddatz/isphere-dvp/QRPGLESRC/DEMO12.RPGLE</code> share the
 * relative path <code>./QRPGLESRC/DEMO12.RPGLE</code>.
 * <p>
 * The absolute path, which is required for copying, displaying and deleting an
 * item, is produced from the root directory and the relative path. See
 * {@link #getAbsolutePath()} and {@link IFSFileHelper}.
 */
public class StreamFileDescription implements Serializable, Comparable<StreamFileDescription> {

    private static final long serialVersionUID = 7291143070825518072L;

    private String type;
    private String connectionName;
    private String rootDirectory;
    private String relativePath;
    private Timestamp lastChangedDate;
    private Long checksum;

    /**
     * The directory description this file or directory was found in, set by the
     * job that loads the compare data. Intentionally excluded from
     * {@link #equals(Object)}, {@link #hashCode()} and
     * {@link #compareTo(StreamFileDescription)} (identity/navigation only, not
     * part of the value) and marked transient, since it is not meant to survive
     * serialization.
     */
    private transient StreamFileDescription parentDirectory;

    public static StreamFileDescription newFileDescription() {
        return new StreamFileDescription("F");
    }

    public static StreamFileDescription newDirectoryDescription() {
        return new StreamFileDescription("D");
    }

    private StreamFileDescription(String type) {
        this.type = type;
    }

    public boolean isDirectory() {
        return "D".equals(type);
    }

    public boolean isFile() {
        return !("D".equals(type));
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public String getConnectionName() {
        return connectionName;
    }

    /**
     * @return absolute path of the root directory of the compared directory
     *         tree, for example <code>/home/raddatz/isphere</code>
     */
    public String getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = IFSFileHelper.normalizeDirectory(rootDirectory);
    }

    /**
     * @return path of this item relative to the root directory, for example
     *         <code>./QRPGLESRC/DEMO12.RPGLE</code>, or
     *         {@link IFSFileHelper#RELATIVE_ROOT} for the root directory itself
     */
    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = IFSFileHelper.normalizeRelativePath(relativePath);
    }

    /**
     * @return absolute path of this item, for example
     *         <code>/home/raddatz/isphere/QRPGLESRC/DEMO12.RPGLE</code>
     */
    public String getAbsolutePath() {
        return IFSFileHelper.toAbsolutePath(getRootDirectory(), getRelativePath());
    }

    public Timestamp getLastChangedDate() {
        return lastChangedDate;
    }

    public void setLastChangedDate(Timestamp lastChangedDate) {
        this.lastChangedDate = lastChangedDate;
    }

    public Long getChecksum() {
        return checksum;
    }

    public void setChecksum(Long checksum) {
        this.checksum = checksum;
    }

    /**
     * @return absolute path of this item, for displaying it to the user
     */
    public String getQualifiedIfsFileName() {
        return getAbsolutePath();
    }

    public StreamFileDescription getParentDirectory() {
        return parentDirectory;
    }

    public void setParentDirectory(StreamFileDescription parentDirectory) {
        this.parentDirectory = parentDirectory;
    }

    /**
     * Compares this IFS file description with another IFS file description. The
     * <i>root directory</i> is intentionally not compared, because the left and
     * the right root directory of a compared directory tree differ by design.
     */
    public int compareTo(StreamFileDescription other) {

        if (other == null) {
            return 1;
        }

        int rc = compareTo(relativePath, other.getRelativePath());
        if (rc == 0) {
            rc = compareTo(lastChangedDate, other.getLastChangedDate());
            if (rc == 0) {
                rc = compareTo(checksum, other.getChecksum());
            }
        }

        return rc;
    }

    private int compareTo(String me, String other) {
        if (me == null && other == null) {
            return 0;
        } else if (me == null && other != null) {
            return -1;
        } else if (me != null && other == null) {
            return 1;
        } else {
            return me.compareToIgnoreCase(other);
        }
    }

    private int compareTo(Timestamp me, Timestamp other) {
        if (me == null && other == null) {
            return 0;
        } else if (me == null && other != null) {
            return -1;
        } else if (me != null && other == null) {
            return 1;
        } else {
            return me.compareTo(other);
        }
    }

    private int compareTo(Long me, Long other) {
        if (me == null && other == null) {
            return 0;
        } else if (me == null && other != null) {
            return -1;
        } else if (me != null && other == null) {
            return 1;
        } else {
            return me.compareTo(other);
        }
    }

    /**
     * Produces the hash code of this IFS file description. The <i>root
     * directory</i> is intentionally not included.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((checksum == null) ? 0 : checksum.hashCode());
        result = prime * result + ((relativePath == null) ? 0 : relativePath.hashCode());
        result = prime * result + ((lastChangedDate == null) ? 0 : lastChangedDate.hashCode());
        return result;
    }

    /**
     * Tests whether this IFS file description equals another IFS file
     * description. The <i>root directory</i> is intentionally not compared.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        StreamFileDescription other = (StreamFileDescription)obj;
        if (checksum == null) {
            if (other.checksum != null) return false;
        } else if (!checksum.equals(other.checksum)) return false;
        if (relativePath == null) {
            if (other.relativePath != null) return false;
        } else if (!relativePath.equals(other.relativePath)) return false;
        if (lastChangedDate == null) {
            if (other.lastChangedDate != null) return false;
        } else if (!lastChangedDate.equals(other.lastChangedDate)) return false;
        return true;
    }

    @Override
    public String toString() {
        return getAbsolutePath();
    }
}
