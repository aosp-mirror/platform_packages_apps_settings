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

package com.android.settings.datetime;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AutoTimeZonePreferenceControllerTest {

    @Mock
    private UpdateTimeAndDateCallback mCallback;
    @Mock
    private Context mContext;
    private AutoTimeZonePreferenceController mController;
    private Preference mPreference;
    @Mock
    private TelephonyManager mTelephonyManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mPreference = new Preference(mContext);

        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.isDataCapable()).thenReturn(true);
    }

    @Test
    public void isFromSUW_notAvailable() {
        mController = new AutoTimeZonePreferenceController(
                mContext, null /* callback */, true /* isFromSUW */);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void notFromSUW_isAvailable() {
        mController = new AutoTimeZonePreferenceController(
                mContext, null /* callback */, false /* isFromSUW */);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isWifiOnly_notAvailable() {
        when(mTelephonyManager.isDataCapable()).thenReturn(false);
        mController = new AutoTimeZonePreferenceController(
                mContext, null /* callback */, false /* fromSUW */);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isFromSUW_notEnable() {
        mController =
            new AutoTimeZonePreferenceController(mContext, null /* callback */, true /* fromSUW */);

        assertThat(mController.isEnabled()).isFalse();
    }

    @Test
    public void isWifiOnly_notEnable() {
        when(mTelephonyManager.isDataCapable()).thenReturn(false);
        mController = new AutoTimeZonePreferenceController(
                mContext, null /* callback */, false /* fromSUW */);

        assertThat(mController.isEnabled()).isFalse();
    }

    @Test
    public void testIsEnabled_shouldReadFromSettingsProvider() {
        mController = new AutoTimeZonePreferenceController(
                mContext, null /* callback */, false /* fromSUW */);

        // Disabled
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 0);
        assertThat(mController.isEnabled()).isFalse();

        // Enabled
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 1);
        assertThat(mController.isEnabled()).isTrue();
    }

    @Test
    public void updatePreferenceChange_prefIsChecked_shouldUpdatePreferenceAndNotifyCallback() {
        mController =
            new AutoTimeZonePreferenceController(mContext, mCallback, false /* fromSUW */);

        mController.onPreferenceChange(mPreference, true);

        assertThat(mController.isEnabled()).isTrue();
        verify(mCallback).updateTimeAndDateDisplay(mContext);
    }

    @Test
    public void updatePreferenceChange_prefIsUnchecked_shouldUpdatePreferenceAndNotifyCallback() {
        mController =
            new AutoTimeZonePreferenceController(mContext, mCallback, false /* fromSUW */);

        mController.onPreferenceChange(mPreference, false);

        assertThat(mController.isEnabled()).isFalse();
        verify(mCallback).updateTimeAndDateDisplay(mContext);
    }
}
