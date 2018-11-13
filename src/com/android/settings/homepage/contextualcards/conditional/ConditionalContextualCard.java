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

import androidx.annotation.VisibleForTesting;

import com.android.settings.homepage.contextualcards.ContextualCard;

/**
 * Data class representing a conditional {@link ContextualCard}.
 *
 * Use this class to store additional attributes on top of {@link ContextualCard} for
 * {@link ConditionalCardController}.
 */
public class ConditionalContextualCard extends ContextualCard {

    @VisibleForTesting
    static final double UNSUPPORTED_RANKING_SCORE = -100.0;

    private final long mConditionId;
    private final int mMetricsConstant;
    private final CharSequence mActionText;

    private ConditionalContextualCard(Builder builder) {
        super(builder);

        mConditionId = builder.mConditionId;
        mMetricsConstant = builder.mMetricsConstant;
        mActionText = builder.mActionText;
    }

    @Override
    public int getCardType() {
        return CardType.CONDITIONAL;
    }

    public long getConditionId() {
        return mConditionId;
    }

    public int getMetricsConstant() {
        return mMetricsConstant;
    }

    public CharSequence getActionText() {
        return mActionText;
    }

    public static class Builder extends ContextualCard.Builder {

        private long mConditionId;
        private int mMetricsConstant;
        private CharSequence mActionText;

        public Builder setConditionId(long id) {
            mConditionId = id;
            return this;
        }

        public Builder setMetricsConstant(int metricsConstant) {
            mMetricsConstant = metricsConstant;
            return this;
        }

        public Builder setActionText(CharSequence actionText) {
            mActionText = actionText;
            return this;
        }

        @Override
        public Builder setCardType(int cardType) {
            throw new IllegalArgumentException(
                    "Cannot change card type for " + getClass().getName());
        }

        public ConditionalContextualCard build() {
            setRankingScore(UNSUPPORTED_RANKING_SCORE);
            return new ConditionalContextualCard(this);
        }
    }
}