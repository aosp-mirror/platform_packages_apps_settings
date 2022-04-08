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
import android.preference.SeekBarVolumizer;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;

import java.util.Objects;

/** A slider preference that directly controls an audio stream volume (no dialog) **/
public class VolumeSeekBarPreference extends SeekBarPreference {
    private static final String TAG = "VolumeSeekBarPreference";

    protected SeekBar mSeekBar;
    private int mStream;
    private SeekBarVolumizer mVolumizer;
    private Callback mCallback;
    private ImageView mIconView;
    private TextView mSuppressionTextView;
    private String mSuppressionText;
    private boolean mMuted;
    private boolean mZenMuted;
    private int mIconResId;
    private int mMuteIconResId;
    private boolean mStopped;
    @VisibleForTesting
    AudioManager mAudioManager;

    public VolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_volume_slider);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_volume_slider);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_volume_slider);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public VolumeSeekBarPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_volume_slider);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void setStream(int stream) {
        mStream = stream;
        setMax(mAudioManager.getStreamMaxVolume(mStream));
        // Use getStreamMinVolumeInt for non-public stream type
        // eg: AudioManager.STREAM_BLUETOOTH_SCO
        setMin(mAudioManager.getStreamMinVolumeInt(mStream));
        setProgress(mAudioManager.getStreamVolume(mStream));
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void onActivityResume() {
        if (mStopped) {
            init();
        }
    }

    public void onActivityPause() {
        mStopped = true;
        if (mVolumizer != null) {
            mVolumizer.stop();
            mVolumizer = null;
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mSeekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
        mIconView = (ImageView) view.findViewById(com.android.internal.R.id.icon);
        mSuppressionTextView = (TextView) view.findViewById(R.id.suppression_text);
        init();
    }

    protected void init() {
        if (mSeekBar == null) return;
        final SeekBarVolumizer.Callback sbvc = new SeekBarVolumizer.Callback() {
            @Override
            public void onSampleStarting(SeekBarVolumizer sbv) {
                if (mCallback != null) {
                    mCallback.onSampleStarting(sbv);
                }
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                if (mCallback != null) {
                    mCallback.onStreamValueChanged(mStream, progress);
                }
            }
            @Override
            public void onMuted(boolean muted, boolean zenMuted) {
                if (mMuted == muted && mZenMuted == zenMuted) return;
                mMuted = muted;
                mZenMuted = zenMuted;
                updateIconView();
            }
        };
        final Uri sampleUri = mStream == AudioManager.STREAM_MUSIC ? getMediaVolumeUri() : null;
        if (mVolumizer == null) {
            mVolumizer = new SeekBarVolumizer(getContext(), mStream, sampleUri, sbvc);
        }
        mVolumizer.start();
        mVolumizer.setSeekBar(mSeekBar);
        updateIconView();
        updateSuppressionText();
        if (!isEnabled()) {
            mSeekBar.setEnabled(false);
            mVolumizer.stop();
        }
    }

    protected void updateIconView() {
        if (mIconView == null) return;
        if (mIconResId != 0) {
            mIconView.setImageResource(mIconResId);
        } else if (mMuteIconResId != 0 && mMuted && !mZenMuted) {
            mIconView.setImageResource(mMuteIconResId);
        } else {
            mIconView.setImageDrawable(getIcon());
        }
    }

    public void showIcon(int resId) {
        // Instead of using setIcon, which will trigger listeners, this just decorates the
        // preference temporarily with a new icon.
        if (mIconResId == resId) return;
        mIconResId = resId;
        updateIconView();
    }

    public void setMuteIcon(int resId) {
        if (mMuteIconResId == resId) return;
        mMuteIconResId = resId;
        updateIconView();
    }

    private Uri getMediaVolumeUri() {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + getContext().getPackageName()
                + "/" + R.raw.media_volume);
    }

    public void setSuppressionText(String text) {
        if (Objects.equals(text, mSuppressionText)) return;
        mSuppressionText = text;
        updateSuppressionText();
    }

    protected void updateSuppressionText() {
        if (mSuppressionTextView != null && mSeekBar != null) {
            mSuppressionTextView.setText(mSuppressionText);
            final boolean showSuppression = !TextUtils.isEmpty(mSuppressionText);
            mSuppressionTextView.setVisibility(showSuppression ? View.VISIBLE : View.GONE);
        }
    }

    public interface Callback {
        void onSampleStarting(SeekBarVolumizer sbv);
        void onStreamValueChanged(int stream, int progress);
    }
}
