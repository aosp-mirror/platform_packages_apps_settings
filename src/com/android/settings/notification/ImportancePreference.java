/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_MIN;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.TextView;

import com.android.settingslib.R;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class ImportancePreference extends Preference {

    private boolean mIsConfigurable = true;
    private int mImportance;
    private boolean mDisplayInStatusBar;
    private boolean mDisplayOnLockscreen;
    private Button mSilenceButton;
    private Button mAlertButton;
    private Context mContext;
    Drawable selectedBackground;
    Drawable unselectedBackground;

    public ImportancePreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public ImportancePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public ImportancePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ImportancePreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        selectedBackground = mContext.getDrawable(R.drawable.button_border_selected);
        unselectedBackground = mContext.getDrawable(R.drawable.button_border_unselected);
        setLayoutResource(R.layout.notif_importance_preference);
    }

    public void setImportance(int importance) {
        mImportance = importance;
    }

    public void setConfigurable(boolean configurable) {
        mIsConfigurable = configurable;
    }

    public void setDisplayInStatusBar(boolean display) {
        mDisplayInStatusBar = display;
    }

    public void setDisplayOnLockscreen(boolean display) {
        mDisplayOnLockscreen = display;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        TextView textView = (TextView) holder.findViewById(R.id.description);
        mSilenceButton = (Button) holder.findViewById(R.id.silence);
        mAlertButton = (Button) holder.findViewById(R.id.alert);

        if (!mIsConfigurable) {
            mSilenceButton.setEnabled(false);
            mAlertButton.setEnabled(false);
        }

        switch (mImportance) {
            case IMPORTANCE_MIN:
            case IMPORTANCE_LOW:
                mAlertButton.setBackground(unselectedBackground);
                mSilenceButton.setBackground(selectedBackground);
                break;
            case IMPORTANCE_HIGH:
            default:
                mSilenceButton.setBackground(unselectedBackground);
                mAlertButton.setBackground(selectedBackground);
                break;
        }
        setImportanceSummary(textView, mImportance);

        mSilenceButton.setOnClickListener(v -> {
            callChangeListener(IMPORTANCE_LOW);
            mAlertButton.setBackground(unselectedBackground);
            mSilenceButton.setBackground(selectedBackground);
            mSilenceButton.setTextAppearance(
                    R.style.TextAppearance_NotificationImportanceButton_Selected);
            mAlertButton.setTextAppearance(
                    R.style.TextAppearance_NotificationImportanceButton_Unselected);
            setImportanceSummary(textView, IMPORTANCE_LOW);
        });
        mAlertButton.setOnClickListener(v -> {
            callChangeListener(IMPORTANCE_DEFAULT);
            mSilenceButton.setBackground(unselectedBackground);
            mAlertButton.setBackground(selectedBackground);
            mAlertButton.setTextAppearance(
                    R.style.TextAppearance_NotificationImportanceButton_Selected);
            mSilenceButton.setTextAppearance(
                    R.style.TextAppearance_NotificationImportanceButton_Unselected);
            setImportanceSummary(textView, IMPORTANCE_DEFAULT);
        });
    }

    void setImportanceSummary(TextView view, int importance) {
        if (importance >= IMPORTANCE_DEFAULT) {
            view.setText(R.string.notification_channel_summary_default);
        } else {
            if (mDisplayInStatusBar) {
                 if (mDisplayOnLockscreen) {
                     view.setText(R.string.notification_channel_summary_low_status_lock);
                 } else {
                     view.setText(R.string.notification_channel_summary_low_status);
                 }
            } else if (mDisplayOnLockscreen) {
                view.setText(R.string.notification_channel_summary_low_lock);
            } else {
                view.setText(R.string.notification_channel_summary_low);
            }
        }
    }
}
