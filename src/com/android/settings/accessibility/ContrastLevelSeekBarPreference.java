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
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;

import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;

/** A slider preference that directly controls the contrast level **/
public class ContrastLevelSeekBarPreference extends SeekBarPreference {

    /**
     * The number of ticks of the slider (the more ticks, the more continuous the slider feels).
     */
    public static final int CONTRAST_SLIDER_TICKS = 2;

    private final Context mContext;
    private ContrastLevelSeekBar mSeekBar;

    public ContrastLevelSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs, TypedArrayUtils.getAttr(context,
                R.attr.preferenceStyle,
                android.R.attr.preferenceStyle));
        mContext = context;
        setLayoutResource(R.layout.preference_contrast_level_slider);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mSeekBar = (ContrastLevelSeekBar) view.findViewById(
                com.android.internal.R.id.seekbar);
        init();
    }

    private void init() {
        if (mSeekBar == null) {
            return;
        }
        final float contrastLevel = Settings.Secure.getFloatForUser(
                mContext.getContentResolver(), Settings.Secure.CONTRAST_LEVEL,
                0.f /* default */, UserHandle.USER_CURRENT);

        mSeekBar.setMax(CONTRAST_SLIDER_TICKS);

        // Rescale contrast from [0, 0.5, 1] to [0, 1, 2]
        int progress = Math.max(0, Math.round(contrastLevel * CONTRAST_SLIDER_TICKS));

        mSeekBar.setProgress(progress);
        mSeekBar.setEnabled(isEnabled());
    }
}
