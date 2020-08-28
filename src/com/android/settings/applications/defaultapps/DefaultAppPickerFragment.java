/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.defaultapps;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.RadioButtonPreference;

/**
 * A generic app picker fragment that shows a list of app as radio button group.
 */
public abstract class DefaultAppPickerFragment extends RadioButtonPickerFragment {

    protected PackageManager mPm;
    protected BatteryUtils mBatteryUtils;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPm = context.getPackageManager();
        mBatteryUtils = BatteryUtils.getInstance(context);
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference selected) {
        final String selectedKey = selected.getKey();
        final CharSequence confirmationMessage = getConfirmationMessage(getCandidate(selectedKey));
        final FragmentActivity activity = getActivity();
        if (TextUtils.isEmpty(confirmationMessage)) {
            super.onRadioButtonClicked(selected);
        } else if (activity != null) {
            final DialogFragment fragment =
                    newConfirmationDialogFragment(selectedKey, confirmationMessage);
            fragment.show(activity.getSupportFragmentManager(), ConfirmationDialogFragment.TAG);
        }
    }

    @Override
    protected void onRadioButtonConfirmed(String selectedKey) {
        mMetricsFeatureProvider.action(
                mMetricsFeatureProvider.getAttribution(getActivity()),
                SettingsEnums.ACTION_SETTINGS_UPDATE_DEFAULT_APP,
                getMetricsCategory(),
                selectedKey,
                0 /* value */);
        super.onRadioButtonConfirmed(selectedKey);
    }

    @Override
    public void bindPreferenceExtra(RadioButtonPreference pref,
            String key, CandidateInfo info, String defaultKey, String systemDefaultKey) {
        if (!(info instanceof DefaultAppInfo)) {
            return;
        }
        if (TextUtils.equals(systemDefaultKey, key)) {
            pref.setSummary(R.string.system_app);
        } else if (!TextUtils.isEmpty(((DefaultAppInfo) info).summary)) {
            pref.setSummary(((DefaultAppInfo) info).summary);
        }
    }

    protected ConfirmationDialogFragment newConfirmationDialogFragment(String selectedKey,
            CharSequence confirmationMessage) {
        final ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
        fragment.init(this, selectedKey, confirmationMessage);
        return fragment;
    }

    protected CharSequence getConfirmationMessage(CandidateInfo info) {
        return null;
    }

    public static class ConfirmationDialogFragment extends InstrumentedDialogFragment
            implements DialogInterface.OnClickListener {

        public static final String TAG = "DefaultAppConfirm";
        public static final String EXTRA_KEY = "extra_key";
        public static final String EXTRA_MESSAGE = "extra_message";

        private DialogInterface.OnClickListener mCancelListener;

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DEFAULT_APP_PICKER_CONFIRMATION_DIALOG;
        }

        /**
         * Initializes the fragment.
         *
         * <p>Should be called after it's constructed.
         */
        public void init(DefaultAppPickerFragment parent, String key, CharSequence message) {
            final Bundle argument = new Bundle();
            argument.putString(EXTRA_KEY, key);
            argument.putCharSequence(EXTRA_MESSAGE, message);
            setArguments(argument);
            setTargetFragment(parent, 0);
        }

        // TODO: add test case for cancelListener
        public void setCancelListener(DialogInterface.OnClickListener cancelListener) {
            this.mCancelListener = cancelListener;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle bundle = getArguments();
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setMessage(bundle.getCharSequence(EXTRA_MESSAGE))
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, mCancelListener);
            return builder.create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            final Fragment fragment = getTargetFragment();
            if (fragment instanceof DefaultAppPickerFragment) {
                final Bundle bundle = getArguments();
                ((DefaultAppPickerFragment) fragment).onRadioButtonConfirmed(
                        bundle.getString(EXTRA_KEY));
            }
        }
    }
}
