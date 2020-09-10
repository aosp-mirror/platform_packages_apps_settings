/*
 * Copyright (C) 2020 The Android Open Source Project
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
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_UNSPECIFIED;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.Utils;
import com.android.settingslib.R;

public class ConversationPriorityPreference extends Preference {

    private boolean mIsConfigurable = true;
    private int mImportance;
    private int mOriginalImportance;
    private boolean mPriorityConversation;
    private View mSilenceButton;
    private View mAlertButton;
    private View mPriorityButton;
    private Context mContext;
    Drawable selectedBackground;
    Drawable unselectedBackground;
    private static final int BUTTON_ANIM_TIME_MS = 100;

    public ConversationPriorityPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public ConversationPriorityPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public ConversationPriorityPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ConversationPriorityPreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        selectedBackground = mContext.getDrawable(R.drawable.button_border_selected);
        unselectedBackground = mContext.getDrawable(R.drawable.button_border_unselected);
        setLayoutResource(R.layout.notif_priority_conversation_preference);
    }

    public void setImportance(int importance) {
        mImportance = importance;
    }

    public void setConfigurable(boolean configurable) {
        mIsConfigurable = configurable;
    }

    public void setPriorityConversation(boolean priorityConversation) {
        mPriorityConversation = priorityConversation;
    }

    public void setOriginalImportance(int importance) {
        mOriginalImportance = importance;
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setClickable(false);

        mSilenceButton = holder.findViewById(R.id.silence);
        mAlertButton = holder.findViewById(R.id.alert);
        mPriorityButton = holder.findViewById(R.id.priority_group);

        if (!mIsConfigurable) {
            mSilenceButton.setEnabled(false);
            mAlertButton.setEnabled(false);
            mPriorityButton.setEnabled(false);
        }

        updateToggles((ViewGroup) holder.itemView, mImportance, mPriorityConversation,
                false);

        mSilenceButton.setOnClickListener(v -> {
            callChangeListener(new Pair(IMPORTANCE_LOW, false));
            updateToggles((ViewGroup) holder.itemView, IMPORTANCE_LOW, false, true);
        });
        mAlertButton.setOnClickListener(v -> {
            int newImportance = Math.max(mOriginalImportance, IMPORTANCE_DEFAULT);
            callChangeListener(new Pair(newImportance, false));
            updateToggles((ViewGroup) holder.itemView, newImportance, false, true);
        });
        mPriorityButton.setOnClickListener(v -> {
            int newImportance = Math.max(mOriginalImportance, IMPORTANCE_DEFAULT);
            callChangeListener(new Pair(newImportance, true));
            updateToggles((ViewGroup) holder.itemView, newImportance, true, true);
        });
    }

    private ColorStateList getAccentTint() {
        return Utils.getColorAccent(getContext());
    }

    private ColorStateList getRegularTint() {
        return Utils.getColorAttr(getContext(), android.R.attr.textColorPrimary);
    }

    void updateToggles(ViewGroup parent, int importance, boolean isPriority,
            boolean fromUser) {
        if (fromUser) {
            AutoTransition transition = new AutoTransition();
            transition.setDuration(BUTTON_ANIM_TIME_MS);
            TransitionManager.beginDelayedTransition(parent, transition);
        }

        ColorStateList colorAccent = getAccentTint();
        ColorStateList colorNormal = getRegularTint();
        ImageView silenceIcon = parent.findViewById(R.id.silence_icon);
        TextView silenceLabel = parent.findViewById(R.id.silence_label);
        TextView silenceSummary = parent.findViewById(R.id.silence_summary);
        ImageView alertIcon = parent.findViewById(R.id.alert_icon);
        TextView alertLabel = parent.findViewById(R.id.alert_label);
        TextView alertSummary = parent.findViewById(R.id.alert_summary);
        ImageView priorityIcon = parent.findViewById(R.id.priority_icon);
        TextView priorityLabel = parent.findViewById(R.id.priority_label);
        TextView prioritySummary = parent.findViewById(R.id.priority_summary);

        if (importance <= IMPORTANCE_LOW && importance > IMPORTANCE_UNSPECIFIED) {
            alertSummary.setVisibility(GONE);
            alertIcon.setImageTintList(colorNormal);
            alertLabel.setTextColor(colorNormal);

            prioritySummary.setVisibility(GONE);
            priorityIcon.setImageTintList(colorNormal);
            priorityLabel.setTextColor(colorNormal);

            silenceIcon.setImageTintList(colorAccent);
            silenceLabel.setTextColor(colorAccent);
            silenceSummary.setVisibility(VISIBLE);

            mAlertButton.setBackground(unselectedBackground);
            mPriorityButton.setBackground(unselectedBackground);
            mSilenceButton.setBackground(selectedBackground);
            // a11y service won't always read the newly appearing text in the right order if the
            // selection happens too soon (readback happens on a different thread as layout). post
            // the selection to make that conflict less likely
            parent.post(() -> {
                mSilenceButton.setSelected(true);
                mAlertButton.setSelected(false);
                mPriorityButton.setSelected(false);
            });
        } else {
            if (isPriority) {
                alertSummary.setVisibility(GONE);
                alertIcon.setImageTintList(colorNormal);
                alertLabel.setTextColor(colorNormal);

                prioritySummary.setVisibility(VISIBLE);
                priorityIcon.setImageTintList(colorAccent);
                priorityLabel.setTextColor(colorAccent);

                silenceIcon.setImageTintList(colorNormal);
                silenceLabel.setTextColor(colorNormal);
                silenceSummary.setVisibility(GONE);

                mAlertButton.setBackground(unselectedBackground);
                mPriorityButton.setBackground(selectedBackground);
                mSilenceButton.setBackground(unselectedBackground);
                parent.post(() -> {
                    mSilenceButton.setSelected(false);
                    mAlertButton.setSelected(false);
                    mPriorityButton.setSelected(true);
                });
            } else {
                alertSummary.setVisibility(VISIBLE);
                alertIcon.setImageTintList(colorAccent);
                alertLabel.setTextColor(colorAccent);

                prioritySummary.setVisibility(GONE);
                priorityIcon.setImageTintList(colorNormal);
                priorityLabel.setTextColor(colorNormal);

                silenceIcon.setImageTintList(colorNormal);
                silenceLabel.setTextColor(colorNormal);
                silenceSummary.setVisibility(GONE);

                mAlertButton.setBackground(selectedBackground);
                mPriorityButton.setBackground(unselectedBackground);
                mSilenceButton.setBackground(unselectedBackground);
                parent.post(() -> {
                    mSilenceButton.setSelected(false);
                    mAlertButton.setSelected(true);
                    mPriorityButton.setSelected(false);
                });
            }
        }
    }
}
