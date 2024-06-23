/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.app.IActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Ignore
@RunWith(AndroidJUnit4.class)
public class RequestToggleWiFiActivityTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private IActivityManager mIActivityManager;

    private ActivityScenario<RequestToggleWiFiActivity> mActivityScenario;
    private RequestToggleWiFiActivity mActivity;

    @Before
    public void setUp() {
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);
        when(mWifiManager.getWifiState()).thenReturn(WifiManager.WIFI_STATE_DISABLED);

        mActivityScenario = ActivityScenario.launch(new Intent(WifiManager.ACTION_REQUEST_ENABLE));
        mActivityScenario.onActivity(activity -> mActivity = activity);

    }

    @After
    public void cleanUp() {
        mActivity = null;
        if (mActivityScenario != null) {
            mActivityScenario.close();
        }
    }

    @Test
    public void getAppLabel_nullPackageName_returnNull() {
        fakeCallingPackage(null);

        assertThat(mActivity.getAppLabel()).isNull();
    }

    @Test
    public void getAppLabel_settingsPackageName_returnNotNull() {
        fakeCallingPackage("com.android.settings");

        assertThat(mActivity.getAppLabel()).isNotNull();
    }

    private void fakeCallingPackage(@Nullable String packageName) {
        assertThat(mActivity).isNotNull();
        mActivity.mActivityManager = mIActivityManager;
        try {
            when(mIActivityManager.getLaunchedFromPackage(any())).thenReturn(packageName);
        } catch (RemoteException e) {
            // Do nothing.
        }
    }
}
