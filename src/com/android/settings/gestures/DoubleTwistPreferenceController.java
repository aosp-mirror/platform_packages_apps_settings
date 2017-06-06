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

package com.android.settings.gestures;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.lifecycle.Lifecycle;

public class DoubleTwistPreferenceController extends GesturePreferenceController {

    private static final String PREF_KEY_VIDEO = "gesture_double_twist_video";
    private final String mDoubleTwistPrefKey;
    private final UserManager mUserManager;

    public DoubleTwistPreferenceController(Context context, Lifecycle lifecycle, String key) {
        super(context, lifecycle);
        mDoubleTwistPrefKey = key;
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    @Override
    public boolean isAvailable() {
        return hasSensor(R.string.gesture_double_twist_sensor_name,
                R.string.gesture_double_twist_sensor_vendor);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public String getPreferenceKey() {
        return mDoubleTwistPrefKey;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final int enabled = (boolean) newValue ? 1 : 0;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, enabled);
        final int managedProfileUserId = getManagedProfileUserId();
        if (managedProfileUserId != UserHandle.USER_NULL) {
            Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, enabled, managedProfileUserId);
        }
        return true;
    }

    @Override
    protected boolean isSwitchPrefEnabled() {
        final int doubleTwistEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED, 1);
        return doubleTwistEnabled != 0;
    }

    @VisibleForTesting
    int getManagedProfileUserId() {
        return Utils.getManagedProfileId(mUserManager, UserHandle.myUserId());
    }

    private boolean hasSensor(int nameResId, int vendorResId) {
        final Resources resources = mContext.getResources();
        final String name = resources.getString(nameResId);
        final String vendor = resources.getString(vendorResId);
        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(vendor)) {
            final SensorManager sensorManager =
                    (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
            for (Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL)) {
                if (name.equals(s.getName()) && vendor.equals(s.getVendor())) {
                    return true;
                }
            }
        }
        return false;
    }
}
