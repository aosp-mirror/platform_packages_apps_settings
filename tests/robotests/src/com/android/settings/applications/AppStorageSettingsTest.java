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

package com.android.settings.applications;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Button;

import com.android.settingslib.applications.StorageStatsSource.AppStorageStats;
import com.android.settingslib.widget.ActionButtonsPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AppStorageSettingsTest {

    @Mock
    private AppStorageSizesController mSizesController;
    private ActionButtonsPreference mButtonsPref;
    private AppStorageSettings mSettings;
    private Button mLeftButton;
    private Button mRightButton;
    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLeftButton = new Button(RuntimeEnvironment.application);
        mRightButton = new Button(RuntimeEnvironment.application);
        mSettings = spy(new AppStorageSettings());
        mSettings.mPm = mPackageManager;
        mSettings.mPackageName = "Package";
        mSettings.mSizeController = mSizesController;
        mButtonsPref = createMock();
        mSettings.mButtonsPref = mButtonsPref;

        when(mButtonsPref.setButton1OnClickListener(any(View.OnClickListener.class)))
                .thenAnswer(invocation -> {
                    final Object[] args = invocation.getArguments();
                    mLeftButton.setOnClickListener((View.OnClickListener) args[0]);
                    return mButtonsPref;
                });
        when(mButtonsPref.setButton2OnClickListener(any(View.OnClickListener.class)))
                .thenAnswer(invocation -> {
                    final Object[] args = invocation.getArguments();
                    mRightButton.setOnClickListener((View.OnClickListener) args[0]);
                    return mButtonsPref;
                });
    }

    @Test
    public void updateUiWithSize_noAppStats_shouldDisableClearButtons()
            throws PackageManager.NameNotFoundException {
        mockMainlineModule(mSettings.mPackageName, false /* isMainlineModule */);
        mSettings.updateUiWithSize(null);

        verify(mSizesController).updateUi(nullable(Context.class));
        verify(mButtonsPref).setButton1Enabled(false);
        verify(mButtonsPref).setButton2Enabled(false);
    }

    @Test
    public void updateUiWithSize_hasDataAndCache_shouldEnableClearButtons()
            throws PackageManager.NameNotFoundException {
        final AppStorageStats stats = mock(AppStorageStats.class);
        when(stats.getCacheBytes()).thenReturn(5000L);
        when(stats.getDataBytes()).thenReturn(10000L);
        doNothing().when(mSettings).handleClearCacheClick();
        doNothing().when(mSettings).handleClearDataClick();
        mockMainlineModule(mSettings.mPackageName, false /* isMainlineModule */);


        mSettings.updateUiWithSize(stats);
        verify(mButtonsPref).setButton1Enabled(true);
        verify(mButtonsPref).setButton2Enabled(true);
        mLeftButton.performClick();
        verify(mSettings).handleClearDataClick();
        verify(mSettings, never()).handleClearCacheClick();

        mRightButton.performClick();
        verify(mSettings).handleClearDataClick();
        verify(mSettings).handleClearCacheClick();
    }

    @Test
    public void updateUiWithSize_mainlineModule_shouldDisableClearButtons()
            throws PackageManager.NameNotFoundException {
        final AppStorageStats stats = mock(AppStorageStats.class);
        when(stats.getCacheBytes()).thenReturn(5000L);
        when(stats.getDataBytes()).thenReturn(10000L);
        doNothing().when(mSettings).handleClearCacheClick();
        doNothing().when(mSettings).handleClearDataClick();
        mockMainlineModule(mSettings.mPackageName, true /* isMainlineModule */);


        mSettings.updateUiWithSize(stats);
        verify(mButtonsPref).setButton1Enabled(false);
        verify(mButtonsPref).setButton2Enabled(false);
    }

    private ActionButtonsPreference createMock() {
        final ActionButtonsPreference pref = mock(ActionButtonsPreference.class);
        when(pref.setButton1Text(anyInt())).thenReturn(pref);
        when(pref.setButton1Icon(anyInt())).thenReturn(pref);
        when(pref.setButton1Enabled(anyBoolean())).thenReturn(pref);
        when(pref.setButton1Visible(anyBoolean())).thenReturn(pref);
        when(pref.setButton1OnClickListener(any(View.OnClickListener.class))).thenReturn(pref);

        when(pref.setButton2Text(anyInt())).thenReturn(pref);
        when(pref.setButton2Icon(anyInt())).thenReturn(pref);
        when(pref.setButton2Enabled(anyBoolean())).thenReturn(pref);
        when(pref.setButton2Visible(anyBoolean())).thenReturn(pref);
        when(pref.setButton2OnClickListener(any(View.OnClickListener.class))).thenReturn(pref);

        return pref;
    }

    private void mockMainlineModule(String packageName, boolean isMainlineModule)
            throws PackageManager.NameNotFoundException {
        final PackageInfo packageInfo = new PackageInfo();
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.sourceDir = "apex";
        packageInfo.applicationInfo = applicationInfo;

        if (isMainlineModule) {
            when(mPackageManager.getModuleInfo(packageName, 0 /* flags */)).thenReturn(
                    new ModuleInfo());
        } else {
            when(mPackageManager.getPackageInfo(packageName, 0 /* flags */)).thenReturn(
                    packageInfo);
            when(mPackageManager.getModuleInfo(packageName, 0 /* flags */)).thenThrow(
                    new PackageManager.NameNotFoundException());
        }
    }
}

