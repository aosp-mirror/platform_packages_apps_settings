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

import android.content.Context;
import android.database.ContentObserver;
import android.provider.Settings.Global;

import androidx.lifecycle.LifecycleOwner;

import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settings.testutils.shadow.ShadowWirelessDebuggingPreferenceController;
import com.android.settings.widget.SwitchWidgetController;
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
public class WirelessDebuggingEnablerTest {

    @Mock
    private SwitchWidgetController mSwitchWidgetController;
    @Mock
    private WirelessDebuggingEnabler.OnEnabledListener mListener;

    private WirelessDebuggingEnabler mWirelessDebuggingEnabler;
    private Context mContext;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        LifecycleOwner mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mWirelessDebuggingEnabler = spy(new WirelessDebuggingEnabler(
                mContext, mSwitchWidgetController, mListener, mLifecycle));
    }

    @After
    public void tearDown() {
        ShadowWirelessDebuggingPreferenceController.reset();
    }

    @Test
    public void onCreation_shouldShowSwitchBar() {
        verify(mSwitchWidgetController).setupView();
    }

    @Test
    public void teardownSwitchController_shouldHideSwitchBar() {
        mWirelessDebuggingEnabler.teardownSwitchController();

        verify(mSwitchWidgetController).teardownView();
    }

    @Test
    public void adbWifiEnabled_switchBarShouldBeChecked() {
        // Set to disabled first otherwise we won't get the onChange() event
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 0 /* setting disabled */);
        mWirelessDebuggingEnabler.onResume();

        verify(mSwitchWidgetController).setChecked(false);
        verify(mListener).onEnabled(false);

        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 1 /* setting enabled */);
        final ContentObserver observer =
                ReflectionHelpers.getField(mWirelessDebuggingEnabler, "mSettingsObserver");
        observer.onChange(true, Global.getUriFor(Global.ADB_WIFI_ENABLED));

        verify(mSwitchWidgetController).setChecked(true);
        // Should also get a callback
        verify(mListener).onEnabled(true);
    }

    @Test
    public void adbWifiEnabled_switchBarShouldNotBeChecked() {
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 1 /* setting enabled */);
        mWirelessDebuggingEnabler.onResume();

        verify(mSwitchWidgetController).setChecked(true);
        verify(mListener).onEnabled(true);

        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 0 /* setting disabled */);
        final ContentObserver observer =
                ReflectionHelpers.getField(mWirelessDebuggingEnabler, "mSettingsObserver");
        observer.onChange(true, Global.getUriFor(Global.ADB_WIFI_ENABLED));

        verify(mSwitchWidgetController).setChecked(false);
        // Should also get a callback
        verify(mListener).onEnabled(false);
    }

    @Test
    public void onSwitchToggled_true_wifiConnected_shouldSetAdbWifiEnabledTrue() {
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(true);
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 0 /* setting disabled */);
        mWirelessDebuggingEnabler.onResume();

        verify(mSwitchWidgetController).setChecked(false);
        verify(mListener).onEnabled(false);

        mWirelessDebuggingEnabler.onSwitchToggled(true);

        assertThat(Global.getInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, -1)).isEqualTo(1);
    }

    @Test
    public void onSwitchToggled_true_wifiNotConnected_shouldSetAdbWifiEnabledFalse() {
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(false);
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 0 /* setting disabled */);
        mWirelessDebuggingEnabler.onResume();

        verify(mSwitchWidgetController).setChecked(false);
        verify(mListener).onEnabled(false);

        mWirelessDebuggingEnabler.onSwitchToggled(true);

        assertThat(Global.getInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, -1)).isEqualTo(0);
    }

    @Test
    public void onSwitchToggled_false_wifiConnected_shouldSetAdbWifiEnabledFalse() {
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(true);
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 1 /* setting disabled */);
        mWirelessDebuggingEnabler.onResume();

        verify(mSwitchWidgetController).setChecked(true);
        verify(mListener).onEnabled(true);

        mWirelessDebuggingEnabler.onSwitchToggled(false);

        assertThat(Global.getInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, -1)).isEqualTo(0);
    }

    @Test
    public void onSwitchToggled_false_wifiNotConnected_shouldSetAdbWifiEnabledFalse() {
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(false);
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 1 /* setting disabled */);
        mWirelessDebuggingEnabler.onResume();

        verify(mSwitchWidgetController).setChecked(true);
        verify(mListener).onEnabled(true);

        mWirelessDebuggingEnabler.onSwitchToggled(false);

        assertThat(Global.getInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, -1)).isEqualTo(0);
    }
}
