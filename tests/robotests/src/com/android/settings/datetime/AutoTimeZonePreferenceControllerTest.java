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

import android.content.Context;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AutoTimeZonePreferenceControllerTest {

    @Mock
    private Context mMockContext;
    @Mock
    private ConnectivityManager mCm;
    @Mock
    private UpdateTimeAndDateCallback mCallback;

    private Context mContext;
    private AutoTimeZonePreferenceController mController;
    private Preference mPreference;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication.getInstance().setSystemService(Context.CONNECTIVITY_SERVICE, mCm);
        mContext = ShadowApplication.getInstance().getApplicationContext();
        mPreference = new Preference(mContext);
        when(mMockContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(mCm);
        when(mCm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)).thenReturn(true);
    }

    @Test
    public void isFromSUW_notAvailable() {
        mController = new AutoTimeZonePreferenceController(
                mMockContext, null /* callback */, true /* isFromSUW */);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void notFromSUW_isAvailable() {
        mController = new AutoTimeZonePreferenceController(
                mMockContext, null /* callback */, false /* isFromSUW */);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isWifiOnly_notAvailable() {
        when(mCm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)).thenReturn(false);

        mController = new AutoTimeZonePreferenceController(
                mMockContext, null /* callback */, false /* isFromSUW */);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isFromSUW_notEnable() {
        mController = new AutoTimeZonePreferenceController(
            mMockContext, null /* callback */, true /* isFromSUW */);

        assertThat(mController.isEnabled()).isFalse();
    }

    @Test
    public void isWifiOnly_notEnable() {
        when(mCm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)).thenReturn(false);

        mController = new AutoTimeZonePreferenceController(
            mMockContext, null /* callback */, false /* isFromSUW */);

        assertThat(mController.isEnabled()).isFalse();
    }

    @Test
    public void testIsEnabled_shouldReadFromSettingsProvider() {
        mController = new AutoTimeZonePreferenceController(
                mContext, null /* callback */, false /* isFromSUW */);

        // Disabled
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AUTO_TIME_ZONE, 0);
        assertThat(mController.isEnabled()).isFalse();

        // Enabled
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AUTO_TIME_ZONE, 1);
        assertThat(mController.isEnabled()).isTrue();
    }

    @Test
    public void updatePreferenceChange_prefIsChecked_shouldUpdatePreferenceAndNotifyCallback() {
        mController = new AutoTimeZonePreferenceController(
                mContext, mCallback, false /* isFromSUW */);

        mController.onPreferenceChange(mPreference, true);

        assertThat(mController.isEnabled()).isTrue();
        verify(mCallback).updateTimeAndDateDisplay(mContext);
    }

    @Test
    public void updatePreferenceChange_prefIsUnchecked_shouldUpdatePreferenceAndNotifyCallback() {
        mController = new AutoTimeZonePreferenceController(
                mContext, mCallback, false /* isFromSUW */);

        mController.onPreferenceChange(mPreference, false);

        assertThat(mController.isEnabled()).isFalse();
        verify(mCallback).updateTimeAndDateDisplay(mContext);
    }
}
