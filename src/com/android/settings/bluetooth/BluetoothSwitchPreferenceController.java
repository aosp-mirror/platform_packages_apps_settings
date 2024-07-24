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
package com.android.settings.bluetooth;

import static com.android.settings.bluetooth.BluetoothAutoOnPreferenceController.SETTING_NAME;
import static com.android.settings.bluetooth.BluetoothAutoOnPreferenceController.UNSET;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.location.BluetoothScanningFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.flags.Flags;
import com.android.settingslib.widget.FooterPreference;

/**
 * PreferenceController to update of bluetooth state. All behavior except managing the footer text
 * is delegated to the SwitchWidgetController it uses.
 */
public class BluetoothSwitchPreferenceController
        implements LifecycleObserver,
                OnStart,
                OnStop,
                SwitchWidgetController.OnSwitchChangeListener,
                View.OnClickListener {

    private BluetoothEnabler mBluetoothEnabler;
    private RestrictionUtils mRestrictionUtils;
    private SwitchWidgetController mSwitch;
    private Context mContext;
    private FooterPreference mFooterPreference;
    private boolean mIsAlwaysDiscoverable;

    @VisibleForTesting AlwaysDiscoverable mAlwaysDiscoverable;

    public BluetoothSwitchPreferenceController(
            Context context,
            SwitchWidgetController switchController,
            FooterPreference footerPreference) {
        this(context, new RestrictionUtils(), switchController, footerPreference);
    }

    @VisibleForTesting
    public BluetoothSwitchPreferenceController(
            Context context,
            RestrictionUtils restrictionUtils,
            SwitchWidgetController switchController,
            FooterPreference footerPreference) {
        mRestrictionUtils = restrictionUtils;
        mSwitch = switchController;
        mContext = context;
        mFooterPreference = footerPreference;

        mSwitch.setupView();
        updateText(mSwitch.isChecked());

        mBluetoothEnabler =
                new BluetoothEnabler(
                        context,
                        switchController,
                        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider(),
                        SettingsEnums.ACTION_SETTINGS_MASTER_SWITCH_BLUETOOTH_TOGGLE,
                        mRestrictionUtils);
        mBluetoothEnabler.setToggleCallback(this);
        mAlwaysDiscoverable = new AlwaysDiscoverable(context);
    }

    @Override
    public void onStart() {
        mBluetoothEnabler.resume(mContext);
        if (mIsAlwaysDiscoverable) {
            mAlwaysDiscoverable.start();
        }
        if (mSwitch != null) {
            updateText(mSwitch.isChecked());
        }
    }

    @Override
    public void onStop() {
        mBluetoothEnabler.pause();
        if (mIsAlwaysDiscoverable) {
            mAlwaysDiscoverable.stop();
        }
    }

    /**
     * Set whether the device can be discovered. By default the value will be {@code false}.
     *
     * @param isAlwaysDiscoverable {@code true} if the device can be discovered, otherwise {@code
     *     false}
     */
    public void setAlwaysDiscoverable(boolean isAlwaysDiscoverable) {
        mIsAlwaysDiscoverable = isAlwaysDiscoverable;
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        updateText(isChecked);
        return true;
    }

    @Override
    public void onClick(View v) {
        // send users to scanning settings if they click on the link in the summary text
        new SubSettingLauncher(mContext)
                .setDestination(BluetoothScanningFragment.class.getName())
                .setSourceMetricsCategory(SettingsEnums.BLUETOOTH_FRAGMENT)
                .launch();
    }

    @VisibleForTesting
    void updateText(boolean isChecked) {
        if (!isChecked && Utils.isBluetoothScanningEnabled(mContext)) {
            if (isAutoOnFeatureAvailable()) {
                mFooterPreference.setTitle(
                        R.string.bluetooth_scanning_on_info_message_auto_on_available);
            } else {
                mFooterPreference.setTitle(R.string.bluetooth_scanning_on_info_message);
            }
            mFooterPreference.setLearnMoreText(mContext.getString(R.string.bluetooth_scan_change));
            mFooterPreference.setLearnMoreAction(v -> onClick(v));
        } else {
            if (isAutoOnFeatureAvailable()) {
                mFooterPreference.setTitle(
                        R.string.bluetooth_empty_list_bluetooth_off_auto_on_available);
            } else {
                mFooterPreference.setTitle(R.string.bluetooth_empty_list_bluetooth_off);
            }
            mFooterPreference.setLearnMoreText("");
            mFooterPreference.setLearnMoreAction(null);
        }
    }

    private boolean isAutoOnFeatureAvailable() {
        if (!Flags.bluetoothQsTileDialogAutoOnToggle()) {
            return false;
        }
        return Settings.Secure.getIntForUser(
                        mContext.getContentResolver(), SETTING_NAME, UNSET, UserHandle.myUserId())
                != UNSET;
    }
}
