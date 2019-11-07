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

import android.content.Context;
import android.hardware.display.AmbientDisplayConfiguration;

import androidx.annotation.NonNull;

import com.android.settings.aware.AwareFeatureProvider;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class GesturesSettingPreferenceController extends BasePreferenceController {
    private final AssistGestureFeatureProvider mFeatureProvider;
    private final AwareFeatureProvider mAwareFeatureProvider;
    private List<AbstractPreferenceController> mGestureControllers;

    private static final String KEY_GESTURES_SETTINGS = "gesture_settings";
    private static final String FAKE_PREF_KEY = "fake_key_only_for_get_available";

    public GesturesSettingPreferenceController(Context context) {
        super(context, KEY_GESTURES_SETTINGS);
        mFeatureProvider = FeatureFactory.getFactory(context).getAssistGestureFeatureProvider();
        mAwareFeatureProvider = FeatureFactory.getFactory(context).getAwareFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        if (mGestureControllers == null) {
            mGestureControllers = buildAllPreferenceControllers(mContext);
        }
        boolean isAvailable = false;
        for (AbstractPreferenceController controller : mGestureControllers) {
            isAvailable = isAvailable || controller.isAvailable();
        }
        return isAvailable ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    /**
     * Get all controllers for their availability status when doing getAvailabilityStatus.
     * Do not use this method to add controllers into fragment, most of below controllers already
     * convert to TogglePreferenceController, please register them in xml.
     * The key is fake because those controllers won't be use to control preference.
     */
    private static List<AbstractPreferenceController> buildAllPreferenceControllers(
            @NonNull Context context) {
        final AmbientDisplayConfiguration ambientDisplayConfiguration =
                new AmbientDisplayConfiguration(context);
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        controllers.add(new AssistGestureSettingsPreferenceController(context, FAKE_PREF_KEY)
                .setAssistOnly(false));
        controllers.add(new SwipeToNotificationPreferenceController(context, FAKE_PREF_KEY));
        controllers.add(new DoubleTwistPreferenceController(context, FAKE_PREF_KEY));
        controllers.add(new DoubleTapPowerPreferenceController(context, FAKE_PREF_KEY));
        controllers.add(new PickupGesturePreferenceController(context, FAKE_PREF_KEY)
                .setConfig(ambientDisplayConfiguration));
        controllers.add(new DoubleTapScreenPreferenceController(context, FAKE_PREF_KEY)
                .setConfig(ambientDisplayConfiguration));
        controllers.add(new PreventRingingParentPreferenceController(context, FAKE_PREF_KEY));
        controllers.add(new SwipeToScreenshotPreferenceController(context, FAKE_PREF_KEY));
        return controllers;
    }
}
