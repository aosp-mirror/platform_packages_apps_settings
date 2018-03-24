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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;

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
        mActivity = Robolectric.buildActivity(Activity.class).create().visible().get();
        mProvider = spy(new DeviceIndexFeatureProviderImpl());
    }

    @Test
    public void verifyDisabled() {
        when(mProvider.isIndexingEnabled()).thenReturn(false);

        mProvider.updateIndex(mActivity, false);
        verify(mProvider, never()).index(any(), any(), any(), any());
    }

    @Test
    public void verifyIndexing() {
        when(mProvider.isIndexingEnabled()).thenReturn(true);

        mProvider.updateIndex(mActivity, false);
        verify(mProvider, atLeastOnce()).index(any(), any(), any(), any());
    }
}
