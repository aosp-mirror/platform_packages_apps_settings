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

import static android.provider.SettingsSlicesContract.KEY_AIRPLANE_MODE;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.SettingsSlicesContract;
import android.telephony.TelephonyManager;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.AirplaneModeEnabler;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class AirplaneModePreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop, OnDestroy,
        AirplaneModeEnabler.OnAirplaneModeChangedListener {

    public static final int REQUEST_CODE_EXIT_ECM = 1;

    /**
     * Uri for Airplane mode Slice.
     */
    public static final Uri SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(SettingsSlicesContract.KEY_AIRPLANE_MODE)
            .build();

    private Fragment mFragment;
    private AirplaneModeEnabler mAirplaneModeEnabler;
    private TwoStatePreference mAirplaneModePreference;

    public AirplaneModePreferenceController(Context context, String key) {
        super(context, key);

        if (isAvailable(mContext)) {
            mAirplaneModeEnabler = new AirplaneModeEnabler(mContext, this);
        }
    }

    public void setFragment(Fragment hostFragment) {
        mFragment = hostFragment;
    }

    @VisibleForTesting
    void setAirplaneModeEnabler(AirplaneModeEnabler airplaneModeEnabler) {
        mAirplaneModeEnabler = airplaneModeEnabler;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_AIRPLANE_MODE.equals(preference.getKey()) && isAvailable()
                && mAirplaneModeEnabler.isInEcmMode()) {
            // In ECM mode launch ECM app dialog
            if (mFragment != null) {
                mFragment.startActivityForResult(
                        new Intent(TelephonyManager.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                        REQUEST_CODE_EXIT_ECM);
            }
            return true;
        }

        return false;
    }

    @Override
    public Uri getSliceUri() {
        return SLICE_URI;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mAirplaneModePreference = screen.findPreference(getPreferenceKey());
    }

    public static boolean isAvailable(Context context) {
        return context.getResources().getBoolean(R.bool.config_show_toggle_airplane)
                && !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return isAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_network;
    }

    @Override
    public void onStart() {
        if (isAvailable()) {
            mAirplaneModeEnabler.start();
        }
    }

    @Override
    public void onStop() {
        if (isAvailable()) {
            mAirplaneModeEnabler.stop();
        }
    }

    @Override
    public void onDestroy() {
        if (isAvailable()) {
            mAirplaneModeEnabler.close();
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EXIT_ECM && isAvailable()) {
            final boolean isChoiceYes = (resultCode == Activity.RESULT_OK);
            // Set Airplane mode based on the return value and checkbox state
            mAirplaneModeEnabler.setAirplaneModeInECM(isChoiceYes,
                    mAirplaneModePreference.isChecked());
        }
    }

    @Override
    public boolean isChecked() {
        return isAvailable() && mAirplaneModeEnabler.isAirplaneModeOn();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked() == isChecked) {
            return false;
        }
        if (isAvailable()) {
            mAirplaneModeEnabler.setAirplaneMode(isChecked);
        }
        return true;
    }

    @Override
    public void onAirplaneModeChanged(boolean isAirplaneModeOn) {
        if (mAirplaneModePreference != null) {
            mAirplaneModePreference.setChecked(isAirplaneModeOn);
        }
    }
}
