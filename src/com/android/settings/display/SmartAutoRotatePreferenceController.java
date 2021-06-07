/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.display;

import static android.provider.Settings.Secure.CAMERA_AUTOROTATE;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.view.RotationPolicy;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * SmartAutoRotatePreferenceController provides auto rotate summary in display settings
 */
public class SmartAutoRotatePreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private RotationPolicy.RotationPolicyListener mRotationPolicyListener;
    private Preference mPreference;

    public SmartAutoRotatePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return RotationPolicy.isRotationLockToggleVisible(mContext)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        if (mRotationPolicyListener == null) {
            mRotationPolicyListener = new RotationPolicy.RotationPolicyListener() {
                @Override
                public void onChange() {
                    if (mPreference != null) {
                        refreshSummary(mPreference);
                    }
                }
            };
        }
        RotationPolicy.registerRotationPolicyListener(mContext,
                mRotationPolicyListener);
    }

    @Override
    public void onStop() {
        if (mRotationPolicyListener != null) {
            RotationPolicy.unregisterRotationPolicyListener(mContext,
                    mRotationPolicyListener);
        }
    }

    @Override
    public CharSequence getSummary() {
        int activeStringId = R.string.auto_rotate_option_off;
        if (!RotationPolicy.isRotationLocked(mContext)) {
            final int cameraRotate = Settings.Secure.getIntForUser(
                    mContext.getContentResolver(),
                    CAMERA_AUTOROTATE,
                    0, UserHandle.USER_CURRENT);
            activeStringId = cameraRotate == 1 ? R.string.auto_rotate_option_face_based
                    : R.string.auto_rotate_option_on;
        }
        return mContext.getString(activeStringId);
    }
}
