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

package com.android.settings.network;

import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.DialogInterface;

import com.android.settings.system.ResetDashboardFragment;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
public class EuiccRacConnectivityDialogFragmentTest {
    private static final int CONTINUE_VALUE = 1;
    private static final int CANCEL_VALUE = 0;

    private EuiccRacConnectivityDialogFragment mRacDialogFragment;
    private FakeFeatureFactory mFeatureFactory;
    @Mock private DialogInterface mDialogInterface;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mRacDialogFragment = new EuiccRacConnectivityDialogFragment();

        FragmentController.setupFragment(mRacDialogFragment);
        mRacDialogFragment.setTargetFragment(new ResetDashboardFragment(), /* requestCode= */ 0);
    }

    @Test
    public void dialogAction_continue_intentResetESIMS_metricsLogged() {
        mRacDialogFragment.onClick(mDialogInterface, DialogInterface.BUTTON_NEGATIVE);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mRacDialogFragment.getActivity(),
                        SettingsEnums.ACTION_RESET_ESIMS_RAC_CONNECTIVITY_WARNING,
                        CONTINUE_VALUE);
    }

    @Test
    public void dialogAction_backCancel_intentResetESIMS_metricsLogged() {
        mRacDialogFragment.onCancel(mDialogInterface);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mRacDialogFragment.getActivity(),
                        SettingsEnums.ACTION_RESET_ESIMS_RAC_CONNECTIVITY_WARNING,
                        CANCEL_VALUE);
    }

    @Test
    public void dialogAction_buttonCancel_intentResetESIMS_metricsLogged() {
        mRacDialogFragment.onCancel(mDialogInterface);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mRacDialogFragment.getActivity(),
                        SettingsEnums.ACTION_RESET_ESIMS_RAC_CONNECTIVITY_WARNING,
                        CANCEL_VALUE);
    }
}
