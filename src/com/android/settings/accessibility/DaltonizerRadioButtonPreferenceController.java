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
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import androidx.lifecycle.LifecycleObserver;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.RadioButtonPreference;

import com.google.common.primitives.Ints;

import java.util.HashMap;
import java.util.Map;

/** Controller class that control radio button of accessibility daltonizer settings. */
public class DaltonizerRadioButtonPreferenceController extends BasePreferenceController implements
        LifecycleObserver, RadioButtonPreference.OnClickListener {
    private static final String TYPE = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER;

    // pair the preference key and daltonizer value.
    private final Map<String, Integer> mAccessibilityDaltonizerKeyToValueMap = new HashMap<>();

    // RadioButtonPreference key, each preference represent a daltonizer value.
    private final ContentResolver mContentResolver;
    private final Resources mResources;
    private DaltonizerRadioButtonPreferenceController.OnChangeListener mOnChangeListener;
    private RadioButtonPreference mPreference;
    private int mAccessibilityDaltonizerValue;

    public DaltonizerRadioButtonPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mContentResolver = context.getContentResolver();
        mResources = context.getResources();
    }

    public DaltonizerRadioButtonPreferenceController(Context context, Lifecycle lifecycle,
            String preferenceKey) {
        super(context, preferenceKey);

        mContentResolver = context.getContentResolver();
        mResources = context.getResources();

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    protected static int getSecureAccessibilityDaltonizerValue(ContentResolver resolver,
            String name) {
        final String daltonizerStringValue = Settings.Secure.getString(resolver, name);
        if (daltonizerStringValue == null) {
            return AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY;
        }
        final Integer daltonizerIntValue = Ints.tryParse(daltonizerStringValue);
        return daltonizerIntValue == null ? AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY
                : daltonizerIntValue;
    }

    public void setOnChangeListener(
            DaltonizerRadioButtonPreferenceController.OnChangeListener listener) {
        mOnChangeListener = listener;
    }

    private Map<String, Integer> getDaltonizerValueToKeyMap() {
        if (mAccessibilityDaltonizerKeyToValueMap.size() == 0) {

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

    private void putSecureString(String name, String value) {
        Settings.Secure.putString(mContentResolver, name, value);
    }

    private void handlePreferenceChange(String value) {
        putSecureString(TYPE, value);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (RadioButtonPreference)
                screen.findPreference(getPreferenceKey());
        mPreference.setOnClickListener(this);
        mPreference.setAppendixVisibility(View.GONE);
        updateState(mPreference);
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
        final int value = getDaltonizerValueToKeyMap().get(mPreferenceKey);
        handlePreferenceChange(String.valueOf(value));
        if (mOnChangeListener != null) {
            mOnChangeListener.onCheckedChanged(mPreference);
        }
    }

    private int getAccessibilityDaltonizerValue() {
        final int daltonizerValue = getSecureAccessibilityDaltonizerValue(mContentResolver,
                TYPE);
        return daltonizerValue;
    }

    protected void updatePreferenceCheckedState(int value) {
        if (mAccessibilityDaltonizerValue == value) {
            mPreference.setChecked(true);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mAccessibilityDaltonizerValue = getAccessibilityDaltonizerValue();

        // reset RadioButton
        mPreference.setChecked(false);
        final int preferenceValue = getDaltonizerValueToKeyMap().get(mPreference.getKey());
        updatePreferenceCheckedState(preferenceValue);
    }

    /** Listener interface handles checked event. */
    public interface OnChangeListener {
        /** A hook that is called when preference checked. */
        void onCheckedChanged(Preference preference);
    }
}
