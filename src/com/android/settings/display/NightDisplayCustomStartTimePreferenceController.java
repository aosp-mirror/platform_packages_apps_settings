/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.content.Context;

import androidx.preference.Preference;

import android.hardware.display.ColorDisplayManager;

import com.android.internal.app.ColorDisplayController;
import com.android.settings.core.BasePreferenceController;

public class NightDisplayCustomStartTimePreferenceController extends BasePreferenceController {

    private ColorDisplayController mController;
    private NightDisplayTimeFormatter mTimeFormatter;

    public NightDisplayCustomStartTimePreferenceController(Context context, String key) {
        super(context, key);

        mController = new ColorDisplayController(context);
        mTimeFormatter = new NightDisplayTimeFormatter(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return ColorDisplayManager.isNightDisplayAvailable(mContext) ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public final void updateState(Preference preference) {
        preference.setVisible(mController.getAutoMode() == ColorDisplayController.AUTO_MODE_CUSTOM);
        preference.setSummary(mTimeFormatter.getFormattedTimeString(
                mController.getCustomStartTime()));
    }
}
