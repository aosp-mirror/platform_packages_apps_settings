/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.notification.zen;

import static android.provider.Settings.Global.ZEN_MODE;
import static android.provider.Settings.Global.ZEN_MODE_ALARMS;
import static android.provider.Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
import static android.provider.Settings.Global.ZEN_MODE_NO_INTERRUPTIONS;
import static android.provider.Settings.Global.ZEN_MODE_OFF;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ZenRule;
import android.util.ArrayMap;

import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.notification.zen.AbstractZenModePreferenceController.ZenModeConfigWrapper;
import com.android.settings.notification.zen.ZenModeSettingsFooterPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class ZenModeSettingsFooterPreferenceControllerTest {

    private static final String TEST_APP_NAME = "test_app";
    private static final String TEST_RULE_NAME = "test_rule_name";
    private static final String MANUAL_RULE_FIELD = "manualRule";
    private static final String AUTOMATIC_RULES_FIELD = "automaticRules";

    private ZenModeSettingsFooterPreferenceController mController;

    private final ArrayMap<String, ZenRule> mInjectedAutomaticRules = new ArrayMap<>();

    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private Preference mockPref;
    @Mock
    private ZenModeConfig mZenModeConfig;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private ZenModeConfigWrapper mConfigWrapper;

    private Context mContext;
    private ContentResolver mContentResolver;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);

        mContext = RuntimeEnvironment.application;
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        when(mNotificationManager.getZenModeConfig()).thenReturn(mZenModeConfig);

        mController = new ZenModeSettingsFooterPreferenceController(mContext, mock(Lifecycle.class),
                mock(FragmentManager.class));
        ReflectionHelpers.setField(mZenModeConfig, AUTOMATIC_RULES_FIELD, mInjectedAutomaticRules);
        ReflectionHelpers.setField(mController, "mZenModeConfigWrapper", mConfigWrapper);

        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(mockPref);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void totalSilence_footerIsAvailable() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_NO_INTERRUPTIONS);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void alarmsOnly_footerIsAvailable() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_ALARMS);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void priorityOnly_footerIsAvailable() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void zenModeOff_footerIsNotAvailable() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_OFF);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testDefaultNotifPolicy_app_manualRule_setFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        injectManualRuleFromApp();
        mController.updateState(mockPref);

        verify(mockPref).setTitle(mContext.getString(
                com.android.settings.R.string.zen_mode_settings_dnd_automatic_rule_app,
                TEST_APP_NAME));
    }

    @Test
    public void testDefaultNotifPolicy_time_manualRule_setFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        String placeholder = "placeholder";
        injectManualRuleWithTimeCountdown(1000, placeholder);
        mController.updateState(mockPref);

        verify(mockPref).setTitle(mContext.getString(
                com.android.settings.R.string.zen_mode_settings_dnd_manual_end_time, placeholder));
    }

    @Test
    public void testDefaultNotifPolicy_forever_manualRule_setFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        injectManualRuleWithIndefiniteEnd();
        mController.updateState(mockPref);

        verify(mockPref).setTitle(mContext.getString(
                com.android.settings.R.string.zen_mode_settings_dnd_manual_indefinite));
    }

    @Test
    public void testDefaultNotifPolicy_automaticRule_noManualRule_setFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        // no manual rule
        ReflectionHelpers.setField(mZenModeConfig, MANUAL_RULE_FIELD, null);

        // adding automatic rule
        injectNewAutomaticRule(TEST_RULE_NAME, true, false);

        mController.updateState(mockPref);

        verify(mockPref).setTitle(mContext.getString(
                com.android.settings.R.string.zen_mode_settings_dnd_automatic_rule,
                TEST_RULE_NAME));
    }

    @Test
    public void testDefaultNotifPolicy_manualRuleEndsLast_hasAutomaticRule_setFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        // manual rule that ends after automatic rule ends
        injectManualRuleWithIndefiniteEnd();

        // automatic rule that ends before manual rule ends
        injectNewAutomaticRule(TEST_RULE_NAME, true, false);

        mController.updateState(mockPref);

        // manual rule end time is after automatic rule end time, so it is displayed
        verify(mockPref).setTitle(mContext.getString(
                com.android.settings.R.string.zen_mode_settings_dnd_manual_indefinite));
    }

    @Test
    public void testDefaultNotifPolicy_automaticRuleEndsLast_hasManualRule_setFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        // manual rule that ends before automatic rule ends
        injectManualRuleWithTimeCountdown(1000, "");

        // automatic rule that ends after manual rule ends
        ZenRule rule = injectNewAutomaticRule(TEST_RULE_NAME, true, false);
        when(mConfigWrapper.parseAutomaticRuleEndTime(rule.conditionId)).thenReturn(2000L);

        mController.updateState(mockPref);

        // automatic rule end time is after manual rule end time, so it is displayed
        verify(mockPref).setTitle(mContext.getString(
                com.android.settings.R.string.zen_mode_settings_dnd_automatic_rule,
                TEST_RULE_NAME));
    }

    @Test
    public void testDefaultNotifPolicy_multipleAutomaticRules_autoRuleApp_setFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);

        // automatic rule that ends after manual rule ends
        ZenRule rule1 = injectNewAutomaticRule(TEST_RULE_NAME + "1", false, false);
        when(mConfigWrapper.parseAutomaticRuleEndTime(rule1.conditionId)).thenReturn(10000L);

        // automatic rule that is an app
        injectNewAutomaticRule(TEST_RULE_NAME + "2", true, true);

        ZenRule rule3 = injectNewAutomaticRule(TEST_RULE_NAME + "3", true, false);
        when(mConfigWrapper.parseAutomaticRuleEndTime(rule3.conditionId)).thenReturn(9000L);

        mController.updateState(mockPref);

        // automatic rule from app is displayed
        verify(mockPref).setTitle(mContext.getString(
                com.android.settings.R.string.zen_mode_settings_dnd_automatic_rule,
                TEST_RULE_NAME + "2"));
    }

    @Test
    public void testDefaultNotifPolicy_multipleAutomaticRules_setFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);

        // automatic rule that ends after manual rule ends
        ZenRule rule1 = injectNewAutomaticRule(TEST_RULE_NAME + "1", true, false);
        when(mConfigWrapper.parseAutomaticRuleEndTime(rule1.conditionId)).thenReturn(2000L);

        ZenRule rule2 = injectNewAutomaticRule(TEST_RULE_NAME + "2", true, false);
        when(mConfigWrapper.parseAutomaticRuleEndTime(rule2.conditionId)).thenReturn(8000L);

        ZenRule rule3 = injectNewAutomaticRule(TEST_RULE_NAME + "3", false, false);
        when(mConfigWrapper.parseAutomaticRuleEndTime(rule3.conditionId)).thenReturn(12000L);

        mController.updateState(mockPref);

        // active automatic rule with the latest end time will display
        verify(mockPref).setTitle(mContext.getString(
                com.android.settings.R.string.zen_mode_settings_dnd_automatic_rule,
                TEST_RULE_NAME + "2"));
    }

    // manual rule that has no end condition (forever)
    private void injectManualRuleWithIndefiniteEnd() {
        ZenRule injectedManualRule = new ZenRule();
        injectedManualRule.zenMode = ZEN_MODE_IMPORTANT_INTERRUPTIONS;
        injectedManualRule.conditionId = null;
        injectedManualRule.enabler = null;
        ReflectionHelpers.setField(mZenModeConfig, MANUAL_RULE_FIELD, injectedManualRule);
    }

    // manual rule triggered by an app
    private void injectManualRuleFromApp() {
        ZenRule injectedManualRule = new ZenRule();
        injectedManualRule.zenMode = ZEN_MODE_IMPORTANT_INTERRUPTIONS;
        injectedManualRule.enabler = TEST_APP_NAME;
        when(mConfigWrapper.getOwnerCaption(injectedManualRule.enabler)).thenReturn(TEST_APP_NAME);
        ReflectionHelpers.setField(mZenModeConfig, MANUAL_RULE_FIELD, injectedManualRule);
    }

    // manual rule that ends in specified time
    private void injectManualRuleWithTimeCountdown(long time, String timePlaceholder) {
        ZenRule injectedManualRule = new ZenRule();
        injectedManualRule.zenMode = ZEN_MODE_IMPORTANT_INTERRUPTIONS;
        injectedManualRule.enabler = null;
        injectedManualRule.conditionId = mock(Uri.class);
        when(mConfigWrapper.parseManualRuleTime(injectedManualRule.conditionId)).thenReturn(time);
        when(mConfigWrapper.getFormattedTime(time, mContext.getUserId()))
                .thenReturn(timePlaceholder);
        ReflectionHelpers.setField(mZenModeConfig, MANUAL_RULE_FIELD, injectedManualRule);
    }

    // manual rule that ends in time
    private ZenRule injectNewAutomaticRule(String nameAndId, boolean isActive, boolean isApp) {
        ZenRule injectedRule = spy(new ZenRule());
        injectedRule.zenMode = ZEN_MODE_NO_INTERRUPTIONS;
        injectedRule.component = mock(ComponentName.class);
        injectedRule.name = nameAndId;
        injectedRule.conditionId = new Uri.Builder().authority(nameAndId).build(); // unique uri
        when(injectedRule.isAutomaticActive()).thenReturn(isActive);
        when(mConfigWrapper.isTimeRule(injectedRule.conditionId)).thenReturn(!isApp);
        if (isApp) {
            when(injectedRule.component.getPackageName()).thenReturn(TEST_APP_NAME);
        }
        mInjectedAutomaticRules.put(nameAndId, injectedRule);

        return injectedRule;
    }
}
