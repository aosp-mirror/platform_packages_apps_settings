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

import android.text.TextUtils;

import com.android.settings.homepage.contextualcards.ContextualCard;

import java.util.List;
import java.util.Objects;

/**
 * Data class representing a condition header {@link ContextualCard}.
 *
 * Use this class to store additional attributes on top of {@link ContextualCard} for
 * {@link ConditionHeaderContextualCardRenderer} and {@link ConditionContextualCardController}.
 */
public class ConditionHeaderContextualCard extends ContextualCard {

    private final List<ContextualCard> mConditionalCards;

    private ConditionHeaderContextualCard(Builder builder) {
        super(builder);
        mConditionalCards = builder.mConditionalCards;
    }

    @Override
    public int getCardType() {
        return CardType.CONDITIONAL_HEADER;
    }

    public List<ContextualCard> getConditionalCards() {
        return mConditionalCards;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), mConditionalCards);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ConditionHeaderContextualCard)) {
            return false;
        }
        final ConditionHeaderContextualCard that = (ConditionHeaderContextualCard) obj;

        return TextUtils.equals(getName(), that.getName()) && mConditionalCards.equals(
                that.mConditionalCards);
    }

    public static class Builder extends ContextualCard.Builder {

        private List<ContextualCard> mConditionalCards;

        public Builder setConditionalCards(List<ContextualCard> conditionalCards) {
            mConditionalCards = conditionalCards;
            return this;
        }

        @Override
        public Builder setCardType(int cardType) {
            throw new IllegalArgumentException(
                    "Cannot change card type for " + getClass().getName());
        }

        public ConditionHeaderContextualCard build() {
            return new ConditionHeaderContextualCard(this);
        }
    }
}