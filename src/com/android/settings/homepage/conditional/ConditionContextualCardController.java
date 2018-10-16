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

package com.android.settings.homepage.conditional;

import android.content.Context;
import android.util.ArrayMap;

import com.android.settings.homepage.ContextualCard;
import com.android.settings.homepage.ContextualCardController;
import com.android.settings.homepage.ContextualCardUpdateListener;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;
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
    public void onConditionsChanged() {
        final List<ContextualCard> conditionCards = new ArrayList<>();
        final List<ConditionalCard> conditionList = mConditionManager.getDisplayableCards();

        for (ConditionalCard condition : conditionList) {
            final ContextualCard conditionCard =
                    new ConditionalContextualCard.Builder()
                            .setConditionId(condition.getId())
                            .setMetricsConstant(condition.getMetricsConstant())
                            .setActionText(condition.getActionText())
                            .setName(mContext.getPackageName() + "/"
                                    + condition.getTitle().toString())
                            .setTitleText(condition.getTitle().toString())
                            .setSummaryText(condition.getSummary().toString())
                            .setIconDrawable(condition.getIcon())
                            .build();

            conditionCards.add(conditionCard);
        }

        if (mListener != null) {
            final Map<Integer, List<ContextualCard>> conditionalCards = new ArrayMap<>();
            conditionalCards.put(ContextualCard.CardType.CONDITIONAL, conditionCards);
            mListener.onContextualCardUpdated(conditionalCards);
        }
    }
}
