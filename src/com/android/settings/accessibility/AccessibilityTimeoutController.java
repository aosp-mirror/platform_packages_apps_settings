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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Controller class that control accessibility timeout settings. */
public class AccessibilityTimeoutController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop , SelectorWithWidgetPreference.OnClickListener {

    private static final List<String> ACCESSIBILITY_TIMEOUT_FEATURE_KEYS = Arrays.asList(
            Settings.Secure.ACCESSIBILITY_NON_INTERACTIVE_UI_TIMEOUT_MS,
            Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS
    );

    // pair the preference key and timeout value.
    private final Map<String, Integer> mAccessibilityTimeoutKeyToValueMap = new HashMap<>();
    // RadioButtonPreference key, each preference represent a timeout value.
    private final ContentResolver mContentResolver;
    private final Resources mResources;
    private SelectorWithWidgetPreference mPreference;
    private int mAccessibilityUiTimeoutValue;
    private AccessibilitySettingsContentObserver mSettingsContentObserver;

    public AccessibilityTimeoutController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
        mResources = context.getResources();
        mSettingsContentObserver = new AccessibilitySettingsContentObserver(
                new Handler(Looper.getMainLooper()));
        mSettingsContentObserver.registerKeysToObserverCallback(ACCESSIBILITY_TIMEOUT_FEATURE_KEYS,
                key -> updateState(mPreference));
    }

    @VisibleForTesting
    AccessibilityTimeoutController(Context context, String preferenceKey,
            AccessibilitySettingsContentObserver contentObserver) {
        this(context, preferenceKey);
        mSettingsContentObserver = contentObserver;
    }

    @Override
    public void onStart() {
        mSettingsContentObserver.register(mContentResolver);
    }

    @Override
    public void onStop() {
        mSettingsContentObserver.unregister(mContentResolver);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setOnClickListener(this);
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
        final String value = String.valueOf(getTimeoutValueToKeyMap().get(mPreferenceKey));

        // save value to both content and control timeout setting.
        Settings.Secure.putString(mContentResolver,
                Settings.Secure.ACCESSIBILITY_NON_INTERACTIVE_UI_TIMEOUT_MS, value);
        Settings.Secure.putString(mContentResolver,
                Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS, value);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mAccessibilityUiTimeoutValue = AccessibilityTimeoutUtils.getSecureAccessibilityTimeoutValue(
                mContentResolver);

        // reset RadioButton
        mPreference.setChecked(false);
        final int preferenceValue = getTimeoutValueToKeyMap().get(mPreference.getKey());
        if (mAccessibilityUiTimeoutValue == preferenceValue) {
            mPreference.setChecked(true);
        }
    }

    private Map<String, Integer> getTimeoutValueToKeyMap() {
        if (mAccessibilityTimeoutKeyToValueMap.size() == 0) {
            String[] timeoutKeys = mResources.getStringArray(
                    R.array.accessibility_timeout_control_selector_keys);
            final int[] timeoutValues = mResources.getIntArray(
                    R.array.accessibility_timeout_selector_values);
            final int timeoutValueCount = timeoutValues.length;
            for (int i = 0; i < timeoutValueCount; i++) {
                mAccessibilityTimeoutKeyToValueMap.put(timeoutKeys[i], timeoutValues[i]);
            }
        }
        return mAccessibilityTimeoutKeyToValueMap;
    }
}
