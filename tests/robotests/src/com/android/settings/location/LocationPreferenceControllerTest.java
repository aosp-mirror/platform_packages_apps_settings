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
package com.android.settings.location;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.display.AutoBrightnessPreferenceController;
import com.android.settings.search.InlineListPayload;
import com.android.settings.search.InlinePayload;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.ShadowSecureSettings;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class LocationPreferenceControllerTest {
    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mScreen;

    private Lifecycle mLifecycle;
    private LocationPreferenceController mController;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycle = new Lifecycle();
        mController = new LocationPreferenceController(mContext, mLifecycle);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void isAvailable_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_shouldSetSummary() {
        mController.updateState(mPreference);

        verify(mPreference).setSummary(nullable(String.class));
    }

    @Test
    public void updateSummary_shouldSetSummary() {
        mController.displayPreference(mScreen);
        mController.updateSummary();

        verify(mPreference).setSummary(nullable(String.class));
    }

    @Test
    public void getLocationSummary_locationOff_shouldSetSummaryOff() {
        Secure.putInt(mContext.getContentResolver(),
                Secure.LOCATION_MODE, Secure.LOCATION_MODE_OFF);

        assertThat(mController.getLocationSummary(mContext)).isEqualTo(
                mContext.getString(R.string.location_off_summary));
    }

    @Test
    public void getLocationSummary_sensorsOnly_shouldSetSummarySensorsOnly() {
        Secure.putInt(mContext.getContentResolver(),
                Secure.LOCATION_MODE, Secure.LOCATION_MODE_SENSORS_ONLY);

        assertThat(mController.getLocationSummary(mContext)).isEqualTo(
                mContext.getString(R.string.location_on_summary,
                        mContext.getString(R.string.location_mode_sensors_only_title)));
    }

    @Test
    public void getLocationSummary_highAccuracy_shouldSetSummarHighAccuracy() {
        Secure.putInt(mContext.getContentResolver(),
                Secure.LOCATION_MODE, Secure.LOCATION_MODE_HIGH_ACCURACY);

        assertThat(mController.getLocationSummary(mContext)).isEqualTo(
                mContext.getString(R.string.location_on_summary,
                        mContext.getString(R.string.location_mode_high_accuracy_title)));
    }

    @Test
    public void getLocationSummary_batterySaving_shouldSetSummaryBatterySaving() {
        Secure.putInt(mContext.getContentResolver(),
                Secure.LOCATION_MODE, Secure.LOCATION_MODE_BATTERY_SAVING);

        assertThat(mController.getLocationSummary(mContext)).isEqualTo(
                mContext.getString(R.string.location_on_summary,
                        mContext.getString(R.string.location_mode_battery_saving_title)));
    }

    @Test
    public void getLocationString_shouldCorrectString() {
        assertThat(mController.getLocationString(Secure.LOCATION_MODE_OFF)).isEqualTo(
                R.string.location_mode_location_off_title);
        assertThat(mController.getLocationString(Secure.LOCATION_MODE_SENSORS_ONLY)).isEqualTo(
                R.string.location_mode_sensors_only_title);
        assertThat(mController.getLocationString(Secure.LOCATION_MODE_BATTERY_SAVING)).isEqualTo(
                R.string.location_mode_battery_saving_title);
        assertThat(mController.getLocationString(Secure.LOCATION_MODE_HIGH_ACCURACY)).isEqualTo(
                R.string.location_mode_high_accuracy_title);
    }

    @Test
    public void onResume_shouldRegisterObserver() {
        mLifecycle.onResume();
        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
    }

    @Test
    public void onPause_shouldUnregisterObserver() {
        mLifecycle.onPause();
        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
    }

    @Test
    public void locationProvidersChangedReceiver_updatesPreferenceSummary() {
        mController.displayPreference(mScreen);
        mController.onResume();

        mController.mLocationProvidersChangedReceiver.onReceive(
                mContext,
                new Intent().setAction(LocationManager.PROVIDERS_CHANGED_ACTION));

        verify(mPreference).setSummary(any());
    }

    @Test
    public void testPreferenceController_ProperResultPayloadType() {
        final Context context = RuntimeEnvironment.application;
        mController = new LocationPreferenceController(context, null /* lifecycle */);
        ResultPayload payload = mController.getResultPayload();
        assertThat(payload).isInstanceOf(InlineListPayload.class);
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void testSetValue_updatesCorrectly() {
        int newValue = Secure.LOCATION_MODE_BATTERY_SAVING;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver, Secure.LOCATION_MODE, Secure.LOCATION_MODE_OFF);

        ((InlinePayload) mController.getResultPayload()).setValue(mContext, newValue);
        int updatedValue = Settings.Secure.getInt(resolver, Secure.LOCATION_MODE,
                Secure.LOCATION_MODE_OFF);

        assertThat(updatedValue).isEqualTo(newValue);
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void testGetValue_correctValueReturned() {
        int expectedValue = Secure.LOCATION_MODE_BATTERY_SAVING;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver, Secure.LOCATION_MODE, expectedValue);

        int newValue = ((InlinePayload) mController.getResultPayload()).getValue(mContext);

        assertThat(newValue).isEqualTo(expectedValue);
    }
}
