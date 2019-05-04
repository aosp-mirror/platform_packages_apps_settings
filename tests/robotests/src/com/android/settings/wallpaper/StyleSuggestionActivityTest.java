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
 * limitations under the License
 */

package com.android.settings.wallpaper;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class StyleSuggestionActivityTest {

    @Mock
    private Context mContext;
    @Mock
    private Resources mResources;
    @Mock
    private ContentResolver mContentResolver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getResources()).thenReturn(mResources);
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
    }

    @Test
    public void wallpaperServiceEnabled_no_shouldReturnTrue() {
        when(mResources.getBoolean(com.android.internal.R.bool.config_enableWallpaperService))
                .thenReturn(false);
        assertThat(StyleSuggestionActivity.isSuggestionComplete(mContext)).isTrue();
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void hasStyleSet_yes_shouldReturnTrue() {
        when(mResources.getBoolean(com.android.internal.R.bool.config_enableWallpaperService))
                .thenReturn(true);

        Settings.Secure.putString(mContentResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES, "test");
        assertThat(StyleSuggestionActivity.isSuggestionComplete(mContext)).isTrue();
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void hasStyleSet_no_shouldReturnFalse() {
        when(mResources.getBoolean(com.android.internal.R.bool.config_enableWallpaperService))
                .thenReturn(true);

        Settings.Secure.putString(mContentResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES, null);
        assertThat(StyleSuggestionActivity.isSuggestionComplete(mContext)).isFalse();
    }
}
