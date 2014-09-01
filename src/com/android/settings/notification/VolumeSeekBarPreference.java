/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.preference.SeekBarPreference;
import android.preference.SeekBarVolumizer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.android.settings.R;

/** A slider preference that directly controls an audio stream volume (no dialog) **/
public class VolumeSeekBarPreference extends SeekBarPreference
        implements PreferenceManager.OnActivityStopListener {
    private static final String TAG = "VolumeSeekBarPreference";

    private int mStream;
    private SeekBar mSeekBar;
    private SeekBarVolumizer mVolumizer;
    private Callback mCallback;
    private ImageView mIconView;

    public VolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VolumeSeekBarPreference(Context context) {
        this(context, null);
    }

    public void setStream(int stream) {
        mStream = stream;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void onActivityStop() {
        if (mVolumizer != null) {
            mVolumizer.stop();
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if (mStream == 0) {
            Log.w(TAG, "No stream found, not binding volumizer");
            return;
        }
        getPreferenceManager().registerOnActivityStopListener(this);
        final SeekBar seekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
        if (seekBar == mSeekBar) return;
        mSeekBar = seekBar;
        final SeekBarVolumizer.Callback sbvc = new SeekBarVolumizer.Callback() {
            @Override
            public void onSampleStarting(SeekBarVolumizer sbv) {
                if (mCallback != null) {
                    mCallback.onSampleStarting(sbv);
                }
            }
        };
        final Uri sampleUri = mStream == AudioManager.STREAM_MUSIC ? getMediaVolumeUri() : null;
        if (mVolumizer == null) {
            mVolumizer = new SeekBarVolumizer(getContext(), mStream, sampleUri, sbvc) {
                // we need to piggyback on SBV's SeekBar listener to update our icon
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                        boolean fromTouch) {
                    super.onProgressChanged(seekBar, progress, fromTouch);
                    mCallback.onStreamValueChanged(mStream, progress);
                }
            };
        }
        mVolumizer.setSeekBar(mSeekBar);
        mIconView = (ImageView) view.findViewById(com.android.internal.R.id.icon);
        mCallback.onStreamValueChanged(mStream, mSeekBar.getProgress());
    }

    // during initialization, this preference is the SeekBar listener
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromTouch) {
        super.onProgressChanged(seekBar, progress, fromTouch);
        mCallback.onStreamValueChanged(mStream, progress);
    }

    public void showIcon(int resId) {
        // Instead of using setIcon, which will trigger listeners, this just decorates the
        // preference temporarily with a new icon.
        if (mIconView != null) {
            mIconView.setImageResource(resId);
        }
    }

    private Uri getMediaVolumeUri() {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + getContext().getPackageName()
                + "/" + R.raw.media_volume);
    }

    public interface Callback {
        void onSampleStarting(SeekBarVolumizer sbv);
        void onStreamValueChanged(int stream, int progress);
    }
}
