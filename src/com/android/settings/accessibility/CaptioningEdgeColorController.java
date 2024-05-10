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

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.ListDialogPreference.OnValueChangedListener;
import com.android.settings.core.BasePreferenceController;

/** Preference controller for captioning edge color. */
public class CaptioningEdgeColorController extends BasePreferenceController
        implements OnValueChangedListener {

    private final CaptionHelper mCaptionHelper;

    public CaptioningEdgeColorController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mCaptionHelper = new CaptionHelper(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
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
        preference.setValue(mCaptionHelper.getEdgeColor());
        preference.setOnValueChangedListener(this);
    }

    @Override
    public void onValueChanged(ListDialogPreference preference, int value) {
        mCaptionHelper.setEdgeColor(value);
        mCaptionHelper.setEnabled(true);
    }
}
