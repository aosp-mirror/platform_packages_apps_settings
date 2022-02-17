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
import android.view.accessibility.CaptioningManager;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;

/** Controller that shows the caption scale and style summary. */
public class CaptionAppearancePreferenceController extends BasePreferenceController {

    private final CaptioningManager mCaptioningManager;

    public CaptionAppearancePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mCaptioningManager = context.getSystemService(CaptioningManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getString(R.string.preference_summary_default_combination,
                geFontScaleSummary(), getPresetSummary());
    }

    private float[] getFontScaleValuesArray() {
        final String[] fontScaleValuesStrArray = mContext.getResources().getStringArray(
                R.array.captioning_font_size_selector_values);
        final int length = fontScaleValuesStrArray.length;
        final float[] fontScaleValuesArray = new float[length];
        for (int i = 0; i < length; ++i) {
            fontScaleValuesArray[i] = Float.parseFloat(fontScaleValuesStrArray[i]);
        }
        return fontScaleValuesArray;
    }

    private CharSequence geFontScaleSummary() {
        final float[] fontScaleValuesArray = getFontScaleValuesArray();
        final String[] fontScaleSummaries = mContext.getResources().getStringArray(
                R.array.captioning_font_size_selector_titles);
        final float fontScale = mCaptioningManager.getFontScale();
        final int idx = Floats.indexOf(fontScaleValuesArray, fontScale);
        return fontScaleSummaries[idx == /* not exist */ -1 ? 0 : idx];
    }

    private CharSequence getPresetSummary() {
        final int[] presetValuesArray = mContext.getResources().getIntArray(
                R.array.captioning_preset_selector_values);
        final String[] presetSummaries = mContext.getResources().getStringArray(
                R.array.captioning_preset_selector_titles);
        final int preset = mCaptioningManager.getRawUserStyle();
        final int idx = Ints.indexOf(presetValuesArray, preset);
        return presetSummaries[idx];
    }
}
