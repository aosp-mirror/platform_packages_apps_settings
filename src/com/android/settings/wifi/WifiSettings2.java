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
 * limitations under the License.
 */

package com.android.settings.wifi;

import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

import android.app.settings.SettingsEnums;

import com.android.settings.RestrictedSettingsFragment;

public class WifiSettings2 extends RestrictedSettingsFragment {

    public WifiSettings2() {
        super(DISALLOW_CONFIG_WIFI);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WIFI;
    }
}
