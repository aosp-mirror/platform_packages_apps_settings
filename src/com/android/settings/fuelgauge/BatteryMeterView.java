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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.graphics.ColorFilter;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.graph.ThemedBatteryDrawable;

public class BatteryMeterView extends ImageView {
    @VisibleForTesting BatteryMeterDrawable mDrawable;
    @VisibleForTesting ColorFilter mErrorColorFilter;
    @VisibleForTesting ColorFilter mAccentColorFilter;
    @VisibleForTesting ColorFilter mForegroundColorFilter;

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final int frameColor =
                context.getColor(com.android.settingslib.R.color.meter_background_color);
        mAccentColorFilter =
                Utils.getAlphaInvariantColorFilterForColor(
                        Utils.getColorAttrDefaultColor(context, android.R.attr.colorAccent));
        mErrorColorFilter =
                Utils.getAlphaInvariantColorFilterForColor(
                        context.getColor(R.color.battery_icon_color_error));
        mForegroundColorFilter =
                Utils.getAlphaInvariantColorFilterForColor(
                        Utils.getColorAttrDefaultColor(context, android.R.attr.colorForeground));
        mDrawable = new BatteryMeterDrawable(context, frameColor);
        mDrawable.setColorFilter(mAccentColorFilter);
        setImageDrawable(mDrawable);
    }

    public void setBatteryLevel(int level) {
        mDrawable.setBatteryLevel(level);
        updateColorFilter();
    }

    public void setPowerSave(boolean powerSave) {
        mDrawable.setPowerSaveEnabled(powerSave);
        updateColorFilter();
    }

    public boolean getPowerSave() {
        return mDrawable.getPowerSaveEnabled();
    }

    public int getBatteryLevel() {
        return mDrawable.getBatteryLevel();
    }

    public void setCharging(boolean charging) {
        mDrawable.setCharging(charging);
        postInvalidate();
    }

    public boolean getCharging() {
        return mDrawable.getCharging();
    }

    private void updateColorFilter() {
        final boolean powerSaveEnabled = mDrawable.getPowerSaveEnabled();
        final int level = mDrawable.getBatteryLevel();
        if (powerSaveEnabled) {
            mDrawable.setColorFilter(mForegroundColorFilter);
        } else if (level < mDrawable.getCriticalLevel()) {
            mDrawable.setColorFilter(mErrorColorFilter);
        } else {
            mDrawable.setColorFilter(mAccentColorFilter);
        }
    }

    public static class BatteryMeterDrawable extends ThemedBatteryDrawable {
        private final int mIntrinsicWidth;
        private final int mIntrinsicHeight;

        public BatteryMeterDrawable(Context context, int frameColor) {
            super(context, frameColor);

            mIntrinsicWidth =
                    context.getResources().getDimensionPixelSize(R.dimen.battery_meter_width);
            mIntrinsicHeight =
                    context.getResources().getDimensionPixelSize(R.dimen.battery_meter_height);
        }

        public BatteryMeterDrawable(Context context, int frameColor, int width, int height) {
            super(context, frameColor);

            mIntrinsicWidth = width;
            mIntrinsicHeight = height;
        }

        @Override
        public int getIntrinsicWidth() {
            return mIntrinsicWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return mIntrinsicHeight;
        }
    }
}
