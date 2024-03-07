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
package com.android.settings.security;

import static com.android.internal.R.string.config_defaultContentProtectionService;

import android.content.ComponentName;
import android.content.Context;
import android.provider.DeviceConfig;
import android.view.contentcapture.ContentCaptureManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.core.BasePreferenceController;

public class ContentProtectionPreferenceController extends BasePreferenceController {

    public ContentProtectionPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return ContentProtectionPreferenceUtils.isAvailable(mContext)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }
}
