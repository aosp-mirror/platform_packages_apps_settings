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

import android.annotation.Nullable;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.graph.BatteryMeterDrawableBase;

public class BatteryMeterView extends ImageView {
    private BatteryMeterDrawable mDrawable;

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final int frameColor = context.getColor(R.color.batterymeter_frame_color);
        final int tintColor = Utils.getColorAttr(context, android.R.attr.colorAccent);

        mDrawable = new BatteryMeterDrawable(context, frameColor);
        mDrawable.setColorFilter(new PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN));
        mDrawable.setShowPercent(true);
        setImageDrawable(mDrawable);
    }

    public void setBatteryInfo(int level) {
        mDrawable.setBatteryLevel(level);
    }

    @VisibleForTesting
    void setBatteryDrawable(BatteryMeterDrawable drawable) {
        mDrawable = drawable;
    }

    public static class BatteryMeterDrawable extends BatteryMeterDrawableBase {
        private final int mIntrinsicWidth;
        private final int mIntrinsicHeight;

        public BatteryMeterDrawable(Context context, int frameColor) {
            super(context, frameColor);

            mIntrinsicWidth = context.getResources()
                    .getDimensionPixelSize(R.dimen.battery_meter_width);
            mIntrinsicHeight = context.getResources()
                    .getDimensionPixelSize(R.dimen.battery_meter_height);
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
