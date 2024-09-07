/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network.telephony;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class EuiccRacConnectivityDialogActivityTest {
    private static final boolean CONFIRMED = true;

    private FakeFeatureFactory mFeatureFactory;
    private EuiccRacConnectivityDialogActivity mActivity;

    @Before
    public void setUp() {
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mActivity = spy(Robolectric.buildActivity(EuiccRacConnectivityDialogActivity.class).get());
        mActivity.onCreate(null);
    }

    @Test
    public void dialogAction_continue_intentResetMobileNetwork_metricsLogged() {
        mActivity.onConfirm(
                SettingsEnums.ACTION_RESET_MOBILE_NETWORK_RAC_CONNECTIVITY_WARNING, CONFIRMED);

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mActivity,
                        SettingsEnums.ACTION_RESET_MOBILE_NETWORK_RAC_CONNECTIVITY_WARNING,
                        getMetricsValue(CONFIRMED));
    }

    @Test
    public void dialogAction_back_intentResetMobileNetwork_metricsLogged() {
        mActivity.onConfirm(
                SettingsEnums.ACTION_RESET_MOBILE_NETWORK_RAC_CONNECTIVITY_WARNING, !CONFIRMED);

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mActivity,
                        SettingsEnums.ACTION_RESET_MOBILE_NETWORK_RAC_CONNECTIVITY_WARNING,
                        getMetricsValue(!CONFIRMED));
    }

    @Test
    public void dialogAction_continue_intentSettingsEsimDelete_metricsLogged() {
        mActivity.onConfirm(SettingsEnums.ACTION_SETTINGS_ESIM_RAC_CONNECTIVITY_WARNING, CONFIRMED);

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mActivity,
                        SettingsEnums.ACTION_SETTINGS_ESIM_RAC_CONNECTIVITY_WARNING,
                        getMetricsValue(CONFIRMED));
    }

    @Test
    public void dialogAction_back_intentSettingsEsimDelete_metricsLogged() {
        mActivity.onConfirm(
                SettingsEnums.ACTION_SETTINGS_ESIM_RAC_CONNECTIVITY_WARNING, !CONFIRMED);

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mActivity,
                        SettingsEnums.ACTION_SETTINGS_ESIM_RAC_CONNECTIVITY_WARNING,
                        getMetricsValue(!CONFIRMED));
    }

    private int getMetricsValue(boolean confirmed) {
        return confirmed ? 1 : 0;
    }
}
