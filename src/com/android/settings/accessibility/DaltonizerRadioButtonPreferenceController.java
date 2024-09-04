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

package com.android.settings.accessibility;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.HashMap;
import java.util.Map;

/** Controller class that control radio button of accessibility daltonizer settings. */
public class DaltonizerRadioButtonPreferenceController extends BasePreferenceController implements
        DefaultLifecycleObserver, SelectorWithWidgetPreference.OnClickListener {
    private static final String DALTONIZER_TYPE_SETTINGS_KEY =
            Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER;

    // pair the preference key and daltonizer value.
    private final Map<String, Integer> mAccessibilityDaltonizerKeyToValueMap = new HashMap<>();

    // RadioButtonPreference key, each preference represent a daltonizer value.
    private final ContentResolver mContentResolver;
    private final ContentObserver mSettingsContentObserver;
    private final Resources mResources;
    private SelectorWithWidgetPreference mPreference;

    public DaltonizerRadioButtonPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
        mResources = context.getResources();
        mSettingsContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                if (mPreference != null) {
                    updateState(mPreference);
                }
            }
        };
    }

    private Map<String, Integer> getDaltonizerValueToKeyMap() {
        if (mAccessibilityDaltonizerKeyToValueMap.isEmpty()) {

            final String[] daltonizerKeys = mResources.getStringArray(
                    R.array.daltonizer_mode_keys);

            final int[] daltonizerValues = mResources.getIntArray(
                    R.array.daltonizer_type_values);

            final int daltonizerValueCount = daltonizerValues.length;
            for (int i = 0; i < daltonizerValueCount; i++) {
                mAccessibilityDaltonizerKeyToValueMap.put(daltonizerKeys[i], daltonizerValues[i]);
            }
        }
        return mAccessibilityDaltonizerKeyToValueMap;
    }

    private void handlePreferenceChange(String value) {
        Settings.Secure.putString(mContentResolver, DALTONIZER_TYPE_SETTINGS_KEY, value);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (SelectorWithWidgetPreference)
                screen.findPreference(getPreferenceKey());
        mPreference.setOnClickListener(this);
        mPreference.setAppendixVisibility(View.GONE);
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
        final int value = getDaltonizerValueToKeyMap().get(mPreferenceKey);
        handlePreferenceChange(String.valueOf(value));
    }

    private int getAccessibilityDaltonizerValue() {
        final int daltonizerValue =
                DaltonizerPreferenceUtil.getSecureAccessibilityDaltonizerValue(mContentResolver);
        return daltonizerValue;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final int daltonizerValueInSetting = getAccessibilityDaltonizerValue();
        final int preferenceValue = getDaltonizerValueToKeyMap().get(mPreference.getKey());
        mPreference.setChecked(preferenceValue == daltonizerValueInSetting);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        mContentResolver.registerContentObserver(
                Settings.Secure.getUriFor(DALTONIZER_TYPE_SETTINGS_KEY),
                /* notifyForDescendants= */ false,
                mSettingsContentObserver
        );
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        mContentResolver.unregisterContentObserver(mSettingsContentObserver);
    }
}
