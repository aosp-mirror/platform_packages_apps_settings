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

package com.android.settings.notification.zen;

import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS;
import static android.app.NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON;

import static com.android.settings.notification.zen.ZenOnboardingActivity.ALWAYS_SHOW_THRESHOLD;
import static com.android.settings.notification.zen.ZenOnboardingActivity.PREF_KEY_SUGGESTION_FIRST_DISPLAY_TIME;
import static com.android.settings.notification.zen.ZenOnboardingActivity.isSuggestionComplete;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class ZenOnboardingActivityTest {

    @Mock
    private MetricsLogger mMetricsLogger;
    @Mock
    private NotificationManager mNm;

    private ZenOnboardingActivity mActivity;

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);

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
    public void saveNewSetting() {
        Policy policy = new Policy(PRIORITY_CATEGORY_ALARMS, 0, 0, SUPPRESSED_EFFECT_SCREEN_ON);
        when(mNm.getNotificationPolicy()).thenReturn(policy);

        mActivity.mNewSetting.performClick();
        mActivity.save(null);

        verify(mMetricsLogger).action(MetricsEvent.ACTION_ZEN_ONBOARDING_OK);

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        if (android.app.Flags.modesApi()) {
            verify(mNm).setNotificationPolicy(captor.capture(), eq(true));
        } else {
            verify(mNm).setNotificationPolicy(captor.capture());
        }

        Policy actual = captor.getValue();
        assertThat(actual.priorityCategories).isEqualTo(PRIORITY_CATEGORY_ALARMS
                | PRIORITY_CATEGORY_REPEAT_CALLERS);
        assertThat(actual.priorityCallSenders).isEqualTo(Policy.PRIORITY_SENDERS_STARRED);
        assertThat(actual.priorityMessageSenders).isEqualTo(Policy.PRIORITY_SENDERS_ANY);
        assertThat(actual.suppressedVisualEffects).isEqualTo(
                Policy.getAllSuppressedVisualEffects());
    }

    @Test
    public void keepCurrentSetting() {
        Policy policy = new Policy(PRIORITY_CATEGORY_ALARMS, 0, 0, SUPPRESSED_EFFECT_SCREEN_ON);
        when(mNm.getNotificationPolicy()).thenReturn(policy);

        mActivity.mKeepCurrentSetting.performClick();
        mActivity.save(null);

        verify(mMetricsLogger).action(MetricsEvent.ACTION_ZEN_ONBOARDING_KEEP_CURRENT_SETTINGS);
        if (Flags.modesApi()) {
            verify(mNm, never()).setNotificationPolicy(any(), anyBoolean());
        } else {
            verify(mNm, never()).setNotificationPolicy(any());
        }
    }

    @Test
    public void isSuggestionComplete_zenUpdated() {
        Policy policy = new Policy(0, 0, 0, 0);
        when(mNm.getNotificationPolicy()).thenReturn(policy);

        setZenUpdated(true);
        setShowSettingsSuggestion(false);
        setWithinTimeThreshold(true);
        assertThat(isSuggestionComplete(mContext)).isTrue();
    }

    @Test
    public void isSuggestionComplete_withinTimeThreshold() {
        Policy policy = new Policy(0, 0, 0, 0);
        when(mNm.getNotificationPolicy()).thenReturn(policy);

        setZenUpdated(false);
        setShowSettingsSuggestion(false);
        setWithinTimeThreshold(true);
        assertThat(isSuggestionComplete(mContext)).isFalse();
    }

    @Test
    public void isSuggestionComplete_showSettingsSuggestionTrue() {
        Policy policy = new Policy(0, 0, 0, 0);
        when(mNm.getNotificationPolicy()).thenReturn(policy);

        setZenUpdated(false);
        setShowSettingsSuggestion(true);
        setWithinTimeThreshold(false);
        assertThat(isSuggestionComplete(mContext)).isFalse();
    }

    @Test
    public void isSuggestionComplete_showSettingsSuggestionFalse_notWithinTimeThreshold() {
        Policy policy = new Policy(0, 0, 0, 0);
        when(mNm.getNotificationPolicy()).thenReturn(policy);

        setZenUpdated(false);
        setShowSettingsSuggestion(false);
        setWithinTimeThreshold(false);
        assertThat(isSuggestionComplete(mContext)).isTrue();
    }


    @Test
    public void isSuggestionComplete_visualEffectsUpdated() {
        // all values suppressed
        Policy policy = new Policy(0, 0, 0, 511);
        when(mNm.getNotificationPolicy()).thenReturn(policy);

        setZenUpdated(false);
        setShowSettingsSuggestion(true);
        setWithinTimeThreshold(true);
        assertThat(isSuggestionComplete(mContext)).isTrue();
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ZEN_SETTINGS_UPDATED, -1)).isEqualTo(1);
    }


    private void setZenUpdated(boolean updated) {
        int zenUpdated = updated ? 1 : 0;

        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ZEN_SETTINGS_UPDATED, zenUpdated);
    }

    private void setWithinTimeThreshold(boolean withinTime) {
        long firstTime = System.currentTimeMillis();

        if (withinTime) {
            firstTime -= ALWAYS_SHOW_THRESHOLD / 2;
        } else {
            firstTime -= ALWAYS_SHOW_THRESHOLD * 2;
        }

        getSharedPreferences().edit().putLong(PREF_KEY_SUGGESTION_FIRST_DISPLAY_TIME,
               firstTime).commit();
    }

    private void setShowSettingsSuggestion(boolean show) {
        int showZenSuggestion = 0;
        if (show) {
            showZenSuggestion = 1;
        }

        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.SHOW_ZEN_SETTINGS_SUGGESTION, showZenSuggestion);
    }

    private SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences("test_zen_sugg", Context.MODE_PRIVATE);
    }
}
