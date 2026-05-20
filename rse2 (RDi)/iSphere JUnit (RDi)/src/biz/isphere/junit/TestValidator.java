/*******************************************************************************
 * Copyright (c) project_year-2018 project_team
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.junit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import biz.isphere.core.internal.Validator;

public class TestValidator {

    @Test
    public void testNameInstanceWithoutCcsid() throws Exception {

        try {
            Validator.getNameInstance(null);
            fail("Expected an IllegalArgumentEception due to nissing ccsid.");
        } catch (IllegalArgumentException e) {
            // Exception seen
        }
    }

    @Test
    public void testNameInstanceWithCcsid() throws Exception {

        Validator nameValidator = Validator.getNameInstance(Integer.valueOf(1141));

        boolean isValid = false;

        isValid = nameValidator.validate("");
        assertFalse("isValid must be false, because 'name' is empty", isValid);

        isValid = nameValidator.validate("F:OO");
        assertFalse("isValid must be false, because of ivalid character ':'", isValid);

        isValid = nameValidator.validate("*FOO");
        assertFalse("isValid must be false, because of invalid character '*'", isValid);

        // German special characters
        isValid = nameValidator.validate("FOO$§#");
        assertTrue("isValid must be true, because 'name' is valid", isValid);

        // US special characters, but German validator
        isValid = nameValidator.validate("FOO$@#");
        assertFalse("isValid must be false, because of invalid character '@'", isValid);

        nameValidator = Validator.getNameInstance(37);

        // US special characters
        isValid = nameValidator.validate("FOO$@#");
        assertTrue("isValid must be true, because 'name' is valid", isValid);
    }

    @Test
    public void testLibraryNameInstanceWithoutCcsid() throws Exception {

        try {
            Validator.getLibraryNameInstance(null);
            fail("Expected an IllegalArgumentEception due to nissing ccsid.");
        } catch (IllegalArgumentException e) {
            // Exception seen
        }
    }

    @Test
    public void testLibraryNameInstanceWithCcsid() throws Exception {

        Validator libraryNameValidator = Validator.getLibraryNameInstance(Integer.valueOf(1141));

        boolean isValid = false;

        isValid = libraryNameValidator.validate("");
        assertFalse("isValid must be false, because 'name' is empty", isValid);

        isValid = libraryNameValidator.validate("F:OO");
        assertFalse("isValid must be false, because of ivalid character ':'", isValid);

        isValid = libraryNameValidator.validate("*FOO");
        assertFalse("isValid must be false, because of invalid character '*'", isValid);

        // German special characters
        isValid = libraryNameValidator.validate("FOO$§#");
        assertTrue("isValid must be true, because 'name' is valid", isValid);

        // US special characters, but German validator
        isValid = libraryNameValidator.validate("FOO$@#");
        assertFalse("isValid must be true, because of invalid character '@'", isValid);

        libraryNameValidator = Validator.getLibraryNameInstance(Integer.valueOf(37));

        // US special characters
        isValid = libraryNameValidator.validate("FOO$@#");
        assertTrue("isValid must be true, because 'name' is valid", isValid);
    }

    @Test
    public void testLibraryNameInstanceWithCcsidAndWithSpecialValues() throws Exception {

        Validator libraryNameValidator = Validator.getLibraryNameInstance(Integer.valueOf(1141), "*LIBL", "*CURLIB");

        boolean isValid = false;

        isValid = libraryNameValidator.validate("*LIBL");
        assertTrue("isValid must be true, because '*LIBL' is valid", isValid);

        libraryNameValidator = Validator.getLibraryNameInstance(Integer.valueOf(37), "*LIBL", "*CURLIB");

        isValid = libraryNameValidator.validate("*CURLIB");
        assertTrue("isValid must be true, because '*CURLIB' is valid", isValid);

        isValid = libraryNameValidator.validate("*ALLUSR");
        assertFalse("isValid must be false, because '*ALLUSR' is invalid", isValid);
    }

}
