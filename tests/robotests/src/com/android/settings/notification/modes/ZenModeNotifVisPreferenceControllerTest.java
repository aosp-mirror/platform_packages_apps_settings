/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.service.notification.ZenPolicy.STATE_ALLOW;
import static android.service.notification.ZenPolicy.STATE_DISALLOW;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_LIGHTS;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_NOTIFICATION_LIST;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_PEEK;
import static android.service.notification.ZenPolicy.VISUAL_EFFECT_STATUS_BAR;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.content.Context;
import android.content.res.Resources;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenPolicy;

import androidx.preference.TwoStatePreference;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public final class ZenModeNotifVisPreferenceControllerTest {

    private Context mContext;
    @Mock
    private ZenModesBackend mBackend;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private ZenModeNotifVisPreferenceController mController;
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new ZenModeNotifVisPreferenceController(mContext,
                "zen_effect_peek", VISUAL_EFFECT_PEEK, null, mBackend);
    }
    @Test
    public void isAvailable() {
        // SUPPRESSED_EFFECT_PEEK is always available:
        assertThat(mController.isAvailable()).isTrue();

        // SUPPRESSED_EFFECT_LIGHTS is only available if the device has an LED:
        Context mockContext = mock(Context.class);
        mController = new ZenModeNotifVisPreferenceController(mockContext,
                "zen_effect_light", VISUAL_EFFECT_LIGHTS, null, mBackend);
        Resources mockResources = mock(Resources.class);
        when(mockContext.getResources()).thenReturn(mockResources);

        when(mockResources.getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed))
                .thenReturn(false); // no light
        assertThat(mController.isAvailable()).isFalse();

        when(mockResources.getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed))
                .thenReturn(true); // has light
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_notChecked() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .showAllVisualEffects()
                        .build())
                .build();

        mController.updateZenMode(preference, zenMode);

        verify(preference).setChecked(false);
        verify(preference).setEnabled(true);
    }

    @Test
    public void updateState_checked() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .showVisualEffect(VISUAL_EFFECT_PEEK, false)
                        .build())
                .build();

        mController.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
        verify(preference).setEnabled(true);
    }

    @Test
    public void updateState_checkedFalse_parentChecked() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        mController = new ZenModeNotifVisPreferenceController(mContext,
                "zen_effect_status", VISUAL_EFFECT_STATUS_BAR,
                new int[]{VISUAL_EFFECT_NOTIFICATION_LIST}, mBackend);

        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .showVisualEffect(VISUAL_EFFECT_NOTIFICATION_LIST, false)
                        .showVisualEffect(VISUAL_EFFECT_STATUS_BAR, true)
                        .build())
                .build();

        mController.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
        verify(preference).setEnabled(false);
        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getVisualEffectStatusBar())
                .isEqualTo(STATE_DISALLOW);
        assertThat(captor.getValue().getPolicy().getVisualEffectNotificationList())
                .isEqualTo(STATE_DISALLOW); // Untouched
    }

    @Test
    public void updateState_checkedFalse_parentNotChecked() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        mController = new ZenModeNotifVisPreferenceController(mContext,
                "zen_effect_status", VISUAL_EFFECT_STATUS_BAR,
                new int[]{VISUAL_EFFECT_NOTIFICATION_LIST}, mBackend);

        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .showAllVisualEffects()
                        .build())
                .build();

        mController.updateZenMode(preference, zenMode);

        verify(preference).setChecked(false);
        verify(preference).setEnabled(true);
        verify(mBackend, never()).updateMode(any());
    }

    @Test
    public void onPreferenceChanged_checkedFalse() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .hideAllVisualEffects()
                        .build())
                .build();

        mController.updateZenMode(preference, zenMode);

        mController.onPreferenceChange(preference, false);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getVisualEffectPeek())
                .isEqualTo(STATE_ALLOW);
        assertThat(captor.getValue().getPolicy().getVisualEffectNotificationList())
                .isEqualTo(STATE_DISALLOW); // Untouched
    }

    @Test
    public void onPreferenceChanged_checkedTrue() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowAlarms(true)
                        .showAllVisualEffects()
                        .build())
                .build();

        mController.updateZenMode(preference, zenMode);

        mController.onPreferenceChange(preference, true);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getPolicy().getVisualEffectPeek())
                .isEqualTo(STATE_DISALLOW);
        assertThat(captor.getValue().getPolicy().getVisualEffectNotificationList())
                .isEqualTo(STATE_ALLOW); // Untouched
    }
}