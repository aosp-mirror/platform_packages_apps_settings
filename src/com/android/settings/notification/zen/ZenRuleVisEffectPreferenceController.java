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

import android.content.Context;
import android.service.notification.ZenPolicy;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.widget.DisabledCheckBoxPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenRuleVisEffectPreferenceController extends AbstractZenCustomRulePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private final int mMetricsCategory;

    @VisibleForTesting protected @ZenPolicy.VisualEffect int mEffect;

    // if any of these effects are suppressed, this effect must be too
    @VisibleForTesting protected @ZenPolicy.VisualEffect int[] mParentSuppressedEffects;

    public ZenRuleVisEffectPreferenceController(Context context, Lifecycle lifecycle, String key,
            @ZenPolicy.VisualEffect int visualEffect, int metricsCategory,
            @ZenPolicy.VisualEffect int[] parentSuppressedEffects) {
        super(context, key, lifecycle);
        mEffect = visualEffect;
        mMetricsCategory = metricsCategory;
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
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mRule == null || mRule.getZenPolicy() == null) {
            return;
        }

        boolean suppressed = !mRule.getZenPolicy().isVisualEffectAllowed(mEffect, false);
        boolean parentSuppressed = false;
        if (mParentSuppressedEffects != null) {
            for (@ZenPolicy.VisualEffect int parentEffect : mParentSuppressedEffects) {
                if (!mRule.getZenPolicy().isVisualEffectAllowed(parentEffect, true)) {
                    parentSuppressed = true;
                }
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
        mMetricsFeatureProvider.action(mContext, mMetricsCategory,
                Pair.create(MetricsProto.MetricsEvent.FIELD_ZEN_TOGGLE_EXCEPTION,
                        suppressEffect ? 1 : 0),
                Pair.create(MetricsProto.MetricsEvent.FIELD_ZEN_RULE_ID, mId));

        mRule.setZenPolicy(new ZenPolicy.Builder(mRule.getZenPolicy())
                .showVisualEffect(mEffect, !suppressEffect)
                .build());
        mBackend.updateZenRule(mId, mRule);
        return true;
    }
}
