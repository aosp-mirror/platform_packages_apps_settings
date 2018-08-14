/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.widget;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.android.settings.R;

public class ActionButtonPreference extends Preference {

    private final ButtonInfo mButton1Info = new ButtonInfo();
    private final ButtonInfo mButton2Info = new ButtonInfo();

    public ActionButtonPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public ActionButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ActionButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ActionButtonPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.two_action_buttons);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
        holder.setDividerAllowedBelow(false);
        mButton1Info.mPositiveButton = (Button) holder.findViewById(R.id.button1_positive);
        mButton1Info.mNegativeButton = (Button) holder.findViewById(R.id.button1_negative);
        mButton2Info.mPositiveButton = (Button) holder.findViewById(R.id.button2_positive);
        mButton2Info.mNegativeButton = (Button) holder.findViewById(R.id.button2_negative);

        mButton1Info.setUpButton();
        mButton2Info.setUpButton();
    }

    public ActionButtonPreference setButton1Text(@StringRes int textResId) {
        final String newText = getContext().getString(textResId);
        if (!TextUtils.equals(newText, mButton1Info.mText)) {
            mButton1Info.mText = newText;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton1Enabled(boolean isEnabled) {
        if (isEnabled != mButton1Info.mIsEnabled) {
            mButton1Info.mIsEnabled = isEnabled;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton2Text(@StringRes int textResId) {
        final String newText = getContext().getString(textResId);
        if (!TextUtils.equals(newText, mButton2Info.mText)) {
            mButton2Info.mText = newText;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton2Enabled(boolean isEnabled) {
        if (isEnabled != mButton2Info.mIsEnabled) {
            mButton2Info.mIsEnabled = isEnabled;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton1OnClickListener(View.OnClickListener listener) {
        if (listener != mButton1Info.mListener) {
            mButton1Info.mListener = listener;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton2OnClickListener(View.OnClickListener listener) {
        if (listener != mButton2Info.mListener) {
            mButton2Info.mListener = listener;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton1Positive(boolean isPositive) {
        if (isPositive != mButton1Info.mIsPositive) {
            mButton1Info.mIsPositive = isPositive;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton2Positive(boolean isPositive) {
        if (isPositive != mButton2Info.mIsPositive) {
            mButton2Info.mIsPositive = isPositive;
            notifyChanged();
        }
        return this;
    }
    public ActionButtonPreference setButton1Visible(boolean isPositive) {
        if (isPositive != mButton1Info.mIsVisible) {
            mButton1Info.mIsVisible = isPositive;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton2Visible(boolean isPositive) {
        if (isPositive != mButton2Info.mIsVisible) {
            mButton2Info.mIsVisible = isPositive;
            notifyChanged();
        }
        return this;
    }

    static class ButtonInfo {
        private Button mPositiveButton;
        private Button mNegativeButton;
        private CharSequence mText;
        private View.OnClickListener mListener;
        private boolean mIsPositive = true;
        private boolean mIsEnabled = true;
        private boolean mIsVisible = true;

        void setUpButton() {
            setUpButton(mPositiveButton);
            setUpButton(mNegativeButton);
            if (!mIsVisible) {
                mPositiveButton.setVisibility(View.INVISIBLE);
                mNegativeButton.setVisibility(View.INVISIBLE);
            } else if (mIsPositive) {
                mPositiveButton.setVisibility(View.VISIBLE);
                mNegativeButton.setVisibility(View.INVISIBLE);
            } else {
                mPositiveButton.setVisibility(View.INVISIBLE);
                mNegativeButton.setVisibility(View.VISIBLE);
            }
        }

        private void setUpButton(Button button) {
            button.setText(mText);
            button.setOnClickListener(mListener);
            button.setEnabled(mIsEnabled);
        }
    }
}