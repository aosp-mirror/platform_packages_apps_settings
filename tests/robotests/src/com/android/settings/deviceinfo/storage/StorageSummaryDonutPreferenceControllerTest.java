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

package com.android.settings.deviceinfo.storage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class StorageSummaryDonutPreferenceControllerTest {
    private Context mContext;
    private StorageSummaryDonutPreferenceController mController;
    private StorageSummaryDonutPreference mPreference;
    private PreferenceViewHolder mHolder;

    @Before
    public void setUp() throws Exception {
        mContext = RuntimeEnvironment.application;
        mController = new StorageSummaryDonutPreferenceController(mContext);
        mPreference = new StorageSummaryDonutPreference(mContext);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view =
                inflater.inflate(
                        mPreference.getLayoutResource(), new LinearLayout(mContext), false);

        mHolder = new PreferenceViewHolder(view);
    }

    @Test
    public void testEmpty() throws Exception {
        mController.updateBytes(0, 0);
        mController.updateState(mPreference);

        assertThat(mPreference.getTitle().toString()).isEqualTo("0.00B used");
        assertThat(mPreference.getSummary().toString()).isEqualTo("0.00B free");
    }

    @Test
    public void testUsedStorage() throws Exception {
        mController.updateBytes(1024, 1024 * 10);
        mController.updateState(mPreference);

        assertThat(mPreference.getTitle().toString()).isEqualTo("1.00KB used");
        assertThat(mPreference.getSummary().toString()).isEqualTo("9.00KB free");
    }

    @Test
    public void testPopulateWithVolume() throws Exception {
        VolumeInfo volume = Mockito.mock(VolumeInfo.class);
        File file = Mockito.mock(File.class);
        StorageVolumeProvider svp = Mockito.mock(StorageVolumeProvider.class);
        when(volume.getPath()).thenReturn(file);
        when(file.getTotalSpace()).thenReturn(1024L * 10);
        when(file.getFreeSpace()).thenReturn(1024L);
        when(svp.getPrimaryStorageSize()).thenReturn(1024L * 10);

        mController.updateSizes(svp, volume);
        mController.updateState(mPreference);

        assertThat(mPreference.getTitle().toString()).isEqualTo("9.00KB used");
        assertThat(mPreference.getSummary().toString()).isEqualTo("1.00KB free");
    }

    @Test
    public void testAutomaticStorageManagerLabelOff() throws Exception {
        mPreference.onBindViewHolder(mHolder);
        TextView asmTextView = (TextView) mHolder.findViewById(R.id.storage_manager_indicator);
        assertThat(asmTextView.getText().toString()).isEqualTo("Storage Manager: OFF");
    }

    @Test
    public void testAutomaticStorageManagerLabelOn() throws Exception {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED, 1);

        mPreference.onBindViewHolder(mHolder);

        TextView asmTextView = (TextView) mHolder.findViewById(R.id.storage_manager_indicator);
        assertThat(asmTextView.getText().toString()).isEqualTo("Storage Manager: ON");
    }
}
