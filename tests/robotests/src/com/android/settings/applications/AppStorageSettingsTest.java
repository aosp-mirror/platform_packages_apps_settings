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

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.widget.ActionButtonPreference;
import com.android.settings.widget.ActionButtonPreferenceTest;
import com.android.settingslib.applications.StorageStatsSource.AppStorageStats;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class AppStorageSettingsTest {

    @Mock
    private AppStorageSizesController mSizesController;
    private ActionButtonPreference mButtonsPref;
    private AppStorageSettings mSettings;
    private Button mLeftButton;
    private Button mRightButton;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLeftButton = new Button(RuntimeEnvironment.application);
        mRightButton = new Button(RuntimeEnvironment.application);
        mSettings = spy(new AppStorageSettings());
        mSettings.mSizeController = mSizesController;
        mButtonsPref = ActionButtonPreferenceTest.createMock();
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
    public void updateUiWithSize_noAppStats_shouldDisableClearButtons() {
        mSettings.updateUiWithSize(null);

        verify(mSizesController).updateUi(nullable(Context.class));
        verify(mButtonsPref).setButton1Enabled(false);
        verify(mButtonsPref).setButton2Enabled(false);
    }

    @Test
    public void updateUiWithSize_hasDataAndCache_shouldEnableClearButtons() {
        final AppStorageStats stats = mock(AppStorageStats.class);
        when(stats.getCacheBytes()).thenReturn(5000L);
        when(stats.getDataBytes()).thenReturn(10000L);
        doNothing().when(mSettings).handleClearCacheClick();
        doNothing().when(mSettings).handleClearDataClick();

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
}

