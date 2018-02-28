/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

public class GesturesSettingPreferenceController extends BasePreferenceController {
    private final AssistGestureFeatureProvider mFeatureProvider;
    private List<AbstractPreferenceController> mGestureControllers;

    private static final String KEY_GESTURES_SETTINGS = "gesture_settings";

    public GesturesSettingPreferenceController(Context context) {
        super(context, KEY_GESTURES_SETTINGS);
        mFeatureProvider = FeatureFactory.getFactory(context).getAssistGestureFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        if (mGestureControllers == null) {
            mGestureControllers = GestureSettings.buildPreferenceControllers(mContext,
                    null /* lifecycle */, new AmbientDisplayConfiguration(mContext));
        }
        boolean isAvailable = false;
        for (AbstractPreferenceController controller : mGestureControllers) {
            isAvailable = isAvailable || controller.isAvailable();
        }
        return isAvailable
                ? AVAILABLE
                : DISABLED_UNSUPPORTED;
    }

    @Override
    public CharSequence getSummary() {
        if (!mFeatureProvider.isSensorAvailable(mContext)) {
            return "";
        }
        final ContentResolver contentResolver = mContext.getContentResolver();
        final boolean assistGestureEnabled = Settings.Secure.getInt(
                contentResolver, Settings.Secure.ASSIST_GESTURE_ENABLED, 1) != 0;
        final boolean assistGestureSilenceEnabled = Settings.Secure.getInt(
                contentResolver, Settings.Secure.ASSIST_GESTURE_SILENCE_ALERTS_ENABLED, 1) != 0;

        if (mFeatureProvider.isSupported(mContext) && assistGestureEnabled) {
            return mContext.getText(
                    R.string.language_input_gesture_summary_on_with_assist);
        }
        if (assistGestureSilenceEnabled) {
            return mContext.getText(
                    R.string.language_input_gesture_summary_on_non_assist);
        }
        return mContext.getText(R.string.language_input_gesture_summary_off);
    }
}