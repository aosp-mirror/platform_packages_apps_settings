/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.sim.smartForwarding;

import static com.android.settings.sim.smartForwarding.SmartForwardingUtils.getPhoneNumber;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.EditTextPreference.OnBindEditTextListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.settings.R;
import com.android.settingslib.core.instrumentation.Instrumentable;

public class MDNHandlerHeaderFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener, OnBindEditTextListener, Instrumentable {

    public static final String KEY_SLOT0_PHONE_NUMBER = "slot0_phone_number";
    public static final String KEY_SLOT1_PHONE_NUMBER = "slot1_phone_number";

    public MDNHandlerHeaderFragment() {}

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.smart_forwarding_mdn_handler_header, rootKey);

        EditTextPreference slot0EditText = findPreference(KEY_SLOT0_PHONE_NUMBER);
        slot0EditText.setOnBindEditTextListener(this);
        slot0EditText.setOnPreferenceChangeListener(this);
        String slot0PhoneNumber = getPhoneNumber(getContext(), 0);
        slot0EditText.setSummary(slot0PhoneNumber);
        slot0EditText.setText(slot0PhoneNumber);

        EditTextPreference slot1EditText = findPreference(KEY_SLOT1_PHONE_NUMBER);
        slot1EditText.setOnPreferenceChangeListener(this);
        slot1EditText.setOnBindEditTextListener(this);
        String slot1PhoneNumber = getPhoneNumber(getContext(), 1);
        slot1EditText.setSummary(slot1PhoneNumber);
        slot1EditText.setText(slot1PhoneNumber);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        preference.setSummary(newValue.toString());
        return true;
    }

    @Override
    public void onBindEditText(@NonNull EditText editText) {
        editText.setInputType(InputType.TYPE_CLASS_PHONE);
        editText.setSingleLine(true);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MOBILE_NETWORK;
    }
}
