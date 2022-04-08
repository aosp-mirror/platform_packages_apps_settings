/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.gestures;

import static android.provider.Settings.Secure.VOLUME_HUSH_GESTURE;
import static android.provider.Settings.Secure.VOLUME_HUSH_MUTE;
import static android.provider.Settings.Secure.VOLUME_HUSH_VIBRATE;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class PreventRingingParentPreferenceController extends BasePreferenceController {

    final String SECURE_KEY = VOLUME_HUSH_GESTURE;

    public PreventRingingParentPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        int value = Settings.Secure.getInt(
                mContext.getContentResolver(), SECURE_KEY, VOLUME_HUSH_VIBRATE);
        int summary;
        switch (value) {
            case VOLUME_HUSH_VIBRATE:
                summary = R.string.prevent_ringing_option_vibrate_summary;
                break;
            case VOLUME_HUSH_MUTE:
                summary = R.string.prevent_ringing_option_mute_summary;
                break;
            default:
                summary = R.string.prevent_ringing_option_none_summary;
        }
        return mContext.getText(summary);
    }
}
