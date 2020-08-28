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

package com.android.settings.widget;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

/**
 * A {@link VideoPreference.AnimationController} containing a {@link
 * MediaPlayer}. The controller is used by {@link VideoPreference} to display
 * a mp4 resource.
 */
class MediaAnimationController implements VideoPreference.AnimationController {
    private MediaPlayer mMediaPlayer;
    private boolean mVideoReady;
    private Surface mSurface;

    MediaAnimationController(Context context, int videoId) {
        final Uri videoPath = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.getPackageName())
                .appendPath(String.valueOf(videoId))
                .build();
        mMediaPlayer = MediaPlayer.create(context, videoPath);
        // when the playback res is invalid or others, MediaPlayer create may fail
        // and return null, so need add the null judgement.
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(0);
            mMediaPlayer.setOnSeekCompleteListener(mp -> mVideoReady = true);
            mMediaPlayer.setOnPreparedListener(mediaPlayer -> mediaPlayer.setLooping(true));
        }
    }

    @Override
    public int getVideoWidth() {
        return mMediaPlayer.getVideoWidth();
    }

    @Override
    public int getVideoHeight() {
        return mMediaPlayer.getVideoHeight();
    }

    @Override
    public void pause() {
        mMediaPlayer.pause();
    }

    @Override
    public void start() {
        mMediaPlayer.start();
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Override
    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    @Override
    public void attachView(TextureView video, View preview, View playButton) {
        updateViewStates(preview, playButton);
        video.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width,
                    int height) {
                setSurface(surfaceTexture);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width,
                    int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                preview.setVisibility(View.VISIBLE);
                mSurface = null;
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                setSurface(surfaceTexture);
                if (mVideoReady) {
                    if (preview.getVisibility() == View.VISIBLE) {
                        preview.setVisibility(View.GONE);
                    }
                    if (mMediaPlayer != null
                            && !mMediaPlayer.isPlaying()) {
                        mMediaPlayer.start();
                        playButton.setVisibility(View.GONE);
                    }
                }
                if (mMediaPlayer != null && !mMediaPlayer.isPlaying()
                        && playButton.getVisibility() != View.VISIBLE) {
                    playButton.setVisibility(View.VISIBLE);
                }
            }
        });
        video.setOnClickListener(v -> updateViewStates(preview, playButton));
    }

    @Override
    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mVideoReady = false;
        }
    }

    private void updateViewStates(View imageView, View playButton) {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            playButton.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.GONE);
            playButton.setVisibility(View.GONE);
            mMediaPlayer.start();
        }
    }

    private void setSurface(SurfaceTexture surfaceTexture) {
        if (mMediaPlayer != null && mSurface == null) {
            mSurface = new Surface(surfaceTexture);
            mMediaPlayer.setSurface(mSurface);
        }
    }
}
