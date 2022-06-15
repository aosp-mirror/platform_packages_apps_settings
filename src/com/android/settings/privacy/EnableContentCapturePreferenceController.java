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

package com.android.settings.privacy;

import android.annotation.NonNull;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;
import com.android.settings.utils.ContentCaptureUtils;

public final class EnableContentCapturePreferenceController extends TogglePreferenceController {

    public EnableContentCapturePreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        return ContentCaptureUtils.isEnabledForUser(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        ContentCaptureUtils.setEnabledForUser(mContext, isChecked);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        boolean available = ContentCaptureUtils.isFeatureAvailable()
                && ContentCaptureUtils.getServiceSettingsComponentName() == null;
        return available ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}
