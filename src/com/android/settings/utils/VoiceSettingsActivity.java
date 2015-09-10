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

package com.android.settings.utils;

import android.app.Activity;
import android.app.VoiceInteractor.AbortVoiceRequest;
import android.app.VoiceInteractor.CompleteVoiceRequest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Activity for modifying a setting using the Voice Interaction API. This activity
 * will only allow modifying the setting if the intent was sent using
 * {@link android.service.voice.VoiceInteractionSession#startVoiceActivity startVoiceActivity}
 * by the current Voice Interaction Service.
 */
abstract public class VoiceSettingsActivity extends Activity {

    private static final String TAG = "VoiceSettingsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isVoiceInteractionRoot()) {
            // Only permit if this is a voice interaction.
            if (onVoiceSettingInteraction(getIntent())) {
                // If it's complete, finish.
                finish();
            }
        } else {
            Log.v(TAG, "Cannot modify settings without voice interaction");
            finish();
        }
    }

    /**
     * Modify the setting as a voice interaction. Should return true if the
     * voice interaction is complete or false if more interaction is required.
     */
    abstract protected boolean onVoiceSettingInteraction(Intent intent);

    /**
     * Send a notification that the interaction was successful. If {@param prompt} is
     * not null, then it will be read to the user.
     */
    protected void notifySuccess(CharSequence prompt) {
        if (getVoiceInteractor() != null) {
            getVoiceInteractor().submitRequest(new CompleteVoiceRequest(prompt, null) {
                @Override
                public void onCompleteResult(Bundle options) {
                    finish();
                }
            });
        }
    }

    /**
     * Indicates when the setting could not be changed.
     */
    protected void notifyFailure(CharSequence prompt) {
        if (getVoiceInteractor() != null) {
            getVoiceInteractor().submitRequest(new AbortVoiceRequest(prompt, null));
        }
    }
}
