/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.provider.Settings;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

@RunWith(RobolectricTestRunner.class)
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
        FakeFeatureFactory.setupForTest();
        mNetworkServices.mPolicyEditor = mock(NetworkPolicyEditor.class);
        mDataUsageList = spy(DataUsageList.class);

        doReturn(mContext).when(mDataUsageList).getContext();
        ReflectionHelpers.setField(mDataUsageList, "mDataStateListener", mListener);
        ReflectionHelpers.setField(mDataUsageList, "services", mNetworkServices);
    }

    @Test
    public void resumePause_shouldListenUnlistenDataStateChange() {
        ReflectionHelpers.setField(
                mDataUsageList, "mVisibilityLoggerMixin", mock(VisibilityLoggerMixin.class));
        ReflectionHelpers.setField(
                mDataUsageList, "mPreferenceManager", mock(PreferenceManager.class));

        mDataUsageList.onResume();

        verify(mListener).setListener(true, mDataUsageList.mSubId, mContext);

        mDataUsageList.onPause();

        verify(mListener).setListener(false, mDataUsageList.mSubId, mContext);
    }

    @Test
    public void processArgument_shouldGetTemplateFromArgument() {
        final Bundle args = new Bundle();
        args.putParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE, mock(NetworkTemplate.class));
        args.putInt(DataUsageList.EXTRA_SUB_ID, 3);
        mDataUsageList.setArguments(args);

        mDataUsageList.processArgument();

        assertThat(mDataUsageList.mTemplate).isNotNull();
        assertThat(mDataUsageList.mSubId).isEqualTo(3);
    }

    @Test
    public void processArgument_shouldGetNetworkTypeFromArgument() {
        final Bundle args = new Bundle();
        args.putInt(DataUsageList.EXTRA_NETWORK_TYPE, ConnectivityManager.TYPE_WIFI);
        args.putInt(DataUsageList.EXTRA_SUB_ID, 3);
        mDataUsageList.setArguments(args);

        mDataUsageList.processArgument();

        assertThat(mDataUsageList.mNetworkType).isEqualTo(ConnectivityManager.TYPE_WIFI);
    }

    @Test
    public void processArgument_fromIntent_shouldGetTemplateFromIntent() {
        final FragmentActivity activity = mock(FragmentActivity.class);
        final Intent intent = new Intent();
        intent.putExtra(Settings.EXTRA_NETWORK_TEMPLATE, mock(NetworkTemplate.class));
        intent.putExtra(Settings.EXTRA_SUB_ID, 3);
        when(activity.getIntent()).thenReturn(intent);
        doReturn(activity).when(mDataUsageList).getActivity();

        mDataUsageList.processArgument();

        assertThat(mDataUsageList.mTemplate).isNotNull();
        assertThat(mDataUsageList.mSubId).isEqualTo(3);
    }
}
