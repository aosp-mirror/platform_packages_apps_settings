/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.testutils.shadow;

import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TtsEngines;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.List;
import java.util.Locale;

@Implements(TtsEngines.class)
public class ShadowTtsEngines {
    private static TtsEngines sInstance;

    public static void setInstance(TtsEngines ttsEngines) {
        sInstance = ttsEngines;
    }

    @Resetter
    public static void reset() {
        sInstance = null;
    }

    @Implementation
    protected List<TextToSpeech.EngineInfo> getEngines() {
        return sInstance.getEngines();
    }

    @Implementation
    protected TextToSpeech.EngineInfo getEngineInfo(String packageName) {
        return sInstance.getEngineInfo(packageName);
    }

    @Implementation
    protected String getDefaultEngine() {
        return sInstance.getDefaultEngine();
    }

    @Implementation
    protected Intent getSettingsIntent(String engine) {
        return sInstance.getSettingsIntent(engine);
    }

    @Implementation
    protected boolean isEngineInstalled(String engine) {
        return sInstance.isEngineInstalled(engine);
    }

    @Implementation
    protected boolean isLocaleSetToDefaultForEngine(String engineName) {
        return sInstance.isLocaleSetToDefaultForEngine(engineName);
    }

    @Implementation
    protected Locale getLocalePrefForEngine(String engineName) {
        return sInstance.getLocalePrefForEngine(engineName);
    }

    @Implementation
    protected synchronized void updateLocalePrefForEngine(String engineName, Locale newLocale) {
        sInstance.updateLocalePrefForEngine(engineName, newLocale);
    }

    @Implementation
    protected Locale parseLocaleString(String localeString) {
        return sInstance.parseLocaleString(localeString);
    }
}