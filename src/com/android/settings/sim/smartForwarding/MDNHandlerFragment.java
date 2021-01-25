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

import static com.android.settings.sim.smartForwarding.MDNHandlerHeaderFragment.KEY_SLOT0_PHONE_NUMBER;
import static com.android.settings.sim.smartForwarding.MDNHandlerHeaderFragment.KEY_SLOT1_PHONE_NUMBER;

import android.app.AlertDialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settingslib.core.instrumentation.Instrumentable;

public class MDNHandlerFragment extends Fragment implements Instrumentable {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.xml.smart_forwarding_mdn_handler, container, false);
        getActivity().getActionBar().setTitle(
                getResources().getString(R.string.smart_forwarding_input_mdn_title));

        Button processBtn = view.findViewById(R.id.process);
        processBtn.setOnClickListener((View v)-> {
            pressButtonOnClick();
        });

        Button cancelBtn = view.findViewById(R.id.cancel);
        cancelBtn.setOnClickListener((View v)-> {
            switchToMainFragment(true);
        });
        return view;
    }

    private void pressButtonOnClick() {
        // Get the phone number from the UI
        MDNHandlerHeaderFragment fragment = (MDNHandlerHeaderFragment) this
                .getChildFragmentManager()
                .findFragmentById(R.id.fragment_settings);

        String slot0Number = "";
        String slot1Number = "";
        if (fragment != null) {
            slot0Number = fragment.findPreference(KEY_SLOT0_PHONE_NUMBER)
                    .getSummary().toString();
            slot1Number = fragment.findPreference(KEY_SLOT1_PHONE_NUMBER)
                    .getSummary().toString();
        }
        final String[] phoneNumber = {slot1Number, slot0Number};

        // If phone number is empty, popup an alert dialog
        if(TextUtils.isEmpty(phoneNumber[0])
                || TextUtils.isEmpty(phoneNumber[1])) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.smart_forwarding_failed_title)
                    .setMessage(R.string.smart_forwarding_missing_mdn_text)
                    .setPositiveButton(
                            R.string.smart_forwarding_missing_alert_dialog_text,
                            (dialog, which) -> { dialog.dismiss(); })
                    .create()
                    .show();
        } else {
            switchToMainFragment(false);
            ((SmartForwardingActivity)getActivity()).enableSmartForwarding(phoneNumber);
        }
    }

    private void switchToMainFragment(boolean turnoffSwitch) {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, new SmartForwardingFragment(turnoffSwitch))
                .commit();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MOBILE_NETWORK;
    }
}
