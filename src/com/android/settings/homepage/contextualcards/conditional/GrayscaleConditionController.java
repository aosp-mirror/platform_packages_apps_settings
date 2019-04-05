/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.Manifest;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.ColorDisplayManager;
import android.os.UserHandle;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;

import java.net.URISyntaxException;
import java.util.Objects;

public class GrayscaleConditionController implements ConditionalCardController {
    static final int ID = Objects.hash("GrayscaleConditionController");

    private static final String TAG = "GrayscaleCondition";
    private static final String ACTION_GRAYSCALE_CHANGED =
            "android.settings.action.GRAYSCALE_CHANGED";
    private static final IntentFilter GRAYSCALE_CHANGED_FILTER = new IntentFilter(
            ACTION_GRAYSCALE_CHANGED);

    private final Context mAppContext;
    private final ConditionManager mConditionManager;
    private final ColorDisplayManager mColorDisplayManager;
    private final Receiver mReceiver;

    private Intent mIntent;

    public GrayscaleConditionController(Context appContext, ConditionManager conditionManager) {
        mAppContext = appContext;
        mConditionManager = conditionManager;
        mColorDisplayManager = mAppContext.getSystemService(ColorDisplayManager.class);
        mReceiver = new Receiver();
    }

    @Override
    public long getId() {
        return ID;
    }

    @Override
    public boolean isDisplayable() {
        try {
            mIntent = Intent.parseUri(
                    mAppContext.getString(R.string.config_grayscale_settings_intent),
                    Intent.URI_INTENT_SCHEME);
        } catch (URISyntaxException e) {
            Log.w(TAG, "Failure parsing grayscale settings intent, skipping", e);
            return false;
        }
        return mColorDisplayManager.isSaturationActivated();
    }

    @Override
    public void onPrimaryClick(Context context) {
        mAppContext.startActivity(mIntent);
    }

    @Override
    public void onActionClick() {
        // Turn off grayscale
        mColorDisplayManager.setSaturationLevel(100 /* staturationLevel */);
        sendBroadcast();
        mConditionManager.onConditionChanged();
    }

    @Override
    public ContextualCard buildContextualCard() {
        return new ConditionalContextualCard.Builder()
                .setConditionId(ID)
                .setMetricsConstant(SettingsEnums.SETTINGS_CONDITION_GRAYSCALE_MODE)
                .setActionText(mAppContext.getText(R.string.condition_turn_off))
                .setName(mAppContext.getPackageName() + "/" + mAppContext.getText(
                        R.string.condition_grayscale_title))
                .setTitleText(mAppContext.getText(R.string.condition_grayscale_title).toString())
                .setSummaryText(
                        mAppContext.getText(R.string.condition_grayscale_summary).toString())
                .setIconDrawable(mAppContext.getDrawable(R.drawable.ic_gray_scale_24dp))
                .setViewType(ConditionContextualCardRenderer.VIEW_TYPE_HALF_WIDTH)
                .build();
    }

    @Override
    public void startMonitoringStateChange() {
        mAppContext.registerReceiver(mReceiver, GRAYSCALE_CHANGED_FILTER,
                Manifest.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS, null /* scheduler */);
    }

    @Override
    public void stopMonitoringStateChange() {
        mAppContext.unregisterReceiver(mReceiver);
    }

    private void sendBroadcast() {
        final Intent intent = new Intent(ACTION_GRAYSCALE_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        mAppContext.sendBroadcastAsUser(intent, UserHandle.CURRENT,
                Manifest.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS);
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_GRAYSCALE_CHANGED.equals(intent.getAction())) {
                mConditionManager.onConditionChanged();
            }
        }
    }
}
