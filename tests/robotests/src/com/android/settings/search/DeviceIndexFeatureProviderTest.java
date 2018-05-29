/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.job.JobScheduler;
import android.provider.Settings;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

@RunWith(SettingsRobolectricTestRunner.class)
public class DeviceIndexFeatureProviderTest {

    private DeviceIndexFeatureProvider mProvider;
    private Activity mActivity;

    @Before
    public void setUp() {
        FakeFeatureFactory.setupForTest();
        mActivity = spy(Robolectric.buildActivity(Activity.class).create().visible().get());
        mProvider = spy(new DeviceIndexFeatureProviderImpl());
    }

    @Test
    public void updateIndex_disabled_shouldDoNothing() {
        when(mProvider.isIndexingEnabled()).thenReturn(false);

        mProvider.updateIndex(mActivity, false);
        verify(mProvider, never()).index(any(), any(), any(), any(), any());
    }

    @Test
    public void updateIndex_enabled_unprovisioned_shouldDoNothing() {
        when(mProvider.isIndexingEnabled()).thenReturn(true);
        Settings.Global.putInt(mActivity.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);

        mProvider.updateIndex(mActivity, false);

        verify(mProvider, never()).index(any(), any(), any(), any(), any());
    }

    @Test
    public void updateIndex_enabled_provisioned_shouldIndex() {
        Settings.Global.putInt(mActivity.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);
        JobScheduler jobScheduler = mock(JobScheduler.class);
        when(mProvider.isIndexingEnabled()).thenReturn(true);
        when(mActivity.getSystemService(JobScheduler.class)).thenReturn(jobScheduler);

        mProvider.updateIndex(mActivity, false);
        verify(jobScheduler).schedule(any());
    }

    @Test
    public void updateIndex_enabled_provisioned_newBuild_shouldIndex() {
        Settings.Global.putInt(mActivity.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);
        DeviceIndexFeatureProvider.setIndexState(mActivity);
        Settings.Global.putString(mActivity.getContentResolver(),
                DeviceIndexFeatureProvider.INDEX_VERSION, "new version");
        Settings.Global.putString(mActivity.getContentResolver(),
                DeviceIndexFeatureProvider.LANGUAGE.toString(),
                DeviceIndexFeatureProvider.INDEX_LANGUAGE);
        JobScheduler jobScheduler = mock(JobScheduler.class);
        when(mProvider.isIndexingEnabled()).thenReturn(true);
        when(mActivity.getSystemService(JobScheduler.class)).thenReturn(jobScheduler);

        mProvider.updateIndex(mActivity, false);
        verify(jobScheduler).schedule(any());
    }

    @Test
    public void updateIndex_enabled_provisioned_newIndex_shouldIndex() {
        Settings.Global.putInt(mActivity.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);
        DeviceIndexFeatureProvider.setIndexState(mActivity);
        Settings.Global.putString(mActivity.getContentResolver(),
                DeviceIndexFeatureProvider.INDEX_LANGUAGE, "new language");
        JobScheduler jobScheduler = mock(JobScheduler.class);
        when(mProvider.isIndexingEnabled()).thenReturn(true);
        when(mActivity.getSystemService(JobScheduler.class)).thenReturn(jobScheduler);

        mProvider.updateIndex(mActivity, false);
        verify(jobScheduler).schedule(any());
    }

    @Test
    public void updateIndex_enabled_provisioned_sameBuild_sameLang_shouldNotIndex() {
        // Enabled
        when(mProvider.isIndexingEnabled()).thenReturn(true);
        // Provisioned
        Settings.Global.putInt(mActivity.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);
        // Same build and same language
        DeviceIndexFeatureProvider.setIndexState(mActivity);

        final JobScheduler jobScheduler = mock(JobScheduler.class);
        when(mActivity.getSystemService(JobScheduler.class)).thenReturn(jobScheduler);

        mProvider.updateIndex(mActivity, false);

        verify(jobScheduler, never()).schedule(any());
    }
}
