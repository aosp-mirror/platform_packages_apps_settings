/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility.shortcuts;

import android.content.Context;
import android.content.res.Resources;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.widget.LottieColorUtils;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

/**
 * A preference represents an accessibility shortcut option with a checkbox and a tutorial image
 */
public class ShortcutOptionPreference extends CheckBoxPreference {

    private static final String TAG = "ShortcutOptionPreference";

    @DrawableRes
    private int mIntroImageResId = Resources.ID_NULL;
    @RawRes
    private int mIntroImageRawResId = Resources.ID_NULL;

    private int mSummaryTextLineHeight;

    public ShortcutOptionPreference(
            @NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public ShortcutOptionPreference(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ShortcutOptionPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ShortcutOptionPreference(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.accessibility_shortcut_option_checkable);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        if (mIntroImageResId == Resources.ID_NULL && mIntroImageRawResId == Resources.ID_NULL) {
            holder.findViewById(R.id.image).setVisibility(View.GONE);
        } else {
            holder.findViewById(R.id.image).setVisibility(View.VISIBLE);
            LottieAnimationView imageView = holder.itemView.findViewById(R.id.image);

            if (mIntroImageRawResId != Resources.ID_NULL) {
                imageView.setFailureListener(result ->
                        Log.w(TAG,
                                "Invalid image raw resource id: "
                                        + getContext().getResources()
                                                .getResourceEntryName(mIntroImageRawResId),
                                result));
                imageView.setAnimation(mIntroImageRawResId);
                imageView.setRepeatCount(LottieDrawable.INFINITE);
                LottieColorUtils.applyDynamicColors(getContext(), imageView);
                imageView.playAnimation();
            } else {
                imageView.setImageResource(mIntroImageResId);
            }
        }

        final TextView summaryView = (TextView) holder.findViewById(android.R.id.summary);
        if (summaryView != null) {
            mSummaryTextLineHeight = summaryView.getLineHeight();
            summaryView.setMovementMethod(LinkMovementMethod.getInstance());
        }

        syncSummaryView(holder);
    }


    /**
     * Sets the introduction image for this preference with a drawable resource ID.
     */
    public void setIntroImageResId(@DrawableRes int introImageResId) {
        if (introImageResId != mIntroImageResId) {
            mIntroImageResId = introImageResId;
            mIntroImageRawResId = Resources.ID_NULL;
            notifyChanged();
        }
    }

    /**
     * Sets the introduction image for this preference with a raw resource ID for an animated image.
     */
    public void setIntroImageRawResId(@RawRes int introImageRawResId) {
        if (introImageRawResId != mIntroImageRawResId) {
            mIntroImageRawResId = introImageRawResId;
            mIntroImageResId = Resources.ID_NULL;
            notifyChanged();
        }
    }

    public int getSummaryTextLineHeight() {
        return mSummaryTextLineHeight;
    }
}
