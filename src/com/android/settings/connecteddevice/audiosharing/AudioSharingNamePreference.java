/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsQrCodeFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.widget.ValidatedEditTextPreference;

public class AudioSharingNamePreference extends ValidatedEditTextPreference {
    private static final String TAG = "AudioSharingNamePreference";
    private boolean mShowQrCodeIcon = false;

    public AudioSharingNamePreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    public AudioSharingNamePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    public AudioSharingNamePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public AudioSharingNamePreference(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        setLayoutResource(
                com.android.settingslib.widget.preference.twotarget.R.layout.preference_two_target);
        setWidgetLayoutResource(R.layout.preference_widget_qrcode);
    }

    void setShowQrCodeIcon(boolean show) {
        mShowQrCodeIcon = show;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        ImageButton shareButton = (ImageButton) holder.findViewById(R.id.button_icon);
        View divider =
                holder.findViewById(
                        com.android.settingslib.widget.preference.twotarget.R.id
                                .two_target_divider);

        if (shareButton != null && divider != null) {
            if (mShowQrCodeIcon) {
                configureVisibleStateForQrCodeIcon(shareButton, divider);
            } else {
                configureInvisibleStateForQrCodeIcon(shareButton, divider);
            }
        } else {
            Log.w(TAG, "onBindViewHolder() : shareButton or divider is null!");
        }
    }

    private void configureVisibleStateForQrCodeIcon(ImageButton shareButton, View divider) {
        divider.setVisibility(View.VISIBLE);
        shareButton.setVisibility(View.VISIBLE);
        shareButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_qrcode_24dp));
        shareButton.setOnClickListener(unused -> launchAudioSharingQrCodeFragment());
        shareButton.setContentDescription(
                getContext().getString(R.string.audio_sharing_qrcode_button_label));
    }

    private void configureInvisibleStateForQrCodeIcon(ImageButton shareButton, View divider) {
        divider.setVisibility(View.GONE);
        shareButton.setVisibility(View.GONE);
        shareButton.setOnClickListener(null);
    }

    private void launchAudioSharingQrCodeFragment() {
        new SubSettingLauncher(getContext())
                .setTitleRes(R.string.audio_streams_qr_code_page_title)
                .setDestination(AudioStreamsQrCodeFragment.class.getName())
                .setSourceMetricsCategory(SettingsEnums.AUDIO_SHARING_SETTINGS)
                .launch();
    }
}
