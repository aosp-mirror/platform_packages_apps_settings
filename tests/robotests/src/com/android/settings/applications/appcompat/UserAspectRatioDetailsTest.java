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

import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_3_2;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_APP_DEFAULT;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_UNSET;

import static com.android.settings.applications.AppInfoBase.ARG_PACKAGE_NAME;
import static com.android.settings.applications.appcompat.UserAspectRatioDetails.KEY_HEADER_BUTTONS;
import static com.android.settings.applications.appcompat.UserAspectRatioDetails.KEY_PREF_3_2;
import static com.android.settings.applications.appcompat.UserAspectRatioDetails.KEY_PREF_DEFAULT;
import static com.android.settings.applications.appcompat.UserAspectRatioDetails.KEY_PREF_FULLSCREEN;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.app.Application;
import android.app.IActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.SettingsActivity;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowActivityManager;
import com.android.settings.testutils.shadow.ShadowFragment;
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
@Config(shadows = {ShadowActivityManager.class, ShadowFragment.class})
public class UserAspectRatioDetailsTest {

    @Mock
    private UserAspectRatioManager mUserAspectRatioManager;
    @Mock
    private IActivityManager mAm;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private SettingsActivity mSettingsActivity;

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
        when(mFragment.getActivity()).thenReturn(mSettingsActivity);
        when(mSettingsActivity.getApplication()).thenReturn((Application) mContext);
        when(mSettingsActivity.getInitialCallingPackage()).thenReturn("test.package");
        when(mSettingsActivity.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.checkPermission(eq(Manifest.permission.INTERACT_ACROSS_USERS_FULL),
                any())).thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mUserAspectRatioManager.isOverrideToFullscreenEnabled(anyString(), anyInt()))
                .thenReturn(false);
        ShadowActivityManager.setService(mAm);
        mRadioButtonPref = new RadioWithImagePreference(mContext);
        final FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = featureFactory.metricsFeatureProvider;
    }

    @Test
    public void testOrderOfOptionsFollowsConfig() {
        doReturn(true).when(mUserAspectRatioManager)
                .hasAspectRatioOption(anyInt(), anyString());
        doReturn(0).when(mUserAspectRatioManager)
                .getUserMinAspectRatioOrder(USER_MIN_ASPECT_RATIO_3_2);
        doReturn(1).when(mUserAspectRatioManager)
                .getUserMinAspectRatioOrder(USER_MIN_ASPECT_RATIO_FULLSCREEN);
        doReturn(2).when(mUserAspectRatioManager)
                .getUserMinAspectRatioOrder(USER_MIN_ASPECT_RATIO_UNSET);
        final Bundle args = new Bundle();
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_USER_HANDLE, new UserHandle(0));
        args.putParcelable("intent", intent);
        args.putString(ARG_PACKAGE_NAME, anyString());
        mFragment.setArguments(args);
        mFragment.onCreate(Bundle.EMPTY);

        final int topOfList = mFragment.findPreference(KEY_HEADER_BUTTONS).getOrder();

        assertTrue(topOfList < mFragment.findPreference(KEY_PREF_3_2).getOrder());
        assertTrue(mFragment.findPreference(KEY_PREF_3_2).getOrder()
                < mFragment.findPreference(KEY_PREF_FULLSCREEN).getOrder());
        assertTrue(mFragment.findPreference(KEY_PREF_FULLSCREEN).getOrder()
                < mFragment.findPreference(KEY_PREF_DEFAULT).getOrder());
    }

    @Test
    public void onRadioButtonClicked_prefChange_shouldStopActivity() throws RemoteException {
        doReturn(USER_MIN_ASPECT_RATIO_UNSET).when(mFragment)
                .getSelectedUserMinAspectRatio(anyString());
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
        doReturn(USER_MIN_ASPECT_RATIO_UNSET).when(mFragment)
                .getSelectedUserMinAspectRatio(anyString());
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
        doReturn(USER_MIN_ASPECT_RATIO_UNSET).when(mFragment)
                .getSelectedUserMinAspectRatio(anyString());
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

    @Test
    public void onButtonClicked_overrideEnabled_fullscreenPreselected()
            throws RemoteException {
        doReturn(true).when(mUserAspectRatioManager)
                .isOverrideToFullscreenEnabled(anyString(), anyInt());
        doReturn(USER_MIN_ASPECT_RATIO_UNSET).when(mUserAspectRatioManager)
                .getUserMinAspectRatioValue(anyString(), anyInt());
        doReturn(mRadioButtonPref).when(mFragment).findPreference(KEY_PREF_DEFAULT);
        doReturn(mRadioButtonPref).when(mFragment).findPreference(KEY_PREF_FULLSCREEN);
        doReturn(true).when(mUserAspectRatioManager)
                .hasAspectRatioOption(anyInt(), anyString());

        final Bundle args = new Bundle();
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_USER_HANDLE, new UserHandle(0));
        args.putParcelable("intent", intent);
        args.putString(ARG_PACKAGE_NAME, anyString());
        mFragment.setArguments(args);
        mFragment.onCreate(Bundle.EMPTY);

        // Fullscreen should be pre-selected
        assertEquals(KEY_PREF_FULLSCREEN, mFragment.mSelectedKey);
        assertEquals(USER_MIN_ASPECT_RATIO_FULLSCREEN,
                mFragment.getSelectedUserMinAspectRatio(mFragment.mSelectedKey));

        // Revert to app default, should be set to app default from unset
        mRadioButtonPref.setKey(KEY_PREF_DEFAULT);
        mFragment.onRadioButtonClicked(mRadioButtonPref);
        verify(mUserAspectRatioManager).setUserMinAspectRatio(
                any(), anyInt(), anyInt());
        assertEquals(USER_MIN_ASPECT_RATIO_APP_DEFAULT,
                mFragment.getSelectedUserMinAspectRatio(mFragment.mSelectedKey));
        assertEquals(KEY_PREF_DEFAULT, mFragment.mSelectedKey);

        // Fullscreen override disabled, should be changed to unset from app default
        when(mUserAspectRatioManager.isOverrideToFullscreenEnabled(anyString(), anyInt()))
                .thenReturn(false);
        mFragment.mKeyToAspectRatioMap.clear();
        mFragment.onCreate(Bundle.EMPTY);

        assertEquals(KEY_PREF_DEFAULT, mFragment.mSelectedKey);
        assertEquals(USER_MIN_ASPECT_RATIO_UNSET,
                mFragment.getSelectedUserMinAspectRatio(mFragment.mSelectedKey));
    }
}
