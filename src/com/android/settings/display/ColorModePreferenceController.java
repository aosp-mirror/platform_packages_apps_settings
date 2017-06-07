/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.PreferenceController;

public class ColorModePreferenceController extends PreferenceController implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "ColorModePreference";

    private static final String KEY_COLOR_MODE = "color_mode";

    @VisibleForTesting
    static final float COLOR_SATURATION_DEFAULT = 1.0f;
    @VisibleForTesting
    static final float COLOR_SATURATION_VIVID = 1.1f;

    private static final int SURFACE_FLINGER_TRANSACTION_SATURATION = 1022;
    @VisibleForTesting
    static final String PERSISTENT_PROPERTY_SATURATION = "persist.sys.sf.color_saturation";

    private final IBinder mSurfaceFlinger;
    private final ConfigurationWrapper mConfigWrapper;

    public ColorModePreferenceController(Context context) {
        super(context);
        mSurfaceFlinger = ServiceManager.getService("SurfaceFlinger");
        mConfigWrapper = new ConfigurationWrapper(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_COLOR_MODE;
    }

    @Override
    public void updateState(Preference preference) {
        TwoStatePreference colorMode = (TwoStatePreference) preference;
        colorMode.setChecked(getSaturationValue() > 1.0f);
    }

    @Override
    public boolean isAvailable() {
        return mConfigWrapper.isScreenWideColorGamut();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        float saturation = (boolean) newValue
                ? COLOR_SATURATION_VIVID : COLOR_SATURATION_DEFAULT;

        SystemProperties.set(PERSISTENT_PROPERTY_SATURATION, Float.toString(saturation));
        applySaturation(saturation);

        return true;
    }

    /**
     * Propagates the provided saturation to the SurfaceFlinger.
     */
    private void applySaturation(float saturation) {
        if (mSurfaceFlinger != null) {
            final Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeFloat(saturation);
            try {
                mSurfaceFlinger.transact(SURFACE_FLINGER_TRANSACTION_SATURATION, data, null, 0);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to set saturation", ex);
            } finally {
                data.recycle();
            }
        }
    }

    private static float getSaturationValue() {
        try {
            return Float.parseFloat(SystemProperties.get(
                    PERSISTENT_PROPERTY_SATURATION, Float.toString(COLOR_SATURATION_DEFAULT)));
        } catch (NumberFormatException e) {
            return COLOR_SATURATION_DEFAULT;
        }
    }

    @VisibleForTesting
    static class ConfigurationWrapper {
        private final Context mContext;

        ConfigurationWrapper(Context context) {
            mContext = context;
        }

        boolean isScreenWideColorGamut() {
            return mContext.getResources().getConfiguration().isScreenWideColorGamut();
        }
    }
}
