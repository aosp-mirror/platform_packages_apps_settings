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
import android.service.notification.NotificationListenerService;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * A slider preference that controls notification importance.
 **/
public class ImportanceSeekBarPreference extends SeekBarPreference implements
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "ImportanceSeekBarPref";

    private Callback mCallback;
    private TextView mSummaryTextView;
    private String mSummary;

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

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mSummaryTextView = (TextView) view.findViewById(com.android.internal.R.id.summary);
    }

    @Override
    public void setProgress(int progress) {
        mSummary = getProgressSummary(progress);
        super.setProgress(progress);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromTouch) {
        super.onProgressChanged(seekBar, progress, fromTouch);
        if (mSummaryTextView != null) {
            mSummaryTextView.setText(getProgressSummary(progress));
        }
        if (fromTouch) {
            mCallback.onImportanceChanged(progress);
        }
    }

    @Override
    public CharSequence getSummary() {
        return mSummary;
    }

    private String getProgressSummary(int progress) {
        switch (progress) {
            case NotificationListenerService.Ranking.IMPORTANCE_NONE:
                return getContext().getString(
                        com.android.internal.R.string.notification_importance_blocked);
            case NotificationListenerService.Ranking.IMPORTANCE_LOW:
                return getContext().getString(
                        com.android.internal.R.string.notification_importance_low);
            case NotificationListenerService.Ranking.IMPORTANCE_DEFAULT:
                return getContext().getString(
                        com.android.internal.R.string.notification_importance_default);
            case NotificationListenerService.Ranking.IMPORTANCE_HIGH:
                return getContext().getString(
                        com.android.internal.R.string.notification_importance_high);
            case NotificationListenerService.Ranking.IMPORTANCE_MAX:
                return getContext().getString(
                        com.android.internal.R.string.notification_importance_max);
            default:
                return "";
        }
    }

    public interface Callback {
        void onImportanceChanged(int progress);
    }
}
