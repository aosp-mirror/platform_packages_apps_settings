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
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.Utils;
import com.android.settingslib.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreferenceHelper;

/**
 * A tri-state preference allowing a user to specify what gets to bubble.
 */
public class BubblePreference extends Preference implements View.OnClickListener {
    RestrictedPreferenceHelper mHelper;

    private int mSelectedPreference;

    private Context mContext;

    private ButtonViewHolder mBubbleAllButton;
    private ButtonViewHolder mBubbleSelectedButton;
    private ButtonViewHolder mBubbleNoneButton;

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
        mContext = context;
        setLayoutResource(R.layout.bubble_preference);
    }

    public void setSelectedPreference(int preference) {
        mSelectedPreference = preference;
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
        mSelectedVisible = visible;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
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

        View bubbleAll = holder.findViewById(R.id.bubble_all);
        ImageView bubbleAllImage = (ImageView) holder.findViewById(R.id.bubble_all_icon);
        TextView bubbleAllText = (TextView) holder.findViewById(R.id.bubble_all_label);
        mBubbleAllButton = new ButtonViewHolder(bubbleAll, bubbleAllImage, bubbleAllText,
                BUBBLE_PREFERENCE_ALL);
        mBubbleAllButton.setSelected(mContext, mSelectedPreference == BUBBLE_PREFERENCE_ALL);
        bubbleAll.setTag(BUBBLE_PREFERENCE_ALL);
        bubbleAll.setOnClickListener(this);
        bubbleAll.setVisibility(disabledByAdmin ? View.GONE : View.VISIBLE);

        View bubbleSelected = holder.findViewById(R.id.bubble_selected);
        ImageView bubbleSelectedImage = (ImageView) holder.findViewById(R.id.bubble_selected_icon);
        TextView bubbleSelectedText = (TextView) holder.findViewById(R.id.bubble_selected_label);
        mBubbleSelectedButton = new ButtonViewHolder(bubbleSelected, bubbleSelectedImage,
                bubbleSelectedText, BUBBLE_PREFERENCE_SELECTED);
        mBubbleSelectedButton.setSelected(mContext,
                mSelectedPreference == BUBBLE_PREFERENCE_SELECTED);
        bubbleSelected.setTag(BUBBLE_PREFERENCE_SELECTED);
        bubbleSelected.setOnClickListener(this);
        bubbleSelected.setVisibility((!mSelectedVisible || disabledByAdmin)
                ? View.GONE : View.VISIBLE);

        View bubbleNone = holder.findViewById(R.id.bubble_none);
        ImageView bubbleNoneImage = (ImageView) holder.findViewById(R.id.bubble_none_icon);
        TextView bubbleNoneText = (TextView) holder.findViewById(R.id.bubble_none_label);
        mBubbleNoneButton = new ButtonViewHolder(bubbleNone, bubbleNoneImage, bubbleNoneText,
                BUBBLE_PREFERENCE_NONE);
        mBubbleNoneButton.setSelected(mContext, mSelectedPreference == BUBBLE_PREFERENCE_NONE);
        bubbleNone.setTag(BUBBLE_PREFERENCE_NONE);
        bubbleNone.setOnClickListener(this);
        bubbleNone.setVisibility(disabledByAdmin ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        final int selected = (int) v.getTag();
        callChangeListener(selected);

        mBubbleAllButton.setSelected(mContext, selected == BUBBLE_PREFERENCE_ALL);
        mBubbleSelectedButton.setSelected(mContext, selected == BUBBLE_PREFERENCE_SELECTED);
        mBubbleNoneButton.setSelected(mContext, selected == BUBBLE_PREFERENCE_NONE);
    }

    private class ButtonViewHolder {
        private View mView;
        private ImageView mImageView;
        private TextView mTextView;
        private int mId;

        ButtonViewHolder(View v, ImageView iv, TextView tv, int identifier) {
            mView = v;
            mImageView = iv;
            mTextView = tv;
            mId = identifier;
        }

        void setSelected(Context context, boolean selected) {
            mView.setBackground(mContext.getDrawable(selected
                ? R.drawable.button_border_selected
                : R.drawable.button_border_unselected));
            mView.setSelected(selected);

            ColorStateList stateList = selected
                    ? Utils.getColorAccent(context)
                    : Utils.getColorAttr(context, android.R.attr.textColorPrimary);
            mImageView.setImageTintList(stateList);
            mTextView.setTextColor(stateList);
        }
    }
}
