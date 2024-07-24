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

import android.app.Activity;
import android.app.Flags;
import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;

import androidx.annotation.VisibleForTesting;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

public class ZenOnboardingActivity extends Activity {

    private static final String TAG = "ZenOnboardingActivity";

    @VisibleForTesting
    static final String PREF_KEY_SUGGESTION_FIRST_DISPLAY_TIME =
            "pref_zen_suggestion_first_display_time_ms";
    @VisibleForTesting
    static final long ALWAYS_SHOW_THRESHOLD = DateUtils.DAY_IN_MILLIS * 14;

    View mNewSetting;
    View mKeepCurrentSetting;
    RadioButton mNewSettingButton;
    RadioButton mKeepCurrentSettingButton;

    private NotificationManager mNm;
    private MetricsLogger mMetrics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setNotificationManager(getSystemService(NotificationManager.class));
        setMetricsLogger(new MetricsLogger());

        Context context = getApplicationContext();
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.ZEN_SETTINGS_SUGGESTION_VIEWED, 1);

        setupUI();
    }

    @VisibleForTesting
    protected void setupUI() {
        setContentView(R.layout.zen_onboarding);

        mNewSetting = findViewById(R.id.zen_onboarding_new_setting);
        mKeepCurrentSetting = findViewById(R.id.zen_onboarding_current_setting);
        mNewSettingButton = findViewById(R.id.zen_onboarding_new_setting_button);
        mKeepCurrentSettingButton = findViewById(R.id.zen_onboarding_current_setting_button);

        View.OnClickListener newSettingClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mKeepCurrentSettingButton.setChecked(false);
                mNewSettingButton.setChecked(true);
            }
        };

        View.OnClickListener currentSettingClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mKeepCurrentSettingButton.setChecked(true);
                mNewSettingButton.setChecked(false);
            }
        };

        mNewSetting.setOnClickListener(newSettingClickListener);
        mNewSettingButton.setOnClickListener(newSettingClickListener);

        mKeepCurrentSetting.setOnClickListener(currentSettingClickListener);
        mKeepCurrentSettingButton.setOnClickListener(currentSettingClickListener);

        mKeepCurrentSettingButton.setChecked(true);
        mMetrics.visible(SettingsEnums.SETTINGS_ZEN_ONBOARDING);
    }

    @VisibleForTesting
    protected void setNotificationManager(NotificationManager nm) {
        mNm = nm;
    }

    @VisibleForTesting
    protected void setMetricsLogger(MetricsLogger ml) {
        mMetrics = ml;
    }

    public void launchSettings(View button) {
        mMetrics.action(SettingsEnums.ACTION_ZEN_ONBOARDING_SETTINGS);
        Intent settings = new Intent(Settings.ACTION_ZEN_MODE_SETTINGS);
        settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(settings);
    }

    public void save(View button) {
        NotificationManager.Policy policy = mNm.getNotificationPolicy();

        if (mNewSettingButton.isChecked()) {
            NotificationManager.Policy newPolicy = new NotificationManager.Policy(
                    Policy.PRIORITY_CATEGORY_REPEAT_CALLERS | policy.priorityCategories,
                    Policy.PRIORITY_SENDERS_STARRED,
                    policy.priorityMessageSenders,
                    NotificationManager.Policy.getAllSuppressedVisualEffects());
            if (Flags.modesApi()) {
                mNm.setNotificationPolicy(newPolicy, /* fromUser= */ true);
            } else {
                mNm.setNotificationPolicy(newPolicy);
            }
            mMetrics.action(SettingsEnums.ACTION_ZEN_ONBOARDING_OK);
        } else {
            mMetrics.action(SettingsEnums.ACTION_ZEN_ONBOARDING_KEEP_CURRENT_SETTINGS);
        }

        Settings.Secure.putInt(getApplicationContext().getContentResolver(),
                Settings.Secure.ZEN_SETTINGS_UPDATED, 1);

        finishAndRemoveTask();
    }

    public static boolean isSuggestionComplete(Context context) {
        if (wasZenUpdated(context)) {
            return true;
        }

        if (showSuggestion(context) || withinShowTimeThreshold(context)) {
            return false;
        }

        return true;
    }

    private static boolean wasZenUpdated(Context context) {
        // ZEN_SETTINGS_UPDATED is true for:
        // - fresh P+ device
        // - if zen visual effects values were changed by the user in Settings
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (NotificationManager.Policy.areAllVisualEffectsSuppressed(
                nm.getNotificationPolicy().suppressedVisualEffects)) {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.ZEN_SETTINGS_UPDATED, 1);
        }
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.ZEN_SETTINGS_UPDATED, 0) != 0;
    }

    private static boolean showSuggestion(Context context) {
        // SHOW_ZEN_SETTINGS_SUGGESTION is by default true, but false when:
        // - user manually turns on dnd

        // SHOW_ZEN_SETTINGS_SUGGESTION is also true when:
        // - automatic rule has started DND and user has not seen the first use dialog
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.SHOW_ZEN_SETTINGS_SUGGESTION, 0) != 0;

    }

    private static boolean withinShowTimeThreshold(Context context) {
        final SuggestionFeatureProvider featureProvider =
                FeatureFactory.getFeatureFactory().getSuggestionFeatureProvider();
        final SharedPreferences prefs = featureProvider.getSharedPrefs(context);
        final long currentTimeMs = System.currentTimeMillis();
        final long firstDisplayTimeMs;

        if (!prefs.contains(PREF_KEY_SUGGESTION_FIRST_DISPLAY_TIME)) {
            firstDisplayTimeMs = currentTimeMs;
            prefs.edit().putLong(PREF_KEY_SUGGESTION_FIRST_DISPLAY_TIME, currentTimeMs).commit();
        } else {
            firstDisplayTimeMs = prefs.getLong(PREF_KEY_SUGGESTION_FIRST_DISPLAY_TIME, -1);
        }

        final long showTimeMs = firstDisplayTimeMs + ALWAYS_SHOW_THRESHOLD;
        final boolean stillShow = currentTimeMs < showTimeMs;

        Log.d(TAG, "still show zen suggestion based on time: " + stillShow + " showTimeMs="
            + showTimeMs);
        return stillShow;
    }
}
