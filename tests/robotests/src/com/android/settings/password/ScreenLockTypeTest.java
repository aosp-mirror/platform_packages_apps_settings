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
 * limitations under the License
 */

package com.android.settings.password;

import static com.google.common.truth.Truth.assertThat;

import android.app.admin.DevicePolicyManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ScreenLockTypeTest {

    @Test
    public void fromQuality_shouldReturnLockWithAssociatedQuality() {
        assertThat(ScreenLockType.fromQuality(DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC))
                .isEqualTo(ScreenLockType.PASSWORD);
        assertThat(ScreenLockType.fromQuality(DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC))
                .isEqualTo(ScreenLockType.PASSWORD);
        assertThat(ScreenLockType.fromQuality(DevicePolicyManager.PASSWORD_QUALITY_BIOMETRIC_WEAK))
                .isNull();
        assertThat(ScreenLockType.fromQuality(DevicePolicyManager.PASSWORD_QUALITY_COMPLEX))
                .isEqualTo(ScreenLockType.PASSWORD);
        assertThat(ScreenLockType.fromQuality(DevicePolicyManager.PASSWORD_QUALITY_MANAGED))
                .isEqualTo(ScreenLockType.MANAGED);
        assertThat(ScreenLockType.fromQuality(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC))
                .isEqualTo(ScreenLockType.PIN);
        assertThat(ScreenLockType.fromQuality(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX))
                .isEqualTo(ScreenLockType.PIN);
        assertThat(ScreenLockType.fromQuality(DevicePolicyManager.PASSWORD_QUALITY_SOMETHING))
                .isEqualTo(ScreenLockType.PATTERN);
        assertThat(ScreenLockType.fromQuality(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED))
                .isEqualTo(ScreenLockType.SWIPE);
    }

    @Test
    public void fromKey_shouldReturnLockWithGivenKey() {
        for (ScreenLockType lock : ScreenLockType.values()) {
            assertThat(ScreenLockType.fromKey(lock.preferenceKey)).isEqualTo(lock);
        }
        assertThat(ScreenLockType.fromKey("nonexistent")).isNull();
    }
}
