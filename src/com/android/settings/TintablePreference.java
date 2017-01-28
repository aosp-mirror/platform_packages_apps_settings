/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.support.annotation.ColorInt;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.ImageView;

public class TintablePreference extends Preference {
    @ColorInt
    private int mTintColor;

    public TintablePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TintablePreference);
        mTintColor = a.getColor(R.styleable.TintablePreference_android_tint, 0);
        a.recycle();
    }

    public void setTint(int color) {
        mTintColor = color;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        if (mTintColor != 0) {
            ((ImageView) view.findViewById(android.R.id.icon)).setImageTintList(
                    ColorStateList.valueOf(mTintColor));
        } else {
            ((ImageView) view.findViewById(android.R.id.icon)).setImageTintList(null);
        }
    }
}
