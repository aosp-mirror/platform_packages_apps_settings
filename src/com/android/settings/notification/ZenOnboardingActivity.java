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

package com.android.settings.notification;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.widget.CheckBox;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;

public class ZenOnboardingActivity extends Activity {

    private NotificationManager mNm;
    private MetricsLogger mMetrics;
    CheckBox mScreenOn;
    CheckBox mScreenOff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setNotificationManager(getSystemService(NotificationManager.class));
        setMetricsLogger(new MetricsLogger());

        setupUI();
    }

    @VisibleForTesting
    protected void setupUI() {
        setContentView(R.layout.zen_onboarding);
        mScreenOn = findViewById(R.id.screen_on_option);
        mScreenOff = findViewById(R.id.screen_off_option);
        mScreenOn.setChecked(true);
        mScreenOff.setChecked(true);

        mMetrics.visible(MetricsEvent.SETTINGS_ZEN_ONBOARDING);
    }

    @VisibleForTesting
    protected void setNotificationManager(NotificationManager nm) {
        mNm = nm;
    }

    @VisibleForTesting
    protected void setMetricsLogger(MetricsLogger ml) {
        mMetrics = ml;
    }

    public void logClick(View view) {
        CheckBox checkbox = (CheckBox) view;
        switch (checkbox.getId()) {
            case R.id.screen_on_option:
                mMetrics.action(MetricsEvent.ACTION_ZEN_ONBOARDING_SCREEN_ON, checkbox.isChecked());
                break;
            case R.id.screen_off_option:
                mMetrics.action(MetricsEvent.ACTION_ZEN_ONBOARDING_SCREEN_OFF,
                        checkbox.isChecked());
                break;
        }
    }

    public void launchSettings(View button) {
        mMetrics.action(MetricsEvent.ACTION_ZEN_ONBOARDING_SETTINGS);
        Intent settings = new Intent(Settings.ZEN_MODE_BLOCKED_EFFECTS_SETTINGS);
        settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(settings);
    }

    public void save(View button) {
        mMetrics.action(MetricsEvent.ACTION_ZEN_ONBOARDING_OK);
        NotificationManager.Policy policy = mNm.getNotificationPolicy();
        int currentEffects = policy.suppressedVisualEffects;

        currentEffects = NotificationManager.Policy.toggleScreenOnEffectsSuppressed(
                currentEffects, mScreenOn != null && mScreenOn.isChecked());
        currentEffects = NotificationManager.Policy.toggleScreenOffEffectsSuppressed(
                currentEffects, mScreenOff != null && mScreenOff.isChecked());

        NotificationManager.Policy newPolicy = new NotificationManager.Policy(
                policy.priorityCategories, policy.priorityCallSenders,
                policy.priorityMessageSenders, currentEffects);
        mNm.setNotificationPolicy(newPolicy);

        finishAndRemoveTask();
    }
}
