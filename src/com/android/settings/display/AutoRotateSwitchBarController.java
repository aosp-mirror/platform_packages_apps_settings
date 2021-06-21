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
import android.widget.Switch;

import com.android.internal.view.RotationPolicy;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

/**
 * The switch controller for the location.
 */
public class AutoRotateSwitchBarController implements OnMainSwitchChangeListener,
        LifecycleObserver, OnStart, OnStop {

    private final SettingsMainSwitchBar mSwitchBar;
    private final Context mContext;
    private boolean mValidListener;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public AutoRotateSwitchBarController(Context context, SettingsMainSwitchBar switchBar,
            Lifecycle lifecycle) {
        mSwitchBar = switchBar;
        mContext = context;
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onStart() {
        if (!mValidListener) {
            mSwitchBar.addOnSwitchChangeListener(this);
            mValidListener = true;
        }
        onChange();
    }

    @Override
    public void onStop() {
        if (mValidListener) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mValidListener = false;
        }
    }

    /**
     * Listens to the state change of the rotation primary switch.
     */
    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        setRotationLock(isChecked);
    }


    protected void onChange() {
        final boolean isEnabled = !RotationPolicy.isRotationLocked(mContext);
        if (isEnabled != mSwitchBar.isChecked()) {
            // set listener to null so that that code below doesn't trigger onCheckedChanged()
            if (mValidListener) {
                mSwitchBar.removeOnSwitchChangeListener(this);
            }
            mSwitchBar.setChecked(isEnabled);
            if (mValidListener) {
                mSwitchBar.addOnSwitchChangeListener(this);
            }
        }
    }

    private boolean setRotationLock(boolean isChecked) {
        final boolean isLocked = !isChecked;
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_ROTATE_ROTATE_MASTER_TOGGLE,
                isChecked);
        RotationPolicy.setRotationLock(mContext, isLocked);
        return true;
    }

}

