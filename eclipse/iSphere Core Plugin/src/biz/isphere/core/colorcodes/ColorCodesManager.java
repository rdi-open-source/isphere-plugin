/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Team
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.colorcodes;

import java.util.HashSet;
import java.util.Set;

public class ColorCodesManager {

    /**
     * Color codes (x'20' to x'3F') as they are translated from EBDIC to UTF-8.
     * Some EBCDIC codes are translated to the same value in UTF-8. Duplicates
     * are resolved by variable 'colorCodesSet'.
     */
    private static final int X20 = 128; // green
    private static final int X21 = 129; // green/reverse
    private static final int X22 = 130; // white
    private static final int X23 = 131; // white/inverse
    private static final int X24 = 132; // green/underline
    private static final int X25 = 9226; // green/inverse/underline
    private static final int X26 = 23; // white/underline
    private static final int X27 = 27; // non-display 1
    private static final int X28 = 136; // red
    private static final int X29 = 137; // red/underline
    private static final int X2A = 129; // red/blink
    private static final int X2B = 139; // red/inverse/blink
    private static final int X2C = 140; // red/underline
    private static final int X2D = 5; // red/inverse/underline
    private static final int X2E = 6; // red/blink/underline
    private static final int X2F = 7; // non-display 2

    private static final int X30 = 144; // turquoise
    private static final int X31 = 145; // turquoise/inverse/colum separator
    private static final int X32 = 22; // yellow
    private static final int X33 = 147; // yellow/inverse/colum separator
    private static final int X34 = 148; // turquoise/underline
    private static final int X35 = 149; // turquoise/inverse/underline
    private static final int X36 = 150; // yellow/underline
    private static final int X37 = 4; // non-display 3
    private static final int X38 = 152; // pink
    private static final int X39 = 153; // pink/inverse
    private static final int X3A = 154; // blue
    private static final int X3B = 155; // blue/inverse
    private static final int X3C = 20; // pink/underline
    private static final int X3D = 21; // pink/inverse/underline
    private static final int X3E = 158; // blue/underline
    private static final int X3F = 26; // non-display 4

    private static final String SPACE = " ";

    private static final int[] COLOR_CODES_LIST = { X20, X21, X22, X23, X24, X25, X26, X27, X28, X29, X2A, X2B, X2C, X2D, X2E, X2F, X30, X31, X32,
        X33, X34, X35, X36, X37, X38, X39, X3A, X3B, X3C, X3D, X3E, X3F };

    private Set<Integer> colorCodesSet;

    public ColorCodesManager() {
        colorCodesSet = new HashSet<Integer>();
        for (int i : COLOR_CODES_LIST) {
            colorCodesSet.add(i);
        }
    }

    public String replaceAll(String value) {

        StringBuilder valueReplaced = new StringBuilder(value);

        for (int charOffset = 0; charOffset < valueReplaced.length(); charOffset++) {
            int charAt = valueReplaced.charAt(charOffset);
            if (colorCodesSet.contains(charAt)) {
                valueReplaced.replace(charOffset, charOffset + 1, SPACE);
            }
        }

        return valueReplaced.toString();
    }

    public Set<Integer> getColorCodes() {
        return colorCodesSet;
    }

}
