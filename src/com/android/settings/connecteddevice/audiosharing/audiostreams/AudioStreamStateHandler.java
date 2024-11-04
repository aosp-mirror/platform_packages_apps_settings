/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static android.text.Spanned.SPAN_EXCLUSIVE_INCLUSIVE;

import static com.android.settingslib.flags.Flags.audioSharingHysteresisModeFix;

import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.Utils;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

class AudioStreamStateHandler {
    private static final String TAG = "AudioStreamStateHandler";
    private static final boolean DEBUG = BluetoothUtils.D;
    @VisibleForTesting static final int EMPTY_STRING_RES = 0;

    final Handler mHandler = new Handler(Looper.getMainLooper());
    final MetricsFeatureProvider mMetricsFeatureProvider =
            FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    AudioStreamsRepository mAudioStreamsRepository = AudioStreamsRepository.getInstance();

    AudioStreamStateHandler() {}

    void handleStateChange(
            AudioStreamPreference preference,
            AudioStreamsProgressCategoryController controller,
            AudioStreamsHelper helper) {
        var newState = getStateEnum();
        if (preference.getAudioStreamState() == newState) {
            return;
        }
        if (DEBUG) {
            Log.d(
                    TAG,
                    "moveToState() : moving preference : ["
                            + preference.getAudioStreamBroadcastId()
                            + ", "
                            + preference.getAudioStreamBroadcastName()
                            + "] from state : "
                            + preference.getAudioStreamState()
                            + " to state : "
                            + newState);
        }
        preference.setAudioStreamState(newState);

        performAction(preference, controller, helper);

        // Update UI
        ThreadUtils.postOnMainThread(
                () -> {
                    String summary =
                            getSummary() != EMPTY_STRING_RES
                                    ? preference.getContext().getString(getSummary())
                                    : "";
                    if (newState
                            == AudioStreamsProgressCategoryController.AudioStreamState
                                    .ADD_SOURCE_BAD_CODE) {
                        SpannableString summarySpan = new SpannableString(summary);
                        int colorError = Utils.getColorErrorDefaultColor(preference.getContext());
                        summarySpan.setSpan(
                                new ForegroundColorSpan(colorError),
                                0,
                                summary.length(),
                                SPAN_EXCLUSIVE_INCLUSIVE);
                        preference.setSummary(summarySpan);
                    } else {
                        preference.setSummary(summary);
                    }
                    preference.setIsConnected(
                            newState
                                            == AudioStreamsProgressCategoryController
                                                    .AudioStreamState.SOURCE_ADDED
                                    || (audioSharingHysteresisModeFix()
                                            && newState
                                                    == AudioStreamsProgressCategoryController
                                                            .AudioStreamState.SOURCE_PRESENT));
                    preference.setOnPreferenceClickListener(getOnClickListener(controller));
                });
    }

    /**
     * Perform action related to the audio stream state (e.g, addSource) This method is intended to
     * be optionally overridden by subclasses to provide custom behavior based on the audio stream
     * state change.
     */
    void performAction(
            AudioStreamPreference preference,
            AudioStreamsProgressCategoryController controller,
            AudioStreamsHelper helper) {}

    /**
     * The preference summary for the audio stream state (e.g, Scanning...) This method is intended
     * to be optionally overridden.
     */
    @StringRes
    int getSummary() {
        return EMPTY_STRING_RES;
    }

    /**
     * The preference on click event for the audio stream state (e.g, open up a dialog) This method
     * is intended to be optionally overridden.
     */
    @Nullable
    Preference.OnPreferenceClickListener getOnClickListener(
            AudioStreamsProgressCategoryController controller) {
        return null;
    }

    /** Subclasses should always override. */
    AudioStreamsProgressCategoryController.AudioStreamState getStateEnum() {
        return AudioStreamsProgressCategoryController.AudioStreamState.UNKNOWN;
    }

    @VisibleForTesting
    void setAudioStreamsRepositoryForTesting(AudioStreamsRepository repository) {
        mAudioStreamsRepository = repository;
    }
}
