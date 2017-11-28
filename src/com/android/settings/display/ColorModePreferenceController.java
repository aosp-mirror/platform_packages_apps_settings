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
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class ColorModePreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {
    private static final String TAG = "ColorModePreference";
    private static final String KEY_COLOR_MODE = "color_mode";

    private static final int SURFACE_FLINGER_TRANSACTION_QUERY_WIDE_COLOR = 1024;

    private final ConfigurationWrapper mConfigWrapper;

    public ColorModePreferenceController(Context context) {
        super(context);
        mConfigWrapper = new ConfigurationWrapper();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_COLOR_MODE;
    }

    @Override
    public boolean isAvailable() {
        return mConfigWrapper.isScreenWideColorGamut();
    }

    @VisibleForTesting
    static class ConfigurationWrapper {
        private final IBinder mSurfaceFlinger;

        ConfigurationWrapper() {
            mSurfaceFlinger = ServiceManager.getService("SurfaceFlinger");
        }

        boolean isScreenWideColorGamut() {
            if (mSurfaceFlinger != null) {
                final Parcel data = Parcel.obtain();
                final Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                try {
                    mSurfaceFlinger.transact(SURFACE_FLINGER_TRANSACTION_QUERY_WIDE_COLOR,
                            data, reply, 0);
                    return reply.readBoolean();
                } catch (RemoteException ex) {
                    Log.e(TAG, "Failed to query wide color support", ex);
                } finally {
                    data.recycle();
                    reply.recycle();
                }
            }
            return false;
        }
    }
}
