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
package com.android.settings.fuelgauge.batterytip;

import static com.android.settings.fuelgauge.batterytip.tips.BatteryTip.TipType.SMART_BATTERY_MANAGER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.SettingsActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.widget.CardPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BatteryTipPreferenceControllerTest {

    private static final String KEY_PREF = "battery_tip";
    private static final String KEY_TIP = "key_battery_tip";

    @Rule public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private BatteryTipPreferenceController.BatteryTipListener mBatteryTipListener;
    @Mock private PreferenceScreen mPreferenceScreen;
    @Mock private BatteryTip mBatteryTip;
    @Mock private SettingsActivity mSettingsActivity;
    @Mock private InstrumentedPreferenceFragment mFragment;

    private Context mContext;
    private CardPreference mCardPreference;
    private BatteryTipPreferenceController mBatteryTipPreferenceController;
    private List<BatteryTip> mNewBatteryTips;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();

        mCardPreference = new CardPreference(mContext);
        when(mPreferenceScreen.getContext()).thenReturn(mContext);
        doReturn(mCardPreference).when(mPreferenceScreen).findPreference(KEY_PREF);

        mNewBatteryTips = new ArrayList<>();

        mBatteryTipPreferenceController = buildBatteryTipPreferenceController();
        mBatteryTipPreferenceController.mCardPreference = mCardPreference;
        mBatteryTipPreferenceController.mPrefContext = mContext;
    }

    @Test
    public void displayPreference_isInvisible() {
        mBatteryTipPreferenceController.displayPreference(mPreferenceScreen);

        assertThat(mCardPreference.isVisible()).isFalse();
    }

    @Test
    public void updateBatteryTips_tipsStateInvisible_isInvisible() {
        mBatteryTipPreferenceController.updateBatteryTips(mNewBatteryTips);

        assertThat(mCardPreference.isVisible()).isFalse();
    }

    @Test
    public void getCurrentBatteryTip_noTips_isNull() {
        assertThat(mBatteryTipPreferenceController.getCurrentBatteryTip()).isNull();
    }

    @Test
    public void getCurrentBatteryTip_tipsInvisible_isNull() {
        mBatteryTipPreferenceController.updateBatteryTips(mNewBatteryTips);
        assertThat(mBatteryTipPreferenceController.getCurrentBatteryTip()).isNull();
    }

    @Test
    public void restoreFromNull_shouldNotCrash() {
        final Bundle bundle = new Bundle();
        // Battery tip list is null at this time
        mBatteryTipPreferenceController.saveInstanceState(bundle);

        final BatteryTipPreferenceController controller = buildBatteryTipPreferenceController();

        // Should not crash
        controller.restoreInstanceState(bundle);
    }

    @Test
    public void handlePreferenceTreeClick_noDialog_invokeCallback() {
        when(mBatteryTip.getType()).thenReturn(SMART_BATTERY_MANAGER);
        List<BatteryTip> batteryTips = new ArrayList<>();
        batteryTips.add(mBatteryTip);
        doReturn(false).when(mBatteryTip).shouldShowDialog();
        doReturn(KEY_TIP).when(mBatteryTip).getKey();
        mBatteryTipPreferenceController.updateBatteryTips(batteryTips);

        mBatteryTipPreferenceController.handlePreferenceTreeClick(mCardPreference);

        verify(mBatteryTipListener).onBatteryTipHandled(mBatteryTip);
    }

    @Test
    public void getAvailabilityStatus_returnAvailableUnsearchable() {
        assertThat(mBatteryTipPreferenceController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE_UNSEARCHABLE);
    }

    private BatteryTipPreferenceController buildBatteryTipPreferenceController() {
        final BatteryTipPreferenceController controller =
                new BatteryTipPreferenceController(mContext, KEY_PREF);
        controller.setActivity(mSettingsActivity);
        controller.setFragment(mFragment);
        controller.setBatteryTipListener(mBatteryTipListener);

        return controller;
    }
}
