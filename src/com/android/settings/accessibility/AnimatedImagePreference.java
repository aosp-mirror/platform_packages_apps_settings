/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * A custom {@link ImageView} preference for showing animated or static image, such as <a
 * href="https://developers.google.com/speed/webp/">animated webp</a> and static png.
 */
public class AnimatedImagePreference extends Preference {

    private boolean mDividerAllowedAbove = false;
    private Uri mImageUri;

    AnimatedImagePreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_animated_image);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(mDividerAllowedAbove);

        final ImageView imageView = holder.itemView.findViewById(R.id.animated_img);
        if (imageView != null && mImageUri != null) {
            imageView.setImageURI(mImageUri);

            final Drawable drawable = imageView.getDrawable();
            if (drawable != null) {
                if (drawable instanceof AnimatedImageDrawable) {
                    ((AnimatedImageDrawable) drawable).start();
                }
            }
        }
    }

    /**
     * Sets divider whether to show in preference above.
     *
     * @param allowed true will be drawn on above this item
     */
    public void setDividerAllowedAbove(boolean allowed) {
        if (allowed != mDividerAllowedAbove) {
            mDividerAllowedAbove = allowed;
            notifyChanged();
        }
    }

    /**
     * Set image uri to display image in {@link ImageView}
     *
     * @param imageUri the Uri of an image
     */
    public void setImageUri(Uri imageUri) {
        if (imageUri != null && !imageUri.equals(mImageUri)) {
            mImageUri = imageUri;
            notifyChanged();
        }
    }
}
