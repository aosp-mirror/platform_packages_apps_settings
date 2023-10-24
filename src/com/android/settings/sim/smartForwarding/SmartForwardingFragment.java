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

import static com.android.settings.sim.smartForwarding.SmartForwardingUtils.TAG;
import static com.android.settings.sim.smartForwarding.SmartForwardingUtils.getPhoneNumber;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settingslib.core.instrumentation.Instrumentable;

public class SmartForwardingFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener, Instrumentable {

    private static final String KEY_SMART_FORWARDING_SWITCH = "smart_forwarding_switch";
    private boolean turnOffSwitch;

    public SmartForwardingFragment() { }

    public SmartForwardingFragment(boolean turnOffSwitch) {
        this.turnOffSwitch = turnOffSwitch;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.smart_forwarding_switch, rootKey);

        String title = getResources().getString(R.string.smart_forwarding_title);
        getActivity().getActionBar().setTitle(title);

        TwoStatePreference smartForwardingSwitch = findPreference(KEY_SMART_FORWARDING_SWITCH);
        if (turnOffSwitch) {
            smartForwardingSwitch.setChecked(false);
        }
        smartForwardingSwitch.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (boolean) newValue;

        Log.d(TAG, "onPreferenceChange. Update value to " + value);

        if (value) {
            String slot0PhoneNumber = getPhoneNumber(getContext(), 0);
            String slot1PhoneNumber = getPhoneNumber(getContext(), 1);

            String[] phoneNumber = new String[]{slot1PhoneNumber, slot0PhoneNumber};

            if (TextUtils.isEmpty(slot0PhoneNumber) || TextUtils.isEmpty(slot1PhoneNumber)) {
                Log.d(TAG, "Slot 0 or Slot 1 phone number missing.");
                switchToMDNFragment();
            } else {
                ((SmartForwardingActivity) getActivity()).enableSmartForwarding(phoneNumber);
            }
            return false;
        } else {
            ((SmartForwardingActivity) getActivity()).disableSmartForwarding();
        }

        return true;
    }


    private void switchToMDNFragment() {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, new MDNHandlerFragment())
                .commit();
    }

    public void turnOnSwitchPreference() {
        TwoStatePreference smartForwardingSwitch = findPreference(KEY_SMART_FORWARDING_SWITCH);
        smartForwardingSwitch.setChecked(true);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MOBILE_NETWORK;
    }
}
