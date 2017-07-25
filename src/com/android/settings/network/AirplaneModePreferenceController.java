/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.network;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.AirplaneModeEnabler;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class AirplaneModePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause {

    public static final int REQUEST_CODE_EXIT_ECM = 1;

    public static final String KEY_TOGGLE_AIRPLANE = "toggle_airplane";

    private static final String EXIT_ECM_RESULT = "exit_ecm_result";

    private final Fragment mFragment;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private AirplaneModeEnabler mAirplaneModeEnabler;
    private SwitchPreference mAirplaneModePreference;


    public AirplaneModePreferenceController(Context context, Fragment hostFragment) {
        super(context);
        mFragment = hostFragment;
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_TOGGLE_AIRPLANE.equals(preference.getKey()) && Boolean.parseBoolean(
                SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
            // In ECM mode launch ECM app dialog
            if (mFragment != null) {
                mFragment.startActivityForResult(
                        new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                        REQUEST_CODE_EXIT_ECM);
            }
            return true;
        }

        return false;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        if (isAvailable()) {
            mAirplaneModePreference = (SwitchPreference) screen.findPreference(getPreferenceKey());
            if (mAirplaneModePreference != null) {
                mAirplaneModeEnabler = new AirplaneModeEnabler(mContext, mAirplaneModePreference,
                        mMetricsFeatureProvider);
            }
        } else {
            removePreference(screen, getPreferenceKey());
        }
    }

    @Override
    public boolean isAvailable() {
        return isAvailable(mContext);
    }

    public static boolean isAvailable(Context context) {
        return !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TOGGLE_AIRPLANE;
    }

    public void onResume() {
        mAirplaneModeEnabler.resume();
    }

    @Override
    public void onPause() {
        mAirplaneModeEnabler.pause();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EXIT_ECM) {
            Boolean isChoiceYes = data.getBooleanExtra(EXIT_ECM_RESULT, false);
            // Set Airplane mode based on the return value and checkbox state
            mAirplaneModeEnabler.setAirplaneModeInECM(isChoiceYes,
                    mAirplaneModePreference.isChecked());
        }
    }
}
