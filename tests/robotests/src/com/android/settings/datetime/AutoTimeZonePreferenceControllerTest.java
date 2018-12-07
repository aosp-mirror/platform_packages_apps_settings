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

import static org.mockito.Mockito.verify;
import static org.robolectric.shadow.api.Shadow.extract;

import android.content.Context;
import android.net.ConnectivityManager;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.testutils.shadow.ShadowConnectivityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowConnectivityManager.class)
public class AutoTimeZonePreferenceControllerTest {

    @Mock
    private UpdateTimeAndDateCallback mCallback;

    private Context mContext;
    private AutoTimeZonePreferenceController mController;
    private Preference mPreference;
    private ShadowConnectivityManager connectivityManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new Preference(mContext);
        connectivityManager = extract(mContext.getSystemService(ConnectivityManager.class));
        connectivityManager.setNetworkSupported(ConnectivityManager.TYPE_MOBILE, true);
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
        connectivityManager.setNetworkSupported(ConnectivityManager.TYPE_MOBILE, false);

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
        connectivityManager.setNetworkSupported(ConnectivityManager.TYPE_MOBILE, false);

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
