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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AccessibilityTimeoutUtils}. */
@RunWith(RobolectricTestRunner.class)
public final class AccessibilityTimeoutUtilsTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void getSecureAccessibilityTimeoutValue_byDefault_shouldReturnDefaultValue() {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS, "0");

        final int timeOutValue = AccessibilityTimeoutUtils.getSecureAccessibilityTimeoutValue(
                mContext.getContentResolver());
        assertThat(timeOutValue).isEqualTo(0);
    }

    @Test
    public void getSecureAccessibilityTimeoutValue_invalidTimeout_shouldReturnDefaultValue() {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS, "invalid_timeout");

        final int timeOutValue = AccessibilityTimeoutUtils.getSecureAccessibilityTimeoutValue(
                mContext.getContentResolver());
        assertThat(timeOutValue).isEqualTo(0);
    }

    @Test
    public void getSecureAccessibilityTimeoutValue_validTimeout_shouldReturnValidValue() {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS, "60000");

        final int timeOutValue = AccessibilityTimeoutUtils.getSecureAccessibilityTimeoutValue(
                mContext.getContentResolver());
        assertThat(timeOutValue).isEqualTo(60000);
    }
}
