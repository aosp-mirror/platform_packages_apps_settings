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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;

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

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    private PulseNotificationPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new PulseNotificationPreferenceController(mContext);
        mPreference = new Preference(RuntimeEnvironment.application);
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

        mController = new PulseNotificationPreferenceController(context);
        mController.updateState(preference);

        verify(preference).setChecked(true);
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenSettingIsOff() {
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        final Context context = RuntimeEnvironment.application;
        Settings.System.putInt(context.getContentResolver(), NOTIFICATION_LIGHT_PULSE, 0);

        mController = new PulseNotificationPreferenceController(context);
        mController.updateState(preference);

        verify(preference).setChecked(false);
    }
}
