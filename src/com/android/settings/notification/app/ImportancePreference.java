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

package com.android.settings.notification.app;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_MIN;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.Utils;
import com.android.settingslib.R;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class ImportancePreference extends Preference {

    private boolean mIsConfigurable = true;
    private int mImportance;
    private boolean mDisplayInStatusBar;
    private boolean mDisplayOnLockscreen;
    private View mSilenceButton;
    private View mAlertButton;
    private Context mContext;
    Drawable selectedBackground;
    Drawable unselectedBackground;
    private static final int BUTTON_ANIM_TIME_MS = 100;
    private static final boolean SHOW_BUTTON_SUMMARY = false;

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
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setClickable(false);

        mSilenceButton = holder.findViewById(R.id.silence);
        mAlertButton = holder.findViewById(R.id.alert);

        if (!mIsConfigurable) {
            mSilenceButton.setEnabled(false);
            mAlertButton.setEnabled(false);
        }

        setImportanceSummary((ViewGroup) holder.itemView, mImportance, false);
        switch (mImportance) {
            case IMPORTANCE_MIN:
            case IMPORTANCE_LOW:
                mAlertButton.setBackground(unselectedBackground);
                mSilenceButton.setBackground(selectedBackground);
                mSilenceButton.setSelected(true);
                break;
            case IMPORTANCE_HIGH:
            default:
                mSilenceButton.setBackground(unselectedBackground);
                mAlertButton.setBackground(selectedBackground);
                mAlertButton.setSelected(true);
                break;
        }

        mSilenceButton.setOnClickListener(v -> {
            callChangeListener(IMPORTANCE_LOW);
            mAlertButton.setBackground(unselectedBackground);
            mSilenceButton.setBackground(selectedBackground);
            setImportanceSummary((ViewGroup) holder.itemView, IMPORTANCE_LOW, true);
            // a11y service won't always read the newly appearing text in the right order if the
            // selection happens too soon (readback happens on a different thread as layout). post
            // the selection to make that conflict less likely
            holder.itemView.post(() -> {
                mAlertButton.setSelected(false);
                mSilenceButton.setSelected(true);
            });
        });
        mAlertButton.setOnClickListener(v -> {
            callChangeListener(IMPORTANCE_DEFAULT);
            mSilenceButton.setBackground(unselectedBackground);
            mAlertButton.setBackground(selectedBackground);
            setImportanceSummary((ViewGroup) holder.itemView, IMPORTANCE_DEFAULT, true);
            holder.itemView.post(() -> {
                mSilenceButton.setSelected(false);
                mAlertButton.setSelected(true);
            });
        });
    }

    private ColorStateList getAccentTint() {
        return Utils.getColorAccent(getContext());
    }

    private ColorStateList getRegularTint() {
        return Utils.getColorAttr(getContext(), android.R.attr.textColorPrimary);
    }

    void setImportanceSummary(ViewGroup parent, int importance, boolean fromUser) {
        if (fromUser) {
            AutoTransition transition = new AutoTransition();
            transition.setDuration(BUTTON_ANIM_TIME_MS);
            TransitionManager.beginDelayedTransition(parent, transition);
        }

        ColorStateList colorAccent = getAccentTint();
        ColorStateList colorNormal = getRegularTint();

        if (importance >= IMPORTANCE_DEFAULT) {
            parent.findViewById(R.id.silence_summary).setVisibility(GONE);
            ((ImageView) parent.findViewById(R.id.silence_icon)).setImageTintList(colorNormal);
            ((TextView) parent.findViewById(R.id.silence_label)).setTextColor(colorNormal);

            ((ImageView) parent.findViewById(R.id.alert_icon)).setImageTintList(colorAccent);
            ((TextView) parent.findViewById(R.id.alert_label)).setTextColor(colorAccent);

            parent.findViewById(R.id.alert_summary).setVisibility(VISIBLE);
        } else {
            parent.findViewById(R.id.alert_summary).setVisibility(GONE);
            ((ImageView) parent.findViewById(R.id.alert_icon)).setImageTintList(colorNormal);
            ((TextView) parent.findViewById(R.id.alert_label)).setTextColor(colorNormal);

            ((ImageView) parent.findViewById(R.id.silence_icon)).setImageTintList(colorAccent);
            ((TextView) parent.findViewById(R.id.silence_label)).setTextColor(colorAccent);
            parent.findViewById(R.id.silence_summary).setVisibility(VISIBLE);
        }
    }
}
