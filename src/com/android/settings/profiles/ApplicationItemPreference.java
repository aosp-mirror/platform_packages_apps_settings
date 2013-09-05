/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.profiles;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.android.settings.R;

public class ApplicationItemPreference extends Preference {

    private static String TAG = "ApplicationItemPreference";
    
    private Drawable mIcon;

    public ApplicationItemPreference(Context context) {
        this(context, null, 0);
    }

    public ApplicationItemPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.preference_icon);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.IconPreferenceScreen, defStyle, 0);
        mIcon = a.getDrawable(R.styleable.IconPreferenceScreen_icon);
    }

    public void setIcon(Drawable icon){
        mIcon = icon;
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);

        float valueDips = 36.0f;
        final float scale = getContext().getResources().getDisplayMetrics().density;
        int valuePixels = (int) (valueDips * scale + 0.5f);

        ImageView imageView = (ImageView) view.findViewById(R.id.icon);
        if (imageView != null && mIcon != null) {
            imageView.setAdjustViewBounds(true);
            imageView.setMaxHeight(valuePixels);
            imageView.setMaxWidth(valuePixels);
            imageView.setImageDrawable(mIcon);
        }
    }
}
