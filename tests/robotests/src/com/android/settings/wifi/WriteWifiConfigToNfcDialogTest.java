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
 * limitations under the License
 */

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.app.Activity;
import android.net.wifi.WifiManager;

import com.android.settings.testutils.shadow.ShadowNfcAdapter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowNfcAdapter.class)
public class WriteWifiConfigToNfcDialogTest {

    @Mock
    private WifiManager mWifiManager;

    private WriteWifiConfigToNfcDialog mWriteWifiConfigToNfcDialog;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Activity activity = Robolectric.setupActivity(Activity.class);
        mWriteWifiConfigToNfcDialog = new WriteWifiConfigToNfcDialog(activity, 0 /* security */);
        ReflectionHelpers.setField(mWriteWifiConfigToNfcDialog, "mWifiManager", mWifiManager);
        mWriteWifiConfigToNfcDialog.setOwnerActivity(activity);
        mWriteWifiConfigToNfcDialog.onCreate(null /* savedInstanceState */);
    }

    @After
    public void tearDown() {
        ShadowNfcAdapter.reset();
    }

    @Test
    public void testOnClick_nfcConfigurationTokenDoesNotContainPasswordHex() {
        when(mWifiManager.getCurrentNetworkWpsNfcConfigurationToken()).thenReturn("blah");

        mWriteWifiConfigToNfcDialog.onClick(null);

        assertThat(ShadowNfcAdapter.isReaderModeEnabled()).isFalse();
    }

    @Test
    public void testOnClick_nfcConfigurationTokenIsNull() {
        when(mWifiManager.getCurrentNetworkWpsNfcConfigurationToken()).thenReturn(null);

        mWriteWifiConfigToNfcDialog.onClick(null);

        assertThat(ShadowNfcAdapter.isReaderModeEnabled()).isFalse();
    }

    @Test
    public void testOnClick_nfcConfigurationTokenContainsPasswordHex() {
        // This is the corresponding passwordHex for an empty string password.
        when(mWifiManager.getCurrentNetworkWpsNfcConfigurationToken()).thenReturn("10270000");

        mWriteWifiConfigToNfcDialog.onClick(null);

        assertThat(ShadowNfcAdapter.isReaderModeEnabled()).isTrue();
    }
}
