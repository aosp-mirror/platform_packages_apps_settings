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
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.notification.zen.ZenModeSettings;

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
                .setSourceMetricsCategory(SettingsEnums.SETTINGS_HOMEPAGE)
                .setTitleRes(R.string.zen_mode_settings_title)
                .launch();
    }

    @Override
    public void onActionClick() {
        mNotificationManager.setZenMode(Settings.Global.ZEN_MODE_OFF, null, TAG);
    }

    @Override
    public ContextualCard buildContextualCard() {
        return new ConditionalContextualCard.Builder()
                .setConditionId(ID)
                .setMetricsConstant(SettingsEnums.SETTINGS_CONDITION_DND)
                .setActionText(mAppContext.getText(R.string.condition_turn_off))
                .setName(mAppContext.getPackageName() + "/"
                        + mAppContext.getText(R.string.condition_zen_title))
                .setTitleText(mAppContext.getText(R.string.condition_zen_title).toString())
                .setSummaryText(getSummary())
                .setIconDrawable(mAppContext.getDrawable(R.drawable.ic_do_not_disturb_on_24dp))
                .setViewType(ConditionContextualCardRenderer.VIEW_TYPE_HALF_WIDTH)
                .build();
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

    private String getSummary() {
        if (ZenModeConfig.areAllZenBehaviorSoundsMuted(mNotificationManager.getZenModeConfig())) {
            return mAppContext.getText(R.string.condition_zen_summary_phone_muted).toString();
        }
        return mAppContext.getText(R.string.condition_zen_summary_with_exceptions).toString();
    }
}
