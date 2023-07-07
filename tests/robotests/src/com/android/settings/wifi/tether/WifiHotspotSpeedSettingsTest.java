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

import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_6GHZ;
import static com.android.settings.wifi.tether.WifiHotspotSpeedSettings.KEY_SPEED_2GHZ;
import static com.android.settings.wifi.tether.WifiHotspotSpeedSettings.KEY_SPEED_2GHZ_5GHZ;
import static com.android.settings.wifi.tether.WifiHotspotSpeedSettings.KEY_SPEED_5GHZ;
import static com.android.settings.wifi.tether.WifiHotspotSpeedSettings.KEY_SPEED_6GHZ;

import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.lifecycle.ViewModelStoreOwner;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.wifi.factory.WifiFeatureProvider;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class WifiHotspotSpeedSettingsTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    ViewModelStoreOwner mViewModelStoreOwner;
    @Mock
    WifiHotspotSpeedViewModel mViewModel;
    @Mock
    SelectorWithWidgetPreference mRadioButton;

    Map<Integer, WifiHotspotSpeedViewModel.SpeedInfo> mSpeedInfoMap = new HashMap<>();
    WifiHotspotSpeedViewModel.SpeedInfo mSpeedInfo2g =
            new WifiHotspotSpeedViewModel.SpeedInfo(false, true, false);
    WifiHotspotSpeedViewModel.SpeedInfo mSpeedInfo5g =
            new WifiHotspotSpeedViewModel.SpeedInfo(false, true, false);
    WifiHotspotSpeedViewModel.SpeedInfo mSpeedInfo2g5g =
            new WifiHotspotSpeedViewModel.SpeedInfo(false, true, true);
    WifiHotspotSpeedViewModel.SpeedInfo mSpeedInfo6g =
            new WifiHotspotSpeedViewModel.SpeedInfo(false, true, true);

    WifiHotspotSpeedSettings mSettings;

    @Before
    public void setUp() {
        WifiFeatureProvider provider = FakeFeatureFactory.setupForTest().getWifiFeatureProvider();
        when(provider.getWifiHotspotSpeedViewModel(mViewModelStoreOwner)).thenReturn(mViewModel);

        mSettings = spy(new WifiHotspotSpeedSettings());
        mSettings.mWifiHotspotSpeedViewModel = mViewModel;
    }

    @Test
    public void onSpeedInfoMapDataChanged_checkedSpeed2g_checkedToRadioButton2g() {
        mSpeedInfo2g = new WifiHotspotSpeedViewModel.SpeedInfo(false, true, false);
        updateSpeedInfoMap();
        mockRadioButton(true, false, true);
        mSettings.mSpeedPreferenceMap.put(SPEED_2GHZ, mRadioButton);

        mSettings.onSpeedInfoMapDataChanged(mSpeedInfoMap);

        verifyRadioButton(false, true, false);
    }

    @Test
    public void onSpeedInfoMapDataChanged_uncheckedSpeed2g_uncheckedToRadioButton2g() {
        mSpeedInfo2g = new WifiHotspotSpeedViewModel.SpeedInfo(true, false, true);
        updateSpeedInfoMap();
        mockRadioButton(false, true, false);
        mSettings.mSpeedPreferenceMap.put(SPEED_2GHZ, mRadioButton);

        mSettings.onSpeedInfoMapDataChanged(mSpeedInfoMap);

        verifyRadioButton(true, false, true);
    }

    @Test
    public void onSpeedInfoMapDataChanged_checkedSpeed5g_checkedToRadioButton5g() {
        mSpeedInfo5g = new WifiHotspotSpeedViewModel.SpeedInfo(false, true, false);
        updateSpeedInfoMap();
        mockRadioButton(true, false, true);
        mSettings.mSpeedPreferenceMap.put(SPEED_5GHZ, mRadioButton);

        mSettings.onSpeedInfoMapDataChanged(mSpeedInfoMap);

        verifyRadioButton(false, true, false);
    }

    @Test
    public void onSpeedInfoMapDataChanged_uncheckedSpeed5g_uncheckedToRadioButton5g() {
        mSpeedInfo5g = new WifiHotspotSpeedViewModel.SpeedInfo(true, false, true);
        updateSpeedInfoMap();
        mockRadioButton(false, true, false);
        mSettings.mSpeedPreferenceMap.put(SPEED_5GHZ, mRadioButton);

        mSettings.onSpeedInfoMapDataChanged(mSpeedInfoMap);

        verifyRadioButton(true, false, true);
    }

    @Test
    public void onSpeedInfoMapDataChanged_checkedSpeed2g5g_checkedToRadioButton2g5g() {
        mSpeedInfo2g5g = new WifiHotspotSpeedViewModel.SpeedInfo(false, true, false);
        updateSpeedInfoMap();
        mockRadioButton(true, false, true);
        mSettings.mSpeedPreferenceMap.put(SPEED_2GHZ_5GHZ, mRadioButton);

        mSettings.onSpeedInfoMapDataChanged(mSpeedInfoMap);

        verifyRadioButton(false, true, false);
    }

    @Test
    public void onSpeedInfoMapDataChanged_uncheckedSpeed25g_uncheckedToRadioButton25g() {
        mSpeedInfo2g5g = new WifiHotspotSpeedViewModel.SpeedInfo(true, false, true);
        updateSpeedInfoMap();
        mockRadioButton(false, true, false);
        mSettings.mSpeedPreferenceMap.put(SPEED_2GHZ_5GHZ, mRadioButton);

        mSettings.onSpeedInfoMapDataChanged(mSpeedInfoMap);

        verifyRadioButton(true, false, true);
    }

    @Test
    public void onSpeedInfoMapDataChanged_checkedSpeed6g_checkedToRadioButton6g() {
        mSpeedInfo6g = new WifiHotspotSpeedViewModel.SpeedInfo(false, true, false);
        updateSpeedInfoMap();
        mockRadioButton(true, false, true);
        mSettings.mSpeedPreferenceMap.put(SPEED_6GHZ, mRadioButton);

        mSettings.onSpeedInfoMapDataChanged(mSpeedInfoMap);

        verifyRadioButton(false, true, false);
    }

    @Test
    public void onSpeedInfoMapDataChanged_uncheckedSpeed6g_uncheckedToRadioButton6g() {
        mSpeedInfo6g = new WifiHotspotSpeedViewModel.SpeedInfo(true, false, true);
        updateSpeedInfoMap();
        mockRadioButton(false, true, false);
        mSettings.mSpeedPreferenceMap.put(SPEED_6GHZ, mRadioButton);

        mSettings.onSpeedInfoMapDataChanged(mSpeedInfoMap);

        verifyRadioButton(true, false, true);
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
    public void onRadioButtonClicked_toSpeed2g_setSpeedType2g() {
        when(mRadioButton.getKey()).thenReturn(KEY_SPEED_2GHZ);

        mSettings.onRadioButtonClicked(mRadioButton);

        verify(mViewModel).setSpeedType(SPEED_2GHZ);
    }

    @Test
    public void onRadioButtonClicked_toSpeed5g_setSpeedType5g() {
        when(mRadioButton.getKey()).thenReturn(KEY_SPEED_5GHZ);

        mSettings.onRadioButtonClicked(mRadioButton);

        verify(mViewModel).setSpeedType(SPEED_5GHZ);
    }

    @Test
    public void onRadioButtonClicked_toSpeed2g5g_setSpeedType2g5g() {
        when(mRadioButton.getKey()).thenReturn(KEY_SPEED_2GHZ_5GHZ);

        mSettings.onRadioButtonClicked(mRadioButton);

        verify(mViewModel).setSpeedType(SPEED_2GHZ_5GHZ);
    }

    @Test
    public void onRadioButtonClicked_toSpeed6g_setSpeedType6g() {
        when(mRadioButton.getKey()).thenReturn(KEY_SPEED_6GHZ);

        mSettings.onRadioButtonClicked(mRadioButton);

        verify(mViewModel).setSpeedType(SPEED_6GHZ);
    }

    private void updateSpeedInfoMap() {
        mSpeedInfoMap.put(SPEED_2GHZ, mSpeedInfo2g);
        mSpeedInfoMap.put(SPEED_5GHZ, mSpeedInfo5g);
        mSpeedInfoMap.put(SPEED_2GHZ_5GHZ, mSpeedInfo2g5g);
        mSpeedInfoMap.put(SPEED_6GHZ, mSpeedInfo6g);
    }

    private void mockRadioButton(boolean isChecked, boolean isEnabled, boolean isVisible) {
        when(mRadioButton.isChecked()).thenReturn(isChecked);
        when(mRadioButton.isEnabled()).thenReturn(isEnabled);
        when(mRadioButton.isVisible()).thenReturn(isVisible);
    }

    private void verifyRadioButton(boolean isChecked, boolean isEnabled, boolean isVisible) {
        verify(mRadioButton).setChecked(isChecked);
        verify(mRadioButton).setEnabled(isEnabled);
        verify(mRadioButton).setVisible(isVisible);
    }

}
