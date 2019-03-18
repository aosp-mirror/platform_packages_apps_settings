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

import android.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.slice.widget.EventInfo;

import com.android.settings.R;
import com.android.settings.intelligence.ContextualCardProto.ContextualCardList;

import java.util.List;

public class ContextualCardFeatureProviderImpl implements ContextualCardFeatureProvider {
    private static final String TAG = "ContextualCardFeature";

    // Contextual card interaction logs
    // Settings Homepage shows
    private static final int CONTEXTUAL_HOME_SHOW = 38;

    // Contextual card shows, log card name and rank
    private static final int CONTEXTUAL_CARD_SHOW = 39;

    // Contextual card is dismissed, log card name
    private static final int CONTEXTUAL_CARD_DISMISS = 41;

    // Contextual card is clicked , log card name, score, tap area
    private static final int CONTEXTUAL_CARD_CLICK = 42;

    // SettingsLogBroadcastReceiver contracts
    // contextual card name
    private static final String EXTRA_CONTEXTUALCARD_NAME = "name";

    // contextual card uri
    private static final String EXTRA_CONTEXTUALCARD_URI = "uri";

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

    // displayed contextual cards
    private static final String EXTRA_CONTEXTUALCARD_VISIBLE = "visible";

    // hidden contextual cards
    private static final String EXTRA_CONTEXTUALCARD_HIDDEN = "hidden";

    // Contextual card tap target
    private static final int TARGET_DEFAULT = 0;

    // Click title area
    private static final int TARGET_TITLE = 1;

    // Click toggle
    private static final int TARGET_TOGGLE = 2;

    // Click slider
    private static final int TARGET_SLIDER = 3;

    private final Context mContext;

    public ContextualCardFeatureProviderImpl(Context context) {
        mContext = context;
    }

    @Override
    public void logHomepageDisplay(long latency) {
        sendBroadcast(new Intent()
                .putExtra(EXTRA_CONTEXTUALCARD_ACTION_TYPE, CONTEXTUAL_HOME_SHOW)
                .putExtra(EXTRA_LATENCY, latency));
    }

    @Override
    public void logContextualCardDismiss(ContextualCard card) {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_CONTEXTUALCARD_ACTION_TYPE, CONTEXTUAL_CARD_DISMISS);
        intent.putExtra(EXTRA_CONTEXTUALCARD_NAME, card.getName());
        intent.putExtra(EXTRA_CONTEXTUALCARD_URI, card.getSliceUri().toString());
        intent.putExtra(EXTRA_CONTEXTUALCARD_SCORE, card.getRankingScore());
        sendBroadcast(intent);
    }

    @Override
    public void logContextualCardDisplay(List<ContextualCard> visibleCards,
            List<ContextualCard> hiddenCards) {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_CONTEXTUALCARD_ACTION_TYPE, CONTEXTUAL_CARD_SHOW);
        intent.putExtra(EXTRA_CONTEXTUALCARD_VISIBLE, serialize(visibleCards));
        intent.putExtra(EXTRA_CONTEXTUALCARD_HIDDEN, serialize(hiddenCards));
        sendBroadcast(intent);
    }

    @Override
    public void logContextualCardClick(ContextualCard card, int row,
            int actionType) {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_CONTEXTUALCARD_ACTION_TYPE, CONTEXTUAL_CARD_CLICK);
        intent.putExtra(EXTRA_CONTEXTUALCARD_NAME, card.getName());
        intent.putExtra(EXTRA_CONTEXTUALCARD_URI, card.getSliceUri().toString());
        intent.putExtra(EXTRA_CONTEXTUALCARD_SCORE, card.getRankingScore());
        intent.putExtra(EXTRA_CONTEXTUALCARD_ROW, row);
        intent.putExtra(EXTRA_CONTEXTUALCARD_TAP_TARGET, actionTypeToTapTarget(actionType));
        sendBroadcast(intent);
    }

    @VisibleForTesting
    void sendBroadcast(final Intent intent) {
        intent.setPackage(mContext.getString(R.string.config_settingsintelligence_package_name));
        final String action = mContext.getString(R.string.config_settingsintelligence_log_action);
        if (!TextUtils.isEmpty(action)) {
            intent.setAction(action);
            mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
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

    @VisibleForTesting
    @NonNull
    byte[] serialize(List<ContextualCard> cards) {
        final ContextualCardList.Builder builder = ContextualCardList.newBuilder();
        cards.stream().forEach(card -> builder.addCard(
                com.android.settings.intelligence.ContextualCardProto.ContextualCard.newBuilder()
                        .setSliceUri(card.getSliceUri().toString())
                        .setCardName(card.getName())
                        .setCardScore(card.getRankingScore())
                        .build()));
        return builder.build().toByteArray();
    }
}
