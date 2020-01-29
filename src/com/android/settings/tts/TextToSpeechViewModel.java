/*
 * Copyright 2020 The Android Open Source Project
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

package com.android.settings.tts;

import android.app.Application;
import android.speech.tts.TextToSpeech;
import android.util.Pair;

import androidx.lifecycle.AndroidViewModel;

/**
 * A helper view model to protect the TTS object from being destroyed and
 * recreated on orientation change.
*/
public class TextToSpeechViewModel extends AndroidViewModel {
    private TextToSpeech mTts;

    // Save the application so we can use it as the TTS context
    private final Application mApplication;

    public TextToSpeechViewModel(Application application) {
        super(application);

        mApplication = application;
    }

    /*
     * Since the view model now controls the TTS object, we need to handle shutting it down
     * ourselves.
     */
    @Override
    protected void onCleared() {
        shutdownTts();
    }

    protected void shutdownTts() {
        mTts.shutdown();
        mTts = null;
    }

    /*
     * An accessor method to get the TTS object. Returns a pair of the TTS object and a boolean
     * indicating whether the TTS object was newly created or not.
     */
    protected Pair<TextToSpeech, Boolean> getTtsAndWhetherNew(
            TextToSpeech.OnInitListener listener) {
        boolean ttsCreated = false;
        if (mTts == null) {
            mTts = new TextToSpeech(this.mApplication, listener);
            ttsCreated = true;
        }

        return Pair.create(mTts, ttsCreated);
    }

}
