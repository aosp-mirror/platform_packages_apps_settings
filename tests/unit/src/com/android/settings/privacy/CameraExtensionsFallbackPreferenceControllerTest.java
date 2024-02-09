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

package com.android.settings.privacy;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.privacy.CameraExtensionsFallbackPreferenceController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CameraExtensionsFallbackPreferenceControllerTest {

    private static final String KEY = "camera_extensions_fallback";
    private final Context mContext = ApplicationProvider.getApplicationContext();

    private int mOriginalPreference;
    private ContentResolver mContentResolver;
    private CameraExtensionsFallbackPreferenceController mController;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mOriginalPreference = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.CAMERA_EXTENSIONS_FALLBACK, 1);
        mController = new CameraExtensionsFallbackPreferenceController(mContext, KEY);
    }

    @After
    public void tearDown() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.CAMERA_EXTENSIONS_FALLBACK,
                mOriginalPreference);
    }

    @Test
    public void getAvailability_returnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void setChecked_disable_shouldTurnOff() {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.CAMERA_EXTENSIONS_FALLBACK, 1);

        assertThat(mController.isChecked()).isTrue();

        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(mContentResolver,
                Settings.Secure.CAMERA_EXTENSIONS_FALLBACK, -1)).isEqualTo(0);
    }

    @Test
    public void setChecked_enable_shouldTurnOn() {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.CAMERA_EXTENSIONS_FALLBACK, 0);

        assertThat(mController.isChecked()).isFalse();

        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(mContentResolver,
                Settings.Secure.CAMERA_EXTENSIONS_FALLBACK, -1)).isEqualTo(1);
    }
}
