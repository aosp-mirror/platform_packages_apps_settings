/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.notification;

import static android.provider.Settings.System.NOTIFICATION_LIGHT_PULSE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PulseNotificationPreferenceControllerTest {

    private Context mContext;
    @Mock
    private Resources mResources;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    private PulseNotificationPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getResources()).thenReturn(mResources);

        mController = new PulseNotificationPreferenceController(mContext, "testkey");
        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);
    }

    @Test
    public void display_configIsTrue_shouldDisplay() {
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed))
                .thenReturn(true);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void display_configIsFalse_shouldNotDisplay() {
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed))
                .thenReturn(false);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateState_preferenceSetCheckedWhenSettingIsOn() {
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        final Context context = RuntimeEnvironment.application;
        Settings.System.putInt(context.getContentResolver(), NOTIFICATION_LIGHT_PULSE, 1);

        mController = new PulseNotificationPreferenceController(context, "testkey");
        mController.updateState(preference);

        verify(preference).setChecked(true);
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenSettingIsOff() {
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        final Context context = RuntimeEnvironment.application;
        Settings.System.putInt(context.getContentResolver(), NOTIFICATION_LIGHT_PULSE, 0);

        mController = new PulseNotificationPreferenceController(context, "testkey");
        mController.updateState(preference);

        verify(preference).setChecked(false);
    }

    @Test
    public void isAvailable_configTrue_shouldReturnTrue() {
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)).thenReturn(
                true);

        assertThat(mController.isAvailable()).isTrue();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void isAvailable_configFalse_shouldReturnFalse() {
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)).thenReturn(
                false);

        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isChecked_configOn_shouldReturnTrue() {
        Settings.System.putInt(mContext.getContentResolver(), NOTIFICATION_LIGHT_PULSE, 1);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_configOff_shouldReturnFalse() {
        Settings.System.putInt(mContext.getContentResolver(), NOTIFICATION_LIGHT_PULSE, 0);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void testSetChecked_configIsSet_shouldReturnTrue() {
        mController.setChecked(true);

        assertThat(mController.isChecked()).isTrue();
        assertThat(
                Settings.System.getInt(mContext.getContentResolver(), NOTIFICATION_LIGHT_PULSE, 0))
                .isEqualTo(1);
    }

    @Test
    public void testSetChecked_configIsNotSet_shouldReturnFalse() {
        mController.setChecked(false);

        assertThat(mController.isChecked()).isFalse();
        assertThat(
                Settings.System.getInt(mContext.getContentResolver(), NOTIFICATION_LIGHT_PULSE, 1))
                .isEqualTo(0);
    }
}
