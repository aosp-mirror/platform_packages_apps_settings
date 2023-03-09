/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link FontSizeData}.
 */
@RunWith(RobolectricTestRunner.class)
public class FontSizeDataTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private FontSizeData mFontSizeData;

    @Before
    public void setUp() {
        mFontSizeData = new FontSizeData(mContext);
    }

    @Test
    public void commit_success() {
        final int progress = 3;

        mFontSizeData.commit(progress);
        final float currentScale =
                Settings.System.getFloat(mContext.getContentResolver(), Settings.System.FONT_SCALE,
                        /* def= */ 1.0f);

        assertThat(currentScale).isEqualTo(mFontSizeData.getValues().get(progress));
    }

    @Test
    public void commit_fontScalingHasBeenChangedIsOn() {
        final int progress = 3;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FONT_SCALING_HAS_BEEN_CHANGED, OFF);

        mFontSizeData.commit(progress);
        final int currentSettings = Settings.Secure.getInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FONT_SCALING_HAS_BEEN_CHANGED,
                /* def= */ OFF);

        assertThat(currentSettings).isEqualTo(ON);
    }
}
