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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.Context;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;

@RunWith(SettingsRobolectricTestRunner.class)
public class ZenOnboardingActivityTest {

    @Mock
    MetricsLogger mMetricsLogger;
    @Mock
    NotificationManager mNm;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    ZenOnboardingActivity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mActivity = Robolectric.buildActivity(ZenOnboardingActivity.class)
                .create()
                .get();
        mActivity.setNotificationManager(mNm);
        mActivity.setMetricsLogger(mMetricsLogger);

        mActivity.setupUI();
    }

    @Test
    public void loadUiRecordsEvent() {
        verify(mMetricsLogger).visible(MetricsEvent.SETTINGS_ZEN_ONBOARDING);
    }

    @Test
    public void toggleCheckBoxRecordsEvents_screenOn() {
        mActivity.findViewById(R.id.screen_on_option).performClick();

        verify(mMetricsLogger).action(MetricsEvent.ACTION_ZEN_ONBOARDING_SCREEN_ON, false);

        mActivity.findViewById(R.id.screen_on_option).performClick();

        verify(mMetricsLogger).action(MetricsEvent.ACTION_ZEN_ONBOARDING_SCREEN_ON, true);
    }

    @Test
    public void toggleCheckBoxRecordsEvents_screenOff() {
        mActivity.findViewById(R.id.screen_off_option).performClick();

        verify(mMetricsLogger).action(MetricsEvent.ACTION_ZEN_ONBOARDING_SCREEN_OFF, false);

        mActivity.findViewById(R.id.screen_off_option).performClick();

        verify(mMetricsLogger).action(MetricsEvent.ACTION_ZEN_ONBOARDING_SCREEN_OFF, true);
    }

    @Test
    public void save_screenOn() {
        Policy policy = new Policy(
                PRIORITY_CATEGORY_ALARMS, 0, 0,
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

        mActivity.findViewById(R.id.screen_off_option).performClick();

        mActivity.save(null);

        verify(mMetricsLogger).action(MetricsEvent.ACTION_ZEN_ONBOARDING_OK);

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        verify(mNm).setNotificationPolicy(captor.capture());

        Policy actual = captor.getValue();
        assertThat(actual.priorityCategories).isEqualTo(PRIORITY_CATEGORY_ALARMS);
        assertThat(actual.suppressedVisualEffects).isEqualTo(
                SUPPRESSED_EFFECT_SCREEN_ON | SUPPRESSED_EFFECT_NOTIFICATION_LIST
                        | SUPPRESSED_EFFECT_BADGE | SUPPRESSED_EFFECT_STATUS_BAR
                        | SUPPRESSED_EFFECT_PEEK);
    }

    @Test
    public void save_screenOff() {
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

        mActivity.findViewById(R.id.screen_on_option).performClick();

        mActivity.save(null);

        verify(mMetricsLogger).action(MetricsEvent.ACTION_ZEN_ONBOARDING_OK);

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        verify(mNm).setNotificationPolicy(captor.capture());

        Policy actual = captor.getValue();
        assertThat(actual.priorityCallSenders).isEqualTo(PRIORITY_SENDERS_ANY);
        assertThat(actual.suppressedVisualEffects).isEqualTo(
                SUPPRESSED_EFFECT_SCREEN_OFF | SUPPRESSED_EFFECT_FULL_SCREEN_INTENT
                        | SUPPRESSED_EFFECT_LIGHTS | SUPPRESSED_EFFECT_AMBIENT);
    }

    @Test
    public void save_none() {
        Policy policy = new Policy(0, 0, 0,
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

        mActivity.findViewById(R.id.screen_on_option).performClick();
        mActivity.findViewById(R.id.screen_off_option).performClick();

        mActivity.save(null);

        verify(mMetricsLogger).action(MetricsEvent.ACTION_ZEN_ONBOARDING_OK);

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        verify(mNm).setNotificationPolicy(captor.capture());

        Policy actual = captor.getValue();
        assertThat(actual.suppressedVisualEffects).isEqualTo(0);
    }

    @Test
    public void save_all() {
        Policy policy = new Policy(0, 0, 0, 0);
        when(mNm.getNotificationPolicy()).thenReturn(policy);

        mActivity.save(null);

        verify(mMetricsLogger).action(MetricsEvent.ACTION_ZEN_ONBOARDING_OK);

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        verify(mNm).setNotificationPolicy(captor.capture());

        Policy actual = captor.getValue();
        assertThat(actual.suppressedVisualEffects).isEqualTo(
                SUPPRESSED_EFFECT_SCREEN_ON
                        | SUPPRESSED_EFFECT_SCREEN_OFF
                        | SUPPRESSED_EFFECT_FULL_SCREEN_INTENT
                        | SUPPRESSED_EFFECT_LIGHTS
                        | SUPPRESSED_EFFECT_PEEK
                        | SUPPRESSED_EFFECT_STATUS_BAR
                        | SUPPRESSED_EFFECT_BADGE
                        | SUPPRESSED_EFFECT_AMBIENT
                        | SUPPRESSED_EFFECT_NOTIFICATION_LIST);
    }
}
