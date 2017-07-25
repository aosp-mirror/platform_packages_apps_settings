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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.storage.VolumeInfo;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;

import static com.android.settings.TestUtils.KILOBYTE;
import static com.android.settings.TestUtils.GIGABYTE;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {
        SettingsShadowResources.class,
        SettingsShadowResources.SettingsShadowTheme.class
})
public class StorageSummaryDonutPreferenceControllerTest {
    private Context mContext;
    private StorageSummaryDonutPreferenceController mController;
    private StorageSummaryDonutPreference mPreference;
    private PreferenceViewHolder mHolder;
    private FakeFeatureFactory mFakeFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Before
    public void setUp() throws Exception {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.string.config_headlineFontFamily, "");
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        FakeFeatureFactory.setupForTest(mContext);
        mFakeFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mMetricsFeatureProvider = mFakeFeatureFactory.getMetricsFeatureProvider();
        mController = new StorageSummaryDonutPreferenceController(mContext);
        mPreference = new StorageSummaryDonutPreference(mContext);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view =
                inflater.inflate(
                        mPreference.getLayoutResource(), new LinearLayout(mContext), false);
        mHolder = PreferenceViewHolder.createInstanceForTests(view);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void testEmpty() throws Exception {
        mController.updateBytes(0, 32 * GIGABYTE);
        mController.updateState(mPreference);

        assertThat(mPreference.getTitle().toString()).isEqualTo("0.00B");
        assertThat(mPreference.getSummary().toString()).isEqualTo("Used of 32GB");
    }

    @Test
    public void testTotalStorage() throws Exception {
        mController.updateBytes(KILOBYTE, KILOBYTE * 10);
        mController.updateState(mPreference);

        assertThat(mPreference.getTitle().toString()).isEqualTo("1.00KB");
        assertThat(mPreference.getSummary().toString()).isEqualTo("Used of 10KB");
    }

    @Test
    public void testPopulateWithVolume() throws Exception {
        VolumeInfo volume = Mockito.mock(VolumeInfo.class);
        File file = Mockito.mock(File.class);
        StorageVolumeProvider svp = Mockito.mock(StorageVolumeProvider.class);
        when(volume.getPath()).thenReturn(file);
        when(file.getTotalSpace()).thenReturn(KILOBYTE * 10);
        when(file.getFreeSpace()).thenReturn(KILOBYTE);
        when(svp.getPrimaryStorageSize()).thenReturn(KILOBYTE * 10);

        mController.updateSizes(svp, volume);
        mController.updateState(mPreference);

        assertThat(mPreference.getTitle().toString()).isEqualTo("9.00KB");
        assertThat(mPreference.getSummary().toString()).isEqualTo("Used of 10KB");
    }

    @Test
    public void testFreeUpSpaceMetricIsTriggered() throws Exception {
        mPreference.onBindViewHolder(mHolder);
        Button button = (Button) mHolder.findViewById(R.id.deletion_helper_button);

        mPreference.onClick(button);

        verify(mMetricsFeatureProvider, times(1)).action(
                any(Context.class), eq(MetricsEvent.STORAGE_FREE_UP_SPACE_NOW));
    }
}
