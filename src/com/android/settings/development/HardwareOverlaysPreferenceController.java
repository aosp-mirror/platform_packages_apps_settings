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

package com.android.settings.development;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class HardwareOverlaysPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String DISABLE_OVERLAYS_KEY = "disable_overlays";

    private static final int SETTING_VALUE_ON = 1;
    private static final int SETTING_VALUE_OFF = 0;
    private static final String SURFACE_FLINGER_SERVICE_KEY = "SurfaceFlinger";

    @VisibleForTesting
    static final int SURFACE_FLINGER_READ_CODE = 1010;

    private static final int SURFACE_FLINGER_DISABLE_OVERLAYS_CODE = 1008;
    private static final String SURFACE_COMPOSER_INTERFACE_KEY = "android.ui.ISurfaceComposer";

    private final IBinder mSurfaceFlinger;

    public HardwareOverlaysPreferenceController(Context context) {
        super(context);
        mSurfaceFlinger = ServiceManager.getService(SURFACE_FLINGER_SERVICE_KEY);
    }

    @Override
    public String getPreferenceKey() {
        return DISABLE_OVERLAYS_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        writeHardwareOverlaysSetting(isEnabled);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateHardwareOverlaysSetting();
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        final TwoStatePreference switchPreference = (TwoStatePreference) mPreference;
        if (switchPreference.isChecked()) {
            // Writing false to the preference when the setting is already off will have a
            // side effect of turning on the preference that we wish to avoid
            writeHardwareOverlaysSetting(false);
            switchPreference.setChecked(false);
        }
    }

    @VisibleForTesting
    void updateHardwareOverlaysSetting() {
        if (mSurfaceFlinger == null) {
            return;
        }
        // magic communication with surface flinger.
        try {
            final Parcel data = Parcel.obtain();
            final Parcel reply = Parcel.obtain();
            data.writeInterfaceToken(SURFACE_COMPOSER_INTERFACE_KEY);
            mSurfaceFlinger.transact(SURFACE_FLINGER_READ_CODE, data, reply, 0 /* flags */);
            @SuppressWarnings("unused") final int showCpu = reply.readInt();
            @SuppressWarnings("unused") final int enableGL = reply.readInt();
            @SuppressWarnings("unused") final int showUpdates = reply.readInt();
            @SuppressWarnings("unused") final int showBackground = reply.readInt();
            final int disableOverlays = reply.readInt();
            ((TwoStatePreference) mPreference).setChecked(disableOverlays != SETTING_VALUE_OFF);
            reply.recycle();
            data.recycle();
        } catch (RemoteException ex) {
            // intentional no-op
        }
    }

    @VisibleForTesting
    void writeHardwareOverlaysSetting(boolean isEnabled) {
        if (mSurfaceFlinger == null) {
            return;
        }
        try {
            final Parcel data = Parcel.obtain();
            data.writeInterfaceToken(SURFACE_COMPOSER_INTERFACE_KEY);
            final int disableOverlays = isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF;
            data.writeInt(disableOverlays);
            mSurfaceFlinger.transact(SURFACE_FLINGER_DISABLE_OVERLAYS_CODE, data,
                    null /* reply */, 0 /* flags */);
            data.recycle();
        } catch (RemoteException ex) {
            // intentional no-op
        }
        updateHardwareOverlaysSetting();
    }
}
