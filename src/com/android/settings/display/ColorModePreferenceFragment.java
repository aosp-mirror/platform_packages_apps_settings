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

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.R;
import com.android.settings.widget.RadioButtonPickerFragment;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class ColorModePreferenceFragment extends RadioButtonPickerFragment {
    private static final String TAG = "ColorModePreferenceFragment";

    @VisibleForTesting
    static final float COLOR_SATURATION_NATURAL = 1.0f;
    @VisibleForTesting
    static final float COLOR_SATURATION_BOOSTED = 1.1f;

    private static final int SURFACE_FLINGER_TRANSACTION_SATURATION = 1022;
    private static final int SURFACE_FLINGER_TRANSACTION_NATIVE_MODE = 1023;

    @VisibleForTesting
    static final String PERSISTENT_PROPERTY_SATURATION = "persist.sys.sf.color_saturation";
    @VisibleForTesting
    static final String PERSISTENT_PROPERTY_NATIVE_MODE = "persist.sys.sf.native_mode";

    @VisibleForTesting
    static final String KEY_COLOR_MODE_NATURAL = "color_mode_natural";
    @VisibleForTesting
    static final String KEY_COLOR_MODE_BOOSTED = "color_mode_boosted";
    @VisibleForTesting
    static final String KEY_COLOR_MODE_SATURATED = "color_mode_saturated";

    private IBinder mSurfaceFlinger;
    private IActivityManager mActivityManager;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSurfaceFlinger = ServiceManager.getService("SurfaceFlinger");
        mActivityManager = ActivityManager.getService();
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        Context c = getContext();
        return Arrays.asList(
            new ColorModeCandidateInfo(c.getString(R.string.color_mode_option_natural),
                    KEY_COLOR_MODE_NATURAL),
            new ColorModeCandidateInfo(c.getString(R.string.color_mode_option_boosted),
                    KEY_COLOR_MODE_BOOSTED),
            new ColorModeCandidateInfo(c.getString(R.string.color_mode_option_saturated),
                    KEY_COLOR_MODE_SATURATED)
        );
    }

    @Override
    protected String getDefaultKey() {
        if (isNativeModeEnabled()) {
            return KEY_COLOR_MODE_SATURATED;
        }
        if (getSaturationValue() > COLOR_SATURATION_NATURAL) {
            return KEY_COLOR_MODE_BOOSTED;
        }
        return KEY_COLOR_MODE_NATURAL;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        switch (key) {
            case KEY_COLOR_MODE_NATURAL:
                applySaturation(COLOR_SATURATION_NATURAL);
                setNativeMode(false);
                break;
            case KEY_COLOR_MODE_BOOSTED:
                applySaturation(COLOR_SATURATION_BOOSTED);
                setNativeMode(false);
                break;
            case KEY_COLOR_MODE_SATURATED:
                applySaturation(COLOR_SATURATION_NATURAL);
                setNativeMode(true);
                break;
        }

        updateConfiguration();

        return true;
    }

    @VisibleForTesting
    void updateConfiguration() {
        try {
            mActivityManager.updateConfiguration(null);
        } catch (RemoteException e) {
            Log.d(TAG, "Could not update configuration", e);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.COLOR_MODE_SETTINGS;
    }

    /**
     * Propagates the provided saturation to the SurfaceFlinger.
     */
    private void applySaturation(float saturation) {
        SystemProperties.set(PERSISTENT_PROPERTY_SATURATION, Float.toString(saturation));
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
                    PERSISTENT_PROPERTY_SATURATION, Float.toString(COLOR_SATURATION_NATURAL)));
        } catch (NumberFormatException e) {
            return COLOR_SATURATION_NATURAL;
        }
    }

    /**
     * Toggles native mode on/off in SurfaceFlinger.
     */
    private void setNativeMode(boolean enabled) {
        SystemProperties.set(PERSISTENT_PROPERTY_NATIVE_MODE, enabled ? "1" : "0");
        if (mSurfaceFlinger != null) {
            final Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(enabled ? 1 : 0);
            try {
                mSurfaceFlinger.transact(SURFACE_FLINGER_TRANSACTION_NATIVE_MODE, data, null, 0);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to set native mode", ex);
            } finally {
                data.recycle();
            }
        }
    }

    private static boolean isNativeModeEnabled() {
        return SystemProperties.getBoolean(PERSISTENT_PROPERTY_NATIVE_MODE, false);
    }

    @VisibleForTesting
    static class ColorModeCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final String mKey;

        ColorModeCandidateInfo(CharSequence label, String key) {
            super(true);
            mLabel = label;
            mKey = key;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }
}
