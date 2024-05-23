/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import android.content.Context;
import android.service.notification.ZenPolicy;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;

import com.android.settings.widget.DisabledCheckBoxPreference;

public class ZenModeNotifVisPreferenceController extends AbstractZenModePreferenceController
        implements Preference.OnPreferenceChangeListener {

    @VisibleForTesting protected @ZenPolicy.VisualEffect int mEffect;

    // if any of these effects are suppressed, this effect must be too
    @VisibleForTesting protected @ZenPolicy.VisualEffect int[] mParentSuppressedEffects;

    public ZenModeNotifVisPreferenceController(Context context, String key,
            @ZenPolicy.VisualEffect int visualEffect,
            @ZenPolicy.VisualEffect int[] parentSuppressedEffects, ZenModesBackend backend) {
        super(context, key, backend);
        mEffect = visualEffect;
        mParentSuppressedEffects = parentSuppressedEffects;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }

        if (mEffect == ZenPolicy.VISUAL_EFFECT_LIGHTS) {
            return mContext.getResources()
                    .getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed);
        }
        return true;
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {

        boolean suppressed = !zenMode.getPolicy().isVisualEffectAllowed(mEffect, false);
        boolean parentSuppressed = false;
        if (mParentSuppressedEffects != null) {
            for (@ZenPolicy.VisualEffect int parentEffect : mParentSuppressedEffects) {
                if (!zenMode.getPolicy().isVisualEffectAllowed(parentEffect, true)) {
                    parentSuppressed = true;
                }
            }
        }
        if (parentSuppressed) {
            ((CheckBoxPreference) preference).setChecked(true);
            onPreferenceChange(preference, true);
            ((DisabledCheckBoxPreference) preference).enableCheckbox(false);
        } else {
            ((DisabledCheckBoxPreference) preference).enableCheckbox(true);
            ((CheckBoxPreference) preference).setChecked(suppressed);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean allowEffect = !((Boolean) newValue);
        return savePolicy(policy -> policy.showVisualEffect(mEffect, allowEffect));
    }
}
