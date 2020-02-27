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

package com.android.settings.notification;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class})
public class RemoteVolumeGroupControllerTest {

    private static final String KEY_REMOTE_VOLUME_GROUP = "remote_media_group";
    private static final String TEST_PACKAGE_LABEL = "music";
    private static final String TEST_DEVICE_1_ID = "test_device_1_id";
    private static final String TEST_DEVICE_1_NAME = "test_device_1_name";
    private static final int CURRENT_VOLUME = 30;
    private static final int MAX_VOLUME = 100;

    @Mock
    private LocalMediaManager mLocalMediaManager;
    @Mock
    private MediaDevice mDevice;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private SharedPreferences mSharedPreferences;

    private final List<MediaDevice> mDevices = new ArrayList<>();

    private Context mContext;
    private RemoteVolumeGroupController mController;
    private PreferenceCategory mPreferenceCategory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new RemoteVolumeGroupController(mContext, KEY_REMOTE_VOLUME_GROUP);
        mController.mLocalMediaManager = mLocalMediaManager;
        mPreferenceCategory = spy(new PreferenceCategory(mContext));
        mPreferenceCategory.setKey(mController.getPreferenceKey());

        when(mPreferenceCategory.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mPreferenceManager.getSharedPreferences()).thenReturn(mSharedPreferences);
        when(mLocalMediaManager.getActiveMediaDevice(
                MediaDevice.MediaDeviceType.TYPE_CAST_DEVICE)).thenReturn(mDevices);
        when(mDevice.getId()).thenReturn(TEST_DEVICE_1_ID);
        when(mDevice.getName()).thenReturn(TEST_DEVICE_1_NAME);
        when(mDevice.getMaxVolume()).thenReturn(MAX_VOLUME);
        when(mDevice.getCurrentVolume()).thenReturn(CURRENT_VOLUME);
        when(mDevice.getClientAppLabel()).thenReturn(TEST_PACKAGE_LABEL);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mPreferenceCategory);
    }

    @Test
    public void getAvailabilityStatus_withActiveDevice_returnAvailableUnsearchable() {
        mDevices.add(mDevice);
        mController.displayPreference(mScreen);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_noActiveDevice_returnConditionallyUnavailable() {
        mController.displayPreference(mScreen);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void displayPreference_noActiveDevice_checkPreferenceCount() {
        mController.displayPreference(mScreen);

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void displayPreference_withActiveDevice_checkPreferenceCount() {
        mDevices.add(mDevice);
        mController.displayPreference(mScreen);

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void displayPreference_withActiveDevice_checkSeekBarTitle() {
        mDevices.add(mDevice);
        mController.displayPreference(mScreen);
        final Preference preference = mPreferenceCategory.findPreference(TEST_DEVICE_1_ID);

        assertThat(preference.getTitle()).isEqualTo(mContext.getText(
                R.string.remote_media_volume_option_title) + " (" + TEST_PACKAGE_LABEL + ")");
    }

    @Test
    public void displayPreference_withActiveDevice_checkSeekBarMaxVolume() {
        mDevices.add(mDevice);
        mController.displayPreference(mScreen);
        final SeekBarPreference preference = mPreferenceCategory.findPreference(TEST_DEVICE_1_ID);

        assertThat(preference.getMax()).isEqualTo(MAX_VOLUME);
    }

    @Test
    public void displayPreference_withActiveDevice_checkSeekBarCurrentVolume() {
        mDevices.add(mDevice);
        mController.displayPreference(mScreen);
        final SeekBarPreference preference = mPreferenceCategory.findPreference(TEST_DEVICE_1_ID);

        assertThat(preference.getProgress()).isEqualTo(CURRENT_VOLUME);
    }

    @Test
    public void displayPreference_withActiveDevice_checkSwitcherPreferenceTitle() {
        mDevices.add(mDevice);
        mController.displayPreference(mScreen);
        final Preference preference = mPreferenceCategory.findPreference(
                RemoteVolumeGroupController.SWITCHER_PREFIX + TEST_DEVICE_1_ID);

        assertThat(preference.getTitle()).isEqualTo(mContext.getText(R.string.media_output_title));
    }

    @Test
    public void displayPreference_withActiveDevice_checkSwitcherPreferenceSummary() {
        mDevices.add(mDevice);
        mController.displayPreference(mScreen);
        final Preference preference = mPreferenceCategory.findPreference(
                RemoteVolumeGroupController.SWITCHER_PREFIX + TEST_DEVICE_1_ID);

        assertThat(preference.getSummary()).isEqualTo(TEST_DEVICE_1_NAME);
    }
}
