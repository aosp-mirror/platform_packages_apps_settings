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

package com.android.settings.display;

import android.os.PowerManager;

/**
 * This class replicates a subset of the android.os.PowerManager. The class exists so that we can
 * use a thin wrapper around the PowerManager in production code and a mock in tests. We cannot
 * directly mock or shadow the PowerManager, because some of the methods we rely on are newer than
 * the API version supported by Robolectric or are hidden.
 */
public class PowerManagerWrapper {
    private final PowerManager mPowerManager;

    public PowerManagerWrapper(PowerManager powerManager) {
        mPowerManager = powerManager;
    }

    public int getMinimumScreenBrightnessSetting() {
        return mPowerManager.getMinimumScreenBrightnessSetting();
    }

    public int getMaximumScreenBrightnessSetting() {
        return mPowerManager.getMaximumScreenBrightnessSetting();
    }

    public int getMinimumScreenBrightnessForVrSetting() {
        return mPowerManager.getMinimumScreenBrightnessForVrSetting();
    }

    public int getMaximumScreenBrightnessForVrSetting() {
        return mPowerManager.getMaximumScreenBrightnessForVrSetting();
    }
}
