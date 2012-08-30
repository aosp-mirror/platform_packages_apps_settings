/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.preference.Preference;

import com.android.settings.R;

public class StorageItemPreference extends Preference {

    private int mColor = Color.MAGENTA;

    public StorageItemPreference(Context context, String key, int titleRes, int colorRes) {
        this(context, key, context.getText(titleRes), colorRes);
    }

    public StorageItemPreference(Context context, String key, CharSequence title, int colorRes) {
        super(context);
        //setLayoutResource(R.layout.app_percentage_item);

        if (colorRes != 0) {
            mColor = context.getResources().getColor(colorRes);

            final Resources res = context.getResources();
            final int width = res.getDimensionPixelSize(R.dimen.device_memory_usage_button_width);
            final int height = res.getDimensionPixelSize(R.dimen.device_memory_usage_button_height);
            setIcon(createRectShape(width, height, mColor));
        }

        setKey(key);
        setTitle(title);
        setSummary(R.string.memory_calculating_size);
    }

    private static ShapeDrawable createRectShape(int width, int height, int color) {
        ShapeDrawable shape = new ShapeDrawable(new RectShape());
        shape.setIntrinsicHeight(height);
        shape.setIntrinsicWidth(width);
        shape.getPaint().setColor(color);
        return shape;
    }

    public int getColor() {
        return mColor;
    }
}
