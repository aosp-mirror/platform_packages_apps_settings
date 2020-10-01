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

import static com.android.settings.TestUtils.GIGABYTE;
import static com.android.settings.TestUtils.KILOBYTE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.os.storage.VolumeInfo;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowPrivateStorageInfo;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowPrivateStorageInfo.class)
public class StorageSummaryDonutPreferenceControllerTest {

    private Context mContext;
    private StorageSummaryDonutPreferenceController mController;
    private StorageSummaryDonutPreference mPreference;
    private PreferenceViewHolder mHolder;
    private FakeFeatureFactory mFakeFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private PreferenceScreen mScreen;

    @Before
    public void setUp() throws Exception {
        ShadowPrivateStorageInfo.setPrivateStorageInfo(new PrivateStorageInfo(10L, 100L));
        mContext = spy(Robolectric.setupActivity(Activity.class));
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFakeFeatureFactory.getMetricsFeatureProvider();
        mController = new StorageSummaryDonutPreferenceController(mContext, "key");
        mPreference = new StorageSummaryDonutPreference(mContext);
        mScreen = spy(new PreferenceScreen(mContext, null));
        when(mScreen.findPreference("key")).thenReturn(mPreference);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view =
                inflater.inflate(mPreference.getLayoutResource(), new LinearLayout(mContext),
                        false);
        mHolder = PreferenceViewHolder.createInstanceForTests(view);
    }

    @After
    public void tearDown() {
        ShadowPrivateStorageInfo.reset();
    }

    @Test
    public void testEmpty() {
        final long totalSpace = 32 * GIGABYTE;
        final long usedSpace = 0;
        mController.displayPreference(mScreen);
        mController.updateBytes(0, 32 * GIGABYTE);
        mController.updateState(mPreference);

        final Formatter.BytesResult usedSpaceResults =
                Formatter.formatBytes(mContext.getResources(), usedSpace, 0 /* flags */);
        assertThat(mPreference.getTitle().toString())
                .isEqualTo(usedSpaceResults.value + " " + usedSpaceResults.units);
        assertThat(mPreference.getSummary().toString())
                .isEqualTo("Used of " + Formatter.formatShortFileSize(mContext, totalSpace));
    }

    @Test
    public void testTotalStorage() {
        final long totalSpace = KILOBYTE * 10;
        final long usedSpace = KILOBYTE;
        mController.displayPreference(mScreen);
        mController.updateBytes(KILOBYTE, totalSpace);
        mController.updateState(mPreference);

        final Formatter.BytesResult usedSpaceResults =
                Formatter.formatBytes(mContext.getResources(), usedSpace, 0 /* flags */);
        assertThat(mPreference.getTitle().toString())
                .isEqualTo(usedSpaceResults.value + " " + usedSpaceResults.units);
        assertThat(mPreference.getSummary().toString())
                .isEqualTo("Used of " + Formatter.formatShortFileSize(mContext, totalSpace));
    }

    @Test
    public void testPopulateWithVolume() {
        final long totalSpace = KILOBYTE * 10;
        final long freeSpace = KILOBYTE;
        final long usedSpace = totalSpace - freeSpace;
        final VolumeInfo volume = Mockito.mock(VolumeInfo.class);
        final File file = Mockito.mock(File.class);
        final StorageVolumeProvider svp = Mockito.mock(StorageVolumeProvider.class);
        when(volume.getPath()).thenReturn(file);
        when(file.getTotalSpace()).thenReturn(totalSpace);
        when(file.getFreeSpace()).thenReturn(freeSpace);
        when(svp.getPrimaryStorageSize()).thenReturn(totalSpace);
        mController.displayPreference(mScreen);

        mController.updateSizes(svp, volume);
        mController.updateState(mPreference);

        final Formatter.BytesResult usedSpaceResults =
                Formatter.formatBytes(mContext.getResources(), usedSpace, 0 /* flags */);
        assertThat(mPreference.getTitle().toString())
                .isEqualTo(usedSpaceResults.value + " " + usedSpaceResults.units);
        assertThat(mPreference.getSummary().toString())
                .isEqualTo("Used of " + Formatter.formatShortFileSize(mContext, totalSpace));
    }

    @Test
    public void testFreeUpSpaceMetricIsTriggered() {
        mPreference.onBindViewHolder(mHolder);
        final Button button = (Button) mHolder.findViewById(R.id.deletion_helper_button);

        mPreference.onClick(button);

        verify(mMetricsFeatureProvider, times(1))
                .action(any(Context.class), eq(MetricsEvent.STORAGE_FREE_UP_SPACE_NOW));
    }
}
