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

package com.android.settings.wifi;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.ActionListener;

import com.android.settings.TestConfig;
import com.android.settingslib.wifi.AccessPoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SavedAccessPointsWifiSettingsTest {

  @Mock private Activity mActivity;
  @Mock private WifiManager mockWifiManager;
  @Mock private WifiDialog mockWifiDialog;
  @Mock private WifiConfigController mockConfigController;
  @Mock private WifiConfiguration mockWifiConfiguration;
  @Mock private AccessPoint mockAccessPoint;

  private Context mContext;

  private SavedAccessPointsWifiSettings mFragment;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mContext = RuntimeEnvironment.application;
    mFragment = new SavedAccessPointsWifiSettings();
    when(mockWifiDialog.getController()).thenReturn(mockConfigController);
    when(mockConfigController.getConfig()).thenReturn(mockWifiConfiguration);

    ReflectionHelpers.setField(mFragment, "mWifiManager", mockWifiManager);
  }

  @Test
  public void onSubmit_shouldInvokeSaveApi() {
    mFragment.onSubmit(mockWifiDialog);
    verify(mockWifiManager).save(eq(mockWifiConfiguration), any(ActionListener.class));
  }

  @Test
  public void onForget_shouldInvokeForgetApi() {
    ReflectionHelpers.setField(mFragment, "mSelectedAccessPoint", mockAccessPoint);
    when(mockAccessPoint.getConfig()).thenReturn(mockWifiConfiguration);

    mFragment.onForget(mockWifiDialog);

    verify(mockWifiManager).forget(eq(mockWifiConfiguration.networkId), any(ActionListener.class));
  }
}
