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

package com.android.settings.wifi;


import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WifiUtilsTest {

    @Test
    public void testSSID() {
        assertThat(WifiUtils.isSSIDTooLong("123")).isFalse();
        assertThat(WifiUtils.isSSIDTooLong("☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎")).isTrue();

        assertThat(WifiUtils.isSSIDTooShort("123")).isFalse();
        assertThat(WifiUtils.isSSIDTooShort("")).isTrue();
    }

    @Test
    public void testPassword() {
        final String longPassword = "123456789012345678901234567890"
                + "1234567890123456789012345678901234567890";
        assertThat(WifiUtils.isPasswordValid("123")).isFalse();
        assertThat(WifiUtils.isPasswordValid("12345678")).isTrue();
        assertThat(WifiUtils.isPasswordValid("1234567890")).isTrue();
        assertThat(WifiUtils.isPasswordValid(longPassword)).isFalse();
    }

}
