/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.gestures;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;
import android.util.AttributeSet;
import android.util.Log;

import com.android.settings.R;

/**
 * Preference item for a gesture with a switch to signify if it should be enabled.
 * This shows the title and description of the gesture along with an animation showing how to do
 * the gesture
 */
public final class GesturePreference extends SwitchPreference {
    private static final String TAG = "GesturePreference";
    private final Context mContext;

    private Uri mVideoPath;
    private MediaPlayer mMediaPlayer;
    private boolean mAnimationAvailable;
    private boolean mVideoReady;
    private boolean mScrolling;
    private int mPreviewResource;

    public GesturePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        TypedArray attributes = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.GesturePreference,
                0, 0);
        try {
            int animation = attributes.getResourceId(R.styleable.GesturePreference_animation, 0);
            mVideoPath = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(context.getPackageName())
                    .appendPath(String.valueOf(animation))
                    .build();
            mMediaPlayer = MediaPlayer.create(mContext, mVideoPath);
            if (mMediaPlayer != null && mMediaPlayer.getDuration() > 0) {
                setLayoutResource(R.layout.gesture_preference);

                mPreviewResource = attributes.getResourceId(
                        R.styleable.GesturePreference_preview, 0);

                mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        mVideoReady = true;
                    }
                });

                mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.setLooping(true);
                    }
                });
                mAnimationAvailable = true;
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

        final TextureView video = (TextureView) holder.findViewById(R.id.gesture_video);
        final ImageView imageView = (ImageView) holder.findViewById(R.id.gesture_image);
        imageView.setImageResource(mPreviewResource);
        final ImageView playButton = (ImageView) holder.findViewById(R.id.gesture_play_button);

        video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaPlayer != null) {
                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.pause();
                        playButton.setVisibility(View.VISIBLE);
                    } else {
                        mMediaPlayer.start();
                        playButton.setVisibility(View.GONE);
                    }
                }
            }
        });

        video.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width,
                    int height) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setSurface(new Surface(surfaceTexture));
                    mVideoReady = false;
                    mMediaPlayer.seekTo(0);
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
                if (mVideoReady && imageView.getVisibility() == View.VISIBLE) {
                    imageView.setVisibility(View.GONE);
                } else if (mScrolling) {
                    mScrolling = false;
                    if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                        mMediaPlayer.pause();
                        playButton.setVisibility(View.VISIBLE);
                    }
                }
                if (mMediaPlayer != null && !mMediaPlayer.isPlaying() &&
                        playButton.getVisibility() != View.VISIBLE) {
                    playButton.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    @Override
    public void onDetached() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
        }
        super.onDetached();
    }

    void setScrolling(boolean scrolling) {
        mScrolling = scrolling;
    }

    void onViewVisible() {
        if (mVideoReady && mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.seekTo(0);
        }
    }

    void onViewInvisible() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }

}
