/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.android.settings.R;

/**
 * A full width preference that hosts a MP4 video or a {@link AnimatedVectorDrawableCompat}.
 */
public class VideoPreference extends Preference {

    private static final String TAG = "VideoPreference";
    private final Context mContext;

    @VisibleForTesting
    AnimationController mAnimationController;
    @VisibleForTesting
    boolean mAnimationAvailable;

    private float mAspectRatio = 1.0f;
    private int mPreviewId;
    private int mAnimationId;
    private int mVectorAnimationId;
    private int mHeight = LinearLayout.LayoutParams.MATCH_PARENT - 1; // video height in pixels
    private TextureView mVideo;
    private ImageView mPreviewImage;
    private ImageView mPlayButton;

    public VideoPreference(Context context) {
        super(context);
        mContext = context;
        initialize(context, null);
    }

    public VideoPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        TypedArray attributes = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.VideoPreference,
                0, 0);
        try {
            // if these are already set that means they were set dynamically and don't need
            // to be loaded from xml
            mAnimationAvailable = false;
            mAnimationId = mAnimationId == 0
                ? attributes.getResourceId(R.styleable.VideoPreference_animation, 0)
                : mAnimationId;
            mPreviewId = mPreviewId == 0
                    ? attributes.getResourceId(R.styleable.VideoPreference_preview, 0)
                    : mPreviewId;
            mVectorAnimationId = attributes.getResourceId(
                    R.styleable.VideoPreference_vectorAnimation, 0);
            if (mPreviewId == 0 && mAnimationId == 0 && mVectorAnimationId == 0) {
                setVisible(false);
                return;
            }
            initAnimationController();
            if (mAnimationController != null && mAnimationController.getDuration() > 0) {
                setVisible(true);
                setLayoutResource(R.layout.video_preference);
                mAnimationAvailable = true;
                updateAspectRatio();
            } else {
                setVisible(false);
            }
        } catch (Exception e) {
            Log.w(TAG, "Animation resource not found. Will not show animation.");
        } finally {
            attributes.recycle();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        if (!mAnimationAvailable) {
            return;
        }

        mVideo = (TextureView) holder.findViewById(R.id.video_texture_view);
        mPreviewImage = (ImageView) holder.findViewById(R.id.video_preview_image);
        mPlayButton = (ImageView) holder.findViewById(R.id.video_play_button);
        final AspectRatioFrameLayout layout = (AspectRatioFrameLayout) holder.findViewById(
                R.id.video_container);

        mPreviewImage.setImageResource(mPreviewId);
        layout.setAspectRatio(mAspectRatio);
        if (mHeight >= LinearLayout.LayoutParams.MATCH_PARENT) {
            layout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, mHeight));
        }
        if (mAnimationController != null) {
            mAnimationController.attachView(mVideo, mPreviewImage, mPlayButton);
        }
    }

    @Override
    public void onDetached() {
        releaseAnimationController();
        super.onDetached();
    }

    /**
     * Called from {@link VideoPreferenceController} when the view is onResume
     */
    public void onViewVisible() {
        initAnimationController();
    }

    /**
     * Called from {@link VideoPreferenceController} when the view is onPause
     */
    public void onViewInvisible() {
        releaseAnimationController();
    }

    /**
     * Sets the video for this preference. If a previous video was set this one will override it
     * and properly release any resources and re-initialize the preference to play the new video.
     *
     * @param videoId   The raw res id of the video
     * @param previewId The drawable res id of the preview image to use if the video fails to load.
     */
    public void setVideo(int videoId, int previewId) {
        mAnimationId = videoId;
        mPreviewId = previewId;
        releaseAnimationController();
        initialize(mContext, null);
    }

    private void initAnimationController() {
        if (mVectorAnimationId != 0) {
            mAnimationController = new VectorAnimationController(mContext, mVectorAnimationId);
            return;
        }
        if (mAnimationId != 0) {
            mAnimationController = new MediaAnimationController(mContext, mAnimationId);
            if (mVideo != null) {
                mAnimationController.attachView(mVideo, mPreviewImage, mPlayButton);
            }
        }
    }

    private void releaseAnimationController() {
        if (mAnimationController != null) {
            mAnimationController.release();
            mAnimationController = null;
        }
    }

    public boolean isAnimationAvailable() {
        return mAnimationAvailable;
    }

    /**
     * sets the height of the video preference
     *
     * @param height in dp
     */
    public void setHeight(float height) {
        mHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height,
                mContext.getResources().getDisplayMetrics());
    }

    @VisibleForTesting
    void updateAspectRatio() {
        mAspectRatio = mAnimationController.getVideoWidth()
                / (float) mAnimationController.getVideoHeight();
    }

    /**
     * Handle animation operations.
     */
    interface AnimationController {
        /**
         * Pauses the animation.
         */
        void pause();

        /**
         * Starts the animation.
         */
        void start();

        /**
         * Releases the animation object.
         */
        void release();

        /**
         * Attaches the animation to UI view.
         */
        void attachView(TextureView video, View preview, View playButton);

        /**
         * Returns the animation Width.
         */
        int getVideoWidth();

        /**
         * Returns the animation Height.
         */
        int getVideoHeight();

        /**
         * Returns the animation duration.
         */
        int getDuration();

        /**
         * Returns if the animation is playing.
         */
        boolean isPlaying();
    }
}
