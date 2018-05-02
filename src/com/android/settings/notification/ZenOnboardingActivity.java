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
import android.app.NotificationManager.Policy;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.view.View;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;

public class ZenOnboardingActivity extends Activity {

    private NotificationManager mNm;
    private MetricsLogger mMetrics;

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

    public void close(View button) {
        mMetrics.action(MetricsEvent.ACTION_ZEN_ONBOARDING_KEEP_CURRENT_SETTINGS);
        finishAndRemoveTask();
    }

    public void save(View button) {
        mMetrics.action(MetricsEvent.ACTION_ZEN_ONBOARDING_OK);
        Policy policy = mNm.getNotificationPolicy();

        Policy newPolicy = new NotificationManager.Policy(
                Policy.PRIORITY_CATEGORY_REPEAT_CALLERS | policy.priorityCategories,
                Policy.PRIORITY_SENDERS_STARRED,
                policy.priorityMessageSenders,
                Policy.getAllSuppressedVisualEffects());
        mNm.setNotificationPolicy(newPolicy);

        finishAndRemoveTask();
    }
}
