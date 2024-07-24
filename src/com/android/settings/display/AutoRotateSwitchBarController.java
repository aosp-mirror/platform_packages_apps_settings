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

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.internal.view.RotationPolicy;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.SettingsMainSwitchPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * The main switch controller for auto-rotate.
 */
public class AutoRotateSwitchBarController extends SettingsMainSwitchPreferenceController implements
        LifecycleObserver, OnStart, OnStop {

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private RotationPolicy.RotationPolicyListener mRotationPolicyListener;

    public AutoRotateSwitchBarController(Context context, String key) {
        super(context, key);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return RotationPolicy.isRotationLockToggleVisible(mContext)
                && !DeviceStateAutoRotationHelper.isDeviceStateRotationEnabled(mContext)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onStart() {
        if (mRotationPolicyListener == null) {
            mRotationPolicyListener = new RotationPolicy.RotationPolicyListener() {
                @Override
                public void onChange() {
                    if (mSwitchPreference != null) {
                        updateState(mSwitchPreference);
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
            RotationPolicy.unregisterRotationPolicyListener(mContext, mRotationPolicyListener);
        }
    }

    @Override
    public boolean isChecked() {
        return !RotationPolicy.isRotationLocked(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final boolean isLocked = !isChecked;
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_ROTATE_ROTATE_MASTER_TOGGLE,
                isLocked);
        RotationPolicy.setRotationLock(mContext, isLocked,
                /* caller= */ "AutoRotateSwitchBarController#setChecked");
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

}

