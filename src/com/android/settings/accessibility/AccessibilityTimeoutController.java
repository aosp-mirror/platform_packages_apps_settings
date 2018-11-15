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
import com.android.settings.widget.RadioButtonPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.lang.Integer;

import java.util.HashMap;
import java.util.Map;

public class AccessibilityTimeoutController extends AbstractPreferenceController implements
        LifecycleObserver, RadioButtonPreference.OnClickListener, PreferenceControllerMixin {

    // pair the preference key and timeout value
    private final Map<String, Integer> mAccessibilityTimeoutKeyToValueMap = new HashMap<>();

    private final String mPreferenceKey;
    private final String mfragmentTag;
    private final ContentResolver mContentResolver;
    private final Resources mResources;
    private OnChangeListener mOnChangeListener;
    private RadioButtonPreference mPreference;
    private int mAccessibilityUiTimeoutValue;

    public AccessibilityTimeoutController(Context context, Lifecycle lifecycle,
            String preferenceKey, String fragmentTag) {
        super(context);

        mContentResolver = context.getContentResolver();
        mResources = context.getResources();

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mPreferenceKey = preferenceKey;
        mfragmentTag = fragmentTag;
    }

    public void setOnChangeListener(OnChangeListener listener) {
        mOnChangeListener = listener;
    }

    private Map<String, Integer> getTimeoutValueToKeyMap() {
        if (mAccessibilityTimeoutKeyToValueMap.size() == 0) {

            String[] timeoutKeys = null;
            if (mfragmentTag.equals(AccessibilityContentTimeoutPreferenceFragment.TAG)) {
                timeoutKeys = mResources.getStringArray(
                        R.array.accessibility_timeout_content_selector_keys);
            } else if (mfragmentTag.equals(AccessibilityControlTimeoutPreferenceFragment.TAG)) {
                timeoutKeys = mResources.getStringArray(
                        R.array.accessibility_timeout_control_selector_keys);
            }

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
        if (mfragmentTag.equals(AccessibilityContentTimeoutPreferenceFragment.TAG)) {
            putSecureString(Settings.Secure.ACCESSIBILITY_NON_INTERACTIVE_UI_TIMEOUT_MS, value);
        } else if (mfragmentTag.equals(AccessibilityControlTimeoutPreferenceFragment.TAG)) {
            putSecureString(Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS, value);
        }
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

    protected void getAccessibilityUiValue() {
        String timeoutValue = null;
        if (mfragmentTag.equals(AccessibilityContentTimeoutPreferenceFragment.TAG)) {
            timeoutValue = Settings.Secure.getString(mContentResolver,
                    Settings.Secure.ACCESSIBILITY_NON_INTERACTIVE_UI_TIMEOUT_MS);
        } else if (mfragmentTag.equals(AccessibilityControlTimeoutPreferenceFragment.TAG)) {
            timeoutValue = Settings.Secure.getString(mContentResolver,
                    Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS);
        }
        mAccessibilityUiTimeoutValue = timeoutValue == null? 0: Integer.parseInt(timeoutValue);
    }

    protected void updatePreferenceCheckedState(int value) {
        if (mAccessibilityUiTimeoutValue == value) {
            mPreference.setChecked(true);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        getAccessibilityUiValue();

        // reset RadioButton
        mPreference.setChecked(false);
        int preferenceValue = getTimeoutValueToKeyMap().get(mPreference.getKey());
        updatePreferenceCheckedState(preferenceValue);
    }

    public static interface OnChangeListener {
        void onCheckedChanged(Preference preference);
    }
}