/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifsfilerename.rules;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.ibm.as400.access.AS400;

import biz.isphere.base.internal.StringHelper;
import biz.isphere.core.Messages;
import biz.isphere.core.memberrename.exceptions.NoMoreNamesAvailableException;

/**
 * Renaming rule that appends a numerical extension to the name of the IFS file
 * that is renamed, for example <code>myFile.txt</code> becomes
 * <code>myFile.txt.01</code>.
 * <p>
 * This rule is the IFS counterpart of
 * {@link biz.isphere.core.memberrename.rules.MemberRenamingRuleNumber}. The
 * default values match those of the member renaming rule.
 */
public class StreamFileRenamingRuleNumber extends AbstractStreamFileRenamingRule {

    private static final long serialVersionUID = -5074836946398130327L;

    public static String ID = "biz.isphere.core.ifsfilerename.rules.number"; //$NON-NLS-1$

    private static final String DEFAULT_DELIMITER = "."; //$NON-NLS-1$
    private static final int DEFAULT_MIN_VALUE = 1;
    private static final int DEFAULT_MAX_VALUE = 99;

    private String delimiter;
    private int minValue;
    private int maxValue;
    private boolean isFillGaps;

    private transient Pattern fileNameFilterPattern;

    private int currentValue;

    public StreamFileRenamingRuleNumber() {
        this(Messages.Label_Renaming_rule_Numerical);
    }

    protected StreamFileRenamingRuleNumber(String label) {
        super(label);

        this.delimiter = DEFAULT_DELIMITER;
        this.minValue = DEFAULT_MIN_VALUE;
        this.maxValue = DEFAULT_MAX_VALUE;
        this.isFillGaps = false;
    }

    /**
     * @return returns <code>true</code>, when gaps in existing IFS file names
     *         are filled, else <code>false</code>.
     */
    public boolean isFillGapsEnabled() {
        return isFillGaps;
    }

    /**
     * @param enabled - specifies whether gaps in the list of existing backup
     *        IFS file names are filled or not.
     */
    public void setFillGapsEnabled(boolean enabled) {
        this.isFillGaps = enabled;
    }

    /**
     * @return delimiter used for producing a backup IFS file name.
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * @param delimiter - specifies the delimiter used for producing a backup
     *        IFS file name.
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter.trim();
    }

    /**
     * @return starting value of the extension that is added to the original IFS
     *         file name, when producing a backup name.
     */
    public int getMinValue() {
        return minValue;
    }

    /**
     * @param minValue - specifies the starting value of the extension that is
     *        added to the original IFS file name, when producing a backup name.
     */
    public void setMinValue(int minValue) {
        this.minValue = minValue;
    }

    /**
     * @return maximum value of the extension that is added to the original IFS
     *         file name, when producing a backup name.
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * @param maxValue - specifies the maximum value of the extension that is
     *        added to the original IFS file name, when producing a backup name.
     */
    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    @Override
    public void initialize(AS400 system, String ifsFilePath) throws Exception {
        super.initialize(system, ifsFilePath);

        if (StringHelper.isNullOrEmpty(getDelimiter())) {
            throw new IllegalArgumentException("Invalid delimter. Delimiter must not be empty"); //$NON-NLS-1$
        }

        String fileNameFilterMask = "^" + Pattern.quote(getBaseFileName() + getDelimiter()) //$NON-NLS-1$
            + "[0-9]{" + getLengthOfExtension() + "}$"; //$NON-NLS-1$ //$NON-NLS-2$

        this.fileNameFilterPattern = Pattern.compile(fileNameFilterMask);

        String[] ifsFilesOnSystem = loadIfsFileList(getFileNameFilter());

        calculateLastFileNameUsedOnSystem(ifsFilesOnSystem);
    }

    public String getNextName() throws NoMoreNamesAvailableException, PropertyVetoException {

        if (currentValue >= getMaxValue()) {
            throw new NoMoreNamesAvailableException();
        }

        if (currentValue <= getMinValue() - 1) {
            currentValue = getMinValue() - 1;
        }

        String nextFileName = null;
        while (nextFileName == null) {

            currentValue++;

            nextFileName = formatName(getBaseFileName(), currentValue);
            if (getAbsolutePath(nextFileName).length() > MAX_PATH_LENGTH) {
                // Name too long. Actor will throw exception.
                return getAbsolutePath(nextFileName);
            }

            if (exists(nextFileName)) {
                // May happen, when fill gaps is enabled.
                nextFileName = null;
                if (currentValue >= getMaxValue()) {
                    throw new NoMoreNamesAvailableException();
                }
            }
        }

        return getAbsolutePath(nextFileName);
    }

    private String getFileNameFilter() {
        return getBaseFileName() + getDelimiter() + "*"; //$NON-NLS-1$
    }

    /*
     * Exported for JUnit tests only.
     */
    public boolean isMatchingName(String fileName) {

        if (fileNameFilterPattern == null) {
            return false;
        }

        return fileNameFilterPattern.matcher(fileName).matches();
    }

    /*
     * Exported for JUnit tests only.
     */
    public void calculateLastFileNameUsedOnSystem(String[] existingFileNames) {

        // Initialize the current value to its starting value.
        this.currentValue = getMinValue() - 1;

        if (existingFileNames == null || existingFileNames.length == 0 || isFillGapsEnabled()) {
            return;
        }

        // Determine the last (highest) IFS file name found on the system.
        List<String> matchingFileNames = new ArrayList<String>();
        for (int i = 0; i < existingFileNames.length; i++) {
            if (isMatchingName(existingFileNames[i])) {
                matchingFileNames.add(existingFileNames[i]);
            }
        }

        if (matchingFileNames.size() <= 0) {
            return;
        }

        Collections.sort(matchingFileNames);
        String lastFileNameUsed = matchingFileNames.get(matchingFileNames.size() - 1);

        // Set the current value to the last (highest) name found on the system.
        this.currentValue = retrieveCurrentValue(lastFileNameUsed);
    }

    private String formatName(String fileName, int currentCount) {

        int numDigits = getLengthOfExtension();
        String extension = StringHelper.getFixLengthLeading(Integer.toString(currentCount), numDigits, "0"); //$NON-NLS-1$

        return String.format("%s%s%s", fileName, getDelimiter(), extension); //$NON-NLS-1$
    }

    private int getLengthOfExtension() {
        return Integer.toString(getMaxValue()).length();
    }

    private int retrieveCurrentValue(String fileName) {

        if (!StringHelper.isNullOrEmpty(fileName)) {

            int i = fileName.lastIndexOf(getDelimiter());
            if (i >= 0) {

                String extension = fileName.substring(i + getDelimiter().length());

                try {
                    return Integer.parseInt(extension);
                } catch (NumberFormatException e) {
                    // Ignore the name and start over again.
                }
            }
        }

        return getMinValue() - 1;
    }
}
