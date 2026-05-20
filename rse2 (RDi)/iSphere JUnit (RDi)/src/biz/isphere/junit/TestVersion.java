/*******************************************************************************
 * Copyright (c) 2012-2014 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import biz.isphere.core.internal.Version;
import biz.isphere.core.internal.exception.InvalidVersionNumberException;

public class TestVersion {

    @Test
    public void testIllegalVersionNumbers() throws Exception {

        try {
            new Version(null);
            fail("NULL is not allowed for the constructor.");
        } catch (InvalidVersionNumberException e) {
            // exception see ==> OK
            assertTrue(e.getMessage().startsWith("Invalid version number: null."));
        }

        try {
            new Version("2.1.a");
            fail("Version number must not contain characters.");
        } catch (InvalidVersionNumberException e) {
            // exception see ==> OK
            assertTrue(e.getMessage().startsWith("Invalid version number: 2.1.a."));
        }

        try {
            new Version(".1.1");
            fail("Version number must not start with a dot.");
        } catch (InvalidVersionNumberException e) {
            // exception see ==> OK
            assertTrue(e.getMessage().startsWith("Invalid version number: .1.1"));
        }

        try {
            new Version("2.1.1.b");
            fail("Beta part must specify the beta number.");
        } catch (InvalidVersionNumberException e) {
            // exception see ==> OK
            assertTrue(e.getMessage().startsWith("Invalid version number: 2.1.1.b"));
        }

        try {
            new Version("2.1.1.b0001");
            fail("Number of beta part is too long.");
        } catch (InvalidVersionNumberException e) {
            // exception see ==> OK
            assertTrue(e.getMessage().startsWith("Invalid version number: 2.1.1.b0001"));
        }

        try {
            new Version("2.b1.1");
            fail("Beta part must be at the end.");
        } catch (InvalidVersionNumberException e) {
            // exception see ==> OK
            assertTrue(e.getMessage().startsWith("Invalid version number: 2.b1.1"));
        }

        try {
            new Version("2.1.1.r1");
            fail("Release qualifier must not specify a number.");
        } catch (InvalidVersionNumberException e) {
            // exception see ==> OK
            assertTrue(e.getMessage().startsWith("Invalid version number: 2.1.1.r1"));
        }

    }

    @Test
    public void testLegalVersionNumbers() throws Exception {

        assertTrue(new Version("1.10.r").compareTo(new Version("1.1.1.r")) > 0);

        assertTrue(new Version("1.1.r").compareTo(new Version("1.1.1.r")) < 0);
        assertFalse(new Version("1.1.r").equals(new Version("1.1.1.r")));

        assertTrue(new Version("2.0.r").compareTo(new Version("1.9.9.r")) > 0);
        assertFalse(new Version("2.0.r").equals(new Version("1.9.9.r")));

        assertTrue(new Version("1.0.r").compareTo(new Version("1.r")) == 0);
        assertTrue(new Version("1.0.r").equals(new Version("1.r")));

        assertTrue(new Version("1.0.r").compareTo(null) > 0);
        assertFalse(new Version("1.0.r").equals(null));

        List<Version> versions = new ArrayList<Version>();
        versions.add(new Version("1.00.1.r")); // max version
        versions.add(new Version("1.0.5.r"));
        versions.add(new Version("1.01.0.r"));
        versions.add(new Version("2.r")); // min version
        String minVersion = Collections.min(versions).get();
        String maxVersion = Collections.max(versions).get();

        assertEquals("1.0.1.r", minVersion);
        assertEquals("2.r", maxVersion);

        Version a = new Version("2.06.r");
        Version b = new Version("2.060.r");
        assertFalse(a.equals(b));
    }

    @Test
    public void testBetaVersionNumbers() throws Exception {

        assertTrue(new Version("2.4.2.r").compareTo(new Version("2.4.2.b16")) > 0);
        assertTrue(new Version("2.4.r").compareTo(new Version("2.4.1.b1")) < 0);
        assertTrue(new Version("2.4.r").compareTo(new Version("2.4.b1")) > 0);

        assertTrue(new Version("2.4.2.b1").compareTo(new Version("2.4.2.b016")) < 0);
        assertTrue(new Version("2.4.2.b005").compareTo(new Version("2.4.2.b6")) < 0);

        assertFalse(new Version("2.4.2.r").isBeta());
        assertTrue(new Version("2.4.2.b010").isBeta());
        assertTrue(new Version("2.4.2.b5").isBeta());
        assertTrue(new Version("2.4.b1").isBeta());

    }

    @Test
    public void testOldVersionNumbers() throws Exception {

        assertTrue(new Version("2.4.0").get().equals("2.4.0.r"));
        assertTrue(new Version("2.5.0").get().equals("2.5.0.r"));
        assertTrue(new Version("2.5.1").get().equals("2.5.1.r"));
        assertTrue(new Version("2.5.2").get().equals("2.5.2.r"));

    }

}
