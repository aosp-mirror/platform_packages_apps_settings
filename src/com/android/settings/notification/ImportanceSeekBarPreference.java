/**
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.notification;

import com.android.settings.R;
import com.android.settings.SeekBarPreference;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.SeekBar;

/**
 * A slider preference that controls notification importance.
 **/
public class ImportanceSeekBarPreference extends SeekBarPreference implements
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "ImportanceSeekBarPref";

    private Callback mCallback;
    private int mMinProgress;
    private boolean mSystemApp;

    public ImportanceSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_importance_slider);
    }

    public ImportanceSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ImportanceSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImportanceSeekBarPreference(Context context) {
        this(context, null);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void setMinimumProgress(int minProgress) {
        mMinProgress = minProgress;
        notifyChanged();
    }

    public void setSystemApp(boolean systemApp) {
        mSystemApp = systemApp;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (mSystemApp) {
            ((ImageView) view.findViewById(R.id.low_importance)).getDrawable().setTint(
                    getContext().getColor(R.color.importance_disabled_tint));
        }
        view.setDividerAllowedAbove(false);
        view.setDividerAllowedBelow(false);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        super.onProgressChanged(seekBar, progress, fromTouch);
        if (progress < mMinProgress) {
            seekBar.setProgress(mMinProgress);
            progress = mMinProgress;
        }
        if (fromTouch) {
            mCallback.onImportanceChanged(progress);
        }
    }

    public interface Callback {
        void onImportanceChanged(int progress);
    }
}
