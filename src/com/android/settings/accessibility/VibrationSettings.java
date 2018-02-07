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

package com.android.settings.accessibility;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity with the accessibility settings.
 */
public class VibrationSettings extends SettingsPreferenceFragment implements Indexable {

    // Preferences
    @VisibleForTesting
    static final String NOTIFICATION_VIBRATION_PREFERENCE_SCREEN =
            "notification_vibration_preference_screen";
    @VisibleForTesting
    static final String TOUCH_VIBRATION_PREFERENCE_SCREEN =
            "touch_vibration_preference_screen";

    private final Handler mHandler = new Handler();
    private final SettingsContentObserver mSettingsContentObserver;

    private Preference mNotificationVibrationPreferenceScreen;
    private Preference mTouchVibrationPreferenceScreen;

    public VibrationSettings() {
        List<String> vibrationSettings = new ArrayList<>();
        vibrationSettings.add(Settings.System.HAPTIC_FEEDBACK_INTENSITY);
        vibrationSettings.add(Settings.System.NOTIFICATION_VIBRATION_INTENSITY);
        mSettingsContentObserver = new SettingsContentObserver(mHandler, vibrationSettings) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                updatePreferences();
            }
        };
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ACCESSIBILITY_VIBRATION;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.accessibility_vibration_settings);
        initializePreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
        mSettingsContentObserver.register(getContentResolver());
    }

    @Override
    public void onPause() {
        mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
    }

    private void initializePreferences() {
        // Notification and notification vibration strength adjustments.
        mNotificationVibrationPreferenceScreen =
                findPreference(NOTIFICATION_VIBRATION_PREFERENCE_SCREEN);

        // Touch feedback strength adjustments.
        mTouchVibrationPreferenceScreen = findPreference(TOUCH_VIBRATION_PREFERENCE_SCREEN);
    }

    private void updatePreferences() {
        updateNotificationVibrationSummary(mNotificationVibrationPreferenceScreen);
        updateTouchVibrationSummary(mTouchVibrationPreferenceScreen);
    }

    private void updateNotificationVibrationSummary(Preference pref) {
        Vibrator vibrator = getContext().getSystemService(Vibrator.class);
        final int intensity = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.NOTIFICATION_VIBRATION_INTENSITY,
                vibrator.getDefaultNotificationVibrationIntensity());
        CharSequence summary = getVibrationIntensitySummary(getContext(), intensity);
        mNotificationVibrationPreferenceScreen.setSummary(summary);
    }

    private void updateTouchVibrationSummary(Preference pref) {
        Vibrator vibrator = getContext().getSystemService(Vibrator.class);
        final int intensity = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_INTENSITY,
                vibrator.getDefaultHapticFeedbackIntensity());
        CharSequence summary = getVibrationIntensitySummary(getContext(), intensity);
        mTouchVibrationPreferenceScreen.setSummary(summary);
    }

    public static String getVibrationIntensitySummary(Context context, int intensity) {
        switch (intensity) {
            case Vibrator.VIBRATION_INTENSITY_OFF:
                return context.getString(R.string.accessibility_vibration_intensity_off);
            case Vibrator.VIBRATION_INTENSITY_LOW:
                return context.getString(R.string.accessibility_vibration_intensity_low);
            case Vibrator.VIBRATION_INTENSITY_MEDIUM:
                return context.getString(R.string.accessibility_vibration_intensity_medium);
            case Vibrator.VIBRATION_INTENSITY_HIGH:
                return context.getString(R.string.accessibility_vibration_intensity_high);
            default:
                return "";
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    List<SearchIndexableResource> indexables = new ArrayList<>();
                    SearchIndexableResource indexable = new SearchIndexableResource(context);
                    indexable.xmlResId = R.xml.accessibility_vibration_settings;
                    indexables.add(indexable);
                    return indexables;
                }
            };
}
