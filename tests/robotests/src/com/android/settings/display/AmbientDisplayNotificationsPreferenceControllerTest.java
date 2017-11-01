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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.search.InlinePayload;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {ShadowSecureSettings.class})
public class AmbientDisplayNotificationsPreferenceControllerTest {

    @Mock Context mContext;
    @Mock AmbientDisplayConfiguration mConfig;
    @Mock SwitchPreference mSwitchPreference;
    @Mock MetricsFeatureProvider mMetricsFeatureProvider;

    AmbientDisplayNotificationsPreferenceController mController;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mController = new AmbientDisplayNotificationsPreferenceController(mContext, mConfig,
                mMetricsFeatureProvider);
    }

    @Test
    public void updateState_enabled() throws Exception {
        when(mConfig.pulseOnNotificationEnabled(anyInt()))
                .thenReturn(true);

        mController.updateState(mSwitchPreference);

        verify(mSwitchPreference).setChecked(true);
    }

    @Test
    public void updateState_disabled() throws Exception {
        when(mConfig.pulseOnNotificationEnabled(anyInt()))
                .thenReturn(false);

        mController.updateState(mSwitchPreference);

        verify(mSwitchPreference).setChecked(false);
    }

    @Test
    public void onPreferenceChange_enable() throws Exception {
        mController.onPreferenceChange(mSwitchPreference, true);

        assertThat(Settings.Secure.getInt(null, Settings.Secure.DOZE_ENABLED, -1))
                .isEqualTo(1);
    }

    @Test
    public void onPreferenceChange_disable() throws Exception {
        mController.onPreferenceChange(mSwitchPreference, false);

        assertThat(Settings.Secure.getInt(null, Settings.Secure.DOZE_ENABLED, -1))
                .isEqualTo(0);
    }

    @Test
    public void isAvailable_available() throws Exception {
        when(mConfig.pulseOnNotificationAvailable())
                .thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_unavailable() throws Exception {
        when(mConfig.pulseOnNotificationAvailable())
                .thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_reportsEventForItsPreference() throws Exception {
        when(mSwitchPreference.getKey()).thenReturn(
                AmbientDisplayNotificationsPreferenceController.KEY_AMBIENT_DISPLAY_NOTIFICATIONS);

        mController.handlePreferenceTreeClick(mSwitchPreference);

        verify(mMetricsFeatureProvider).action(any(), eq(ACTION_AMBIENT_DISPLAY));
    }

    @Test
    public void handlePreferenceTreeClick_doesntReportEventForOtherPreferences() throws Exception {
        when(mSwitchPreference.getKey()).thenReturn("some_other_key");

        mController.handlePreferenceTreeClick(mSwitchPreference);

        verifyNoMoreInteractions(mMetricsFeatureProvider);
    }

    @Test
    public void testPreferenceController_ProperResultPayloadType() {
        assertThat(mController.getResultPayload()).isInstanceOf(InlineSwitchPayload.class);
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void testSetValue_updatesCorrectly() {
        int newValue = 1;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver, Settings.Secure.DOZE_ENABLED, 0);

        ((InlinePayload) mController.getResultPayload()).setValue(mContext, newValue);
        int updatedValue = Settings.Secure.getInt(resolver, Settings.Secure.DOZE_ENABLED, 1);

        assertThat(updatedValue).isEqualTo(newValue);
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void testGetValue_correctValueReturned() {
        int currentValue = 1;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver, Settings.Secure.DOZE_ENABLED, currentValue);

        int newValue = ((InlinePayload) mController.getResultPayload()).getValue(mContext);

        assertThat(newValue).isEqualTo(currentValue);
    }
}
