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

package com.android.settings.homepage.contextualcards.conditional;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;

import androidx.annotation.VisibleForTesting;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.ZenModeSettings;

import java.util.Objects;


public class DndConditionCardController implements ConditionalCardController {
    static final int ID = Objects.hash("DndConditionCardController");

    @VisibleForTesting
    static final IntentFilter DND_FILTER =
            new IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED_INTERNAL);

    private static final String TAG = "DndCondition";
    private final Context mAppContext;
    private final ConditionManager mConditionManager;
    private final NotificationManager mNotificationManager;
    private final Receiver mReceiver;

    public DndConditionCardController(Context appContext, ConditionManager conditionManager) {
        mAppContext = appContext;
        mConditionManager = conditionManager;
        mNotificationManager = mAppContext.getSystemService(NotificationManager.class);
        mReceiver = new Receiver();
    }

    @Override
    public long getId() {
        return ID;
    }

    @Override
    public boolean isDisplayable() {
        return mNotificationManager.getZenMode() != Settings.Global.ZEN_MODE_OFF;
    }

    @Override
    public void startMonitoringStateChange() {
        mAppContext.registerReceiver(mReceiver, DND_FILTER);
    }

    @Override
    public void stopMonitoringStateChange() {
        mAppContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void onPrimaryClick(Context context) {
        new SubSettingLauncher(context)
                .setDestination(ZenModeSettings.class.getName())
                .setSourceMetricsCategory(MetricsProto.MetricsEvent.SETTINGS_HOMEPAGE)
                .setTitleRes(R.string.zen_mode_settings_title)
                .launch();
    }

    @Override
    public void onActionClick() {
        mNotificationManager.setZenMode(Settings.Global.ZEN_MODE_OFF, null, TAG);
    }

    public CharSequence getSummary() {
        final int zen = mNotificationManager.getZenMode();
        final ZenModeConfig config;
        boolean zenModeEnabled = zen != Settings.Global.ZEN_MODE_OFF;
        if (zenModeEnabled) {
            config = mNotificationManager.getZenModeConfig();
        } else {
            config = null;
        }
        return ZenModeConfig.getDescription(mAppContext, zen != Settings.Global.ZEN_MODE_OFF,
                config, true);
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED_INTERNAL
                    .equals(intent.getAction())) {
                mConditionManager.onConditionChanged();
            }
        }
    }
}
