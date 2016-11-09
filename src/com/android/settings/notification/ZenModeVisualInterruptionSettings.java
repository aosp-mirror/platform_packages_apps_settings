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
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;

public class ZenModeVisualInterruptionSettings extends ZenModeSettingsBase {

    private static final String KEY_SCREEN_OFF = "screenOff";
    private static final String KEY_SCREEN_ON = "screenOn";

    private SwitchPreference mScreenOff;
    private SwitchPreference mScreenOn;

    private boolean mDisableListeners;
    private NotificationManager.Policy mPolicy;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.zen_mode_visual_interruptions_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mPolicy = NotificationManager.from(mContext).getNotificationPolicy();

        mScreenOff = (SwitchPreference) root.findPreference(KEY_SCREEN_OFF);
        if (!getResources()
                .getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
            mScreenOff.setSummary(R.string.zen_mode_screen_off_summary_no_led);
        }
        mScreenOff.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                mMetricsFeatureProvider.action(mContext,
                        MetricsEvent.ACTION_ZEN_ALLOW_WHEN_SCREEN_OFF, val);
                if (DEBUG) Log.d(TAG, "onPrefChange suppressWhenScreenOff=" + val);
                savePolicy(getNewSuppressedEffects(val, Policy.SUPPRESSED_EFFECT_SCREEN_OFF));
                return true;
            }
        });

        mScreenOn = (SwitchPreference) root.findPreference(KEY_SCREEN_ON);
        mScreenOn.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                mMetricsFeatureProvider.action(mContext,
                        MetricsEvent.ACTION_ZEN_ALLOW_WHEN_SCREEN_ON, val);
                if (DEBUG) Log.d(TAG, "onPrefChange suppressWhenScreenOn=" + val);
                savePolicy(getNewSuppressedEffects(val, Policy.SUPPRESSED_EFFECT_SCREEN_ON));
                return true;
            }
        });
    }

    @Override
    public int getMetricsCategory() {
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
        mScreenOff.setChecked(isEffectSuppressed(Policy.SUPPRESSED_EFFECT_SCREEN_OFF));
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
