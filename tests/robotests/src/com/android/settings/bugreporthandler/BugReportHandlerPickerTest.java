/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.bugreporthandler;

import static android.provider.Settings.ACTION_BUGREPORT_HANDLER_SETTINGS;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.drawable.ColorDrawable;
import android.util.Pair;

import androidx.fragment.app.FragmentActivity;

import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.RadioButtonPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.util.ReflectionHelpers;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class BugReportHandlerPickerTest {
    private static final String PACKAGE_NAME = "com.example.test";
    private static final int USER_ID = 0;

    @Mock
    private FragmentActivity mActivity;

    private Context mContext;
    private ShadowPackageManager mPackageManager;
    private BugReportHandlerPicker mPicker;
    private BugReportHandlerUtil mBugReportHandlerUtil;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mPackageManager = Shadows.shadowOf(mContext.getPackageManager());

        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.name = PACKAGE_NAME;
        applicationInfo.uid = 0;
        applicationInfo.flags = 0;
        applicationInfo.packageName = PACKAGE_NAME;

        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = PACKAGE_NAME;
        packageInfo.applicationInfo = applicationInfo;
        mPackageManager.addPackage(packageInfo);
        mPackageManager.setUnbadgedApplicationIcon(PACKAGE_NAME, new ColorDrawable());

        mPicker = spy(new BugReportHandlerPicker());
        doNothing().when(mPicker).updateCandidates();
        doNothing().when(mPicker).updateCheckedState(any());
        doReturn(mActivity).when(mPicker).getActivity();

        ReflectionHelpers.setField(mPicker, "mMetricsFeatureProvider",
                mock(MetricsFeatureProvider.class));
        mBugReportHandlerUtil = mock(BugReportHandlerUtil.class);
        mPicker.setBugReportHandlerUtil(mBugReportHandlerUtil);
    }

    @After
    public void tearDown() {
        mPackageManager.removePackage(PACKAGE_NAME);
    }

    @Test
    public void clickItem_success() {
        testClickingItemSuccess();
    }

    @Test
    public void clickItem_fail() {
        testClickingItemFail();
    }

    @Test
    public void clickItem_usingBugReportHandlerSettingIntent_success() {
        useBugReportHandlerSettingIntent();
        testClickingItemSuccess();
        verify(mActivity, times(1)).finish();
    }

    @Test
    public void clickItem_fromBugReportHandlerSettingIntent_fail() {
        useBugReportHandlerSettingIntent();
        testClickingItemFail();
    }

    private static ApplicationInfo createApplicationInfo(String packageName) {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = packageName;
        return applicationInfo;
    }

    private void testClickingItemSuccess() {
        when(mBugReportHandlerUtil.getValidBugReportHandlerInfos(any()))
                .thenReturn(Collections.singletonList(Pair.create(
                        createApplicationInfo(PACKAGE_NAME), USER_ID)));
        when(mBugReportHandlerUtil.setCurrentBugReportHandlerAppAndUser(any(), eq(PACKAGE_NAME),
                eq(USER_ID))).thenReturn(true);

        RadioButtonPreference defaultPackagePref = mock(RadioButtonPreference.class);
        when(defaultPackagePref.getKey()).thenReturn(
                BugReportHandlerPicker.getKey(PACKAGE_NAME, USER_ID));
        mPicker.onRadioButtonClicked(defaultPackagePref);

        verify(mBugReportHandlerUtil, times(1)).setCurrentBugReportHandlerAppAndUser(any(),
                eq(PACKAGE_NAME), eq(USER_ID));
        verify(mPicker, times(1)).updateCheckedState(
                BugReportHandlerPicker.getKey(PACKAGE_NAME, USER_ID));
        verify(mBugReportHandlerUtil, never()).showInvalidChoiceToast(any());
    }

    private void testClickingItemFail() {
        when(mBugReportHandlerUtil.getValidBugReportHandlerInfos(any()))
                .thenReturn(Collections.singletonList(Pair.create(
                        createApplicationInfo(PACKAGE_NAME), USER_ID)));
        when(mBugReportHandlerUtil.setCurrentBugReportHandlerAppAndUser(any(), eq(PACKAGE_NAME),
                eq(USER_ID))).thenReturn(false);

        RadioButtonPreference defaultPackagePref = mock(RadioButtonPreference.class);
        when(defaultPackagePref.getKey()).thenReturn(
                BugReportHandlerPicker.getKey(PACKAGE_NAME, USER_ID));
        mPicker.onRadioButtonClicked(defaultPackagePref);

        verify(mBugReportHandlerUtil, times(1)).setCurrentBugReportHandlerAppAndUser(any(),
                eq(PACKAGE_NAME), eq(USER_ID));
        // Ensure we update the list of packages when we click a non-valid package - the list must
        // have changed, otherwise this click wouldn't fail.
        verify(mPicker, times(1)).updateCandidates();
        verify(mBugReportHandlerUtil, times(1)).showInvalidChoiceToast(any());
    }

    private void useBugReportHandlerSettingIntent() {
        Intent intent = new Intent(ACTION_BUGREPORT_HANDLER_SETTINGS);
        when(mActivity.getIntent()).thenReturn(intent);
    }
}
