/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.provider.Settings;
import android.provider.SettingsSlicesContract;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.AirplaneModeEnabler;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class AirplaneModePreferenceControllerTest {

    private static final int ON = 1;
    private static final int OFF = 0;

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private AirplaneModeEnabler mAirplaneModeEnabler;
    private Context mContext;
    private ContentResolver mResolver;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mScreen;
    private RestrictedSwitchPreference mPreference;
    private AirplaneModePreferenceController mController;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        mResolver = mContext.getContentResolver();
        doReturn(mPackageManager).when(mContext).getPackageManager();
        mController = new AirplaneModePreferenceController(mContext,
                SettingsSlicesContract.KEY_AIRPLANE_MODE);

        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreference = new RestrictedSwitchPreference(mContext);
        mPreference.setKey(SettingsSlicesContract.KEY_AIRPLANE_MODE);
        mScreen.addPreference(mPreference);
        mController.setFragment(null);
    }

    @Test
    public void getSliceUri_shouldUsePlatformAuthority() {
        assertThat(mController.getSliceUri().getAuthority())
                .isEqualTo(SettingsSlicesContract.AUTHORITY);
    }

    @Test
    public void getAvailabilityStatus_hasLeanbackFeature_shouldNotBeAvailable() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(true);

        assertThat(mController.getAvailabilityStatus())
                .isNotEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noLeanbackFeature_shouldBeAvailable() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(false);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void setChecked_setAirplaneModeEnabler_setCheckedTrue() {
        // Set airplane mode ON by setChecked
        mController.setAirplaneModeEnabler(mAirplaneModeEnabler);

        assertThat(mController.setChecked(true)).isTrue();
    }

    @Test
    public void setChecked_isAirplaneModeOnAndSetCheckedTrue_shouldReturnFalse() {
        // Set airplane mode ON by setChecked
        mController.setAirplaneModeEnabler(mAirplaneModeEnabler);
        // Check return value if set same status.
        when(mAirplaneModeEnabler.isAirplaneModeOn()).thenReturn(true);

        assertThat(mController.setChecked(true)).isFalse();
    }

    @Test
    public void setChecked_setChecked_isAirplaneModeOnAndSetCheckedTrue_shouldReturnTrue() {
        // Set airplane mode ON by setChecked
        mController.setAirplaneModeEnabler(mAirplaneModeEnabler);
        // Check return value if set same status.
        when(mAirplaneModeEnabler.isAirplaneModeOn()).thenReturn(true);

        // Set to OFF
        assertThat(mController.setChecked(false)).isTrue();
    }

    @Test
    public void isChecked_airplaneModeTurnOff_returnFalse() {
        // Set airplane mode ON
        Settings.Global.putInt(mResolver, Settings.Global.AIRPLANE_MODE_ON, ON);

        mController.displayPreference(mScreen);
        mController.onStart();

        Settings.Global.putInt(mResolver, Settings.Global.AIRPLANE_MODE_ON, OFF);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void testPreferenceUI_airplaneModeTurnOn_updatesCorrectly() {
        // Airplane mode default off
        Settings.Global.putInt(mResolver, Settings.Global.AIRPLANE_MODE_ON, OFF);

        mController.displayPreference(mScreen);
        mController.onStop();
        mController.onAirplaneModeChanged(true);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void isSliceable_returnsTrue() {
        assertThat(mController.isSliceable()).isTrue();
    }

    @Test
    public void isPublicSlice_returnsTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }
}
