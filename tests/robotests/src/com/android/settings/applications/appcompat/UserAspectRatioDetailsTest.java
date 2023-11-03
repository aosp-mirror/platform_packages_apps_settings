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

package com.android.settings.applications.appcompat;

import static com.android.settings.applications.appcompat.UserAspectRatioDetails.KEY_PREF_3_2;
import static com.android.settings.applications.appcompat.UserAspectRatioDetails.KEY_PREF_DEFAULT;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.IActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.RemoteException;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowActivityManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * To run test: atest SettingsRoboTests:UserAspectRatioDetailsTest
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowActivityManager.class})
public class UserAspectRatioDetailsTest {

    @Mock
    private UserAspectRatioManager mUserAspectRatioManager;
    @Mock
    private IActivityManager mAm;

    private RadioWithImagePreference mRadioButtonPref;
    private Context mContext;
    private UserAspectRatioDetails mFragment;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        mFragment = spy(new UserAspectRatioDetails());
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getAspectRatioManager()).thenReturn(mUserAspectRatioManager);
        ShadowActivityManager.setService(mAm);
        mRadioButtonPref = new RadioWithImagePreference(mContext);
        final FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = featureFactory.metricsFeatureProvider;
    }

    @Test
    public void onRadioButtonClicked_prefChange_shouldStopActivity() throws RemoteException {
        // Default was already selected
        mRadioButtonPref.setKey(KEY_PREF_DEFAULT);
        mFragment.onRadioButtonClicked(mRadioButtonPref);
        // Preference changed
        mRadioButtonPref.setKey(KEY_PREF_3_2);
        mFragment.onRadioButtonClicked(mRadioButtonPref);
        // Only triggered once when preference change
        verify(mAm).stopAppForUser(any(), anyInt());
    }

    @Test
    public void onRadioButtonClicked_prefChange_shouldSetAspectRatio() throws RemoteException {
        // Default was already selected
        mRadioButtonPref.setKey(KEY_PREF_DEFAULT);
        mFragment.onRadioButtonClicked(mRadioButtonPref);
        // Preference changed
        mRadioButtonPref.setKey(KEY_PREF_3_2);
        mFragment.onRadioButtonClicked(mRadioButtonPref);
        // Only triggered once when preference changes
        verify(mUserAspectRatioManager).setUserMinAspectRatio(
                any(), anyInt(), anyInt());
    }

    @Test
    public void onRadioButtonClicked_prefChange_logMetrics() throws NullPointerException {
        // Default was already selected
        mRadioButtonPref.setKey(KEY_PREF_DEFAULT);
        mFragment.onRadioButtonClicked(mRadioButtonPref);
        // Preference changed
        mRadioButtonPref.setKey(KEY_PREF_3_2);
        mFragment.onRadioButtonClicked(mRadioButtonPref);
        InOrder inOrder = inOrder(mMetricsFeatureProvider);
        // Check the old aspect ratio value is logged as having been unselected
        inOrder.verify(mMetricsFeatureProvider)
                .action(
                        eq(SettingsEnums.PAGE_UNKNOWN),
                        eq(SettingsEnums.ACTION_USER_ASPECT_RATIO_APP_DEFAULT_UNSELECTED),
                        eq(SettingsEnums.USER_ASPECT_RATIO_APP_INFO_SETTINGS),
                        any(),
                        anyInt());
        // Check the new aspect ratio value is logged as having been selected
        inOrder.verify(mMetricsFeatureProvider)
                .action(
                        eq(SettingsEnums.PAGE_UNKNOWN),
                        eq(SettingsEnums.ACTION_USER_ASPECT_RATIO_3_2_SELECTED),
                        eq(SettingsEnums.USER_ASPECT_RATIO_APP_INFO_SETTINGS),
                        any(),
                        anyInt());
    }
}
