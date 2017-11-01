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

package com.android.ims;

import android.content.Context;

/**
 * Fake test class to com.android.ims.ImsManager
 */
public class ImsManager {

    public static boolean wfcEnabledByPlatform;
    public static boolean wfcProvisioned;

    public static boolean isWfcEnabledByPlatform(Context context) {
        return wfcEnabledByPlatform;
    }

    public static boolean isWfcProvisionedOnDevice(Context context) {
        return wfcProvisioned;
    }

    public static int getWfcMode(Context context, boolean roaming) {
        return 0;
    }

    public static boolean isWfcEnabledByUser(Context context) {
        return false;
    }
}
