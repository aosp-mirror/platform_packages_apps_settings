/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

public class HighFrequencyDisplayPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String HIGH_FREQUENCY_DISPLAY_KEY = "high_frequency_display_device";

    private static final String SURFACE_FLINGER_SERVICE_KEY = "SurfaceFlinger";
    private static final String SURFACE_COMPOSER_INTERFACE_KEY = "android.ui.ISurfaceComposer";
    private static final int SURFACE_FLINGER_HIGH_FREQUENCY_DISPLAY_CODE = 1029;

    private final IBinder mSurfaceFlingerBinder;

    public HighFrequencyDisplayPreferenceController(Context context) {
        super(context);
        mSurfaceFlingerBinder = ServiceManager.getService(SURFACE_FLINGER_SERVICE_KEY);
    }

    @Override
    public String getPreferenceKey() {
        return HIGH_FREQUENCY_DISPLAY_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Boolean isEnabled = (Boolean) newValue;
        writeHighFrequencyDisplaySetting(isEnabled);
        ((SwitchPreference) preference).setChecked(isEnabled);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        boolean enableHighFrequencyPanel = readHighFrequencyDisplaySetting();
        ((SwitchPreference) preference).setChecked(enableHighFrequencyPanel);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeHighFrequencyDisplaySetting(false);
        ((SwitchPreference) mPreference).setChecked(false);
    }

    @VisibleForTesting
    boolean readHighFrequencyDisplaySetting() {
        boolean isEnabled = false;
        try {
            if (mSurfaceFlingerBinder != null) {
                final Parcel data = Parcel.obtain();
                final Parcel result = Parcel.obtain();
                data.writeInterfaceToken(SURFACE_COMPOSER_INTERFACE_KEY);
                data.writeInt(0);
                data.writeInt(0);
                mSurfaceFlingerBinder.transact(
                        SURFACE_FLINGER_HIGH_FREQUENCY_DISPLAY_CODE,
                        data, result, 0);

                if (result.readInt() != 1 || result.readInt() != 1) {
                    isEnabled = true;
                }
            }
        } catch (RemoteException ex) {
            // intentional no-op
        }
        return isEnabled;
    }

    @VisibleForTesting
    void writeHighFrequencyDisplaySetting(boolean isEnabled) {
        int multiplier;
        int divisor;

        if (isEnabled) {
            // 60Hz * 3/2 = 90Hz
            multiplier = 2;
            divisor = 3;
        } else {
            multiplier = 1;
            divisor = 1;
        }

        try {
            if (mSurfaceFlingerBinder != null) {
                final Parcel data = Parcel.obtain();
                data.writeInterfaceToken(SURFACE_COMPOSER_INTERFACE_KEY);
                data.writeInt(multiplier);
                data.writeInt(divisor);
                mSurfaceFlingerBinder.transact(
                        SURFACE_FLINGER_HIGH_FREQUENCY_DISPLAY_CODE,
                        data, null, 0);
            }
        } catch (RemoteException ex) {
            // intentional no-op
        }
    }
}
