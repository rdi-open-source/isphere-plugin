/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.ide.lpex.menu.logic;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compares LPEX key names, such as <code>c-a-f</code>, <code>a-c-F</code> or
 * <code>c-s-o.t</code>. LPEX accepts the modifiers <code>a</code>,
 * <code>c</code>, <code>m</code>, <code>r</code> and <code>s</code> in any
 * order and case. A key name without context suffix applies to the text area
 * only, like the suffix <code>.t</code>.
 * <p>
 * The comparison ignores the case of the key itself. Two names that LPEX might
 * distinguish are therefore regarded as the same key. That errs on the safe
 * side: a key of the user is never overwritten.
 */
public final class KeyNames {

    private static final String MODIFIERS = "acmrs"; //$NON-NLS-1$
    private static final String CONTEXTS = "tpc"; //$NON-NLS-1$
    private static final String TEXT_AREA = "t"; //$NON-NLS-1$

    private KeyNames() {
    }

    /**
     * Returns <code>true</code>, if both key names address the same key in at
     * least one area (text, prefix area or command line).
     */
    public static boolean isSameKey(String keyName1, String keyName2) {

        if (keyName1 == null || keyName2 == null) {
            return false;
        }

        String[] key1 = parse(keyName1);
        String[] key2 = parse(keyName2);

        if (!key1[0].equals(key2[0])) {
            return false;
        }

        for (char context : key1[1].toCharArray()) {
            if (key2[1].indexOf(context) >= 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns <code>true</code>, if the key is used by one of the specified key
     * actions.
     *
     * @param keyName - key that is checked
     * @param keyActions - key actions, first part of each pair is the key
     */
    public static boolean isAssigned(String keyName, List<Pair> keyActions) {

        for (Pair keyAction : keyActions) {
            if (isSameKey(keyName, keyAction.getFirst())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the normalized key and the sorted contexts.
     */
    private static String[] parse(String keyName) {

        String text = keyName.trim();

        String suffix = ""; //$NON-NLS-1$
        int dot = text.indexOf('.');
        if (dot >= 0) {
            suffix = text.substring(dot + 1);
            text = text.substring(0, dot);
        }

        Set<Character> modifiers = new HashSet<Character>();
        while (text.length() > 2 && text.charAt(1) == '-' && MODIFIERS.indexOf(Character.toLowerCase(text.charAt(0))) >= 0) {
            modifiers.add(Character.valueOf(Character.toLowerCase(text.charAt(0))));
            text = text.substring(2);
        }

        StringBuilder key = new StringBuilder();
        for (char modifier : MODIFIERS.toCharArray()) {
            if (modifiers.contains(Character.valueOf(modifier))) {
                key.append(modifier);
                key.append('-');
            }
        }
        key.append(text.toLowerCase());

        return new String[] { key.toString(), parseContexts(suffix) };
    }

    private static String parseContexts(String suffix) {

        StringBuilder contexts = new StringBuilder();

        // LPEX rejects other tokens than "t", "p", "c" and "secondary".
        for (String token : Arrays.asList(suffix.split("\\."))) { //$NON-NLS-1$
            if (token.length() == 1 && CONTEXTS.indexOf(Character.toLowerCase(token.charAt(0))) >= 0) {
                char context = Character.toLowerCase(token.charAt(0));
                if (contexts.indexOf(String.valueOf(context)) < 0) {
                    contexts.append(context);
                }
            }
        }

        if (contexts.length() == 0) {
            contexts.append(TEXT_AREA);
        }

        return contexts.toString();
    }
}
