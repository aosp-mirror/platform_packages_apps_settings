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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.SummaryTip;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BatteryTipPreferenceControllerTest {
    private static final String KEY_PREF = "battery_tip";
    private static final String KEY_TIP = "key_battery_tip";
    @Mock
    private BatteryTipPreferenceController.BatteryTipListener mBatteryTipListener;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private BatteryTip mBatteryTip;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;

    private Context mContext;
    private PreferenceGroup mPreferenceGroup;
    private BatteryTipPreferenceController mBatteryTipPreferenceController;
    private List<BatteryTip> mOldBatteryTips;
    private List<BatteryTip> mNewBatteryTips;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mPreferenceGroup = spy(new PreferenceCategory(mContext));
        doReturn(mContext).when(mPreferenceScreen).getContext();
        doReturn(mPreferenceManager).when(mPreferenceGroup).getPreferenceManager();
        doReturn(mPreferenceGroup).when(mPreferenceScreen).findPreference(KEY_PREF);
        mPreference = new Preference(mContext);
        mPreference.setKey(KEY_TIP);

        mOldBatteryTips = new ArrayList<>();
        mOldBatteryTips.add(new SummaryTip(BatteryTip.StateType.NEW));
        mNewBatteryTips = new ArrayList<>();
        mNewBatteryTips.add(new SummaryTip(BatteryTip.StateType.INVISIBLE));

        mBatteryTipPreferenceController = new BatteryTipPreferenceController(mContext, KEY_PREF,
                null, mBatteryTipListener);
        mBatteryTipPreferenceController.mPreferenceGroup = mPreferenceGroup;
        mBatteryTipPreferenceController.mPrefContext = mContext;
    }

    @Test
    public void testDisplayPreference_addSummaryTip() {
        mBatteryTipPreferenceController.displayPreference(mPreferenceScreen);

        assertOnlyContainsSummaryTip(mPreferenceGroup);
    }

    @Test
    public void updateBatteryTips_updateTwice_firstShowSummaryTipThenRemoveIt() {
        // Display summary tip because its state is new
        mBatteryTipPreferenceController.updateBatteryTips(mOldBatteryTips);
        assertOnlyContainsSummaryTip(mPreferenceGroup);

        // Remove summary tip because its new state is invisible
        mBatteryTipPreferenceController.updateBatteryTips(mNewBatteryTips);
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void testHandlePreferenceTreeClick_noDialog_invokeAction() {
        List<BatteryTip> batteryTips = new ArrayList<>();
        batteryTips.add(mBatteryTip);
        doReturn(mPreference).when(mBatteryTip).buildPreference(any());
        doReturn(false).when(mBatteryTip).shouldShowDialog();
        doReturn(KEY_TIP).when(mBatteryTip).getKey();
        mBatteryTipPreferenceController.updateBatteryTips(batteryTips);

        mBatteryTipPreferenceController.handlePreferenceTreeClick(mPreference);

        verify(mBatteryTip).action();
    }

    private void assertOnlyContainsSummaryTip(final PreferenceGroup preferenceGroup) {
        assertThat(preferenceGroup.getPreferenceCount()).isEqualTo(1);

        final Preference preference = preferenceGroup.getPreference(0);
        assertThat(preference.getTitle()).isEqualTo("Battery is in good shape");
        assertThat(preference.getSummary()).isEqualTo("Apps are behaving normally");
    }
}
