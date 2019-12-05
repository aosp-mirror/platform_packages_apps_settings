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

package com.android.settings.security;

import android.content.Context;
import android.os.SystemProperties;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.BasePreferenceController;

/**
 *  This controller decides the verification result of the installed app.
 */
public class InstallAppSourceCertificatePreferenceController extends
        BasePreferenceController {

    private static final String APK_VERITY_PROPERTY = "ro.apk_verity.mode";
    private static final int APK_VERITY_MODE_ENABLED = 2;

    public InstallAppSourceCertificatePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return isApkVerityEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @VisibleForTesting
    static boolean isApkVerityEnabled() {
        // TODO(victorhsieh): replace this with a new API in PackageManager once it is landed.
        return SystemProperties.getInt(APK_VERITY_PROPERTY, 0) == APK_VERITY_MODE_ENABLED;
    }
}
