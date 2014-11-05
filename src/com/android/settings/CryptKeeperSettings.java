/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class CryptKeeperSettings extends Fragment {
    private static final String TAG = "CryptKeeper";

    private static final int KEYGUARD_REQUEST = 55;

    // Minimum battery charge level (in percent) to launch encryption.  If the battery charge is
    // lower than this, encryption should not be activated.
    private static final int MIN_BATTERY_LEVEL = 80;

    private View mContentView;
    private Button mInitiateButton;
    private View mPowerWarning;
    private View mBatteryWarning;
    private IntentFilter mIntentFilter;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                final int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                final int invalidCharger = intent.getIntExtra(
                    BatteryManager.EXTRA_INVALID_CHARGER, 0);

                final boolean levelOk = level >= MIN_BATTERY_LEVEL;
                final boolean pluggedOk =
                    ((plugged & BatteryManager.BATTERY_PLUGGED_ANY) != 0) &&
                     invalidCharger == 0;

                // Update UI elements based on power/battery status
                mInitiateButton.setEnabled(levelOk && pluggedOk);
                mPowerWarning.setVisibility(pluggedOk ? View.GONE : View.VISIBLE );
                mBatteryWarning.setVisibility(levelOk ? View.GONE : View.VISIBLE);
            }
        }
    };

    /**
     * If the user clicks to begin the reset sequence, we next require a
     * keyguard confirmation if the user has currently enabled one.  If there
     * is no keyguard available, we prompt the user to set a password.
     */
    private Button.OnClickListener mInitiateListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!runKeyguardConfirmation(KEYGUARD_REQUEST)) {
                // TODO replace (or follow) this dialog with an explicit launch into password UI
                new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.crypt_keeper_dialog_need_password_title)
                    .setMessage(R.string.crypt_keeper_dialog_need_password_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                    .show();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        mContentView = inflater.inflate(R.layout.crypt_keeper_settings, null);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);

        mInitiateButton = (Button) mContentView.findViewById(R.id.initiate_encrypt);
        mInitiateButton.setOnClickListener(mInitiateListener);
        mInitiateButton.setEnabled(false);

        mPowerWarning = mContentView.findViewById(R.id.warning_unplugged);
        mBatteryWarning = mContentView.findViewById(R.id.warning_low_charge);

        return mContentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mIntentReceiver, mIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mIntentReceiver);
    }

    /**
     * If encryption is already started, and this launched via a "start encryption" intent,
     * then exit immediately - it's already up and running, so there's no point in "starting" it.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        Intent intent = activity.getIntent();
        if (DevicePolicyManager.ACTION_START_ENCRYPTION.equals(intent.getAction())) {
            DevicePolicyManager dpm = (DevicePolicyManager)
                    activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm != null) {
                int status = dpm.getStorageEncryptionStatus();
                if (status != DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE) {
                    // There is nothing to do here, so simply finish() (which returns to caller)
                    activity.finish();
                }
            }
        }
    }

    /**
     * Keyguard validation is run using the standard {@link ConfirmLockPattern}
     * component as a subactivity
     * @param request the request code to be returned once confirmation finishes
     * @return true if confirmation launched
     */
    private boolean runKeyguardConfirmation(int request) {
        Resources res = getActivity().getResources();
        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);

        if (helper.utils().getKeyguardStoredPasswordQuality()
                == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
            showFinalConfirmation(StorageManager.CRYPT_TYPE_DEFAULT, "");
            return true;
        }

        return helper.launchConfirmationActivity(request, null,
                res.getText(R.string.crypt_keeper_confirm_encrypt),
                true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != KEYGUARD_REQUEST) {
            return;
        }

        // If the user entered a valid keyguard trace, present the final
        // confirmation prompt; otherwise, go back to the initial state.
        if (resultCode == Activity.RESULT_OK && data != null) {
            int type = data.getIntExtra(ChooseLockSettingsHelper.EXTRA_KEY_TYPE, -1);
            String password = data.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            if (!TextUtils.isEmpty(password)) {
                showFinalConfirmation(type, password);
            }
        }
    }

    private void showFinalConfirmation(int type, String password) {
        Preference preference = new Preference(getActivity());
        preference.setFragment(CryptKeeperConfirm.class.getName());
        preference.setTitle(R.string.crypt_keeper_confirm_title);
        preference.getExtras().putInt("type", type);
        preference.getExtras().putString("password", password);
        ((SettingsActivity) getActivity()).onPreferenceStartFragment(null, preference);
    }
}
