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
 * limitations under the License
 */

package com.android.settings.notification;

import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_ANY;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR;

import static com.android.settings.notification.ZenOnboardingActivity.ALWAYS_SHOW_THRESHOLD;
import static com.android.settings.notification.ZenOnboardingActivity.PREF_KEY_SUGGESTION_VIEWED;
import static com.android.settings.notification.ZenOnboardingActivity
        .PREF_KEY_SUGGGESTION_FIRST_DISPLAY_TIME;
import static com.android.settings.notification.ZenOnboardingActivity.isSuggestionComplete;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class ZenOnboardingActivityTest {

    @Mock
    MetricsLogger mMetricsLogger;
    @Mock
    NotificationManager mNm;

    ZenOnboardingActivity mActivity;

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mActivity = Robolectric.buildActivity(ZenOnboardingActivity.class)
                .create()
                .get();
        mActivity.setNotificationManager(mNm);
        mActivity.setMetricsLogger(mMetricsLogger);

        mActivity.setupUI();

        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFeatureFactory.suggestionsFeatureProvider.getSharedPrefs(any(Context.class)))
                .thenReturn(getSharedPreferences());
    }

    @Test
    public void loadUiRecordsEvent() {
        verify(mMetricsLogger).visible(MetricsEvent.SETTINGS_ZEN_ONBOARDING);
    }

    @Test
    public void save() {
        Policy policy = new Policy(PRIORITY_CATEGORY_ALARMS, 0, 0, SUPPRESSED_EFFECT_SCREEN_ON);
        when(mNm.getNotificationPolicy()).thenReturn(policy);

        mActivity.save(null);

        verify(mMetricsLogger).action(MetricsEvent.ACTION_ZEN_ONBOARDING_OK);

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        verify(mNm).setNotificationPolicy(captor.capture());

        Policy actual = captor.getValue();
        assertThat(actual.priorityCategories).isEqualTo(PRIORITY_CATEGORY_ALARMS
                | PRIORITY_CATEGORY_REPEAT_CALLERS);
        assertThat(actual.priorityCallSenders).isEqualTo(Policy.PRIORITY_SENDERS_STARRED);
        assertThat(actual.priorityMessageSenders).isEqualTo(Policy.PRIORITY_SENDERS_ANY);
        assertThat(actual.suppressedVisualEffects).isEqualTo(
                Policy.getAllSuppressedVisualEffects());
    }

    @Test
    public void close() {
        Policy policy = new Policy(
                PRIORITY_CATEGORY_ALARMS, PRIORITY_SENDERS_ANY, 0,
                SUPPRESSED_EFFECT_SCREEN_ON
                        | SUPPRESSED_EFFECT_SCREEN_OFF
                        | SUPPRESSED_EFFECT_FULL_SCREEN_INTENT
                        | SUPPRESSED_EFFECT_LIGHTS
                        | SUPPRESSED_EFFECT_PEEK
                        | SUPPRESSED_EFFECT_STATUS_BAR
                        | SUPPRESSED_EFFECT_BADGE
                        | SUPPRESSED_EFFECT_AMBIENT
                        | SUPPRESSED_EFFECT_NOTIFICATION_LIST);
        when(mNm.getNotificationPolicy()).thenReturn(policy);

        mActivity.close(null);

        verify(mMetricsLogger).action(MetricsEvent.ACTION_ZEN_ONBOARDING_KEEP_CURRENT_SETTINGS);

        verify(mNm, never()).setNotificationPolicy(any());
    }

    @Test
    public void isSuggestionComplete_zenUpdated() {
        setZenUpdated(true);
        setShowSettingsSuggestion(false);
        setWithinTimeThreshold(true);
        assertThat(isSuggestionComplete(mContext)).isTrue();
    }

    @Test
    public void isSuggestionComplete_withinTimeThreshold() {
        setZenUpdated(false);
        setShowSettingsSuggestion(false);
        setWithinTimeThreshold(true);
        assertThat(isSuggestionComplete(mContext)).isFalse();
    }

    @Test
    public void isSuggestionComplete_showSettingsSuggestionTrue() {
        setZenUpdated(false);
        setShowSettingsSuggestion(true);
        setWithinTimeThreshold(false);
        assertThat(isSuggestionComplete(mContext)).isFalse();
    }

    @Test
    public void isSuggestionComplete_showSettingsSuggestionFalse_notWithinTimeThreshold() {
        setZenUpdated(false);
        setShowSettingsSuggestion(false);
        setWithinTimeThreshold(false);
        assertThat(isSuggestionComplete(mContext)).isTrue();
    }

    private void setZenUpdated(boolean updated) {
        int zenUpdated = 0;
        if (updated) {
            zenUpdated = 1;
        }

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ZEN_SETTINGS_UPDATED, zenUpdated);
    }

    private void setWithinTimeThreshold(boolean withinTime) {
        long firstTime = System.currentTimeMillis();

        if (withinTime) {
            firstTime -= ALWAYS_SHOW_THRESHOLD / 2;
        } else {
            firstTime -= ALWAYS_SHOW_THRESHOLD * 2;
        }

        getSharedPreferences().edit().putLong(PREF_KEY_SUGGGESTION_FIRST_DISPLAY_TIME,
               firstTime).commit();
    }

    private void setShowSettingsSuggestion(boolean show) {
        int showZenSuggestion = 0;
        if (show) {
            showZenSuggestion = 1;
        }

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.SHOW_ZEN_SETTINGS_SUGGESTION, showZenSuggestion);
    }

    private SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences("test_zen_sugg", Context.MODE_PRIVATE);
    }
}
