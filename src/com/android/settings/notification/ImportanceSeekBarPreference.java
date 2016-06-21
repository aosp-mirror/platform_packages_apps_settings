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
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.settings.Utils;

/**
 * A slider preference that controls notification importance.
 **/
public class ImportanceSeekBarPreference extends SeekBarPreference implements
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "ImportanceSeekBarPref";

    private Callback mCallback;
    private int mMinProgress;
    private TextView mSummaryTextView;
    private String mSummary;
    private SeekBar mSeekBar;
    private ColorStateList mActiveSliderTint;
    private ColorStateList mInactiveSliderTint;
    private float mActiveSliderAlpha = 1.0f;
    private float mInactiveSliderAlpha;
    private boolean mAutoOn;
    private Handler mHandler;

    public ImportanceSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_importance_slider);
        mActiveSliderTint = ColorStateList.valueOf(Utils.getColorAccent(context));
        mInactiveSliderTint = ColorStateList.valueOf(
                context.getColor(R.color.importance_disabled_slider_color));
        mHandler = new Handler();
        final TypedArray ta =
                context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.Theme, 0, 0);
        mInactiveSliderAlpha =
                ta.getFloat(com.android.internal.R.styleable.Theme_disabledAlpha, 0.5f);
        ta.recycle();
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

    @Override
    public void setProgress(int progress) {
        mSummary = getProgressSummary(progress);
        super.setProgress(progress);
    }

    public void setAutoOn(boolean autoOn) {
        mAutoOn = autoOn;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mSummaryTextView = (TextView) view.findViewById(com.android.internal.R.id.summary);
        mSeekBar = (SeekBar) view.findViewById(
                com.android.internal.R.id.seekbar);

        final ImageView autoButton = (ImageView) view.findViewById(R.id.auto_importance);
        applyAutoUi(autoButton);
        autoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAuto(autoButton);
            }
        });
    }

    private void applyAuto(ImageView autoButton) {
        mAutoOn = !mAutoOn;
        if (!mAutoOn) {
            setProgress(NotificationListenerService.Ranking.IMPORTANCE_DEFAULT);
            mCallback.onImportanceChanged(
                    NotificationListenerService.Ranking.IMPORTANCE_DEFAULT, true);
        } else {
            mCallback.onImportanceChanged(
                    NotificationListenerService.Ranking.IMPORTANCE_UNSPECIFIED, true);
        }
        applyAutoUi(autoButton);
    }

    private void applyAutoUi(ImageView autoButton) {
        mSeekBar.setEnabled(!mAutoOn);

        final float alpha = mAutoOn ? mInactiveSliderAlpha : mActiveSliderAlpha;
        final ColorStateList starTint = mAutoOn ?  mActiveSliderTint : mInactiveSliderTint;
        Drawable icon = autoButton.getDrawable().mutate();
        icon.setTintList(starTint);
        autoButton.setImageDrawable(icon);
        mSeekBar.setAlpha(alpha);

        if (mAutoOn) {
            setProgress(NotificationListenerService.Ranking.IMPORTANCE_DEFAULT);
            mSummary = getProgressSummary(
                    NotificationListenerService.Ranking.IMPORTANCE_UNSPECIFIED);
        }
        mSummaryTextView.setText(mSummary);
    }

    @Override
    public CharSequence getSummary() {
        return mSummary;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        super.onProgressChanged(seekBar, progress, fromTouch);
        if (progress < mMinProgress) {
            seekBar.setProgress(mMinProgress);
            progress = mMinProgress;
        }
        if (mSummaryTextView != null) {
            mSummary = getProgressSummary(progress);
            mSummaryTextView.setText(mSummary);
        }
        mCallback.onImportanceChanged(progress, fromTouch);
    }

    private String getProgressSummary(int progress) {
        switch (progress) {
            case NotificationListenerService.Ranking.IMPORTANCE_NONE:
                return getContext().getString(R.string.notification_importance_blocked);
            case NotificationListenerService.Ranking.IMPORTANCE_MIN:
                return getContext().getString(R.string.notification_importance_min);
            case NotificationListenerService.Ranking.IMPORTANCE_LOW:
                return getContext().getString(R.string.notification_importance_low);
            case NotificationListenerService.Ranking.IMPORTANCE_DEFAULT:
                return getContext().getString(R.string.notification_importance_default);
            case NotificationListenerService.Ranking.IMPORTANCE_HIGH:
                return getContext().getString(R.string.notification_importance_high);
            case NotificationListenerService.Ranking.IMPORTANCE_MAX:
                return getContext().getString(R.string.notification_importance_max);
            default:
                return getContext().getString(R.string.notification_importance_unspecified);
        }
    }

    @Override
    protected void notifyChanged() {
        mHandler.post(mNotifyChanged);
    }

    private void postNotifyChanged() {
        super.notifyChanged();
    }

    private final Runnable mNotifyChanged = new Runnable() {
        @Override
        public void run() {
            postNotifyChanged();
        }
    };

    public interface Callback {
        void onImportanceChanged(int progress, boolean fromTouch);
    }
}
