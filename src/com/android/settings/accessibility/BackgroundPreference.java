/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * A preference with custom background.
 */
public class BackgroundPreference extends Preference {

    private int mBackgroundId;

    public BackgroundPreference(@NonNull Context context,
            @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_background);
        setIconSpaceReserved(false);

        final TypedArray styledAttrs = context.obtainStyledAttributes(attrs,
                R.styleable.BackgroundPreference);
        mBackgroundId = styledAttrs.getResourceId(R.styleable.BackgroundPreference_background, 0);
        styledAttrs.recycle();
    }

    public BackgroundPreference(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BackgroundPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context,
                androidx.preference.R.attr.preferenceStyle,
                com.android.internal.R.attr.preferenceStyle), 0);
    }

    public BackgroundPreference(@NonNull Context context) {
        this(context, null, TypedArrayUtils.getAttr(context,
                androidx.preference.R.attr.preferenceStyle,
                com.android.internal.R.attr.preferenceStyle), 0);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final LinearLayout layout = (LinearLayout) holder.findViewById(R.id.background);
        if (mBackgroundId != 0) {
            final Drawable backgroundDrawable = getContext().getDrawable(mBackgroundId);
            layout.setBackground(backgroundDrawable);
        }
    }

    /**
     * Sets the background to a given resource. The resource should refer to a Drawable object.
     *
     * @param resId The identifier of the resource.
     */
    public void setBackground(@DrawableRes int resId) {
        if (mBackgroundId != resId) {
            mBackgroundId = resId;
            notifyChanged();
        }
    }
}
