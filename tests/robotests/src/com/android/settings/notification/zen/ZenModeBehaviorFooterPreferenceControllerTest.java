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

import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ZenRule;
import android.util.ArrayMap;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.notification.zen.AbstractZenModePreferenceController.ZenModeConfigWrapper;
import com.android.settings.notification.zen.ZenModeBehaviorFooterPreferenceController;
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
public class ZenModeBehaviorFooterPreferenceControllerTest {

    private static final String TEST_APP_NAME = "test_app";
    private static final String MANUAL_RULE_FIELD = "manualRule";
    private static final String AUTOMATIC_RULES_FIELD = "automaticRules";

    private ZenModeBehaviorFooterPreferenceController mController;
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
    private int mTitleResId = R.string.zen_mode_settings_title;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);

        mContext = RuntimeEnvironment.application;
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        when(mNotificationManager.getZenModeConfig()).thenReturn(mZenModeConfig);

        mController = new ZenModeBehaviorFooterPreferenceController(
                mContext, mock(Lifecycle.class), mTitleResId);
        ReflectionHelpers.setField(mController, "mZenModeConfigWrapper", mConfigWrapper);

        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mockPref);
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
    public void zenModeOff_footerIsAvailable() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_OFF);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void zenModeOff_updateState_noFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_OFF);
        mController.updateState(mockPref);

        verify(mockPref).setTitle(mContext.getString(mTitleResId));
    }

    @Test
    public void zenModeImportantInterruptions_updateState_noFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        mController.updateState(mockPref);

        verify(mockPref).setTitle(mContext.getString(mTitleResId));
    }

    @Test
    public void deprecatedZenModeAlarms_qsManualRule_setFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_ALARMS);

        ZenRule injectedManualRule = new ZenRule();
        injectedManualRule.zenMode = ZEN_MODE_ALARMS;
        ReflectionHelpers.setField(mZenModeConfig, MANUAL_RULE_FIELD, injectedManualRule);

        mController.updateState(mockPref);

        verify(mockPref).setTitle(mContext.getString(
                com.android.settings.R.string.zen_mode_qs_set_behavior));
    }

    @Test
    public void deprecatedZenModeAlarms_appManualRule_setFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_ALARMS);

        ZenRule injectedManualRule = new ZenRule();
        injectedManualRule.zenMode = ZEN_MODE_ALARMS;
        injectedManualRule.enabler = TEST_APP_NAME;
        when(mConfigWrapper.getOwnerCaption(injectedManualRule.enabler)).thenReturn(TEST_APP_NAME);
        ReflectionHelpers.setField(mZenModeConfig, MANUAL_RULE_FIELD, injectedManualRule);

        mController.updateState(mockPref);

        verify(mockPref).setTitle(mContext.getString(
                com.android.settings.R.string.zen_mode_app_set_behavior, TEST_APP_NAME));
    }

    @Test
    public void deprecatedZenModeNoInterruptions_qsManualRule_setFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_NO_INTERRUPTIONS);

        ZenRule injectedManualRule = new ZenRule();
        injectedManualRule.zenMode = ZEN_MODE_NO_INTERRUPTIONS;
        ReflectionHelpers.setField(mZenModeConfig, MANUAL_RULE_FIELD, injectedManualRule);

        mController.updateState(mockPref);

        verify(mockPref).setTitle(mContext.getString(
                com.android.settings.R.string.zen_mode_qs_set_behavior));
    }

    @Test
    public void deprecatedZenModeNoInterruptions_appManualRule_setFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_NO_INTERRUPTIONS);

        ZenRule injectedManualRule = new ZenRule();
        injectedManualRule.zenMode = ZEN_MODE_NO_INTERRUPTIONS;
        injectedManualRule.enabler = TEST_APP_NAME;
        when(mConfigWrapper.getOwnerCaption(injectedManualRule.enabler)).thenReturn(TEST_APP_NAME);
        ReflectionHelpers.setField(mZenModeConfig, MANUAL_RULE_FIELD, injectedManualRule);

        mController.updateState(mockPref);

        verify(mockPref).setTitle(mContext.getString(
                com.android.settings.R.string.zen_mode_app_set_behavior, TEST_APP_NAME));
    }

    @Test
    public void deprecatedZenModeAlarms_automaticRule_setFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_ALARMS);

        ArrayMap<String, ZenRule> injectedAutomaticRules = new ArrayMap<>();
        ZenRule injectedRule = spy(new ZenRule());
        injectedRule.zenMode = ZEN_MODE_ALARMS;
        injectedRule.component = mock(ComponentName.class);
        when(injectedRule.isAutomaticActive()).thenReturn(true);
        when(injectedRule.component.getPackageName()).thenReturn(TEST_APP_NAME);
        injectedAutomaticRules.put("testid", injectedRule);

        ReflectionHelpers.setField(mZenModeConfig, AUTOMATIC_RULES_FIELD, injectedAutomaticRules);

        mController.updateState(mockPref);

        verify(mockPref).setTitle(mContext.getString(
                com.android.settings.R.string.zen_mode_app_set_behavior, TEST_APP_NAME));
    }

    @Test
    public void deprecatedZenModeNoInterruptions_automaticRule_setFooterTitle() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_NO_INTERRUPTIONS);

        ArrayMap<String, ZenRule> injectedAutomaticRules = new ArrayMap<>();
        ZenRule injectedRule = spy(new ZenRule());
        injectedRule.zenMode = ZEN_MODE_NO_INTERRUPTIONS;
        injectedRule.component = mock(ComponentName.class);
        when(injectedRule.isAutomaticActive()).thenReturn(true);
        when(injectedRule.component.getPackageName()).thenReturn(TEST_APP_NAME);
        injectedAutomaticRules.put("testid", injectedRule);

        ReflectionHelpers.setField(mZenModeConfig, AUTOMATIC_RULES_FIELD, injectedAutomaticRules);

        mController.updateState(mockPref);

        verify(mockPref).setTitle(mContext.getString(
                com.android.settings.R.string.zen_mode_app_set_behavior, TEST_APP_NAME));
    }
}