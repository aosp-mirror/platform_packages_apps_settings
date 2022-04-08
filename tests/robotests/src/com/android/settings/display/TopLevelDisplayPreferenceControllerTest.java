/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.display;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TopLevelDisplayPreferenceControllerTest {
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;

    private TopLevelDisplayPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getString(R.string.config_wallpaper_picker_package)).thenReturn("pkg");
        when(mContext.getString(R.string.config_wallpaper_picker_class)).thenReturn("cls");

        mController = new TopLevelDisplayPreferenceController(mContext, "test_key");
    }

    @Test
    public void getAvailibilityStatus_availableByDefault() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getAvailabilityStatus_unsupportedWhenSet() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getSummary_hasWallpaper_shouldReturnWallpaperSummary() {
        final List<ResolveInfo> resolveInfos = new ArrayList<>();
        resolveInfos.add(mock(ResolveInfo.class));
        when(mPackageManager.queryIntentActivities(any(Intent.class), anyInt()))
                .thenReturn(resolveInfos);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.display_dashboard_summary));
    }

    @Test
    public void getSummary_hasWallpaperWithStyles_shouldReturnWallpaperSummary() {
        when(mContext.getString(R.string.config_styles_and_wallpaper_picker_class))
                .thenReturn("any.nonempty.class");
        final List<ResolveInfo> resolveInfos = new ArrayList<>();
        resolveInfos.add(mock(ResolveInfo.class));
        when(mPackageManager.queryIntentActivities(any(Intent.class), anyInt()))
                .thenReturn(resolveInfos);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.display_dashboard_summary_with_style));
    }

    @Test
    public void getSummary_hasWallpaper_shouldReturnNoWallpaperSummary() {
        final List<ResolveInfo> resolveInfos = new ArrayList<>();
        when(mPackageManager.queryIntentActivities(any(Intent.class), anyInt()))
                .thenReturn(resolveInfos);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.display_dashboard_nowallpaper_summary));
    }
}
