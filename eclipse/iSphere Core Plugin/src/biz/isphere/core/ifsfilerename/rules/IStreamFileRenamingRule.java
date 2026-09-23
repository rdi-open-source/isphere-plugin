/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.ifsfilerename.rules;

import java.beans.PropertyVetoException;

import com.ibm.as400.access.AS400;

import biz.isphere.core.memberrename.exceptions.NoMoreNamesAvailableException;

/**
 * This class specifies the rules that are applied when producing a new backup
 * name of an IFS file.
 * <p>
 * The interface is the IFS counterpart of
 * {@link biz.isphere.core.memberrename.rules.IMemberRenamingRule}. Unlike the
 * member renaming rules, the IFS file renaming rules do not yet come with a UI
 * adapter, because they are not yet configurable on a preference page. Add a
 * <code>getAdapter()</code> method here, when the rules become configurable.
 */
public interface IStreamFileRenamingRule {

    /**
     * Returns the UI label of the rule.
     * 
     * @return UI label
     */
    public String getLabel();

    /**
     * Sets the path of the IFS file that is renamed. This is the first method
     * that must be called after a new rule has been created.
     * 
     * @param system - system that hosts the IFS file
     * @param ifsFilePath - absolute path of the IFS file that is renamed
     * @throws Exception
     */
    public void initialize(AS400 system, String ifsFilePath) throws Exception;

    /**
     * Returns the next backup name of the IFS file.
     * 
     * @return absolute path of the next backup IFS file
     * @throws NoMoreNamesAvailableException
     * @throws PropertyVetoException
     */
    public String getNextName() throws NoMoreNamesAvailableException, PropertyVetoException;
}
