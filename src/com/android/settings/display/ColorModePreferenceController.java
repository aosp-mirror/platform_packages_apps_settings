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

import androidx.annotation.VisibleForTesting;

import com.android.internal.app.ColorDisplayController;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class ColorModePreferenceController extends BasePreferenceController {
    private static final String TAG = "ColorModePreference";

    private static final int SURFACE_FLINGER_TRANSACTION_QUERY_COLOR_MANAGEMENT = 1030;

    private final ConfigurationWrapper mConfigWrapper;
    private ColorDisplayController mColorDisplayController;

    public ColorModePreferenceController(Context context, String key) {
        super(context, key);
        mConfigWrapper = new ConfigurationWrapper();
    }

    @Override
    public int getAvailabilityStatus() {
        return mConfigWrapper.isDeviceColorManaged()
                && !getColorDisplayController().getAccessibilityTransformActivated() ?
                AVAILABLE_UNSEARCHABLE : DISABLED_FOR_USER;
    }

    @Override
    public CharSequence getSummary() {
        final int colorMode = getColorDisplayController().getColorMode();
        if (colorMode == ColorDisplayController.COLOR_MODE_AUTOMATIC) {
            return mContext.getText(R.string.color_mode_option_automatic);
        }
        if (colorMode == ColorDisplayController.COLOR_MODE_SATURATED) {
            return mContext.getText(R.string.color_mode_option_saturated);
        }
        if (colorMode == ColorDisplayController.COLOR_MODE_BOOSTED) {
            return mContext.getText(R.string.color_mode_option_boosted);
        }
        return mContext.getText(R.string.color_mode_option_natural);
    }

    @VisibleForTesting
    ColorDisplayController getColorDisplayController() {
        if (mColorDisplayController == null) {
            mColorDisplayController = new ColorDisplayController(mContext);
        }
        return mColorDisplayController;
    }

    @VisibleForTesting
    static class ConfigurationWrapper {
        private final IBinder mSurfaceFlinger;

        ConfigurationWrapper() {
            mSurfaceFlinger = ServiceManager.getService("SurfaceFlinger");
        }

        boolean isDeviceColorManaged() {
            if (mSurfaceFlinger != null) {
                final Parcel data = Parcel.obtain();
                final Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                try {
                    mSurfaceFlinger.transact(SURFACE_FLINGER_TRANSACTION_QUERY_COLOR_MANAGEMENT,
                            data, reply, 0);
                    return reply.readBoolean();
                } catch (RemoteException ex) {
                    Log.e(TAG, "Failed to query color management support", ex);
                } finally {
                    data.recycle();
                    reply.recycle();
                }
            }
            return false;
        }
    }
}
