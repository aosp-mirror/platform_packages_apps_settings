/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.wifi;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.DrawableRes;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * This preference provides one button layout with Settings style.
 * It looks like below
 *
 * --------------------------------------------------------------
 * | icon | title                                    |  button  |
 * --------------------------------------------------------------
 *
 * User can set icon / click listener for button.
 * By default, the button is invisible.
 */
public class ButtonPreference extends Preference {

    private static final String TAG = "ButtonPreference";

    private ImageButton mImageButton;
    private Drawable mButtonIcon;
    private View.OnClickListener mClickListener;
    private String mContentDescription;

    // Used for dummy pref.
    public ButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.wifi_button_preference_widget);
        mImageButton = null;
        mButtonIcon = null;
        mClickListener = null;
        mContentDescription = null;
    }

    public ButtonPreference(Context context) {
        this(context, /* attrs */ null);
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        initButton(view);
    }

    @Override
    public void setOrder(int order) {
        super.setOrder(order);
        setButtonVisibility();
    }

    @VisibleForTesting
    protected void initButton(final PreferenceViewHolder view) {
        if (mImageButton == null) {
            mImageButton = (ImageButton) view.findViewById(R.id.button_icon);
        }
        if (mImageButton != null) {
            mImageButton.setImageDrawable(mButtonIcon);
            mImageButton.setOnClickListener(mClickListener);
            mImageButton.setContentDescription(mContentDescription);
        }
        setButtonVisibility();
    }

    private void setButtonVisibility() {
        if(mImageButton != null) {
            mImageButton.setVisibility(mButtonIcon == null ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Sets the drawable to be displayed in button.
     */
    public void setButtonIcon(@DrawableRes int iconResId) {
        if (iconResId == 0) {
            return;
        }

        try {
            mButtonIcon = getContext().getDrawable(iconResId);
            notifyChanged();
        } catch (Resources.NotFoundException exception) {
            Log.e(TAG, "Resource does not exist: " + iconResId);
        }
    }

    /**
     * Register a callback to be invoked when button is clicked.
     */
    public void setButtonOnClickListener(View.OnClickListener listener) {
        if (listener != mClickListener) {
            mClickListener = listener;
            notifyChanged();
        }
    }

    /**
     * A content description briefly describes the button and is primarily used for accessibility
     * support to determine how a button should be presented to the user.
     */
    public void setButtonContentDescription(String contentDescription) {
        if (contentDescription != mContentDescription) {
            mContentDescription = contentDescription;
            notifyChanged();
        }
    }
}
