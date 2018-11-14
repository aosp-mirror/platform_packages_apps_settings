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

import android.content.Context;
import android.util.ArrayMap;

import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCardController;
import com.android.settings.homepage.contextualcards.ContextualCardUpdateListener;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.List;
import java.util.Map;

/**
 * This controller triggers the loading of conditional cards and monitors state changes to
 * update the homepage.
 */
public class ConditionContextualCardController implements ContextualCardController,
        ConditionListener, LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "ConditionCtxCardCtrl";

    private final Context mContext;
    private final ConditionManager mConditionManager;

    private ContextualCardUpdateListener mListener;

    public ConditionContextualCardController(Context context) {
        mContext = context;
        mConditionManager = new ConditionManager(context.getApplicationContext(), this);
        mConditionManager.startMonitoringStateChange();
    }

    @Override
    public void setCardUpdateListener(ContextualCardUpdateListener listener) {
        mListener = listener;
    }

    @Override
    public int getCardType() {
        return ContextualCard.CardType.CONDITIONAL;
    }

    @Override
    public void onStart() {
        mConditionManager.startMonitoringStateChange();
    }

    @Override
    public void onStop() {
        mConditionManager.stopMonitoringStateChange();
    }

    @Override
    public void onPrimaryClick(ContextualCard contextualCard) {
        final ConditionalContextualCard card = (ConditionalContextualCard) contextualCard;
        mConditionManager.onPrimaryClick(mContext, card.getConditionId());
    }

    @Override
    public void onActionClick(ContextualCard contextualCard) {
        final ConditionalContextualCard card = (ConditionalContextualCard) contextualCard;
        mConditionManager.onActionClick(card.getConditionId());
    }

    @Override
    public void onDismissed(ContextualCard contextualCard) {

    }

    @Override
    public void onConditionsChanged() {
        final List<ContextualCard> conditionCards = mConditionManager.getDisplayableCards();

        final boolean isOddNumber = conditionCards.size() % 2 == 1;
        if (isOddNumber) {
            final int lastIndex = conditionCards.size() - 1;
            final ConditionalContextualCard card = (ConditionalContextualCard) conditionCards
                    .get(lastIndex);
            conditionCards.set(lastIndex, card.mutate().setIsHalfWidth(false).build());
        }

        if (mListener != null) {
            final Map<Integer, List<ContextualCard>> conditionalCards = new ArrayMap<>();
            conditionalCards.put(ContextualCard.CardType.CONDITIONAL, conditionCards);
            mListener.onContextualCardUpdated(conditionalCards);
        }
    }
}
