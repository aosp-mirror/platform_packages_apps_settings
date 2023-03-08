/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics2.ui.view;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.setupdesign.GlifLayout;

/**
 * Utils class for GlifLayout
 */
public class GlifLayoutHelper {

    @NonNull private final Activity mActivity;
    @NonNull private final GlifLayout mGlifLayout;

    public GlifLayoutHelper(@NonNull Activity activity, @NonNull GlifLayout glifLayout) {
        mActivity = activity;
        mGlifLayout = glifLayout;
    }

    /**
     * Sets header text to GlifLayout
     */
    public void setHeaderText(@StringRes int textResId) {
        TextView layoutTitle = mGlifLayout.getHeaderTextView();
        CharSequence previousTitle = layoutTitle.getText();
        CharSequence title = mActivity.getText(textResId);
        if (previousTitle != title) {
            if (!TextUtils.isEmpty(previousTitle)) {
                layoutTitle.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            }
            mGlifLayout.setHeaderText(title);
            mGlifLayout.getHeaderTextView().setContentDescription(title);
            mActivity.setTitle(title);
        }
        mGlifLayout.getHeaderTextView().setContentDescription(title);
    }

    /**
     * Sets description text to GlifLayout
     */
    public void setDescriptionText(CharSequence description) {
        CharSequence previousDescription = mGlifLayout.getDescriptionText();
        // Prevent a11y for re-reading the same string
        if (!TextUtils.equals(previousDescription, description)) {
            mGlifLayout.setDescriptionText(description);
        }
    }

    @NonNull
    public Activity getActivity() {
        return mActivity;
    }

    @NonNull
    public GlifLayout getGlifLayout() {
        return mGlifLayout;
    }
}
