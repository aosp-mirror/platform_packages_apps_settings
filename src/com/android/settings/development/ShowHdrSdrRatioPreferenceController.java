/**
 * Copyright (C) 2023 The Android Open Source Project
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
import android.hardware.display.DisplayManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.Display;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.flags.Flags;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Controller class for controlling the hdr/sdr ratio on SurfaceFlinger
 */
public class ShowHdrSdrRatioPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String SHOW_REFRESH_RATE_KEY = "show_hdr_sdr_ratio";

    private static final int SETTING_VALUE_QUERY = 2;
    private static final int SETTING_VALUE_ON = 1;
    private static final int SETTING_VALUE_OFF = 0;

    private static final String SURFACE_FLINGER_SERVICE_KEY = "SurfaceFlinger";

    private static final int SURFACE_FLINGER_CODE = 1043;

    private static final String SURFACE_COMPOSER_INTERFACE_KEY = "android.ui.ISurfaceComposer";

    private final IBinder mSurfaceFlinger;

    private final boolean mIsHdrSdrRatioAvailable;

    public ShowHdrSdrRatioPreferenceController(Context context) {
        super(context);
        mSurfaceFlinger = ServiceManager.getService(SURFACE_FLINGER_SERVICE_KEY);
        DisplayManager displayManager = context.getSystemService(DisplayManager.class);
        Display display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        mIsHdrSdrRatioAvailable = display != null && display.isHdrSdrRatioAvailable();
    }

    @VisibleForTesting
    ShowHdrSdrRatioPreferenceController(Context context, IBinder surfaceFlinger,
                                        boolean isHdrSdrRatioAvailable) {
        super(context);
        mSurfaceFlinger = surfaceFlinger;
        mIsHdrSdrRatioAvailable = isHdrSdrRatioAvailable;
    }

    @Override
    public String getPreferenceKey() {
        return SHOW_REFRESH_RATE_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        writeShowHdrSdrRatioSetting(isEnabled);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        updateShowHdrSdrRatioSetting();
    }

    @Override
    public boolean isAvailable() {
        return Flags.developmentHdrSdrRatio() && mIsHdrSdrRatioAvailable;
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        final TwoStatePreference preference = (TwoStatePreference) mPreference;
        if (preference.isChecked()) {
            // Writing false to the preference when the setting is already off will have a
            // side effect of turning on the preference that we wish to avoid
            writeShowHdrSdrRatioSetting(false);
            preference.setChecked(false);
        }
    }

    private void updateShowHdrSdrRatioSetting() {
        // magic communication with surface flinger.
        try {
            if (mSurfaceFlinger != null) {
                final Parcel data = Parcel.obtain();
                final Parcel reply = Parcel.obtain();
                data.writeInterfaceToken(SURFACE_COMPOSER_INTERFACE_KEY);
                data.writeInt(SETTING_VALUE_QUERY);
                mSurfaceFlinger.transact(SURFACE_FLINGER_CODE, data, reply, 0 /* flags */);
                final boolean enabled = reply.readBoolean();
                ((TwoStatePreference) mPreference).setChecked(enabled);
                reply.recycle();
                data.recycle();
            }
        } catch (RemoteException ex) {
            // intentional no-op
        }
    }

    @VisibleForTesting
    void writeShowHdrSdrRatioSetting(boolean isEnabled) {
        try {
            if (mSurfaceFlinger != null) {
                final Parcel data = Parcel.obtain();
                data.writeInterfaceToken(SURFACE_COMPOSER_INTERFACE_KEY);
                final int showHdrSdrRatio = isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF;
                data.writeInt(showHdrSdrRatio);
                mSurfaceFlinger.transact(SURFACE_FLINGER_CODE, data,
                        null /* reply */, 0 /* flags */);
                data.recycle();
            }
        } catch (RemoteException ex) {
            // intentional no-op
        }
        updateShowHdrSdrRatioSetting();
    }
}
