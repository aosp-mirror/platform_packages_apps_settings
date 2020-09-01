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

package com.android.settings.homepage.contextualcards;

import static com.android.settings.intelligence.ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE;
import static com.android.settings.intelligence.ContextualCardProto.ContextualCard.Category.STICKY_VALUE;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

/**
 * A DiffCallback to calculate the difference between old and new {@link ContextualCard} List.
 */
public class ContextualCardsDiffCallback extends DiffUtil.Callback {

    private final List<ContextualCard> mOldCards;
    private final List<ContextualCard> mNewCards;

    public ContextualCardsDiffCallback(List<ContextualCard> oldCards,
            List<ContextualCard> newCards) {
        mOldCards = oldCards;
        mNewCards = newCards;
    }

    @Override
    public int getOldListSize() {
        return mOldCards.size();
    }

    @Override
    public int getNewListSize() {
        return mNewCards.size();
    }

    @Override
    public boolean areItemsTheSame(int oldCardPosition, int newCardPosition) {
        return mOldCards.get(oldCardPosition).getName().equals(
                mNewCards.get(newCardPosition).getName());
    }

    @Override
    public boolean areContentsTheSame(int oldCardPosition, int newCardPosition) {
        final ContextualCard newCard = mNewCards.get(newCardPosition);
        // Sticky, important, or toggleable slices need to be updated continuously, which means
        // their contents may change. So here we assume the content will always be different to
        // force view rebinding.
        if (newCard.getCategory() == STICKY_VALUE || newCard.getCategory() == IMPORTANT_VALUE
                || newCard.hasInlineAction()) {
            return false;
        }
        return mOldCards.get(oldCardPosition).equals(newCard);
    }
}