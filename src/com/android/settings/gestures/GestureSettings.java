/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.gestures;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.SearchIndexableResource;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Top level fragment for gesture settings.
 * This will create individual switch preference for each gesture and handle updates when each
 * preference is updated
 */
public class GestureSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {

    private static final String TAG = "GestureSettings";
    private static final String PREF_KEY_DOUBLE_TAP_POWER = "gesture_double_tap_power";
    private static final String PREF_KEY_DOUBLE_TWIST = "gesture_double_twist";
    private static final String PREF_KEY_PICK_UP_AND_NUDGE = "gesture_pick_up_and_nudge";
    private static final String PREF_KEY_SWIPE_DOWN_FINGERPRINT = "gesture_swipe_down_fingerprint";
    private static final String DEBUG_DOZE_COMPONENT = "debug.doze.component";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.gesture_settings);
        Context context = getActivity();

         // Double tap power for camera
        if (isCameraDoubleTapPowerGestureAvailable(getResources())) {
            int cameraDisabled = Secure.getInt(
                    getContentResolver(), Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, 0);
            GesturePreference preference =
                    (GesturePreference) findPreference(PREF_KEY_DOUBLE_TAP_POWER);
            preference.setChecked(cameraDisabled == 0);
            preference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(PREF_KEY_DOUBLE_TAP_POWER);
        }

        // Ambient Display
        if (isDozeAvailable(context)) {
            GesturePreference preference =
                    (GesturePreference) findPreference(PREF_KEY_PICK_UP_AND_NUDGE);
            int dozeEnabled = Secure.getInt(getContentResolver(), Secure.DOZE_ENABLED, 1);
            preference.setChecked(dozeEnabled != 0);
            preference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(PREF_KEY_PICK_UP_AND_NUDGE);
        }

        // Fingerprint slide for notifications
        if (isSystemUINavigationAvailable(context)) {
            GesturePreference preference =
                    (GesturePreference) findPreference(PREF_KEY_SWIPE_DOWN_FINGERPRINT);
            preference.setChecked(isSystemUINavigationEnabled(context));
            preference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(PREF_KEY_SWIPE_DOWN_FINGERPRINT);
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (boolean) newValue;
        String key = preference.getKey();
        if (PREF_KEY_DOUBLE_TAP_POWER.equals(key)) {
            Secure.putInt(getContentResolver(),
                    Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, enabled ? 0 : 1);
        } else if (PREF_KEY_PICK_UP_AND_NUDGE.equals(key)) {
            Secure.putInt(getContentResolver(), Secure.DOZE_ENABLED, enabled ? 1 : 0);
        } else if (PREF_KEY_SWIPE_DOWN_FINGERPRINT.equals(key)) {
            Global.putInt(getContentResolver(),
                    Global.SYSTEM_NAVIGATION_KEYS_ENABLED, enabled ? 1 : 0);
        }
        return true;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.SETTINGS_GESTURES;
    }

    private static boolean isCameraDoubleTapPowerGestureAvailable(Resources res) {
        return res.getBoolean(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled);
    }

    private static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get(DEBUG_DOZE_COMPONENT) : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }

    private static boolean isSystemUINavigationAvailable(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_supportSystemNavigationKeys);
    }

    private static boolean isSystemUINavigationEnabled(Context context) {
        return Global.getInt(context.getContentResolver(), Global.SYSTEM_NAVIGATION_KEYS_ENABLED, 1)
                == 1;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                     boolean enabled) {
                ArrayList<SearchIndexableResource> result =
                        new ArrayList<SearchIndexableResource>();

                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.gesture_settings;
                result.add(sir);

                return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                ArrayList<String> result = new ArrayList<String>();
                if (!isCameraDoubleTapPowerGestureAvailable(context.getResources())) {
                    result.add(PREF_KEY_DOUBLE_TAP_POWER);
                }
                if (!isDozeAvailable(context)) {
                    result.add(PREF_KEY_PICK_UP_AND_NUDGE);
                }
                if (!isSystemUINavigationAvailable(context)) {
                    result.add(PREF_KEY_SWIPE_DOWN_FINGERPRINT);
                }
                return result;
            }
        };

}