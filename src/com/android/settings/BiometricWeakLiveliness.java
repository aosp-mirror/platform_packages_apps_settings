/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.content.Intent;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.android.settings.R;

import com.android.internal.widget.LockPatternUtils;

public class BiometricWeakLiveliness extends Fragment
        implements CompoundButton.OnCheckedChangeListener {
    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF = 125;

    private View mView;
    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockPatternUtils;
    private Switch mActionBarSwitch;
    private boolean mSuppressCheckChanged;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();

        mActionBarSwitch = new Switch(activity);
        mSuppressCheckChanged = false;

        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
                final int padding = activity.getResources().getDimensionPixelSize(
                        R.dimen.action_bar_switch_padding);
                mActionBarSwitch.setPadding(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(mActionBarSwitch, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.RIGHT));
                activity.getActionBar().setTitle(R.string.biometric_weak_liveliness_title);
            }
        }

        mActionBarSwitch.setOnCheckedChangeListener(this);

        mLockPatternUtils = new LockPatternUtils(getActivity());
        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        mActionBarSwitch.setChecked(mLockPatternUtils.isBiometricWeakLivelinessEnabled());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.biometric_weak_liveliness, container, false);
        initView(mView);
        return mView;
    }

    private void initView(View view) {
        mActionBarSwitch.setOnCheckedChangeListener(this);
        mActionBarSwitch.setChecked(mLockPatternUtils.isBiometricWeakLivelinessEnabled());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean desiredState) {
        if (mSuppressCheckChanged) {
            return;
        }
        mActionBarSwitch.setEnabled(false);
        if (desiredState) {
            mLockPatternUtils.setBiometricWeakLivelinessEnabled(true);
            mActionBarSwitch.setChecked(true);
        } else {
            // In this case the user has just turned it off, but this action requires them
            // to confirm their password.  We need to turn the switch back on until
            // they've confirmed their password
            mActionBarSwitch.setChecked(true);
            mActionBarSwitch.requestLayout();
            ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(this.getActivity(), this);
            if (!helper.launchConfirmationActivity(
                            CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF, null, null)) {
                // If this returns false, it means no password confirmation is required, so
                // go ahead and turn it off here.
                // Note: currently a backup is required for biometric_weak so this code path
                // can't be reached, but is here in case things change in the future
                mLockPatternUtils.setBiometricWeakLivelinessEnabled(false);
                mActionBarSwitch.setChecked(false);
                mActionBarSwitch.requestLayout();
            }
        }
        mActionBarSwitch.setEnabled(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF &&
                resultCode == Activity.RESULT_OK) {
            final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
            lockPatternUtils.setBiometricWeakLivelinessEnabled(false);
            mSuppressCheckChanged = true;
            mActionBarSwitch.setChecked(false);
            mSuppressCheckChanged = false;
            return;
        }
    }

}
