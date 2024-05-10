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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import com.android.settingslib.Utils;
import com.android.settingslib.graph.BatteryMeterDrawableBase;

/** Drawable that shows a static battery saver icon - a full battery symbol and a plus sign. */
public class BatterySaverDrawable extends BatteryMeterDrawableBase {

    private static final int MAX_BATTERY = 100;

    public BatterySaverDrawable(Context context, int frameColor) {
        super(context, frameColor);
        // Show as full so it's always uniform color
        setBatteryLevel(MAX_BATTERY);
        setPowerSave(true);
        setCharging(false);
        setPowerSaveAsColorError(false);
        final int tintColor = Utils.getColorAttrDefaultColor(context, android.R.attr.colorAccent);
        setColorFilter(new PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN));
    }
}
