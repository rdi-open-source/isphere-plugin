/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.logic;

/**
 * Entry of an LPEX user parameter:
 * <ul>
 * <li>user actions and user commands: name and class</li>
 * <li>user key actions: key and action</li>
 * <li>user mouse actions: mouse event and action</li>
 * </ul>
 * Two pairs are equal, if both parts are equal.
 */
public final class Pair {

    private final String first;
    private final String second;

    public Pair(String first, String second) {

        if (first == null || second == null) {
            throw new IllegalArgumentException("Parts of a pair must not be null."); //$NON-NLS-1$
        }

        this.first = first;
        this.second = second;
    }

    public String getFirst() {
        return first;
    }

    public String getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }

        if (!(object instanceof Pair)) {
            return false;
        }

        Pair other = (Pair)object;

        return first.equals(other.first) && second.equals(other.second);
    }

    @Override
    public int hashCode() {
        return 31 * first.hashCode() + second.hashCode();
    }

    @Override
    public String toString() {
        return first + " " + second; //$NON-NLS-1$
    }
}
