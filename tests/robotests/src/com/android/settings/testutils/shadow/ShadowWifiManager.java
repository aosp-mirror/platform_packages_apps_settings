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

package com.android.settings.testutils.shadow;

import static org.robolectric.RuntimeEnvironment.application;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.hotspot2.PasspointConfiguration;

import org.robolectric.annotation.HiddenApi;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

import java.util.Collections;
import java.util.List;

@Implements(value = WifiManager.class)
public class ShadowWifiManager extends org.robolectric.shadows.ShadowWifiManager {

    public WifiConfiguration savedWifiConfig;

    @HiddenApi // @SystemApi
    @Implementation
    public void connect(WifiConfiguration config, WifiManager.ActionListener listener) {
        savedWifiConfig = config;
    }

    @HiddenApi
    @Implementation
    public void save(WifiConfiguration config, WifiManager.ActionListener listener) {
        savedWifiConfig = config;
    }

    @Implementation
    public List<PasspointConfiguration> getPasspointConfigurations() {
        return Collections.emptyList();
    }

    @Implementation
    public boolean isDualModeSupported() {
        return false;
    }

    public static ShadowWifiManager get() {
        return Shadow.extract(application.getSystemService(WifiManager.class));
    }
}
