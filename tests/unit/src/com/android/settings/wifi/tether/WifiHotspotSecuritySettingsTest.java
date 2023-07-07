/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.wifi.tether;

import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_OPEN;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA3_SAE;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION;

import static com.android.settings.wifi.tether.WifiHotspotSecurityViewModel.KEY_SECURITY_NONE;
import static com.android.settings.wifi.tether.WifiHotspotSecurityViewModel.KEY_SECURITY_WPA2;
import static com.android.settings.wifi.tether.WifiHotspotSecurityViewModel.KEY_SECURITY_WPA2_WPA3;
import static com.android.settings.wifi.tether.WifiHotspotSecurityViewModel.KEY_SECURITY_WPA3;

import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.HashMap;
import java.util.Map;

public class WifiHotspotSecuritySettingsTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    WifiHotspotSecurityViewModel mViewModel;
    @Mock
    SelectorWithWidgetPreference mRadioButtonWpa3;
    @Mock
    SelectorWithWidgetPreference mRadioButtonWpa2Wpa3;
    @Mock
    SelectorWithWidgetPreference mRadioButtonWpa2;
    @Mock
    SelectorWithWidgetPreference mRadioButtonNone;

    Map<Integer, WifiHotspotSecurityViewModel.ViewItem> mViewItemMap = new HashMap<>();
    WifiHotspotSecurityViewModel.ViewItem mViewItemWpa3 =
            new WifiHotspotSecurityViewModel.ViewItem(KEY_SECURITY_WPA3);
    WifiHotspotSecurityViewModel.ViewItem mViewItemWpa2Wpa3 =
            new WifiHotspotSecurityViewModel.ViewItem(KEY_SECURITY_WPA2_WPA3);
    WifiHotspotSecurityViewModel.ViewItem mViewItemWpa2 =
            new WifiHotspotSecurityViewModel.ViewItem(KEY_SECURITY_WPA2);
    WifiHotspotSecurityViewModel.ViewItem mViewItemNone =
            new WifiHotspotSecurityViewModel.ViewItem(KEY_SECURITY_NONE);

    WifiHotspotSecuritySettings mSettings;

    @Before
    public void setUp() {
        mViewItemMap.put(SECURITY_TYPE_WPA3_SAE, mViewItemWpa3);
        mViewItemMap.put(SECURITY_TYPE_WPA3_SAE_TRANSITION, mViewItemWpa2Wpa3);
        mViewItemMap.put(SECURITY_TYPE_WPA2_PSK, mViewItemWpa2);
        mViewItemMap.put(SECURITY_TYPE_OPEN, mViewItemNone);

        when(mRadioButtonWpa3.getKey()).thenReturn(KEY_SECURITY_WPA3);
        when(mRadioButtonWpa2Wpa3.getKey()).thenReturn(KEY_SECURITY_WPA2_WPA3);
        when(mRadioButtonWpa2.getKey()).thenReturn(KEY_SECURITY_WPA2);
        when(mRadioButtonNone.getKey()).thenReturn(KEY_SECURITY_NONE);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> mSettings = spy(new WifiHotspotSecuritySettings()));
        mSettings.mWifiHotspotSecurityViewModel = mViewModel;
        when(mSettings.findPreference(KEY_SECURITY_WPA3)).thenReturn(mRadioButtonWpa3);
        when(mSettings.findPreference(KEY_SECURITY_WPA2_WPA3)).thenReturn(mRadioButtonWpa2Wpa3);
        when(mSettings.findPreference(KEY_SECURITY_WPA2)).thenReturn(mRadioButtonWpa2);
        when(mSettings.findPreference(KEY_SECURITY_NONE)).thenReturn(mRadioButtonNone);
    }

    @Test
    public void onViewItemListDataChanged_checkedWpa3_setViewItemCorrectly() {
        mViewItemWpa3.mIsChecked = true;

        mSettings.onViewItemListDataChanged(mViewItemMap.values().stream().toList());

        verify(mRadioButtonWpa3).setChecked(true);
    }

    @Test
    public void onViewItemListDataChanged_checkedWpa2Wpa3_setViewItemCorrectly() {
        mViewItemWpa2Wpa3.mIsChecked = true;

        mSettings.onViewItemListDataChanged(mViewItemMap.values().stream().toList());

        verify(mRadioButtonWpa2Wpa3).setChecked(true);
    }

    @Test
    public void onViewItemListDataChanged_checkedWpa2_setViewItemCorrectly() {
        mViewItemWpa2.mIsChecked = true;

        mSettings.onViewItemListDataChanged(mViewItemMap.values().stream().toList());

        verify(mRadioButtonWpa2).setChecked(true);
    }

    @Test
    public void onViewItemListDataChanged_checkedNone_setViewItemCorrectly() {
        mViewItemNone.mIsChecked = true;

        mSettings.onViewItemListDataChanged(mViewItemMap.values().stream().toList());

        verify(mRadioButtonNone).setChecked(true);
    }

    @Test
    public void onViewItemListDataChanged_enabledWpa3Only_setViewItemCorrectly() {
        when(mRadioButtonWpa2Wpa3.isEnabled()).thenReturn(true);
        when(mRadioButtonWpa2.isEnabled()).thenReturn(true);
        when(mRadioButtonNone.isEnabled()).thenReturn(true);
        mViewItemWpa2Wpa3.mIsEnabled = false;
        mViewItemWpa2.mIsEnabled = false;
        mViewItemNone.mIsEnabled = false;

        mSettings.onViewItemListDataChanged(mViewItemMap.values().stream().toList());

        verify(mRadioButtonWpa2Wpa3).setEnabled(false);
        verify(mRadioButtonWpa2).setEnabled(false);
        verify(mRadioButtonNone).setEnabled(false);
    }

    @Test
    public void onRestartingChanged_restartingTrue_setLoadingTrue() {
        doNothing().when(mSettings).setLoading(anyBoolean(), anyBoolean());

        mSettings.onRestartingChanged(true);

        verify(mSettings).setLoading(true, false);
    }

    @Test
    public void onRestartingChanged_restartingFalse_setLoadingFalse() {
        doNothing().when(mSettings).setLoading(anyBoolean(), anyBoolean());

        mSettings.onRestartingChanged(false);

        verify(mSettings).setLoading(false, false);
    }

    @Test
    public void onRadioButtonClicked_clickedWpa3_setSecurityTypeCorrectly() {
        mSettings.onRadioButtonClicked(mRadioButtonWpa3);

        verify(mViewModel).handleRadioButtonClicked(KEY_SECURITY_WPA3);
    }

    @Test
    public void onRadioButtonClicked_clickedWpa2Wpa3_setSecurityTypeCorrectly() {
        mSettings.onRadioButtonClicked(mRadioButtonWpa2Wpa3);

        verify(mViewModel).handleRadioButtonClicked(KEY_SECURITY_WPA2_WPA3);
    }

    @Test
    public void onRadioButtonClicked_clickedWpa2_setSecurityTypeCorrectly() {
        mSettings.onRadioButtonClicked(mRadioButtonWpa2);

        verify(mViewModel).handleRadioButtonClicked(KEY_SECURITY_WPA2);
    }

    @Test
    public void onRadioButtonClicked_clickedNone_setSecurityTypeCorrectly() {
        mSettings.onRadioButtonClicked(mRadioButtonNone);

        verify(mViewModel).handleRadioButtonClicked(KEY_SECURITY_NONE);
    }
}
