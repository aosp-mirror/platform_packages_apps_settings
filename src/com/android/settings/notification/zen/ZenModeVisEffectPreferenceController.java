/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.notification.zen;

import android.app.NotificationManager;
import android.content.Context;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.DisabledCheckBoxPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeVisEffectPreferenceController
        extends AbstractZenModePreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    protected final String mKey;
    protected final int mEffect;
    protected final int mMetricsCategory;
    // if any of these effects are suppressed, this effect must be too
    protected final int[] mParentSuppressedEffects;
    private PreferenceScreen mScreen;

    public ZenModeVisEffectPreferenceController(Context context, Lifecycle lifecycle, String key,
            int visualEffect, int metricsCategory, int[] parentSuppressedEffects) {
        super(context, key, lifecycle);
        mKey = key;
        mEffect = visualEffect;
        mMetricsCategory = metricsCategory;
        mParentSuppressedEffects = parentSuppressedEffects;
    }

    @Override
    public String getPreferenceKey() {
        return mKey;
    }

    @Override
    public boolean isAvailable() {
        if (mEffect == NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS) {
            return mContext.getResources()
                    .getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed);
        }
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mScreen = screen;
        super.displayPreference(screen);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        boolean suppressed = mBackend.isVisualEffectSuppressed(mEffect);
        boolean parentSuppressed = false;
        if (mParentSuppressedEffects != null) {
            for (int parentEffect : mParentSuppressedEffects) {
                parentSuppressed |= mBackend.isVisualEffectSuppressed(parentEffect);
            }
        }
        if (parentSuppressed) {
            ((CheckBoxPreference) preference).setChecked(parentSuppressed);
            onPreferenceChange(preference, parentSuppressed);
            ((DisabledCheckBoxPreference) preference).enableCheckbox(false);
        } else {
            ((DisabledCheckBoxPreference) preference).enableCheckbox(true);
            ((CheckBoxPreference) preference).setChecked(suppressed);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean suppressEffect = (Boolean) newValue;

        mMetricsFeatureProvider.action(mContext, mMetricsCategory, suppressEffect);
        mBackend.saveVisualEffectsPolicy(mEffect, suppressEffect);
        return true;
    }
}
