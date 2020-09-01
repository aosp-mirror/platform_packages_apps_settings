/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.settings.intelligence.ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE;
import static com.android.settings.intelligence.ContextualCardProto.ContextualCard.Category.STICKY_VALUE;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ContextualCardsDiffCallbackTest {

    private static final Uri TEST_SLICE_URI = Uri.parse("content://test/test");

    private ContextualCardsDiffCallback mDiffCallback;
    private List<ContextualCard> mOldCards;
    private List<ContextualCard> mNewCards;

    @Before
    public void setUp() {
        mOldCards = new ArrayList<>();
        mNewCards = new ArrayList<>();
        mOldCards.add(getContextualCard("test1"));
        mNewCards.add(getContextualCard("test1"));
        mNewCards.add(getContextualCard("test2"));
        mDiffCallback = new ContextualCardsDiffCallback(mOldCards, mNewCards);
    }

    @Test
    public void getOldListSize_oneCard_returnOne() {
        assertThat(mDiffCallback.getOldListSize()).isEqualTo(1);
    }

    @Test
    public void getNewListSize_twoCards_returnTwo() {
        assertThat(mDiffCallback.getNewListSize()).isEqualTo(2);
    }

    @Test
    public void areItemsTheSame_sameItems_returnTrue() {
        assertThat(mDiffCallback.areItemsTheSame(0, 0)).isTrue();
    }

    @Test
    public void areItemsTheSame_differentItems_returnFalse() {
        mOldCards.add(getContextualCard("test3"));

        assertThat(mDiffCallback.areItemsTheSame(1, 1)).isFalse();
    }

    @Test
    public void areContentsTheSame_sameContents_returnTrue() {
        assertThat(mDiffCallback.areContentsTheSame(0, 0)).isTrue();
    }

    @Test
    public void areContentsTheSame_sliceWithToggle_returnFalse() {
        final ContextualCard card = getContextualCard("test1").mutate()
                .setHasInlineAction(true).build();
        mNewCards.add(0, card);

        assertThat(mDiffCallback.areContentsTheSame(0, 0)).isFalse();
    }

    @Test
    public void areContentsTheSame_stickySlice_returnFalse() {
        final ContextualCard card = getContextualCard("test1").mutate()
                .setCategory(STICKY_VALUE).build();
        mNewCards.add(0, card);

        assertThat(mDiffCallback.areContentsTheSame(0, 0)).isFalse();
    }

    @Test
    public void areContentsTheSame_importantSlice_returnFalse() {
        final ContextualCard card = getContextualCard("test1").mutate()
                .setCategory(IMPORTANT_VALUE).build();
        mNewCards.add(0, card);

        assertThat(mDiffCallback.areContentsTheSame(0, 0)).isFalse();
    }

    private ContextualCard getContextualCard(String name) {
        return new ContextualCard.Builder()
                .setName(name)
                .setRankingScore(0.5)
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(TEST_SLICE_URI)
                .build();
    }
}
