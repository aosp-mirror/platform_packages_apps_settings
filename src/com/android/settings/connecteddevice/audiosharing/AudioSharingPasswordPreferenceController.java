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

import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioSharingPasswordPreferenceController extends BasePreferenceController
        implements ValidatedEditTextPreference.Validator,
                Preference.OnPreferenceChangeListener,
                DefaultLifecycleObserver {
    private static final String PREF_KEY = "audio_sharing_stream_password";

    private final BluetoothLeBroadcast.Callback mBroadcastCallback =
            new BluetoothLeBroadcast.Callback() {
                @Override
                public void onBroadcastMetadataChanged(
                        int broadcastId, BluetoothLeBroadcastMetadata metadata) {}

                @Override
                public void onBroadcastStartFailed(int reason) {}

                @Override
                public void onBroadcastStarted(int reason, int broadcastId) {}

                @Override
                public void onBroadcastStopFailed(int reason) {}

                @Override
                public void onBroadcastStopped(int reason, int broadcastId) {}

                @Override
                public void onBroadcastUpdateFailed(int reason, int broadcastId) {}

                @Override
                public void onBroadcastUpdated(int reason, int broadcastId) {}

                @Override
                public void onPlaybackStarted(int reason, int broadcastId) {}

                @Override
                public void onPlaybackStopped(int reason, int broadcastId) {}
            };
    @Nullable private final LocalBluetoothLeBroadcast mBroadcast;
    private final Executor mExecutor;
    private final AudioSharingPasswordValidator mAudioSharingPasswordValidator;
    @Nullable private ValidatedEditTextPreference mPreference;

    public AudioSharingPasswordPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBroadcast =
                Utils.getLocalBtManager(context).getProfileManager().getLeAudioBroadcastProfile();
        mAudioSharingPasswordValidator = new AudioSharingPasswordValidator();
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mBroadcast != null) {
            mBroadcast.registerServiceCallBack(mExecutor, mBroadcastCallback);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mBroadcast != null) {
            mBroadcast.unregisterServiceCallBack(mBroadcastCallback);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AudioSharingUtils.isFeatureEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.setValidator(this);
        }
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // TODO(chelseahao): implement
        return true;
    }

    @Override
    public boolean isTextValid(String value) {
        return mAudioSharingPasswordValidator.isTextValid(value);
    }
}
