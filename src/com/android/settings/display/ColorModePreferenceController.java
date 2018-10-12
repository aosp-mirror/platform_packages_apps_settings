/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;

import androidx.annotation.VisibleForTesting;

import com.android.settings.core.BasePreferenceController;

public class ColorModePreferenceController extends BasePreferenceController {

    public ColorModePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        final int[] availableColorModes = mContext.getResources().getIntArray(
                com.android.internal.R.array.config_availableColorModes);
        return availableColorModes.length > 0 &&
                mContext.getSystemService(ColorDisplayManager.class).isDeviceColorManaged()
                && !ColorDisplayManager.areAccessibilityTransformsEnabled(mContext) ?
                AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public CharSequence getSummary() {
        return ColorModeUtils.getColorModeMapping(mContext.getResources()).get(getColorMode());
    }

    @VisibleForTesting
    public int getColorMode() {
        return mContext.getSystemService(ColorDisplayManager.class).getColorMode();
    }
}
