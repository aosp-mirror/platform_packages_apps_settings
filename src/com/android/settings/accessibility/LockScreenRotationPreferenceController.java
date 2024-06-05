/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.view.RotationPolicy;
import com.android.internal.view.RotationPolicy.RotationPolicyListener;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.display.DeviceStateAutoRotationHelper;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class LockScreenRotationPreferenceController extends TogglePreferenceController implements
        LifecycleObserver, OnStart, OnStop {

    private Preference mPreference;
    private RotationPolicyListener mRotationPolicyListener;

    public LockScreenRotationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * Returns true if rotation lock is enabled.
     */
    @Override
    public boolean isChecked() {
        return !RotationPolicy.isRotationLocked(mContext);
    }

    /**
     * Enables or disables screen rotation lock from Accessibility settings. If rotation is locked
     * for accessibility, the toggle in Display settings is hidden to avoid confusion.
     */
    @Override
    public boolean setChecked(boolean isChecked) {
        RotationPolicy.setRotationLock(mContext, !isChecked,
                /* caller= */ "LockScreenRotationPreferenceController#setChecked");
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return RotationPolicy.isRotationSupported(mContext)
                && !DeviceStateAutoRotationHelper.isDeviceStateRotationEnabledForA11y(mContext)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    @Override
    public void onStop() {
        if (mRotationPolicyListener != null) {
            RotationPolicy.unregisterRotationPolicyListener(mContext, mRotationPolicyListener);
        }
    }

    @Override
    public void onStart() {
        if (mRotationPolicyListener == null) {
            mRotationPolicyListener = new RotationPolicyListener() {
                @Override
                public void onChange() {
                    if (mPreference != null) {
                        updateState(mPreference);
                    }
                }
            };
        }
        RotationPolicy.registerRotationPolicyListener(mContext, mRotationPolicyListener);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreference = screen.findPreference(getPreferenceKey());
        super.displayPreference(screen);
    }
}
