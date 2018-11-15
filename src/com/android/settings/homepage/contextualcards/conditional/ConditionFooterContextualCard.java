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

import com.android.settings.homepage.contextualcards.ContextualCard;

/**
 * Data class representing a condition footer {@link ContextualCard}.
 *
 * Use this class for {@link ConditionFooterContextualCardRenderer} and
 * {@link ConditionContextualCardController}.
 */
public class ConditionFooterContextualCard extends ContextualCard {

    private ConditionFooterContextualCard(Builder builder) {
        super(builder);
    }

    @Override
    public int getCardType() {
        return CardType.CONDITIONAL_FOOTER;
    }

    public static class Builder extends ContextualCard.Builder {

        @Override
        public Builder setCardType(int cardType) {
            throw new IllegalArgumentException(
                    "Cannot change card type for " + getClass().getName());
        }

        public ConditionFooterContextualCard build() {
            return new ConditionFooterContextualCard(this);
        }
    }
}