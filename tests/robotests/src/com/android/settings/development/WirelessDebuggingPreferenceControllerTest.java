/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.debug.IAdbManager;
import android.os.RemoteException;
import android.provider.Settings.Global;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settings.testutils.shadow.ShadowWirelessDebuggingPreferenceController;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUtils.class, ShadowWirelessDebuggingPreferenceController.class})
public class WirelessDebuggingPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private MasterSwitchPreference mPreference;
    @Mock
    private IAdbManager mAdbManager;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;

    private WirelessDebuggingPreferenceController mController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private Context mContext;
    private ContentResolver mContentResolver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mContentResolver = mContext.getContentResolver();
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = spy(new WirelessDebuggingPreferenceController(mContext, mLifecycle));
        ReflectionHelpers.setField(mController, "mAdbManager", mAdbManager);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        Global.putInt(mContentResolver, Global.ADB_WIFI_ENABLED, 0);
    }

    @After
    public void tearDown() {
        ShadowWirelessDebuggingPreferenceController.reset();
    }

    @Test
    public void isAvailable_isAdbWifiSupported_yes_shouldBeTrue() throws RemoteException {
        when(mAdbManager.isAdbWifiSupported()).thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_isAdbWifiSupported_shouldBeFalse() throws RemoteException {
        when(mAdbManager.isAdbWifiSupported()).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_adbWifiEnabled_preferenceShouldBeChecked() {
        Global.putInt(mContentResolver,
                Global.ADB_WIFI_ENABLED, 1 /* setting enabled */);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_adbWifiDisabled_preferenceShouldNotBeChecked() {
        Global.putInt(mContentResolver,
                Global.ADB_WIFI_ENABLED, 0 /* setting disabled */);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onPreferenceChange_turnOn_wifiConnected_adbWifiEnabledTrue() {
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(true);
        mController.onPreferenceChange(null, true);

        assertThat(Global.getInt(mContentResolver, Global.ADB_WIFI_ENABLED, -1)).isEqualTo(1);
    }

    @Test
    public void onPreferenceChange_turnOff_wifiConnected_adbWifiEnabledFalse() {
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(true);
        mController.onPreferenceChange(null, false);

        assertThat(Global.getInt(mContentResolver, Global.ADB_WIFI_ENABLED, -1)).isEqualTo(0);
    }

    @Test
    public void onPreferenceChange_turnOn_wifiNotConnected_adbWifiEnabledFalse() {
        // Should not be able to enable wifi debugging without being connected to a wifi network
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(false);
        mController.onPreferenceChange(null, true);

        assertThat(Global.getInt(mContentResolver, Global.ADB_WIFI_ENABLED, -1)).isEqualTo(0);
    }

    @Test
    public void onPreferenceChange_turnOff_wifiNotConnected_adbWifiEnabledFalse() {
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(false);
        mController.onPreferenceChange(null, false);

        assertThat(Global.getInt(mContentResolver, Global.ADB_WIFI_ENABLED, -1)).isEqualTo(0);
    }
}
