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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.job.JobScheduler;
import android.os.Binder;
import android.provider.Settings;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowBinder;

@RunWith(SettingsRobolectricTestRunner.class)
public class DeviceIndexFeatureProviderTest {

    @Mock
    private JobScheduler mJobScheduler;
    private DeviceIndexFeatureProvider mProvider;
    private Activity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowBinder.reset();
        FakeFeatureFactory.setupForTest();
        mActivity = spy(Robolectric.buildActivity(Activity.class).create().visible().get());
        mProvider = spy(new DeviceIndexFeatureProviderImpl());
        when(mActivity.getSystemService(JobScheduler.class)).thenReturn(mJobScheduler);
    }

    @After
    public void tearDown() {
        ShadowBinder.reset();
    }

    @Test
    public void updateIndex_disabled_shouldDoNothing() {
        when(mProvider.isIndexingEnabled()).thenReturn(false);

        mProvider.updateIndex(mActivity, false);
        verify(mJobScheduler, never()).schedule(any());
    }

    @Test
    public void updateIndex_enabled_unprovisioned_shouldDoNothing() {
        when(mProvider.isIndexingEnabled()).thenReturn(true);
        Settings.Global.putInt(mActivity.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);

        mProvider.updateIndex(mActivity, false);

        verify(mJobScheduler, never()).schedule(any());
    }

    @Test
    public void updateIndex_enabled_provisioned_shouldIndex() {
        Settings.Global.putInt(mActivity.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);
        when(mProvider.isIndexingEnabled()).thenReturn(true);

        mProvider.updateIndex(mActivity, false);
        verify(mJobScheduler).schedule(any());
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
        when(mProvider.isIndexingEnabled()).thenReturn(true);

        mProvider.updateIndex(mActivity, false);
        verify(mJobScheduler).schedule(any());
    }

    @Test
    public void updateIndex_enabled_provisioned_differentUid_shouldNotIndex() {
        Settings.Global.putInt(mActivity.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);
        when(mProvider.isIndexingEnabled()).thenReturn(true);

        ShadowBinder.setCallingUid(Binder.getCallingUid() + 2000);

        mProvider.updateIndex(mActivity, false);
        verify(mJobScheduler, never()).schedule(any());
    }

    @Test
    public void updateIndex_enabled_provisioned_newIndex_shouldIndex() {
        Settings.Global.putInt(mActivity.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);
        DeviceIndexFeatureProvider.setIndexState(mActivity);
        Settings.Global.putString(mActivity.getContentResolver(),
                DeviceIndexFeatureProvider.INDEX_LANGUAGE, "new language");

        when(mProvider.isIndexingEnabled()).thenReturn(true);

        mProvider.updateIndex(mActivity, false);
        verify(mJobScheduler).schedule(any());
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

        mProvider.updateIndex(mActivity, false);

        verify(mJobScheduler, never()).schedule(any());
    }
}
