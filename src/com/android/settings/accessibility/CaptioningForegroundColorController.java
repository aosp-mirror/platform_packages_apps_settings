/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.content.res.Resources;
import android.view.accessibility.CaptioningManager.CaptionStyle;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.ListDialogPreference.OnValueChangedListener;

/** Preference controller for captioning foreground color. */
public class CaptioningForegroundColorController extends BaseCaptioningCustomController
        implements OnValueChangedListener {

    private int mCachedNonDefaultOpacity = CaptionStyle.COLOR_UNSPECIFIED;

    public CaptioningForegroundColorController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final ColorPreference preference = screen.findPreference(getPreferenceKey());
        final Resources res = mContext.getResources();
        final int[] colorValues = res.getIntArray(R.array.captioning_color_selector_values);
        final String[] colorTitles = res.getStringArray(
                R.array.captioning_color_selector_titles);
        preference.setTitles(colorTitles);
        preference.setValues(colorValues);
        final int foregroundColor = mCaptionHelper.getForegroundColor();
        final int color = CaptionUtils.parseColor(foregroundColor);
        preference.setValue(color);
        preference.setOnValueChangedListener(this);
    }

    @Override
    public void onValueChanged(ListDialogPreference preference, int value) {
        final boolean isNonDefaultColor = CaptionStyle.hasColor(value);
        final int opacity = getNonDefaultOpacity(isNonDefaultColor);
        final int merged = CaptionUtils.mergeColorOpacity(value, opacity);
        mCaptionHelper.setForegroundColor(merged);
        mCaptionHelper.setEnabled(true);
    }

    private int getNonDefaultOpacity(boolean isNonDefaultColor) {
        final int foregroundColor = mCaptionHelper.getForegroundColor();
        final int opacity = CaptionUtils.parseOpacity(foregroundColor);
        if (isNonDefaultColor) {
            final int lastOpacity = mCachedNonDefaultOpacity != CaptionStyle.COLOR_UNSPECIFIED
                    ? mCachedNonDefaultOpacity : opacity;
            // Reset cached opacity to use current color opacity to merge color.
            mCachedNonDefaultOpacity = CaptionStyle.COLOR_UNSPECIFIED;
            return lastOpacity;
        }
        // When default captioning color was selected, the opacity become 100% and make opacity
        // preference disable. Cache the latest opacity to show the correct opacity later.
        mCachedNonDefaultOpacity = opacity;
        return opacity;
    }
}
