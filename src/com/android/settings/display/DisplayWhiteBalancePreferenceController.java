/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.os.UserHandle;
import android.provider.Settings.Secure;
import androidx.annotation.VisibleForTesting;

import com.android.settings.core.TogglePreferenceController;

public class DisplayWhiteBalancePreferenceController extends TogglePreferenceController {

    private ColorDisplayManager mColorDisplayManager;

    public DisplayWhiteBalancePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        // Display white balance is only valid in linear light space. COLOR_MODE_SATURATED implies
        // unmanaged color mode, and hence unknown color processing conditions.
        return ColorDisplayManager.isDisplayWhiteBalanceAvailable(mContext) &&
                getColorDisplayManager().getColorMode() !=
                        ColorDisplayManager.COLOR_MODE_SATURATED ?
                AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public boolean isChecked() {
        return getColorDisplayManager().isDisplayWhiteBalanceEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return getColorDisplayManager().setDisplayWhiteBalanceEnabled(isChecked);
    }

    @VisibleForTesting
    ColorDisplayManager getColorDisplayManager() {
        if (mColorDisplayManager == null) {
            mColorDisplayManager = mContext.getSystemService(ColorDisplayManager.class);
        }
        return mColorDisplayManager;
    }
}
