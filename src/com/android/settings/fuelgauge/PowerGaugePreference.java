/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.view.View;
import android.widget.ImageView;

import com.android.settings.R;
import com.android.settings.fuelgauge.PowerUsageSummary.BatterySipper;

/**
 * Custom preference for displaying power consumption as a bar and an icon on the left for the
 * subsystem/app type.
 *
 */
public class PowerGaugePreference extends Preference {

    private Drawable mIcon;
    private GaugeDrawable mGauge;
    private double mValue;
    private BatterySipper mInfo;

    public PowerGaugePreference(Context context, Drawable icon, BatterySipper info) {
        super(context);
        setLayoutResource(R.layout.preference_powergauge);
        mIcon = icon;
        mGauge = new GaugeDrawable();
        mGauge.bar = context.getResources().getDrawable(R.drawable.app_gauge);
        mInfo = info;
    }

    /**
     * Sets the width of the gauge in percentage (0 - 100)
     * @param percent
     */
    void setGaugeValue(double percent) {
        mValue = percent;
        mGauge.percent = mValue;
    }

    BatterySipper getInfo() {
        return mInfo;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        ImageView appIcon = (ImageView) view.findViewById(R.id.appIcon);
        if (mIcon == null) {
            mIcon = getContext().getResources().getDrawable(android.R.drawable.sym_def_app_icon);
        }
        appIcon.setImageDrawable(mIcon);

        ImageView appGauge = (ImageView) view.findViewById(R.id.appGauge);
        appGauge.setImageDrawable(mGauge);
    }

    static class GaugeDrawable extends Drawable {
        Drawable bar;
        double percent;
        int lastWidth = -1;

        @Override
        public void draw(Canvas canvas) {
            if (lastWidth == -1) {
                lastWidth = getBarWidth();
                bar.setBounds(0, 0, lastWidth, bar.getIntrinsicHeight());
            }
            bar.draw(canvas);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public void setAlpha(int alpha) {
            // Ignore
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            // Ignore
        }

        private int getBarWidth() {
            int width = (int) ((this.getBounds().width() * percent) / 100);
            int intrinsicWidth = bar.getIntrinsicWidth();
            return Math.max(width, intrinsicWidth);
        }

        @Override
        public int getIntrinsicHeight() {
            return bar.getIntrinsicHeight();
        }
    }
}
