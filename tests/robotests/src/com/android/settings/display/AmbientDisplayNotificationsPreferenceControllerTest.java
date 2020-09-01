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

package com.android.settings.display;

import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_AMBIENT_DISPLAY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import com.android.settings.testutils.shadow.ShadowSecureSettings;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowSecureSettings.class)
public class AmbientDisplayNotificationsPreferenceControllerTest {

    @Mock
    private AmbientDisplayConfiguration mConfig;
    @Mock
    private SwitchPreference mSwitchPreference;
    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;

    private Context mContext;

    private ContentResolver mContentResolver;

    private AmbientDisplayNotificationsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mContentResolver = mContext.getContentResolver();
        mController = new AmbientDisplayNotificationsPreferenceController(mContext,
                AmbientDisplayNotificationsPreferenceController.KEY_AMBIENT_DISPLAY_NOTIFICATIONS);
        mController.setConfig(mConfig);
        ReflectionHelpers.setField(mController, "mMetricsFeatureProvider", mMetricsFeatureProvider);
    }

    @Test
    public void updateState_enabled() {
        when(mConfig.pulseOnNotificationEnabled(anyInt())).thenReturn(true);

        mController.updateState(mSwitchPreference);

        verify(mSwitchPreference).setChecked(true);
    }

    @Test
    public void updateState_disabled() {
        when(mConfig.pulseOnNotificationEnabled(anyInt())).thenReturn(false);

        mController.updateState(mSwitchPreference);

        verify(mSwitchPreference).setChecked(false);
    }

    @Test
    public void onPreferenceChange_enable() {
        mController.onPreferenceChange(mSwitchPreference, true);

        assertThat(Settings.Secure.getInt(mContentResolver, Settings.Secure.DOZE_ENABLED, -1))
                .isEqualTo(1);
    }

    @Test
    public void onPreferenceChange_disable() {
        mController.onPreferenceChange(mSwitchPreference, false);

        assertThat(Settings.Secure.getInt(mContentResolver, Settings.Secure.DOZE_ENABLED, -1))
                .isEqualTo(0);
    }

    @Test
    public void isAvailable_available() {
        when(mConfig.pulseOnNotificationAvailable()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_unavailable() {
        when(mConfig.pulseOnNotificationAvailable()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isChecked_checked_shouldReturnTrue() {
        when(mConfig.pulseOnNotificationEnabled(UserHandle.myUserId())).thenReturn(true);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_checked_shouldReturnFalse() {
        when(mConfig.pulseOnNotificationEnabled(UserHandle.myUserId())).thenReturn(false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_reportsEventForItsPreference() {
        when(mSwitchPreference.getKey()).thenReturn(
                AmbientDisplayNotificationsPreferenceController.KEY_AMBIENT_DISPLAY_NOTIFICATIONS);

        mController.handlePreferenceTreeClick(mSwitchPreference);

        verify(mMetricsFeatureProvider).action(any(), eq(ACTION_AMBIENT_DISPLAY));
    }

    @Test
    public void handlePreferenceTreeClick_doesntReportEventForOtherPreferences() {
        when(mSwitchPreference.getKey()).thenReturn("some_other_key");

        mController.handlePreferenceTreeClick(mSwitchPreference);

        verifyNoMoreInteractions(mMetricsFeatureProvider);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final AmbientDisplayNotificationsPreferenceController controller =
                new AmbientDisplayNotificationsPreferenceController(mContext,
                        "ambient_display_notification");
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final AmbientDisplayNotificationsPreferenceController controller =
                new AmbientDisplayNotificationsPreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }
}
