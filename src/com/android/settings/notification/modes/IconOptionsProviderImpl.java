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

package com.android.settings.notification.modes;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.android.settings.R;

import com.google.common.collect.ImmutableList;

class IconOptionsProviderImpl implements IconOptionsProvider {

    private static final String TAG = "IconOptionsProviderImpl";

    private final Context mContext;

    IconOptionsProviderImpl(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    @NonNull
    public ImmutableList<IconInfo> getIcons() {
        ImmutableList.Builder<IconInfo> list = ImmutableList.builder();
        try (TypedArray icons = mContext.getResources().obtainTypedArray(
                R.array.zen_mode_icon_options)) {
            String[] descriptions = mContext.getResources().getStringArray(
                    R.array.zen_mode_icon_options_descriptions);
            if (icons.length() != descriptions.length) {
                Log.wtf(TAG, "Size mismatch between zen_mode_icon_options (" + icons.length()
                        + ") and zen_mode_icon_options_descriptions (" + descriptions.length + ")");
            }

            for (int i = 0; i < Math.min(icons.length(), descriptions.length); i++) {
                @DrawableRes int resId = icons.getResourceId(i, 0);
                if (resId != 0) {
                    list.add(new IconInfo(resId, descriptions[i]));
                }
            }
        }
        return list.build();
    }
}
