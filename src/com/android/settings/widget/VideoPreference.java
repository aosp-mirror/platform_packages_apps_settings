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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * A full width preference that hosts a MP4 video.
 */
public class VideoPreference extends Preference {

    private static final String TAG = "VideoPreference";
    private final Context mContext;

    private Uri mVideoPath;
    @VisibleForTesting
    MediaPlayer mMediaPlayer;
    @VisibleForTesting
    boolean mAnimationAvailable;
    @VisibleForTesting
    boolean mVideoReady;
    private boolean mVideoPaused;
    private float mAspectRatio = 1.0f;
    private int mPreviewResource;
    private boolean mViewVisible;
    private Surface mSurface;
    private int mAnimationId;
    private int mHeight = LinearLayout.LayoutParams.MATCH_PARENT - 1; // video height in pixels

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
            mVideoPath = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(context.getPackageName())
                    .appendPath(String.valueOf(mAnimationId))
                    .build();
            mPreviewResource = mPreviewResource == 0
                ? attributes.getResourceId(R.styleable.VideoPreference_preview, 0)
                : mPreviewResource;
            if (mPreviewResource == 0 && mAnimationId == 0) {
                setVisible(false);
                return;
            }
            initMediaPlayer();
            if (mMediaPlayer != null && mMediaPlayer.getDuration() > 0) {
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

        final TextureView video = (TextureView) holder.findViewById(R.id.video_texture_view);
        final ImageView imageView = (ImageView) holder.findViewById(R.id.video_preview_image);
        final ImageView playButton = (ImageView) holder.findViewById(R.id.video_play_button);
        final AspectRatioFrameLayout layout = (AspectRatioFrameLayout) holder.findViewById(
                R.id.video_container);

        imageView.setImageResource(mPreviewResource);
        layout.setAspectRatio(mAspectRatio);
        if (mHeight >= LinearLayout.LayoutParams.MATCH_PARENT) {
            layout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, mHeight));
        }
        updateViewStates(imageView, playButton);

        video.setOnClickListener(v -> updateViewStates(imageView, playButton));

        video.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width,
                    int height) {
                if (mMediaPlayer != null) {
                    mSurface = new Surface(surfaceTexture);
                    mMediaPlayer.setSurface(mSurface);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width,
                    int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                imageView.setVisibility(View.VISIBLE);
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                if (!mViewVisible) {
                    return;
                }
                if (mVideoReady) {
                    if (imageView.getVisibility() == View.VISIBLE) {
                        imageView.setVisibility(View.GONE);
                    }
                    if (!mVideoPaused && mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
                        mMediaPlayer.start();
                        playButton.setVisibility(View.GONE);
                    }
                }
                if (mMediaPlayer != null && !mMediaPlayer.isPlaying() &&
                        playButton.getVisibility() != View.VISIBLE) {
                    playButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @VisibleForTesting
    void updateViewStates(ImageView imageView, ImageView playButton) {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                playButton.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.VISIBLE);
                mVideoPaused = true;
            } else {
                imageView.setVisibility(View.GONE);
                playButton.setVisibility(View.GONE);
                mMediaPlayer.start();
                mVideoPaused = false;
            }
        }
    }

    @Override
    public void onDetached() {
        releaseMediaPlayer();
        super.onDetached();
    }

    public void onViewVisible(boolean videoPaused) {
        mViewVisible = true;
        mVideoPaused = videoPaused;
        initMediaPlayer();
    }

    public void onViewInvisible() {
        mViewVisible = false;
        releaseMediaPlayer();
    }

    /**
     * Sets the video for this preference. If a previous video was set this one will override it
     * and properly release any resources and re-initialize the preference to play the new video.
     *
     * @param videoId The raw res id of the video
     * @param previewId The drawable res id of the preview image to use if the video fails to load.
     */
    public void setVideo(int videoId, int previewId) {
        mAnimationId = videoId;
        mPreviewResource = previewId;
        releaseMediaPlayer();
        initialize(mContext, null);
    }

    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer.create(mContext, mVideoPath);
            // when the playback res is invalid or others, MediaPlayer create may fail
            // and return null, so need add the null judgement.
            if (mMediaPlayer != null) {
                mMediaPlayer.seekTo(0);
                mMediaPlayer.setOnSeekCompleteListener(mp -> mVideoReady = true);
                mMediaPlayer.setOnPreparedListener(mediaPlayer -> mediaPlayer.setLooping(true));
                if (mSurface != null) {
                    mMediaPlayer.setSurface(mSurface);
                }
            }
        }
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mVideoReady = false;
        }
    }

    public boolean isAnimationAvailable() {
        return mAnimationAvailable;
    }

    public boolean isVideoPaused() {
        return mVideoPaused;
    }

    /**
     * sets the height of the video preference
     * @param height in dp
     */
    public void setHeight(float height) {
        mHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height,
                mContext.getResources().getDisplayMetrics());
    }

    @VisibleForTesting
    void updateAspectRatio() {
        mAspectRatio = mMediaPlayer.getVideoWidth() / (float) mMediaPlayer.getVideoHeight();
    }
}
