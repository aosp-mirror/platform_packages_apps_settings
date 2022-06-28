/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.net.NetworkPolicyManager;
import android.telephony.TelephonyManager;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.net.DataUsageController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class WifiDataUsageSummaryPreferenceControllerTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    FragmentActivity mActivity;
    @Mock
    Lifecycle mLifecycle;
    @Mock
    TelephonyManager mTelephonyManager;
    @Mock
    NetworkPolicyManager mNetworkPolicyManager;
    @Mock
    DataUsageSummaryPreference mSummaryPreference;
    @Mock
    DataUsageController mDataUsageController;
    @Mock
    DataUsageController.DataUsageInfo mDataUsageInfo;

    WifiDataUsageSummaryPreferenceController mController;
    Set<String> mAllNetworkKeys = new HashSet<>();

    @Before
    public void setUp() {
        doReturn(mContext.getResources()).when(mActivity).getResources();
        doReturn(mTelephonyManager).when(mActivity).getSystemService(TelephonyManager.class);
        doReturn(mNetworkPolicyManager).when(mActivity)
                .getSystemService(NetworkPolicyManager.class);
        doNothing().when(mSummaryPreference).setWifiMode(anyBoolean(), anyString(), anyBoolean());
        doReturn(mDataUsageInfo).when(mDataUsageController).getDataUsageInfo(any());

        mController = spy(new WifiDataUsageSummaryPreferenceController(mActivity, mLifecycle, null,
                mAllNetworkKeys));
        doReturn(mDataUsageController).when(mController).createDataUsageController(any());
    }

    @Test
    public void updateState_nullOfDataUsageController_shouldNotCrash() {
        mController.mDataUsageController = null;

        mController.updateState(mSummaryPreference);
    }
}
