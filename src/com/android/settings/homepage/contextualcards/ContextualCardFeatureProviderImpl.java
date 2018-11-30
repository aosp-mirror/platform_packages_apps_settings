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
 * limitations under the License
 */

package com.android.settings.homepage.contextualcards;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.slice.widget.EventInfo;

import com.android.settings.R;

import java.util.List;

public class ContextualCardFeatureProviderImpl implements ContextualCardFeatureProvider {
    private static final String TAG = "ContextualCardFeature";

    // Contextual card interaction logs
    // Settings Homepage shows
    private static final int CONTEXTUAL_HOME_SHOW = 38;

    // Contextual card shows, log card name and rank
    private static final int CONTEXTUAL_CARD_SHOW = 39;

    // Contextual card is eligible to be shown, but doesn't rank high
    // enough, log card name and score
    private static final int CONTEXTUAL_CARD_NOT_SHOW = 40;

    // Contextual card is dismissed, log card name
    private static final int CONTEXTUAL_CARD_DISMISS = 41;

    // Contextual card is clicked , log card name, score, tap area
    private static final int CONTEXTUAL_CARD_CLICK = 42;

    // SettingsLogBroadcastReceiver contracts
    // contextual card name
    private static final String EXTRA_CONTEXTUALCARD_NAME = "name";

    // contextual card score
    private static final String EXTRA_CONTEXTUALCARD_SCORE = "score";

    // contextual card clicked row
    private static final String EXTRA_CONTEXTUALCARD_ROW = "row";

    // contextual card tap target
    private static final String EXTRA_CONTEXTUALCARD_TAP_TARGET = "target";

    // contextual homepage display latency
    private static final String EXTRA_LATENCY = "latency";

    // log type
    private static final String EXTRA_CONTEXTUALCARD_ACTION_TYPE = "type";


    // Contextual card tap target
    private static final int TARGET_DEFAULT = 0;

    // Click title area
    private static final int TARGET_TITLE = 1;

    // Click toggle
    private static final int TARGET_TOGGLE = 2;

    // Click slider
    private static final int TARGET_SLIDER = 3;

    @Override
    public void logHomepageDisplay(Context context, Long latency) {
    }

    @Override
    public void logContextualCardDismiss(Context context, ContextualCard card) {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_CONTEXTUALCARD_ACTION_TYPE, CONTEXTUAL_CARD_DISMISS);
        intent.putExtra(EXTRA_CONTEXTUALCARD_NAME, card.getName());
        intent.putExtra(EXTRA_CONTEXTUALCARD_SCORE, card.getRankingScore());
        sendBroadcast(context, intent);
    }

    @Override
    public void logContextualCardDisplay(Context context, List<ContextualCard> showCards,
            List<ContextualCard> hiddenCards) {
    }

    @Override
    public void logContextualCardClick(Context context, ContextualCard card, int row,
            int actionType) {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_CONTEXTUALCARD_ACTION_TYPE, CONTEXTUAL_CARD_CLICK);
        intent.putExtra(EXTRA_CONTEXTUALCARD_NAME, card.getName());
        intent.putExtra(EXTRA_CONTEXTUALCARD_SCORE, card.getRankingScore());
        intent.putExtra(EXTRA_CONTEXTUALCARD_ROW, row);
        intent.putExtra(EXTRA_CONTEXTUALCARD_TAP_TARGET, actionTypeToTapTarget(actionType));
        sendBroadcast(context, intent);
    }

    @VisibleForTesting
    void sendBroadcast(final Context context, final Intent intent) {
        intent.setPackage(context.getString(R.string.config_settingsintelligence_package_name));
        final String action = context.getString(R.string.config_settingsintelligence_log_action);
        if (!TextUtils.isEmpty(action)) {
            intent.setAction(action);
            context.sendBroadcast(intent);
        }
    }

    private int actionTypeToTapTarget(int actionType) {
        switch (actionType) {
            case EventInfo.ACTION_TYPE_CONTENT:
                return TARGET_TITLE;
            case EventInfo.ACTION_TYPE_TOGGLE:
                return TARGET_TOGGLE;
            case EventInfo.ACTION_TYPE_SLIDER:
                return TARGET_SLIDER;
            default:
                Log.w(TAG, "unknown type " + actionType);
                return TARGET_DEFAULT;
        }
    }
}
