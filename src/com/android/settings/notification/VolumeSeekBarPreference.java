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

import static com.android.internal.jank.InteractionJankMonitor.CUJ_SETTINGS_SLIDER;

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

import com.android.internal.jank.InteractionJankMonitor;
import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/** A slider preference that directly controls an audio stream volume (no dialog) **/
public class VolumeSeekBarPreference extends SeekBarPreference {
    private static final String TAG = "VolumeSeekBarPreference";

    private final InteractionJankMonitor mJankMonitor = InteractionJankMonitor.getInstance();

    protected SeekBar mSeekBar;
    private int mStream;
    private SeekBarVolumizer mVolumizer;
    @VisibleForTesting
    SeekBarVolumizerFactory mSeekBarVolumizerFactory;
    private Callback mCallback;
    private Listener mListener;
    private ImageView mIconView;
    private TextView mSuppressionTextView;
    private TextView mTitle;
    private String mSuppressionText;
    private boolean mMuted;
    private boolean mZenMuted;
    private int mIconResId;
    private int mMuteIconResId;
    private boolean mStopped;
    @VisibleForTesting
    AudioManager mAudioManager;
    private Locale mLocale;
    private NumberFormat mNumberFormat;

    public VolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_volume_slider);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mSeekBarVolumizerFactory = new SeekBarVolumizerFactory(context);
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_volume_slider);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mSeekBarVolumizerFactory = new SeekBarVolumizerFactory(context);
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_volume_slider);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mSeekBarVolumizerFactory = new SeekBarVolumizerFactory(context);
    }

    public VolumeSeekBarPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_volume_slider);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mSeekBarVolumizerFactory = new SeekBarVolumizerFactory(context);
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

    public void setListener(Listener listener) {
        mListener = listener;
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
        mTitle = (TextView) view.findViewById(com.android.internal.R.id.title);
        init();
    }

    protected void init() {
        if (mSeekBar == null) return;
        // It's unnecessary to set up relevant volumizer configuration if preference is disabled.
        if (!isEnabled()) {
            mSeekBar.setEnabled(false);
            return;
        }
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
                overrideSeekBarStateDescription(formatStateDescription(progress));
            }
            @Override
            public void onMuted(boolean muted, boolean zenMuted) {
                if (mMuted == muted && mZenMuted == zenMuted) return;
                mMuted = muted;
                mZenMuted = zenMuted;
                updateIconView();
                if (mListener != null) {
                    mListener.onUpdateMuteState();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBarVolumizer sbv) {
                if (mCallback != null) {
                    mCallback.onStartTrackingTouch(sbv);
                }
                mJankMonitor.begin(InteractionJankMonitor.Configuration.Builder
                        .withView(CUJ_SETTINGS_SLIDER, mSeekBar)
                        .setTag(getKey()));
            }
            @Override
            public void onStopTrackingTouch(SeekBarVolumizer sbv) {
                mJankMonitor.end(CUJ_SETTINGS_SLIDER);
            }
        };
        final Uri sampleUri = mStream == AudioManager.STREAM_MUSIC ? getMediaVolumeUri() : null;
        if (mVolumizer == null) {
            mVolumizer = mSeekBarVolumizerFactory.create(mStream, sampleUri, sbvc);
        }
        mVolumizer.start();
        mVolumizer.setSeekBar(mSeekBar);
        updateIconView();
        updateSuppressionText();
        if (mListener != null) {
            mListener.onUpdateMuteState();
        }
    }

    protected void updateIconView() {
        if (mIconView == null) return;
        if (mIconResId != 0) {
            mIconView.setImageResource(mIconResId);
        } else if (mMuteIconResId != 0 && isMuted()) {
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

    @VisibleForTesting
    CharSequence formatStateDescription(int progress) {
        // This code follows the same approach in ProgressBar.java, but it rounds down the percent
        // to match it with what the talkback feature says after any progress change. (b/285458191)
        // Cache the locale-appropriate NumberFormat.  Configuration locale is guaranteed
        // non-null, so the first time this is called we will always get the appropriate
        // NumberFormat, then never regenerate it unless the locale changes on the fly.
        Locale curLocale = getContext().getResources().getConfiguration().getLocales().get(0);
        if (mLocale == null || !mLocale.equals(curLocale)) {
            mLocale = curLocale;
            mNumberFormat = NumberFormat.getPercentInstance(mLocale);
        }
        return mNumberFormat.format(getPercent(progress));
    }

    @VisibleForTesting
    double getPercent(float progress) {
        final float maxProgress = getMax();
        final float minProgress = getMin();
        final float diffProgress = maxProgress - minProgress;
        if (diffProgress <= 0.0f) {
            return 0.0f;
        }
        final float percent = (progress - minProgress) / diffProgress;
        return Math.floor(Math.max(0.0f, Math.min(1.0f, percent)) * 100) / 100;
    }

    public void setSuppressionText(String text) {
        if (Objects.equals(text, mSuppressionText)) return;
        mSuppressionText = text;
        updateSuppressionText();
    }

    protected boolean isMuted() {
        return mMuted && !mZenMuted;
    }

    protected void updateSuppressionText() {
        if (mSuppressionTextView != null && mSeekBar != null) {
            mSuppressionTextView.setText(mSuppressionText);
            final boolean showSuppression = !TextUtils.isEmpty(mSuppressionText);
            mSuppressionTextView.setVisibility(showSuppression ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Update content description of title to improve talkback announcements.
     */
    protected void updateContentDescription(CharSequence contentDescription) {
        if (mTitle == null) return;
        mTitle.setContentDescription(contentDescription);
    }

    protected void setAccessibilityLiveRegion(int mode) {
        if (mTitle == null) return;
        mTitle.setAccessibilityLiveRegion(mode);
    }

    public interface Callback {
        void onSampleStarting(SeekBarVolumizer sbv);
        void onStreamValueChanged(int stream, int progress);

        /**
         * Callback reporting that the seek bar is start tracking.
         */
        void onStartTrackingTouch(SeekBarVolumizer sbv);
    }

    /**
     * Listener to view updates in volumeSeekbarPreference.
     */
    public interface Listener {

        /**
         * Listener to mute state updates.
         */
        void onUpdateMuteState();
    }
}
