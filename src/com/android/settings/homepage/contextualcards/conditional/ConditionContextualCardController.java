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

import androidx.annotation.VisibleForTesting;

import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCardController;
import com.android.settings.homepage.contextualcards.ContextualCardUpdateListener;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This controller triggers the loading of conditional cards and monitors state changes to
 * update the homepage.
 */
public class ConditionContextualCardController implements ContextualCardController,
        ConditionListener, LifecycleObserver, OnStart, OnStop {
    public static final int EXPANDING_THRESHOLD = 0;

    private static final double UNSUPPORTED_RANKING = -99999.0;
    private static final String TAG = "ConditionCtxCardCtrl";
    private static final String CONDITION_FOOTER = "condition_footer";
    private static final String CONDITION_HEADER = "condition_header";

    private final Context mContext;
    private final ConditionManager mConditionManager;

    private ContextualCardUpdateListener mListener;
    private boolean mIsExpanded;

    public ConditionContextualCardController(Context context) {
        mContext = context;
        mConditionManager = new ConditionManager(context.getApplicationContext(), this);
        mConditionManager.startMonitoringStateChange();
    }

    public void setIsExpanded(boolean isExpanded) {
        mIsExpanded = isExpanded;
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
        if (mListener == null) {
            return;
        }
        final List<ContextualCard> conditionCards = mConditionManager.getDisplayableCards();
        final Map<Integer, List<ContextualCard>> conditionalCards =
                buildConditionalCardsWithFooterOrHeader(conditionCards);
        mListener.onContextualCardUpdated(conditionalCards);
    }

    /**
     * According to conditional cards, build a map that includes conditional cards, header card and
     * footer card.
     *
     * Rules:
     * - The last one of conditional cards will be displayed as a full-width card if the size of
     * conditional cards is odd number. The rest will be displayed as a half-width card.
     * - By default conditional cards will be collapsed if there are more than TWO cards.
     *
     * For examples:
     * - Only one conditional card: Returns a map that contains a full-width conditional card,
     * no header card and no footer card.
     * <p>Map{(CONDITIONAL, conditionCards), (CONDITIONAL_FOOTER, EMPTY_LIST), (CONDITIONAL_HEADER,
     * EMPTY_LIST)}</p>
     * - Two conditional cards: Returns a map that contains two half-width conditional cards,
     * no header card and no footer card.
     * <p>Map{(CONDITIONAL, conditionCards), (CONDITIONAL_FOOTER, EMPTY_LIST), (CONDITIONAL_HEADER,
     * EMPTY_LIST)}</p>
     * - Three conditional cards or above: By default, returns a map that contains no conditional
     * card, one header card and no footer card. If conditional cards are expanded, will returns a
     * map that contains three conditional cards, no header card and one footer card.
     * If expanding conditional cards:
     * <p>Map{(CONDITIONAL, conditionCards), (CONDITIONAL_FOOTER, footerCards), (CONDITIONAL_HEADER,
     * EMPTY_LIST)}</p>
     * If collapsing conditional cards:
     * <p>Map{(CONDITIONAL, EMPTY_LIST), (CONDITIONAL_FOOTER, EMPTY_LIST), (CONDITIONAL_HEADER,
     * headerCards)}</p>
     *
     * @param conditionCards A list of conditional cards that are from {@link
     * ConditionManager#getDisplayableCards}
     * @return A map contained three types of lists
     */
    @VisibleForTesting
    Map<Integer, List<ContextualCard>> buildConditionalCardsWithFooterOrHeader(
            List<ContextualCard> conditionCards) {
        final Map<Integer, List<ContextualCard>> conditionalCards = new ArrayMap<>();
        conditionalCards.put(ContextualCard.CardType.CONDITIONAL,
                getExpandedConditionalCards(conditionCards));
        conditionalCards.put(ContextualCard.CardType.CONDITIONAL_FOOTER,
                getConditionalFooterCard(conditionCards));
        conditionalCards.put(ContextualCard.CardType.CONDITIONAL_HEADER,
                getConditionalHeaderCard(conditionCards));
        return conditionalCards;
    }

    private List<ContextualCard> getExpandedConditionalCards(List<ContextualCard> conditionCards) {
        if (conditionCards.isEmpty() || (conditionCards.size() > EXPANDING_THRESHOLD
                && !mIsExpanded)) {
            return Collections.EMPTY_LIST;
        }
        final List<ContextualCard> expandedCards = conditionCards.stream().collect(
                Collectors.toList());
        final boolean isOddNumber = expandedCards.size() % 2 == 1;
        if (isOddNumber) {
            final int lastIndex = expandedCards.size() - 1;
            final ConditionalContextualCard card =
                    (ConditionalContextualCard) expandedCards.get(lastIndex);
            expandedCards.set(lastIndex, card.mutate().setViewType(
                    ConditionContextualCardRenderer.VIEW_TYPE_FULL_WIDTH).build());
        }
        return expandedCards;
    }

    private List<ContextualCard> getConditionalFooterCard(List<ContextualCard> conditionCards) {
        if (!conditionCards.isEmpty() && mIsExpanded
                && conditionCards.size() > EXPANDING_THRESHOLD) {
            final List<ContextualCard> footerCards = new ArrayList<>();
            footerCards.add(new ConditionFooterContextualCard.Builder()
                    .setName(CONDITION_FOOTER)
                    .setRankingScore(UNSUPPORTED_RANKING)
                    .setViewType(ConditionFooterContextualCardRenderer.VIEW_TYPE)
                    .build());
            return footerCards;
        }
        return Collections.EMPTY_LIST;
    }

    private List<ContextualCard> getConditionalHeaderCard(List<ContextualCard> conditionCards) {
        if (!conditionCards.isEmpty() && !mIsExpanded
                && conditionCards.size() > EXPANDING_THRESHOLD) {
            final List<ContextualCard> headerCards = new ArrayList<>();
            headerCards.add(new ConditionHeaderContextualCard.Builder()
                    .setConditionalCards(conditionCards)
                    .setName(CONDITION_HEADER)
                    .setRankingScore(UNSUPPORTED_RANKING)
                    .setViewType(ConditionHeaderContextualCardRenderer.VIEW_TYPE)
                    .build());
            return headerCards;
        }
        return Collections.EMPTY_LIST;
    }
}
