/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static android.view.PointerIcon.POINTER_ICON_VECTOR_STYLE_STROKE_WHITE;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class PointerStrokeStylePreferenceController extends BasePreferenceController
    implements LifecycleEventObserver {

    private MetricsFeatureProvider mMetricsFeatureProvider;

    @VisibleForTesting
    static final String KEY_POINTER_STROKE_STYLE = "pointer_stroke_style";

    public PointerStrokeStylePreferenceController(@NonNull Context context) {
        super(context, KEY_POINTER_STROKE_STYLE);

        mMetricsFeatureProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return android.view.flags.Flags.enableVectorCursorA11ySettings() ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference pointerStrokeStylePreference = screen.findPreference(KEY_POINTER_STROKE_STYLE);
        if (pointerStrokeStylePreference == null) {
            return;
        }
        pointerStrokeStylePreference.setPreferenceDataStore(new PreferenceDataStore() {
            @Override
            public void putInt(@NonNull String key, int value) {
                Settings.System.putIntForUser(mContext.getContentResolver(), key, value,
                        UserHandle.USER_CURRENT);
            }

            @Override
            public int getInt(@NonNull String key, int defValue) {
                return Settings.System.getIntForUser(mContext.getContentResolver(), key, defValue,
                        UserHandle.USER_CURRENT);
            }
        });
    }

    @Override
        public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner,
                @NonNull Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_PAUSE) {
                int currentValue =
                        Settings.System.getIntForUser(mContext.getContentResolver(),
                                Settings.System.POINTER_STROKE_STYLE,
                                POINTER_ICON_VECTOR_STYLE_STROKE_WHITE, UserHandle.USER_CURRENT);
                mMetricsFeatureProvider.action(mContext,
                            SettingsEnums.ACTION_POINTER_ICON_STROKE_STYLE_CHANGED, currentValue);
            }
        }
}
