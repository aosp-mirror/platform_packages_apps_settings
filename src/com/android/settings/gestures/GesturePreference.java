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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.MotionEvent;
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
public class GesturePreference extends SwitchPreference {
    private static final String TAG = "GesturePreference";
    private Uri mVideoPath;
    private Context mContext;
    private MediaPlayer mMediaPlayer;
    private MediaMetadataRetriever mMediaMetadata;

    public GesturePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setLayoutResource(R.layout.gesture_preference);
        TypedArray attributes = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.GesturePreference,
                0, 0);
        try {
            int animation = attributes.getResourceId(R.styleable.GesturePreference_animation, 0);
            mVideoPath = Uri.parse(
                "android.resource://" + context.getPackageName() + "/" + animation);
            mMediaMetadata = new MediaMetadataRetriever();
            mMediaMetadata.setDataSource(mContext, mVideoPath);
        } catch (Exception e) {
            // resource not available, show blank view
        } finally {
            attributes.recycle();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final TextureView video = (TextureView) holder.findViewById(R.id.gesture_video);
        final ImageView imageView = (ImageView) holder.findViewById(R.id.gesture_image);

        video.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (mMediaPlayer != null) {
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.pause();
                        } else {
                            mMediaPlayer.start();
                            imageView.setVisibility(View.GONE);
                        }
                    }
                    return true;
                }
                return false;
            }
        });

        video.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                mMediaPlayer = MediaPlayer.create(mContext, mVideoPath);
                if (mMediaPlayer != null) {
                    mMediaPlayer.setSurface(new Surface(surfaceTexture));
                    mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mediaPlayer.setLooping(true);
                        }
                    });
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                imageView.setVisibility(View.VISIBLE);
                if (mMediaPlayer != null) {
                    mMediaPlayer.stop();
                    mMediaPlayer.reset();
                    mMediaPlayer.release();
                }
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            }
        });

        if (mMediaPlayer == null) {
            Bitmap bitmap = mMediaMetadata.getFrameAtTime(0);
            if (bitmap != null) {
                imageView.setImageDrawable(new BitmapDrawable(bitmap));
            }
            imageView.setVisibility(View.VISIBLE);
        }

    }

}
