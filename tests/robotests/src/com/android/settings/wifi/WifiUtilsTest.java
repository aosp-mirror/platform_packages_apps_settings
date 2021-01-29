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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;

import com.android.settingslib.wifi.AccessPoint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiUtilsTest {

    @Test
    public void testSSID() {
        assertThat(WifiUtils.isSSIDTooLong("123")).isFalse();
        assertThat(WifiUtils.isSSIDTooLong("☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎☎")).isTrue();

        assertThat(WifiUtils.isSSIDTooShort("123")).isFalse();
        assertThat(WifiUtils.isSSIDTooShort("")).isTrue();
    }

    @Test
    public void testPassword() {
        final String longPassword = "123456789012345678901234567890"
                + "1234567890123456789012345678901234567890";
        assertThat(WifiUtils.isHotspotWpa2PasswordValid("123")).isFalse();
        assertThat(WifiUtils.isHotspotWpa2PasswordValid("12345678")).isTrue();
        assertThat(WifiUtils.isHotspotWpa2PasswordValid("1234567890")).isTrue();
        assertThat(WifiUtils.isHotspotWpa2PasswordValid(longPassword)).isFalse();
        assertThat(WifiUtils.isHotspotWpa2PasswordValid("")).isFalse();
        assertThat(WifiUtils.isHotspotWpa2PasswordValid("€¥£")).isFalse();
    }

    @Test
    public void getWifiConfigByAccessPoint_shouldReturnCorrectConfig() {
        String testSSID = "WifiUtilsTest";
        Bundle bundle = new Bundle();
        bundle.putString("key_ssid", testSSID);
        Context context = spy(RuntimeEnvironment.application);
        AccessPoint accessPoint = new AccessPoint(context, bundle);

        WifiConfiguration config = WifiUtils.getWifiConfig(accessPoint, null, null);

        assertThat(config).isNotNull();
        assertThat(config.SSID).isEqualTo(AccessPoint.convertToQuotedString(testSSID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getWifiConfigWithNullInput_ThrowIllegalArgumentException() {
        WifiConfiguration config = WifiUtils.getWifiConfig(null, null, null);
    }
}
