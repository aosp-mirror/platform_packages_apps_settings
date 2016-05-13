/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.content.res.Resources;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.Display;
import android.view.View;
import android.widget.EditText;
import com.android.settings.CustomEditTextPreference;
import com.android.settings.R;
import com.android.settingslib.display.DisplayDensityUtils;

public class DensityPreference extends CustomEditTextPreference {
    private static final String TAG = "DensityPreference";

    public DensityPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onAttached() {
        super.onAttached();

        setSummary(getContext().getString(R.string.developer_density_summary, getCurrentSwDp()));
    }

    private int getCurrentSwDp() {
        final Resources res = getContext().getResources();
        final DisplayMetrics metrics = res.getDisplayMetrics();
        final float density = metrics.density;
        final int minDimensionPx = Math.min(metrics.widthPixels, metrics.heightPixels);
        return (int) (minDimensionPx / density);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        final EditText editText = (EditText) view.findViewById(android.R.id.edit);

        if (editText != null) {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setText(getCurrentSwDp() + "");
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            try {
                final Resources res = getContext().getResources();
                final DisplayMetrics metrics = res.getDisplayMetrics();
                final int newSwDp = Math.max(Integer.parseInt(getText()), 320);
                final int minDimensionPx = Math.min(metrics.widthPixels, metrics.heightPixels);
                final int newDensity = DisplayMetrics.DENSITY_MEDIUM * minDimensionPx / newSwDp;
                final int densityDpi = Math.max(newDensity, 120);
                DisplayDensityUtils.setForcedDisplayDensity(Display.DEFAULT_DISPLAY, densityDpi);
            } catch (Exception e) {
                // TODO: display a message instead of silently failing.
                Slog.e(TAG, "Couldn't save density", e);
            }
        }
    }
}
