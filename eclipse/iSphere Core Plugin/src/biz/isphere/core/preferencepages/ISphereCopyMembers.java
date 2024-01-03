/*******************************************************************************
 * Copyright (c) 2012-2015 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.core.preferencepages;

import java.util.Arrays;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import biz.isphere.core.ISpherePlugin;
import biz.isphere.core.Messages;
import biz.isphere.core.memberrename.adapters.IAdapterModificationListener;
import biz.isphere.core.memberrename.adapters.IMemberRenamingRuleAdapter;
import biz.isphere.core.memberrename.factories.MemberRenamingRuleFactory;
import biz.isphere.core.memberrename.rules.IMemberRenamingRule;
import biz.isphere.core.preferences.Preferences;
import biz.isphere.core.swt.widgets.WidgetFactory;

public class ISphereCopyMembers extends PreferencePage implements IWorkbenchPreferencePage, IAdapterModificationListener {

    private Combo comboMemberRenamingRule;
    private Composite mainPageContainer;
    private Group groupRenamingRule;
    private Composite dynamicRuleArea;
    private Button chkBoxEnableMemberPrecheck;

    private IMemberRenamingRule[] rules;

    public ISphereCopyMembers() {
        super();
        setPreferenceStore(ISpherePlugin.getDefault().getPreferenceStore());
        getPreferenceStore();

        this.rules = MemberRenamingRuleFactory.getInstance().getRules();
    }

    public void init(IWorkbench workbench) {
    }

    @Override
    public Control createContents(Composite parent) {

        Composite container = new Composite(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout();
        container.setLayout(gridLayout);

        this.mainPageContainer = container;

        createSectionMemberRenamingRule(container);

        IMemberRenamingRule rule = Preferences.getInstance().getMemberRenamingRule();
        String ruleLabel = rule.getLabel();

        createDynamicRuleArea(ruleLabel);

        createAdditonalOptionsArea(container);

        setScreenToValues(ruleLabel);

        return container;
    }

    private void createSectionMemberRenamingRule(Composite parent) {

        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(2, false));
        main.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label labelMemberRenamingRule = new Label(main, SWT.NONE);
        labelMemberRenamingRule.setLayoutData(new GridData());
        labelMemberRenamingRule.setText(Messages.Label_Rename_member_rule_column);
        labelMemberRenamingRule.setToolTipText(Messages.Tooltip_Specifies_the_rule_for_creating_a_backup_name);

        comboMemberRenamingRule = WidgetFactory.createReadOnlyCombo(main);
        comboMemberRenamingRule.setToolTipText(Messages.Tooltip_Specifies_the_rule_for_creating_a_backup_name);
        GridData comboMemberRenamingRuleLayoutData = new GridData(GridData.FILL_HORIZONTAL);
        // comboMemberRenamingRuleLayoutData.widthHint = 180;
        comboMemberRenamingRule.setLayoutData(comboMemberRenamingRuleLayoutData);
        comboMemberRenamingRule.addSelectionListener(new MemberRenamingRuleSelectionListener());
        fillMemberRenamingRuleCombo();

        groupRenamingRule = new Group(main, SWT.NONE);
        groupRenamingRule.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 2, 1));
        GridLayout dynamicRuleAreaContainerLayout = new GridLayout(1, true);
        groupRenamingRule.setLayout(dynamicRuleAreaContainerLayout);
        groupRenamingRule.setText(Messages.Label_Configuration);
        groupRenamingRule.setToolTipText(Messages.Tooltip_Renaming_rule_configuration);
        dynamicRuleAreaContainerLayout.marginHeight = 0;
        dynamicRuleAreaContainerLayout.marginWidth = 0;
    }

    private void createDynamicRuleArea(String ruleLabel) {

        if (groupRenamingRule == null) {
            // happens, when the page is being build
            return;
        }

        if (dynamicRuleArea != null) {
            dynamicRuleArea.dispose();
            dynamicRuleArea = null;
        }

        IMemberRenamingRuleAdapter adapter = getRuleAdapter(ruleLabel);

        if (adapter == null) {
            dynamicRuleArea = createAdapterNotFoundArea(groupRenamingRule, ruleLabel);
        } else {
            adapter.addAdapterModificationListener(this);
            dynamicRuleArea = adapter.createComposite(groupRenamingRule);
        }

        mainPageContainer.layout(true, true);
    }

    private void createAdditonalOptionsArea(Composite parent) {

        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(2, false));
        main.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        chkBoxEnableMemberPrecheck = WidgetFactory.createCheckbox(main, Messages.Label_Enable_member_precheck);
        chkBoxEnableMemberPrecheck.setToolTipText(Messages.Tooltip_Enable_member_precheck);
        chkBoxEnableMemberPrecheck.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false, 2, 1));

    }

    private void loadMemberRenamingRuleValues() {

        String ruleLabel = getMemberRenamingRuleLabel();

        IMemberRenamingRuleAdapter adapter = getRuleAdapter(ruleLabel);
        if (adapter != null) {
            adapter.loadPreferences();
        }
    }

    private void loadMemberRenamingRuleDefaultValues() {

        String ruleLabel = getMemberRenamingRuleLabel();

        IMemberRenamingRuleAdapter adapter = getRuleAdapter(ruleLabel);
        if (adapter != null) {
            adapter.loadDefaultPreferences();
        }
    }

    private String getMemberRenamingRuleLabel() {
        return comboMemberRenamingRule.getText();
    }

    private Composite createAdapterNotFoundArea(Composite parent, String ruleLabel) {

        Composite mainArea = new Composite(parent, SWT.NONE);
        mainArea.setLayout(new GridLayout(2, false));
        mainArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label label = new Label(mainArea, SWT.NONE);
        label.setLayoutData(new GridData());
        label.setText(NLS.bind(Messages.Error_Renaming_rule_UI_dapter_not_found_A, ruleLabel));

        return mainArea;
    }

    @Override
    protected void performApply() {
        setStoreToValues();
        super.performApply();
    }

    @Override
    protected void performDefaults() {
        setScreenToDefaultValues();
        super.performDefaults();
    }

    @Override
    public boolean performOk() {

        if (!checkAllValues()) {
            return false;
        }

        setStoreToValues();

        return super.performOk();
    }

    protected void setStoreToValues() {

        String ruleLabel = getMemberRenamingRuleLabel();

        for (IMemberRenamingRule rule : rules) {
            if (rule.getLabel().equals(ruleLabel)) {
                Preferences.getInstance().setMemberRenamingRule(rule);
            }
        }

        IMemberRenamingRuleAdapter adapter = getRuleAdapter(ruleLabel);
        adapter.storePreferences();

        Preferences.getInstance().setMemberRenamingPrechek(chkBoxEnableMemberPrecheck.getSelection());
    }

    protected void setScreenToValues(String ruleLabel) {

        comboMemberRenamingRule.setText(ruleLabel);

        IMemberRenamingRuleAdapter adapter = getRuleAdapter(ruleLabel);
        adapter.loadPreferences();

        chkBoxEnableMemberPrecheck.setSelection(Preferences.getInstance().isMemberRenamingPrecheck());
    }

    protected void setScreenToDefaultValues() {

        String ruleLabel = comboMemberRenamingRule.getText();

        createDynamicRuleArea(ruleLabel);
        loadMemberRenamingRuleDefaultValues();

        chkBoxEnableMemberPrecheck.setSelection(Preferences.getInstance().getDefaultIsMemberRenamingPrecheck());
    }

    private void fillMemberRenamingRuleCombo() {

        String[] labels = new String[rules.length];
        for (int i = 0; i < rules.length; i++) {
            labels[i] = rules[i].getLabel();
        }

        Arrays.sort(labels);

        comboMemberRenamingRule.setItems(labels);
    }

    private boolean validateMemberRenamingRule() {

        if (dynamicRuleArea == null || dynamicRuleArea.isDisposed()) {
            return false;
        }

        String ruleLabel = getMemberRenamingRuleLabel();
        IMemberRenamingRuleAdapter adapter = getRuleAdapter(ruleLabel);
        if (adapter == null) {
            comboMemberRenamingRule.setFocus();
            setError(Messages.bind(Messages.Error_Renaming_rule_not_found_A, ruleLabel));
            return false;
        }

        String message = adapter.validatePreferences();
        if (message != null) {
            setError(message);
            return false;
        }

        clearError();

        return true;
    }

    private boolean checkAllValues() {

        if (!validateMemberRenamingRule()) {
            return false;
        }

        return clearError();
    }

    private boolean setError(String message) {
        setErrorMessage(message);
        setValid(false);
        return false;
    }

    private boolean clearError() {
        setErrorMessage(null);
        setValid(true);
        return true;
    }

    private IMemberRenamingRuleAdapter getRuleAdapter(String ruleLabel) {

        for (IMemberRenamingRule rule : rules) {
            if (rule.getLabel().equals(ruleLabel)) {
                return rule.getAdapter();
            }
        }

        return null;
    }

    private class MemberRenamingRuleSelectionListener implements SelectionListener {
        public void widgetDefaultSelected(SelectionEvent arg0) {
            return;
        }

        public void widgetSelected(SelectionEvent event) {
            if (validateMemberRenamingRule()) {
                checkAllValues();
                if (isValid()) {
                    Combo combo = (Combo)event.getSource();
                    String ruleLabel = combo.getText();
                    createDynamicRuleArea(ruleLabel);
                    loadMemberRenamingRuleValues();
                    if (isValid()) {
                        combo.setFocus();
                    }
                }
            }
        }
    }

    public void changed(TypedEvent event) {
        checkAllValues();
    }
}