/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.language;

import android.content.Context;
import android.speech.tts.TtsEngines;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class TtsPreferenceController extends BasePreferenceController {

    @VisibleForTesting
    TtsEngines mTtsEngines;

    public TtsPreferenceController(Context context, String key) {
        super(context, key);
        mTtsEngines = new TtsEngines(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return !mTtsEngines.getEngines().isEmpty() &&
                mContext.getResources().getBoolean(R.bool.config_show_tts_settings_summary)
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }
}
