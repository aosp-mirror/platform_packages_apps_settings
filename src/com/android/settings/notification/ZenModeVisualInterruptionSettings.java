/**
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.notification;

import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.R;

import java.util.Arrays;
import java.util.List;

public class ZenModeVisualInterruptionSettings extends ZenModeSettingsBase {

    private static final String KEY_PEEK = "peek";
    private static final String KEY_LIGHTS = "lights";
    private static final String KEY_SCREEN_ON = "screen_on";

    private SwitchPreference mPeek;
    private SwitchPreference mLights;
    private SwitchPreference mScreenOn;

    private boolean mDisableListeners;
    private NotificationManager.Policy mPolicy;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.zen_mode_visual_interruptions_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mPolicy = NotificationManager.from(mContext).getNotificationPolicy();

        mPeek = (SwitchPreference) root.findPreference(KEY_PEEK);
        mPeek.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                MetricsLogger.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_PEEK, val);
                if (DEBUG) Log.d(TAG, "onPrefChange suppressPeek=" + val);
                savePolicy(getNewSuppressedEffects(val, Policy.SUPPRESSED_EFFECT_PEEK));
                return true;
            }
        });

        mLights = (SwitchPreference) root.findPreference(KEY_LIGHTS);
        mLights.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                MetricsLogger.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_LIGHTS, val);
                if (DEBUG) Log.d(TAG, "onPrefChange suppressLights=" + val);
                savePolicy(getNewSuppressedEffects(val, Policy.SUPPRESSED_EFFECT_LIGHTS));
                return true;
            }
        });

        mScreenOn = (SwitchPreference) root.findPreference(KEY_SCREEN_ON);
        mScreenOn.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                MetricsLogger.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_SCREEN_ON, val);
                if (DEBUG) Log.d(TAG, "onPrefChange suppressScreenOn=" + val);
                savePolicy(getNewSuppressedEffects(val, Policy.SUPPRESSED_EFFECT_SCREEN_ON));
                return true;
            }
        });
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_ZEN_MODE_VISUAL_INTERRUPTIONS;
    }

    @Override
    protected void onZenModeChanged() {
        // Don't care
    }

    @Override
    protected void onZenModeConfigChanged() {
        mPolicy = NotificationManager.from(mContext).getNotificationPolicy();
        updateControls();
    }

    private void updateControls() {
        mDisableListeners = true;
        mPeek.setChecked(isEffectSuppressed(Policy.SUPPRESSED_EFFECT_PEEK));
        mLights.setChecked(isEffectSuppressed(Policy.SUPPRESSED_EFFECT_LIGHTS));
        mScreenOn.setChecked(isEffectSuppressed(Policy.SUPPRESSED_EFFECT_SCREEN_ON));
        mDisableListeners = false;
    }

    private boolean isEffectSuppressed(int effect) {
        return (mPolicy.suppressedVisualEffects & effect) != 0;
    }

    private int getNewSuppressedEffects(boolean suppress, int effectType) {
        int effects = mPolicy.suppressedVisualEffects;
        if (suppress) {
            effects |= effectType;
        } else {
            effects &= ~effectType;
        }
        return effects;
    }

    private void savePolicy(int suppressedVisualEffects) {
        mPolicy = new Policy(mPolicy.priorityCategories,
                mPolicy.priorityCallSenders, mPolicy.priorityMessageSenders,
                suppressedVisualEffects);
        NotificationManager.from(mContext).setNotificationPolicy(mPolicy);
    }
}
