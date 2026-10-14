/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Value of an LPEX user parameter, e.g.
 * <code>c-a-1 CMOneCompile c-a-f iRPGFormatter.Format</code>.
 * <p>
 * The order of the pairs is kept. A single token at the end, that does not make
 * a complete pair, is kept as remainder and written back unchanged.
 */
public final class PairList {

    private static final String DOUBLE_QUOTES = "\""; //$NON-NLS-1$
    private static final String DELIMITER = " "; //$NON-NLS-1$

    private final List<Pair> pairs;
    private final String remainder;

    public PairList(List<Pair> pairs, String remainder) {
        this.pairs = Collections.unmodifiableList(new ArrayList<Pair>(pairs));
        this.remainder = remainder;
    }

    /**
     * Parses the value of an LPEX user parameter. Enclosing double quotes are
     * removed. The pairs are separated by any white space.
     *
     * @param value - value of the parameter, may be <code>null</code>
     * @return pairs of the value
     */
    public static PairList parse(String value) {

        List<Pair> pairs = new ArrayList<Pair>();

        if (value == null) {
            return new PairList(pairs, null);
        }

        String text = value.trim();
        if (text.startsWith(DOUBLE_QUOTES)) {
            text = text.substring(1);
        }

        if (text.endsWith(DOUBLE_QUOTES)) {
            text = text.substring(0, text.length() - 1);
        }

        text = text.trim();
        if (text.length() == 0) {
            return new PairList(pairs, null);
        }

        String[] tokens = text.split("\\s+"); //$NON-NLS-1$

        int i = 0;
        while (i < tokens.length - 1) {
            pairs.add(new Pair(tokens[i], tokens[i + 1]));
            i = i + 2;
        }

        String remainder = null;
        if (i < tokens.length) {
            remainder = tokens[i];
        }

        return new PairList(pairs, remainder);
    }

    public List<Pair> getPairs() {
        return pairs;
    }

    public String getRemainder() {
        return remainder;
    }

    public boolean hasRemainder() {
        return remainder != null;
    }

    public boolean isEmpty() {
        return pairs.isEmpty() && remainder == null;
    }

    /**
     * Returns a list with the specified pairs removed. The remainder is kept.
     */
    public PairList remove(List<Pair> removedPairs) {

        List<Pair> remainingPairs = new ArrayList<Pair>();
        for (Pair pair : pairs) {
            if (!removedPairs.contains(pair)) {
                remainingPairs.add(pair);
            }
        }

        return new PairList(remainingPairs, remainder);
    }

    /**
     * Returns a list with the specified pairs appended. The remainder stays at
     * the end, so that it does not shift the pairs.
     */
    public PairList append(List<Pair> addedPairs) {

        List<Pair> allPairs = new ArrayList<Pair>(pairs);
        allPairs.addAll(addedPairs);

        return new PairList(allPairs, remainder);
    }

    /**
     * Returns the value as it is passed to LPEX.
     */
    public String format() {

        StringBuilder buffer = new StringBuilder();
        for (Pair pair : pairs) {
            if (buffer.length() > 0) {
                buffer.append(DELIMITER);
            }
            buffer.append(pair.getFirst());
            buffer.append(DELIMITER);
            buffer.append(pair.getSecond());
        }

        if (remainder != null) {
            if (buffer.length() > 0) {
                buffer.append(DELIMITER);
            }
            buffer.append(remainder);
        }

        return buffer.toString();
    }

    @Override
    public String toString() {
        return format();
    }
}
