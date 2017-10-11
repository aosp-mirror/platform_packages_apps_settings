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

package com.android.settings.datausage;

import android.content.Context;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.NetworkPolicyEditor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DataUsageListTest {

    @Mock
    private CellDataPreference.DataStateListener mListener;
    @Mock
    private TemplatePreference.NetworkServices mNetworkServices;
    @Mock
    private Context mContext;
    private DataUsageList mDataUsageList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mNetworkServices.mPolicyEditor = mock(NetworkPolicyEditor.class);
        mDataUsageList = spy(DataUsageList.class);

        doReturn(mContext).when(mDataUsageList).getContext();
        ReflectionHelpers.setField(mDataUsageList, "mDataStateListener", mListener);
        ReflectionHelpers.setField(mDataUsageList, "services", mNetworkServices);
    }

    @Test
    public void resumePause_shouldListenUnlistenDataStateChange() {
        mDataUsageList.onResume();

        verify(mListener).setListener(true, 0, mContext);

        mDataUsageList.onPause();

        verify(mListener).setListener(false, 0, mContext);
    }
}
