/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.homepage;

import android.content.Context;
import android.util.AttributeSet;

import androidx.core.content.res.TypedArrayUtils;

import com.android.settings.R;
import com.android.settingslib.RestrictedTopLevelPreference;

/** Homepage preference that can be disabled by a device admin using a user restriction. */
public class RestrictedHomepagePreference extends RestrictedTopLevelPreference {

    public RestrictedHomepagePreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.homepage_preference);
    }

    public RestrictedHomepagePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, /* defStyleRes= */ 0);
    }

    public RestrictedHomepagePreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.preferenceStyle,
                android.R.attr.preferenceStyle));
    }

    public RestrictedHomepagePreference(Context context) {
        this(context, /* attrs= */ null);
    }
}
