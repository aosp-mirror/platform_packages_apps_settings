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

import static android.app.slice.Slice.HINT_ERROR;

import static com.android.settings.homepage.contextualcards.ContextualCardLoader.DEFAULT_CARD_COUNT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.Uri;

import androidx.slice.Slice;

import com.android.settings.R;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
public class ContextualCardLoaderTest {

    private static final String TEST_SLICE_URI = "content://test/test";

    private Context mContext;
    private ContextualCardLoader mContextualCardLoader;
    private EligibleCardChecker mEligibleCardChecker;
    private FakeFeatureFactory mFakeFeatureFactory;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mContextualCardLoader = spy(new ContextualCardLoader(mContext));
        mEligibleCardChecker =
                spy(new EligibleCardChecker(mContext, getContextualCard(TEST_SLICE_URI)));
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @Test
    public void isCardEligibleToDisplay_customCard_returnTrue() {
        final ContextualCard customCard = new ContextualCard.Builder()
                .setName("custom_card")
                .setCardType(ContextualCard.CardType.DEFAULT)
                .setTitleText("custom_title")
                .setSummaryText("custom_summary")
                .build();

        assertThat(mEligibleCardChecker.isCardEligibleToDisplay(customCard)).isTrue();
    }

    @Test
    public void isCardEligibleToDisplay_invalidScheme_returnFalse() {
        final String sliceUri = "contet://com.android.settings.slices/action/flashlight";

        assertThat(mEligibleCardChecker.isCardEligibleToDisplay(getContextualCard(sliceUri)))
                .isFalse();
    }

    @Test
    public void isCardEligibleToDisplay_invalidRankingScore_returnFalse() {
        final ContextualCard card = new ContextualCard.Builder()
                .setName("test_card")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.FLASHLIGHT_SLICE_URI)
                .setRankingScore(-1)
                .build();

        assertThat(mEligibleCardChecker.isCardEligibleToDisplay(card))
                .isFalse();
    }

    @Test
    public void isCardEligibleToDisplay_nullSlice_returnFalse() {
        doReturn(null).when(mEligibleCardChecker).bindSlice(Uri.parse(TEST_SLICE_URI));

        assertThat(mEligibleCardChecker.isCardEligibleToDisplay(getContextualCard(TEST_SLICE_URI)))
                .isFalse();
    }

    @Test
    public void isCardEligibleToDisplay_errorSlice_returnFalse() {
        final Slice slice = new Slice.Builder(Uri.parse(TEST_SLICE_URI))
                .addHints(HINT_ERROR).build();
        doReturn(slice).when(mEligibleCardChecker).bindSlice(Uri.parse(TEST_SLICE_URI));

        assertThat(mEligibleCardChecker.isCardEligibleToDisplay(getContextualCard(TEST_SLICE_URI)))
                .isFalse();
    }

    @Test
    public void getDisplayableCards_twoEligibleCards_shouldShowAll() {
        final List<ContextualCard> cards = getContextualCardList().stream().limit(2)
                .collect(Collectors.toList());
        doReturn(cards).when(mContextualCardLoader).filterEligibleCards(anyList());

        final List<ContextualCard> result = mContextualCardLoader.getDisplayableCards(cards);

        assertThat(result).hasSize(cards.size());
    }

    @Test
    public void getDisplayableCards_fiveEligibleCardsNoLarge_shouldShowDefaultCardCount() {
        final List<ContextualCard> fiveCards = getContextualCardListWithNoLargeCard();
        doReturn(fiveCards).when(mContextualCardLoader).filterEligibleCards(anyList());

        final List<ContextualCard> result = mContextualCardLoader.getDisplayableCards(
                fiveCards);

        assertThat(result).hasSize(DEFAULT_CARD_COUNT);
    }

    @Test
    public void getDisplayableCards_threeEligibleCardsOneLarge_shouldShowThreeCards() {
        final List<ContextualCard> cards = getContextualCardList().stream().limit(2)
                .collect(Collectors.toList());
        cards.add(new ContextualCard.Builder()
                .setName("test_gesture")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(
                        "content://com.android.settings.test.slices/action/gesture_pick_up"))
                .build());
        doReturn(cards).when(mContextualCardLoader).filterEligibleCards(anyList());

        final List<ContextualCard> result = mContextualCardLoader.getDisplayableCards(cards);

        assertThat(result).hasSize(3);
    }

    @Test
    public void getDisplayableCards_threeEligibleCardsTwoLarge_shouldShowTwoCards() {
        final List<ContextualCard> threeCards = getContextualCardList().stream().limit(3)
                .collect(Collectors.toList());
        doReturn(threeCards).when(mContextualCardLoader).filterEligibleCards(anyList());

        final List<ContextualCard> result = mContextualCardLoader.getDisplayableCards(
                threeCards);

        assertThat(result).hasSize(2);
    }

    @Test
    public void loadInBackground_legacyMode_shouldReturnNothing() {
        assertThat(mContext.getResources().getBoolean(R.bool.config_use_legacy_suggestion))
                .isTrue();

        assertThat(mContextualCardLoader.loadInBackground()).isEmpty();
    }

    @Test
    public void getDisplayableCards_refreshCardUri_shouldLogContextualCardDisplay() {
        mContextualCardLoader.mNotifyUri = CardContentProvider.REFRESH_CARD_URI;

        mContextualCardLoader.getDisplayableCards(new ArrayList<>());

        verify(mFakeFeatureFactory.metricsFeatureProvider).action(any(),
                eq(SettingsEnums.ACTION_CONTEXTUAL_CARD_SHOW), any(String.class));
        verify(mFakeFeatureFactory.metricsFeatureProvider).action(any(),
                eq(SettingsEnums.ACTION_CONTEXTUAL_CARD_NOT_SHOW), any(String.class));
    }

    @Test
    public void getDisplayableCards_deleteCardUri_shouldNotLogContextualCardDisplay() {
        mContextualCardLoader.mNotifyUri = CardContentProvider.DELETE_CARD_URI;

        mContextualCardLoader.getDisplayableCards(new ArrayList<>());

        verify(mFakeFeatureFactory.mContextualCardFeatureProvider, never())
                .logContextualCardDisplay(anyList(), anyList());
    }

    private ContextualCard getContextualCard(String sliceUri) {
        return new ContextualCard.Builder()
                .setName("test_card")
                .setRankingScore(0.5)
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(sliceUri))
                .build();
    }

    private List<ContextualCard> getContextualCardList() {
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(new ContextualCard.Builder()
                .setName("test_wifi")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.CONTEXTUAL_WIFI_SLICE_URI)
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_flashlight")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(
                        Uri.parse("content://com.android.settings.test.slices/action/flashlight"))
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_connected")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.BLUETOOTH_DEVICES_SLICE_URI)
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_gesture")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(
                        "content://com.android.settings.test.slices/action/gesture_pick_up"))
                .build());
        return cards;
    }

    private List<ContextualCard> getContextualCardListWithNoLargeCard() {
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(new ContextualCard.Builder()
                .setName("test_rotate")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(
                        Uri.parse("content://com.android.settings.test.slices/action/auto_rotate"))
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_flashlight")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(
                        Uri.parse("content://com.android.settings.test.slices/action/flashlight"))
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_bt")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse("content://android.settings.test.slices/action/bluetooth"))
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_gesture")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(
                        "content://com.android.settings.test.slices/action/gesture_pick_up"))
                .build());
        return cards;
    }
}
