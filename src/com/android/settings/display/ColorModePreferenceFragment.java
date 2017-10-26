/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.graphics.drawable.Drawable;
import android.support.annotation.VisibleForTesting;

import com.android.internal.app.NightDisplayController;
import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.R;
import com.android.settings.widget.RadioButtonPickerFragment;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class ColorModePreferenceFragment extends RadioButtonPickerFragment {

    @VisibleForTesting
    static final String KEY_COLOR_MODE_NATURAL = "color_mode_natural";
    @VisibleForTesting
    static final String KEY_COLOR_MODE_BOOSTED = "color_mode_boosted";
    @VisibleForTesting
    static final String KEY_COLOR_MODE_SATURATED = "color_mode_saturated";

    private NightDisplayController mController;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mController = new NightDisplayController(context);
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        Context c = getContext();
        return Arrays.asList(
            new ColorModeCandidateInfo(c.getString(R.string.color_mode_option_natural),
                    KEY_COLOR_MODE_NATURAL),
            new ColorModeCandidateInfo(c.getString(R.string.color_mode_option_boosted),
                    KEY_COLOR_MODE_BOOSTED),
            new ColorModeCandidateInfo(c.getString(R.string.color_mode_option_saturated),
                    KEY_COLOR_MODE_SATURATED)
        );
    }

    @Override
    protected String getDefaultKey() {
        if (mController.getColorMode() == NightDisplayController.COLOR_MODE_SATURATED) {
            return KEY_COLOR_MODE_SATURATED;
        }
        if (mController.getColorMode() == NightDisplayController.COLOR_MODE_BOOSTED) {
            return KEY_COLOR_MODE_BOOSTED;
        }
        return KEY_COLOR_MODE_NATURAL;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        switch (key) {
            case KEY_COLOR_MODE_NATURAL:
                mController.setColorMode(NightDisplayController.COLOR_MODE_NATURAL);
                break;
            case KEY_COLOR_MODE_BOOSTED:
                mController.setColorMode(NightDisplayController.COLOR_MODE_BOOSTED);
                break;
            case KEY_COLOR_MODE_SATURATED:
                mController.setColorMode(NightDisplayController.COLOR_MODE_SATURATED);
                break;
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.COLOR_MODE_SETTINGS;
    }

    @VisibleForTesting
    static class ColorModeCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final String mKey;

        ColorModeCandidateInfo(CharSequence label, String key) {
            super(true);
            mLabel = label;
            mKey = key;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }

}
