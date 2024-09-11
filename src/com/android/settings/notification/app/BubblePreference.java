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

import static android.app.NotificationManager.BUBBLE_PREFERENCE_ALL;
import static android.app.NotificationManager.BUBBLE_PREFERENCE_NONE;
import static android.app.NotificationManager.BUBBLE_PREFERENCE_SELECTED;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreferenceHelper;

/**
 * A tri-state preference allowing a user to specify what gets to bubble.
 */
public class BubblePreference extends Preference implements RadioGroup.OnCheckedChangeListener {
    RestrictedPreferenceHelper mHelper;

    private int mSelectedPreference;

    private boolean mSelectedVisible;

    public BubblePreference(Context context) {
        this(context, null);
    }

    public BubblePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubblePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BubblePreference(Context context, AttributeSet attrs,
                            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mHelper = new RestrictedPreferenceHelper(context, this, attrs);
        mHelper.useAdminDisabledSummary(true);
        setLayoutResource(R.layout.bubble_preference);
    }

    public void setSelectedPreference(int preference) {
        mSelectedPreference = preference;
        notifyChanged();
    }

    public int getSelectedPreference() {
        return mSelectedPreference;
    }

    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin admin) {
        if (mHelper.setDisabledByAdmin(admin)) {
            notifyChanged();
        }
    }

    public void setSelectedVisibility(boolean visible) {
        if (mSelectedVisible != visible) {
            mSelectedVisible = visible;
            notifyChanged();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final boolean disabledByAdmin = mHelper.isDisabledByAdmin();
        View summary = holder.findViewById(android.R.id.summary);
        if (disabledByAdmin) {
            mHelper.onBindViewHolder(holder);
            summary.setVisibility(View.VISIBLE);
        } else {
            summary.setVisibility(View.GONE);
        }
        holder.itemView.setClickable(false);

        RadioButton bubbleAllButton = (RadioButton) holder.findViewById(R.id.bubble_all);
        bubbleAllButton.setChecked(mSelectedPreference == BUBBLE_PREFERENCE_ALL);
        bubbleAllButton.setTag(BUBBLE_PREFERENCE_ALL);
        bubbleAllButton.setVisibility(disabledByAdmin ? View.GONE : View.VISIBLE);

        RadioButton bubbleSelectedButton = (RadioButton) holder.findViewById(R.id.bubble_selected);
        bubbleSelectedButton.setChecked(mSelectedPreference == BUBBLE_PREFERENCE_SELECTED);
        bubbleSelectedButton.setTag(BUBBLE_PREFERENCE_SELECTED);
        int selectedButtonVisibility =
                (!mSelectedVisible || disabledByAdmin) ? View.GONE : View.VISIBLE;
        bubbleSelectedButton.setVisibility(selectedButtonVisibility);

        RadioButton bubbleNoneButton = (RadioButton) holder.findViewById(R.id.bubble_none);
        bubbleNoneButton.setChecked(mSelectedPreference == BUBBLE_PREFERENCE_NONE);
        bubbleNoneButton.setTag(BUBBLE_PREFERENCE_NONE);
        bubbleNoneButton.setVisibility(disabledByAdmin ? View.GONE : View.VISIBLE);

        RadioGroup bublesRadioGroup = (RadioGroup) holder.findViewById(R.id.radio_group);
        bublesRadioGroup.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(@NonNull RadioGroup group, int checkedId) {
        View v = group.findViewById(checkedId);
        if (v == null || v.getTag() == null) {
            return;
        }
        int selectedTag = (int) v.getTag();
        callChangeListener(selectedTag);
    }
}
