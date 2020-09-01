/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.Context;
import android.service.notification.ZenPolicy;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

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
public class ZenRuleCustomSwitchPreferenceControllerTest extends ZenRuleCustomPrefContrTestBase {
    private ZenRuleCustomSwitchPreferenceController mController;

    @Mock
    private ZenModeBackend mBackend;
    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private SwitchPreference mockPref;
    @Mock
    private NotificationManager.Policy mPolicy;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;

    @Override
    AbstractZenCustomRulePreferenceController getController() {
        return mController;
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);

        mContext = RuntimeEnvironment.application;
        when(mNotificationManager.getNotificationPolicy()).thenReturn(mPolicy);
        mController = new ZenRuleCustomSwitchPreferenceController(mContext, mock(Lifecycle.class),
                PREF_KEY, ZenPolicy.PRIORITY_CATEGORY_ALARMS, 0);
        ReflectionHelpers.setField(mController, "mBackend", mBackend);

        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mockPref);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChanged_enable() {
        updateControllerZenPolicy(new ZenPolicy.Builder()
                .allowMedia(false)
                .allowAlarms(false)
                .build());
        mController.onPreferenceChange(mockPref, true);
        mRule.setZenPolicy(new ZenPolicy.Builder()
                .allowMedia(false)
                .allowAlarms(true)
                .build());
        verify(mBackend).updateZenRule(RULE_ID, mRule);
    }

    @Test
    public void onPreferenceChanged_disable() {
        updateControllerZenPolicy(new ZenPolicy.Builder()
                .allowMedia(false)
                .allowAlarms(true)
                .build());
        mController.onPreferenceChange(mockPref, false);
        mRule.setZenPolicy(new ZenPolicy.Builder()
                .allowMedia(false)
                .allowAlarms(false)
                .build());
        verify(mBackend).updateZenRule(RULE_ID, mRule);
    }
}