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

package com.android.settings.security;

import android.app.KeyguardManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.dpp.WifiDppUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/** Enable/disable user confirmation before deleting an eSim */
public class ConfirmSimDeletionPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener{
    public static final String KEY_CONFIRM_SIM_DELETION = "confirm_sim_deletion";
    private boolean mConfirmationDefaultOn;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    public ConfirmSimDeletionPreferenceController(Context context, String key) {
        super(context, key);
        mConfirmationDefaultOn =
                context.getResources()
                        .getBoolean(R.bool.config_sim_deletion_confirmation_default_on);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        // hide if eSim is not supported on the device
        return MobileNetworkUtils.showEuiccSettings(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    private boolean getGlobalState() {
        return Settings.Global.getInt(
                        mContext.getContentResolver(),
                        KEY_CONFIRM_SIM_DELETION,
                        mConfirmationDefaultOn ? 1 : 0)
                == 1;
    }

    public boolean isChecked() {
        return getGlobalState();
    }

    public boolean setChecked(boolean isChecked) {
        Settings.Global.putInt(
                mContext.getContentResolver(), KEY_CONFIRM_SIM_DELETION, isChecked ? 1 : 0);
        return true;
    }

    // handle UI change
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!preference.getKey().equals(getPreferenceKey())) {
            return false;
        }
        if (!isChecked()) {
            mMetricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_CONFIRM_SIM_DELETION_ON);
            setChecked(true);
            return true;
        } else {
            // prevent disabling the feature until authorized
            WifiDppUtils.showLockScreen(mContext, () -> {
                mMetricsFeatureProvider.action(mContext,
                        SettingsEnums.ACTION_CONFIRM_SIM_DELETION_OFF);
                // set data
                setChecked(false);
                // set UI
                ((TwoStatePreference) preference).setChecked(false);
            });
            return false;
        }
    }

    @Override
    public void updateState(Preference preference) {
        final KeyguardManager keyguardManager = mContext.getSystemService(KeyguardManager.class);
        if (!keyguardManager.isKeyguardSecure()) {
            preference.setEnabled(false);
            if (preference instanceof TwoStatePreference) {
                ((TwoStatePreference) preference).setChecked(false);
            }
            preference.setSummary(R.string.disabled_because_no_backup_security);
        } else {
            preference.setEnabled(true);
            if (preference instanceof TwoStatePreference) {
                ((TwoStatePreference) preference).setChecked(getGlobalState());
            }
            preference.setSummary(R.string.confirm_sim_deletion_description);
        }
    }
}
