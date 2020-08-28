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
import android.provider.Settings;

import androidx.lifecycle.LifecycleObserver;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.RadioButtonPreference;

import com.google.common.primitives.Ints;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller class that control accessibility time out settings.
 */
public class AccessibilityTimeoutController extends AbstractPreferenceController implements
        LifecycleObserver, RadioButtonPreference.OnClickListener, PreferenceControllerMixin {
    static final String CONTENT_TIMEOUT_SETTINGS_SECURE =
            Settings.Secure.ACCESSIBILITY_NON_INTERACTIVE_UI_TIMEOUT_MS;
    static final String CONTROL_TIMEOUT_SETTINGS_SECURE =
            Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS;

    // pair the preference key and timeout value.
    private final Map<String, Integer> mAccessibilityTimeoutKeyToValueMap = new HashMap<>();

    // RadioButtonPreference key, each preference represent a timeout value.
    private final String mPreferenceKey;
    private final ContentResolver mContentResolver;
    private final Resources mResources;
    private OnChangeListener mOnChangeListener;
    private RadioButtonPreference mPreference;
    private int mAccessibilityUiTimeoutValue;

    public AccessibilityTimeoutController(Context context, Lifecycle lifecycle,
            String preferenceKey) {
        super(context);

        mContentResolver = context.getContentResolver();
        mResources = context.getResources();

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mPreferenceKey = preferenceKey;
    }

    protected static int getSecureAccessibilityTimeoutValue(ContentResolver resolver, String name) {
        String timeOutSec = Settings.Secure.getString(resolver, name);
        if (timeOutSec == null) {
            return 0;
        }
        Integer timeOutValue = Ints.tryParse(timeOutSec);
        return timeOutValue == null ? 0 : timeOutValue;
    }

    public void setOnChangeListener(OnChangeListener listener) {
        mOnChangeListener = listener;
    }

    private Map<String, Integer> getTimeoutValueToKeyMap() {
        if (mAccessibilityTimeoutKeyToValueMap.size() == 0) {

            String[] timeoutKeys = mResources.getStringArray(
                    R.array.accessibility_timeout_control_selector_keys);

            int[] timeoutValues = mResources.getIntArray(
                    R.array.accessibility_timeout_selector_values);

            final int timeoutValueCount = timeoutValues.length;
            for (int i = 0; i < timeoutValueCount; i++) {
                mAccessibilityTimeoutKeyToValueMap.put(timeoutKeys[i], timeoutValues[i]);
            }
        }
        return mAccessibilityTimeoutKeyToValueMap;
    }

    private void putSecureString(String name, String value) {
        Settings.Secure.putString(mContentResolver, name, value);
    }

    private void handlePreferenceChange(String value) {
        // save value to both content and control timeout setting.
        putSecureString(Settings.Secure.ACCESSIBILITY_NON_INTERACTIVE_UI_TIMEOUT_MS, value);
        putSecureString(Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS, value);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return mPreferenceKey;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (RadioButtonPreference)
                screen.findPreference(getPreferenceKey());
        mPreference.setOnClickListener(this);
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
        int value = getTimeoutValueToKeyMap().get(mPreferenceKey);
        handlePreferenceChange(String.valueOf(value));
        if (mOnChangeListener != null) {
            mOnChangeListener.onCheckedChanged(mPreference);
        }
    }

    private int getAccessibilityTimeoutValue() {
        // get accessibility control timeout value
        int timeoutValue = getSecureAccessibilityTimeoutValue(mContentResolver,
                CONTROL_TIMEOUT_SETTINGS_SECURE);
        return timeoutValue;
    }

    protected void updatePreferenceCheckedState(int value) {
        if (mAccessibilityUiTimeoutValue == value) {
            mPreference.setChecked(true);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        mAccessibilityUiTimeoutValue = getAccessibilityTimeoutValue();

        // reset RadioButton
        mPreference.setChecked(false);
        int preferenceValue = getTimeoutValueToKeyMap().get(mPreference.getKey());
        updatePreferenceCheckedState(preferenceValue);
    }

    /**
     * Listener interface handles checked event.
     */
    public interface OnChangeListener {
        /**
         * A hook that is called when preference checked.
         */
        void onCheckedChanged(Preference preference);
    }
}
