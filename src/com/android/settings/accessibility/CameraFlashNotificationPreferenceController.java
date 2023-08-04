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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;

/**
 * Controller for Camera flash notification.
 */
public class CameraFlashNotificationPreferenceController extends TogglePreferenceController {

    public CameraFlashNotificationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return FlashNotificationsUtil.isTorchAvailable(mContext)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.CAMERA_FLASH_NOTIFICATION, OFF) != OFF;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().changed(
                getMetricsCategory(), getPreferenceKey(), isChecked ? 1 : 0);
        return Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.CAMERA_FLASH_NOTIFICATION, (isChecked ? ON : OFF));
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}
