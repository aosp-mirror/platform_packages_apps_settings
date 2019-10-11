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

package com.android.settings.homepage.contextualcards.legacysuggestion;

import android.app.PendingIntent;
import android.service.settings.suggestions.Suggestion;

import com.android.settings.homepage.contextualcards.ContextualCard;

public class LegacySuggestionContextualCard extends ContextualCard {

    private final PendingIntent mPendingIntent;
    private final Suggestion mSuggestion;

    public LegacySuggestionContextualCard(Builder builder) {
        super(builder);
        mPendingIntent = builder.mPendingIntent;
        mSuggestion = builder.mSuggestion;
    }

    @Override
    public int getCardType() {
        return CardType.LEGACY_SUGGESTION;
    }

    public PendingIntent getPendingIntent() {
        return mPendingIntent;
    }

    public Suggestion getSuggestion() {
        return mSuggestion;
    }

    public static class Builder extends ContextualCard.Builder {

        private PendingIntent mPendingIntent;
        private Suggestion mSuggestion;

        public Builder setPendingIntent(PendingIntent pendingIntent) {
            mPendingIntent = pendingIntent;
            return this;
        }

        public Builder setSuggestion(Suggestion suggestion) {
            mSuggestion = suggestion;
            return this;
        }

        @Override
        public Builder setCardType(int cardType) {
            throw new IllegalArgumentException(
                    "Cannot change card type for " + getClass().getName());
        }

        public LegacySuggestionContextualCard build() {
            return new LegacySuggestionContextualCard(this);
        }
    }

}
