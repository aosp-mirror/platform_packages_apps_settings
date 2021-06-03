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
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

/**
 * A custom {@link ImageView} preference for showing animated or static image, such as <a
 * href="https://developers.google.com/speed/webp/">animated webp</a> and static png.
 */
public class AnimatedImagePreference extends Preference {

    private static final String TAG = "AnimatedImagePreference";
    private Uri mImageUri;
    private int mMaxHeight = -1;

    private final Animatable2.AnimationCallback mAnimationCallback =
            new Animatable2.AnimationCallback() {
        @Override
        public void onAnimationEnd(Drawable drawable) {
            ((Animatable2) drawable).start();
        }
    };

    AnimatedImagePreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_animated_image);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final ImageView imageView = holder.itemView.findViewById(R.id.animated_img);
        final LottieAnimationView lottieView = holder.itemView.findViewById(R.id.lottie_view);
        if (imageView == null || lottieView == null) {
            return;
        }

        if (mImageUri != null) {
            resetAnimations(imageView, lottieView);
            hideAllChildViews(holder.itemView);

            imageView.setImageURI(mImageUri);
            if (imageView.getDrawable() != null) {
                startAnimationWith(imageView);
            } else {
                // The lottie image from the raw folder also returns null.
                startLottieAnimationWith(lottieView);
            }
        }

        if (mMaxHeight > -1) {
            imageView.setMaxHeight(mMaxHeight);
            lottieView.setMaxHeight(mMaxHeight);
        }
    }

    /**
     * Sets image uri to display image in {@link ImageView}
     *
     * @param imageUri the Uri of an image
     */
    public void setImageUri(Uri imageUri) {
        if (imageUri != null && !imageUri.equals(mImageUri)) {
            mImageUri = imageUri;
            notifyChanged();
        }
    }

    /**
     * Sets the maximum height of the view.
     *
     * @param maxHeight the maximum height of ImageView in terms of pixels.
     */
    public void setMaxHeight(int maxHeight) {
        if (maxHeight != mMaxHeight) {
            mMaxHeight = maxHeight;
            notifyChanged();
        }
    }

    private void startAnimationWith(ImageView imageView) {
        startAnimation(imageView.getDrawable());

        imageView.setVisibility(View.VISIBLE);
    }

    private void startLottieAnimationWith(LottieAnimationView lottieView) {
        final InputStream inputStream = getInputStreamFromUri(mImageUri);
        Objects.requireNonNull(inputStream, "Invalid resource.");
        lottieView.setAnimation(inputStream, /* cacheKey= */ null);
        lottieView.setRepeatCount(LottieDrawable.INFINITE);
        lottieView.playAnimation();

        lottieView.setVisibility(View.VISIBLE);
    }

    private void startAnimation(Drawable drawable) {
        if (!(drawable instanceof Animatable)) {
            return;
        }

        if (drawable instanceof Animatable2) {
            ((Animatable2) drawable).registerAnimationCallback(mAnimationCallback);
        } else if (drawable instanceof AnimationDrawable) {
            ((AnimationDrawable) drawable).setOneShot(false);
        }

        ((Animatable) drawable).start();
    }

    private void resetAnimations(ImageView imageView, LottieAnimationView lottieView) {
        resetAnimation(imageView.getDrawable());

        lottieView.cancelAnimation();
    }

    private void resetAnimation(Drawable drawable) {
        if (!(drawable instanceof Animatable)) {
            return;
        }

        if (drawable instanceof Animatable2) {
            ((Animatable2) drawable).clearAnimationCallbacks();
        }

        ((Animatable) drawable).stop();
    }

    private InputStream getInputStreamFromUri(Uri uri) {
        try {
            return getContext().getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Cannot find content uri: " + uri, e);
            return null;
        }
    }

    private void hideAllChildViews(View itemView) {
        final ViewGroup viewGroup = (ViewGroup) itemView;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            viewGroup.getChildAt(i).setVisibility(View.GONE);
        }
    }
}
