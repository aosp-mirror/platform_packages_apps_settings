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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

public class ActionButtonPreference extends Preference {

    private final String TAG = "ActionButtonPreference";
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
        holder.setDividerAllowedAbove(true);
        holder.setDividerAllowedBelow(true);

        mButton1Info.mButton = (Button) holder.findViewById(R.id.button1);
        mButton2Info.mButton = (Button) holder.findViewById(R.id.button2);

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

    public ActionButtonPreference setButton1Icon(@DrawableRes int iconResId) {
        if (iconResId == 0) {
            return this;
        }

        final Drawable icon;
        try {
            icon = getContext().getDrawable(iconResId);
            mButton1Info.mIcon = icon;
            notifyChanged();
        } catch (Resources.NotFoundException exception) {
            Log.e(TAG, "Resource does not exist: " + iconResId);
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

    public ActionButtonPreference setButton2Icon(@DrawableRes int iconResId) {
        if (iconResId == 0) {
            return this;
        }

        final Drawable icon;
        try {
            icon = getContext().getDrawable(iconResId);
            mButton2Info.mIcon = icon;
            notifyChanged();
        } catch (Resources.NotFoundException exception) {
            Log.e(TAG, "Resource does not exist: " + iconResId);
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

    public ActionButtonPreference setButton1Visible(boolean isVisible) {
        if (isVisible != mButton1Info.mIsVisible) {
            mButton1Info.mIsVisible = isVisible;
            notifyChanged();
        }
        return this;
    }

    public ActionButtonPreference setButton2Visible(boolean isVisible) {
        if (isVisible != mButton2Info.mIsVisible) {
            mButton2Info.mIsVisible = isVisible;
            notifyChanged();
        }
        return this;
    }

    static class ButtonInfo {
        private Button mButton;
        private CharSequence mText;
        private Drawable mIcon;
        private View.OnClickListener mListener;
        private boolean mIsEnabled = true;
        private boolean mIsVisible = true;

        void setUpButton() {
            mButton.setText(mText);
            mButton.setOnClickListener(mListener);
            mButton.setEnabled(mIsEnabled);
            mButton.setCompoundDrawablesWithIntrinsicBounds(
                    null /* left */, mIcon /* top */, null /* right */, null /* bottom */);
            if (mIsVisible) {
                mButton.setVisibility(View.VISIBLE);
            } else {
                mButton.setVisibility(View.GONE);
            }
        }
    }
}