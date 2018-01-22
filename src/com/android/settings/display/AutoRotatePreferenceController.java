/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.view.RotationPolicy;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class AutoRotatePreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener, LifecycleObserver,
        OnResume, OnPause {

    private static final String KEY_AUTO_ROTATE = "auto_rotate";
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private TwoStatePreference mPreference;
    private RotationPolicy.RotationPolicyListener mRotationPolicyListener;

    public AutoRotatePreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AUTO_ROTATE;
    }

    @Override
    public void updateState(Preference preference) {
        mPreference = (TwoStatePreference) preference;
        updatePreference();
    }

    @Override
    public boolean isAvailable() {
        return RotationPolicy.isRotationLockToggleVisible(mContext);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean locked = !(boolean) newValue;
        mMetricsFeatureProvider.action(mContext, MetricsProto.MetricsEvent.ACTION_ROTATION_LOCK,
                locked);
        RotationPolicy.setRotationLock(mContext, locked);
        return true;
    }

    @Override
    public void onResume() {
        if (mRotationPolicyListener == null) {
            mRotationPolicyListener = new RotationPolicy.RotationPolicyListener() {
                @Override
                public void onChange() {
                    updatePreference();
                }
            };
        }
        RotationPolicy.registerRotationPolicyListener(mContext,
                mRotationPolicyListener);
    }

    @Override
    public void onPause() {
        if (mRotationPolicyListener != null) {
            RotationPolicy.unregisterRotationPolicyListener(mContext, mRotationPolicyListener);
        }
    }

    private void updatePreference() {
        if (mPreference == null) {
            return;
        }
        mPreference.setChecked(!RotationPolicy.isRotationLocked(mContext));
    }
}
