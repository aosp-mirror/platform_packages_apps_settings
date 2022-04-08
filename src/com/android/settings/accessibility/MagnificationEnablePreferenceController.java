/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

/** Controller that shows the magnification enable mode summary. */
public class MagnificationEnablePreferenceController extends TogglePreferenceController {

    private static final String KEY_ENABLE = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE;

    public MagnificationEnablePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        final int enableMode = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                KEY_ENABLE,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN,
                UserHandle.USER_CURRENT);
        return enableMode == Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final int value = isChecked ? Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN
                : Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW;
        return Settings.Secure.putIntForUser(mContext.getContentResolver(), KEY_ENABLE, value,
                UserHandle.USER_CURRENT);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
