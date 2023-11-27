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

import static com.android.settings.homepage.contextualcards.ContextualCardLoader.DEFAULT_CARD_COUNT;
import static com.android.settings.intelligence.ContextualCardProto.ContextualCard.Category.STICKY_VALUE;

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
import android.provider.Settings;

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

    private Context mContext;
    private ContextualCardLoader mContextualCardLoader;
    private FakeFeatureFactory mFakeFeatureFactory;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mContextualCardLoader = spy(new ContextualCardLoader(mContext));
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @Test
    public void loadInBackground_legacyMode_shouldReturnNothing() {
        assertThat(mContext.getResources().getBoolean(R.bool.config_use_legacy_suggestion))
                .isTrue();

        assertThat(mContextualCardLoader.loadInBackground()).isEmpty();
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
    public void getDisplayableCards_fourEligibleCards_shouldShowDefaultCardCount() {
        final List<ContextualCard> cards = getContextualCardList().stream().limit(4)
                .collect(Collectors.toList());
        doReturn(cards).when(mContextualCardLoader).filterEligibleCards(anyList());

        final List<ContextualCard> result = mContextualCardLoader.getDisplayableCards(cards);

        assertThat(result).hasSize(DEFAULT_CARD_COUNT);
    }

    @Test
    public void getDisplayableCards_oneStickyCard_shouldShowOneStickyCardAtTheTail() {
        final List<ContextualCard> cards = getContextualCardList().stream().limit(5)
                .collect(Collectors.toList());
        doReturn(cards).when(mContextualCardLoader).filterEligibleCards(anyList());

        final List<ContextualCard> result = mContextualCardLoader.getDisplayableCards(cards);

        assertThat(result).hasSize(DEFAULT_CARD_COUNT);
        assertThat(result.get(DEFAULT_CARD_COUNT - 1).getCategory()).isEqualTo(STICKY_VALUE);
    }

    @Test
    public void getDisplayableCards_threeStickyCards_shouldShowThreeStickyCardAtTheTail() {
        final List<ContextualCard> cards = getContextualCardList();
        doReturn(cards).when(mContextualCardLoader).filterEligibleCards(anyList());

        final List<ContextualCard> result = mContextualCardLoader.getDisplayableCards(cards);

        assertThat(result).hasSize(DEFAULT_CARD_COUNT);
        for (int i = 1; i <= Math.min(3, DEFAULT_CARD_COUNT); i++) {
            assertThat(result.get(DEFAULT_CARD_COUNT - i).getCategory()).isEqualTo(STICKY_VALUE);
        }
    }

    @Test
    public void getDisplayableCards_refreshCardUri_shouldLogContextualCard() {
        mContextualCardLoader.mNotifyUri = CardContentProvider.REFRESH_CARD_URI;

        mContextualCardLoader.getDisplayableCards(new ArrayList<>());

        verify(mFakeFeatureFactory.metricsFeatureProvider).action(any(),
                eq(SettingsEnums.ACTION_CONTEXTUAL_CARD_NOT_SHOW), any(String.class));
    }

    @Test
    public void getDisplayableCards_deleteCardUri_shouldNotLogContextualCard() {
        mContextualCardLoader.mNotifyUri = CardContentProvider.DELETE_CARD_URI;

        mContextualCardLoader.getDisplayableCards(new ArrayList<>());

        verify(mFakeFeatureFactory.metricsFeatureProvider, never()).action(any(),
                eq(SettingsEnums.ACTION_CONTEXTUAL_CARD_NOT_SHOW), any(String.class));
    }

    @Test
    public void getCardCount_noConfiguredCardCount_returnDefaultCardCount() {
        assertThat(mContextualCardLoader.getCardCount()).isEqualTo(DEFAULT_CARD_COUNT);
    }

    @Test
    public void getCardCount_hasConfiguredCardCount_returnConfiguredCardCount() {
        int configCount = 4;
        Settings.Global.putInt(mContext.getContentResolver(),
                ContextualCardLoader.CONTEXTUAL_CARD_COUNT, configCount);

        assertThat(mContextualCardLoader.getCardCount()).isEqualTo(configCount);
    }

    private List<ContextualCard> getContextualCardList() {
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(new ContextualCard.Builder()
                .setName("test_low_storage")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.LOW_STORAGE_SLICE_URI)
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_flashlight")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(
                        "content://com.android.settings.test.slices/action/flashlight"))
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_dark_theme")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.DARK_THEME_SLICE_URI)
                .setCategory(STICKY_VALUE)
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_gesture")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(
                        "content://com.android.settings.test.slices/action/gesture_pick_up"))
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_connected")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.BLUETOOTH_DEVICES_SLICE_URI)
                .setCategory(STICKY_VALUE)
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_sticky")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse("content://com.android.settings.test.slices/action/sticky"))
                .setCategory(STICKY_VALUE)
                .build());
        return cards;
    }
}
