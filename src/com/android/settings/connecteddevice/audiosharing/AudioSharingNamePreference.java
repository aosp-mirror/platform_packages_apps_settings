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
import android.widget.ImageButton;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsQrCodeFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.widget.ValidatedEditTextPreference;

public class AudioSharingNamePreference extends ValidatedEditTextPreference {
    private static final String TAG = "AudioSharingNamePreference";

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

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final ImageButton shareButton = (ImageButton) holder.findViewById(R.id.button_icon);
        shareButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_qrcode_24dp));
        shareButton.setOnClickListener(
                unused ->
                        new SubSettingLauncher(getContext())
                                .setTitleText("Audio sharing QR code")
                                .setDestination(AudioStreamsQrCodeFragment.class.getName())
                                .setSourceMetricsCategory(SettingsEnums.AUDIO_SHARING_SETTINGS)
                                .launch());
    }
}
