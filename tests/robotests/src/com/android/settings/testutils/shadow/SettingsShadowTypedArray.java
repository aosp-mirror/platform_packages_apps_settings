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
 * limitations under the License.
 */

package com.android.settings.testutils.shadow;

import static org.robolectric.shadow.api.Shadow.directlyOn;

import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;

import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadows.ShadowTypedArray;

@Implements(value = TypedArray.class)
public class SettingsShadowTypedArray extends ShadowTypedArray {

    @RealObject
    TypedArray realTypedArray;

    @Implementation
    @Nullable
    public ColorStateList getColorStateList(@StyleableRes int index) {
        if (index == com.android.internal.R.styleable.TextView_textColorLink) {
            return ColorStateList.valueOf(Color.WHITE);
        }
        return directlyOn(realTypedArray, TypedArray.class).getColorStateList(index);
    }
}
