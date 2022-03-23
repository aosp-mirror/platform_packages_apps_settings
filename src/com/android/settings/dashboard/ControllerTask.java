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
 * limitations under the License.
 */

package com.android.settings.dashboard;

import android.app.settings.SettingsEnums;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

/**
 * A {@link Runnable} controller task. This task handle the visibility of the controller in the
 * background. Also handle the state updating in the main thread.
 */
public class ControllerTask implements Runnable {
    private static final String TAG = "ControllerTask";
    private static final int CONTROLLER_UPDATESTATE_TIME_THRESHOLD = 50;

    private final AbstractPreferenceController mController;
    private final PreferenceScreen mScreen;
    private final int mMetricsCategory;
    private final MetricsFeatureProvider mMetricsFeature;

    public ControllerTask(AbstractPreferenceController controller, PreferenceScreen screen,
            MetricsFeatureProvider metricsFeature, int metricsCategory) {
        mController = controller;
        mScreen = screen;
        mMetricsFeature = metricsFeature;
        mMetricsCategory = metricsCategory;
    }

    @Override
    public void run() {
        if (!mController.isAvailable()) {
            return;
        }

        final String key = mController.getPreferenceKey();
        if (TextUtils.isEmpty(key)) {
            Log.d(TAG, String.format("Preference key is %s in Controller %s",
                    key, mController.getClass().getSimpleName()));
            return;
        }

        final Preference preference = mScreen.findPreference(key);
        if (preference == null) {
            Log.d(TAG, String.format("Cannot find preference with key %s in Controller %s",
                    key, mController.getClass().getSimpleName()));
            return;
        }
        ThreadUtils.postOnMainThread(() -> {
            final long t = SystemClock.elapsedRealtime();
            mController.updateState(preference);
            final int elapsedTime = (int) (SystemClock.elapsedRealtime() - t);
            if (elapsedTime > CONTROLLER_UPDATESTATE_TIME_THRESHOLD) {
                Log.w(TAG, "The updateState took " + elapsedTime + " ms in Controller "
                        + mController.getClass().getSimpleName());
                if (mMetricsFeature != null) {
                    mMetricsFeature.action(SettingsEnums.PAGE_UNKNOWN,
                            SettingsEnums.ACTION_CONTROLLER_UPDATE_STATE, mMetricsCategory,
                            mController.getClass().getSimpleName(), elapsedTime);
                }
            }
        });
    }

    AbstractPreferenceController getController() {
        return mController;
    }
}
