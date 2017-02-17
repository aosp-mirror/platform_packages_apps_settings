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

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;

import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static android.provider.Settings.System.NOTIFICATION_LIGHT_PULSE;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PulseNotificationPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    private PulseNotificationPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new PulseNotificationPreferenceController(mContext);
    }

    @Test
    public void display_configIsTrue_shouldDisplay() {
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed))
                .thenReturn(true);
        mController.displayPreference(mScreen);

        verify(mScreen, never()).removePreference(any(Preference.class));
    }

    @Test
    public void display_configIsFalse_shouldNotDisplay() {
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed))
                .thenReturn(false);
        final Preference preference = mock(Preference.class);
        when(mScreen.getPreferenceCount()).thenReturn(1);
        when(mScreen.getPreference(0)).thenReturn(preference);
        when(preference.getKey()).thenReturn(mController.getPreferenceKey());

        mController.displayPreference(mScreen);

        verify(mScreen).removePreference(any(Preference.class));
    }

    @Test
    public void updateState_preferenceSetCheckedWhenSettingIsOn() {
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        final Context context = ShadowApplication.getInstance().getApplicationContext();
        Settings.System.putInt(context.getContentResolver(), NOTIFICATION_LIGHT_PULSE, 1);

        mController = new PulseNotificationPreferenceController(context);
        mController.updateState(preference);

        verify(preference).setChecked(true);
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenSettingIsOff() {
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        final Context context = ShadowApplication.getInstance().getApplicationContext();
        Settings.System.putInt(context.getContentResolver(), NOTIFICATION_LIGHT_PULSE, 0);

        mController = new PulseNotificationPreferenceController(context);
        mController.updateState(preference);

        verify(preference).setChecked(false);
    }
}
