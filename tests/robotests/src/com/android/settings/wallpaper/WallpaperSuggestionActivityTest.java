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

package com.android.settings.wallpaper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.SubSettings;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.util.ActivityController;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WallpaperSuggestionActivityTest {

    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    private ActivityController<WallpaperSuggestionActivity> mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mController = Robolectric.buildActivity(WallpaperSuggestionActivity.class);
    }

    @Test
    public void launch_primarySuggestionActivityDoesNotExist_shouldFallback() {
        ShadowActivity activity = shadowOf(mController.setup().get());
        final Intent intent = activity.getNextStartedActivity();

        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
    }
}
