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

import static com.android.settings.homepage.contextualcards.ContextualCardManager.KEY_CONTEXTUAL_CARDS;
import static com.android.settings.homepage.contextualcards.slices.SliceContextualCardRenderer.VIEW_TYPE_FULL_WIDTH;
import static com.android.settings.homepage.contextualcards.slices.SliceContextualCardRenderer.VIEW_TYPE_HALF_WIDTH;
import static com.android.settings.homepage.contextualcards.slices.SliceContextualCardRenderer.VIEW_TYPE_STICKY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.FeatureFlagUtils;

import androidx.loader.app.LoaderManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.FeatureFlags;
import com.android.settings.homepage.contextualcards.conditional.ConditionContextualCardController;
import com.android.settings.homepage.contextualcards.conditional.ConditionFooterContextualCard;
import com.android.settings.homepage.contextualcards.conditional.ConditionHeaderContextualCard;
import com.android.settings.homepage.contextualcards.conditional.ConditionalContextualCard;
import com.android.settings.intelligence.ContextualCardProto;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSubscriptionManager;
import org.robolectric.shadows.ShadowTelephonyManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
public class ContextualCardManagerTest {
    private static final int SUB_ID = 2;

    private static final String TEST_SLICE_URI = "content://test/test";
    private static final String TEST_SLICE_NAME = "test_name";

    @Mock
    ContextualCardUpdateListener mListener;
    @Mock
    Lifecycle mLifecycle;
    @Mock
    LoaderManager mLoaderManager;

    private Context mContext;
    private ShadowSubscriptionManager mShadowSubscriptionManager;
    private ShadowTelephonyManager mShadowTelephonyManager;
    private ContextualCardManager mManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.CONDITIONAL_CARDS, true);

        mShadowSubscriptionManager = shadowOf(
                mContext.getSystemService(SubscriptionManager.class));
        mShadowSubscriptionManager.setDefaultDataSubscriptionId(SUB_ID);

        final TelephonyManager telephonyManager =
                mContext.getSystemService(TelephonyManager.class);
        mShadowTelephonyManager = shadowOf(telephonyManager);
        mShadowTelephonyManager.setTelephonyManagerForSubscriptionId(SUB_ID, telephonyManager);

        mManager = new ContextualCardManager(mContext, mLifecycle, null /* bundle */);
    }

    @Test
    public void constructor_noSavedInstanceState_shouldSetFirstLaunch() {
        assertThat(mManager.mIsFirstLaunch).isTrue();
    }

    @Test
    public void constructor_noSavedInstanceState_shouldNotHaveSavedCards() {
        assertThat(mManager.mSavedCards).isNull();
    }

    @Test
    public void constructor_hasSavedInstanceState_shouldContainExpectedSavedCards() {
        final Bundle outState = new Bundle();
        final ArrayList<String> cards = getContextualCardList().stream()
                .map(ContextualCard::getName)
                .collect(Collectors.toCollection(ArrayList::new));
        outState.putStringArrayList(KEY_CONTEXTUAL_CARDS, cards);

        mManager = new ContextualCardManager(mContext, mLifecycle, outState);

        final List<String> actualCards = mManager.mSavedCards.stream().collect(Collectors.toList());
        final List<String> expectedCards = Arrays.asList("test_low_storage", "test_flashlight",
                "test_dark_theme", "test_gesture", "test_face_enroll");
        assertThat(actualCards).containsExactlyElementsIn(expectedCards);
    }

    @Test
    public void constructor_hasSettingsCustomCards_shouldSetUpCustomControllers() {
        final ControllerRendererPool pool = mManager.getControllerRendererPool();

        final List<Integer> actual = pool.getControllers().stream()
                .map(ContextualCardController::getCardType)
                .collect(Collectors.toList());
        final List<Integer> expected = Arrays.asList(ContextualCard.CardType.CONDITIONAL,
                ContextualCard.CardType.LEGACY_SUGGESTION);
        assertThat(actual).containsExactlyElementsIn(expected);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void loadContextualCards_restartLoaderNotNeeded_shouldInitLoader() {
        mManager.loadContextualCards(mLoaderManager, false /* restartLoaderNeeded */);

        verify(mLoaderManager).initLoader(anyInt(), nullable(Bundle.class),
                any(ContextualCardManager.CardContentLoaderCallbacks.class));
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void loadContextualCards_restartLoaderNeeded_shouldRestartLoaderAndSetIsFirstLaunch() {
        mManager.mIsFirstLaunch = false;

        mManager.loadContextualCards(mLoaderManager, true /* restartLoaderNeeded */);

        verify(mLoaderManager).restartLoader(anyInt(), nullable(Bundle.class),
                any(ContextualCardManager.CardContentLoaderCallbacks.class));
        assertThat(mManager.mIsFirstLaunch).isTrue();
    }

    @Test
    public void getSettingsCards_conditionalsEnabled_shouldContainLegacyAndConditionals() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.CONDITIONAL_CARDS, true);
        final int[] expected = {ContextualCard.CardType.CONDITIONAL,
                ContextualCard.CardType.LEGACY_SUGGESTION};

        final int[] actual = mManager.getSettingsCards();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void getSettingsCards_conditionalsDisabled_shouldContainLegacy() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.CONDITIONAL_CARDS, false);
        final int[] expected = {ContextualCard.CardType.LEGACY_SUGGESTION};

        final int[] actual = mManager.getSettingsCards();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void setupController_notLifecycleObserverInstance_shouldNotAttachToLifecycle() {
        // 3 invocations in constructor(ContextualCardManager, Conditional and LegacySuggestion)
        verify(mLifecycle, times(3)).addObserver(any(LifecycleObserver.class));

        mManager.setupController(ContextualCard.CardType.SLICE);

        // After 3 times call in the constructor, addObserver() shouldn't be called again.
        verify(mLifecycle, times(3)).addObserver(any(LifecycleObserver.class));
    }

    @Test
    public void sortCards_shouldBeDescendingOrder() {
        final List<ContextualCard> cards = new ArrayList<>();
        final ContextualCard card1 =
                buildContextualCard(TEST_SLICE_URI).mutate().setRankingScore(99.0).build();
        final ContextualCard card2 =
                buildContextualCard("content://test/test2").mutate().setRankingScore(88.0).build();
        cards.add(card1);
        cards.add(card2);

        final List<ContextualCard> sortedCards = mManager.sortCards(cards);

        assertThat(sortedCards.get(0).getSliceUri()).isEqualTo(Uri.parse(TEST_SLICE_URI));
    }

    @Test
    public void sortCards_hasConditionalAndSliceCards_conditionalShouldAlwaysBeTheLast() {
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(new ConditionalContextualCard.Builder().build());
        cards.add(buildContextualCard(TEST_SLICE_URI));

        final List<ContextualCard> sortedCards = mManager.sortCards(cards);

        assertThat(sortedCards.get(cards.size() - 1).getCardType())
                .isEqualTo(ContextualCard.CardType.CONDITIONAL);
    }

    @Test
    public void sortCards_hasStickyCards_stickyShouldAlwaysBeTheLast() {
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(buildContextualCard(CustomSliceRegistry.FACE_ENROLL_SLICE_URI,
                ContextualCardProto.ContextualCard.Category.STICKY_VALUE, 1.02f));
        cards.add(buildContextualCard(CustomSliceRegistry.BLUETOOTH_DEVICES_SLICE_URI,
                ContextualCardProto.ContextualCard.Category.STICKY_VALUE, 1.01f));
        cards.add(buildContextualCard(CustomSliceRegistry.LOW_STORAGE_SLICE_URI,
                ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE, 0.01f));

        final List<ContextualCard> sortedCards = mManager.sortCards(cards);

        assertThat(sortedCards.get(cards.size() - 1).getSliceUri())
                .isEqualTo(CustomSliceRegistry.BLUETOOTH_DEVICES_SLICE_URI);
        assertThat(sortedCards.get(cards.size() - 2).getSliceUri())
                .isEqualTo(CustomSliceRegistry.FACE_ENROLL_SLICE_URI);
    }

    @Test
    public void onContextualCardUpdated_emptyMapWithExistingCards_shouldOnlyKeepConditionalCard() {
        mManager.mContextualCards.add(new ConditionalContextualCard.Builder().build());
        mManager.mContextualCards.add(
                buildContextualCard(TEST_SLICE_URI));
        mManager.setListener(mListener);

        //Simulate database returns no contents.
        mManager.onContextualCardUpdated(new ArrayMap<>());

        assertThat(mManager.mContextualCards).hasSize(1);
        assertThat(mManager.mContextualCards.get(0).getCardType())
                .isEqualTo(ContextualCard.CardType.CONDITIONAL);
    }

    @Test
    public void onContextualCardUpdated_hasEmptyMap_shouldKeepConditionalHeaderCard() {
        mManager.mContextualCards.add(new ConditionHeaderContextualCard.Builder().build());
        mManager.setListener(mListener);

        mManager.onContextualCardUpdated(new ArrayMap<>());

        assertThat(mManager.mContextualCards).hasSize(1);
        assertThat(mManager.mContextualCards.get(0).getCardType())
                .isEqualTo(ContextualCard.CardType.CONDITIONAL_HEADER);
    }

    @Test
    public void onContextualCardUpdated_hasEmptyMap_shouldKeepConditionalFooterCard() {
        mManager.mContextualCards.add(new ConditionFooterContextualCard.Builder().build());
        mManager.setListener(mListener);

        mManager.onContextualCardUpdated(new ArrayMap<>());

        assertThat(mManager.mContextualCards).hasSize(1);
        assertThat(mManager.mContextualCards.get(0).getCardType())
                .isEqualTo(ContextualCard.CardType.CONDITIONAL_FOOTER);
    }

    @Test
    public void getCardLoaderTimeout_noConfiguredTimeout_shouldReturnDefaultTimeout() {
        final long timeout = mManager.getCardLoaderTimeout();

        assertThat(timeout).isEqualTo(ContextualCardManager.CARD_CONTENT_LOADER_TIMEOUT_MS);
    }

    @Test
    public void getCardLoaderTimeout_hasConfiguredTimeout_shouldReturnConfiguredTimeout() {
        final long configuredTimeout = 5000L;
        Settings.Global.putLong(mContext.getContentResolver(),
                ContextualCardManager.KEY_GLOBAL_CARD_LOADER_TIMEOUT, configuredTimeout);

        final long timeout = mManager.getCardLoaderTimeout();

        assertThat(timeout).isEqualTo(configuredTimeout);
    }

    @Test
    public void onFinishCardLoading_fastLoad_shouldCallOnContextualCardUpdated() {
        mManager.mStartTime = System.currentTimeMillis();
        final ContextualCardManager manager = spy(mManager);
        doNothing().when(manager).onContextualCardUpdated(anyMap());

        manager.onFinishCardLoading(new ArrayList<>());

        verify(manager).onContextualCardUpdated(nullable(Map.class));
    }

    @Test
    public void onFinishCardLoading_slowLoad_shouldSkipOnContextualCardUpdated() {
        mManager.mStartTime = 0;
        final ContextualCardManager manager = spy(mManager);
        doNothing().when(manager).onContextualCardUpdated(anyMap());

        manager.onFinishCardLoading(new ArrayList<>());

        verify(manager, never()).onContextualCardUpdated(anyMap());
    }

    @Test
    public void onFinishCardLoading_newLaunch_twoLoadedCards_shouldShowTwoCards() {
        mManager.mStartTime = System.currentTimeMillis();
        mManager.setListener(mListener);
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(buildContextualCard(TEST_SLICE_URI));
        cards.add(buildContextualCard(TEST_SLICE_URI));

        mManager.onFinishCardLoading(cards);

        assertThat(mManager.mContextualCards).hasSize(2);
    }

    @Test
    public void onFinishCardLoading_newLaunch_shouldSetIsFirstLaunchBackToFalse() {
        assertThat(mManager.mIsFirstLaunch).isTrue();

        mManager.onFinishCardLoading(new ArrayList<>());

        assertThat(mManager.mIsFirstLaunch).isFalse();
    }

    @Test
    public void onFinishCardLoading_hasSavedCard_shouldOnlyShowSavedCard() {
        // test screen rotation
        mManager.setListener(mListener);
        final List<String> savedCardNames = new ArrayList<>();
        savedCardNames.add(TEST_SLICE_NAME);
        mManager.mIsFirstLaunch = false;
        mManager.mSavedCards = savedCardNames;
        final ContextualCard newCard =
                new ContextualCard.Builder()
                        .setName("test_name2")
                        .setCardType(ContextualCard.CardType.SLICE)
                        .setSliceUri(Uri.parse("content://test/test2"))
                        .build();
        final List<ContextualCard> loadedCards = new ArrayList<>();
        loadedCards.add(buildContextualCard(TEST_SLICE_URI));
        loadedCards.add(newCard);

        mManager.onFinishCardLoading(loadedCards);

        final List<String> actualCards = mManager.mContextualCards.stream()
                .map(ContextualCard::getName)
                .collect(Collectors.toList());
        final List<String> expectedCards = Arrays.asList(TEST_SLICE_NAME);
        assertThat(actualCards).containsExactlyElementsIn(expectedCards);
    }

    @Test
    public void onFinishCardLoading_reloadData_hasNewCard_shouldOnlyShowOldCard() {
        // test card dismissal cases
        mManager.setListener(mListener);
        mManager.mIsFirstLaunch = false;
        //old card
        mManager.mContextualCards.add(buildContextualCard(TEST_SLICE_URI));
        final ContextualCard newCard =
                new ContextualCard.Builder()
                        .setName("test_name2")
                        .setCardType(ContextualCard.CardType.SLICE)
                        .setSliceUri(Uri.parse("content://test/test2"))
                        .build();
        final List<ContextualCard> loadedCards = new ArrayList<>();
        loadedCards.add(buildContextualCard(TEST_SLICE_URI));
        loadedCards.add(newCard);

        mManager.onFinishCardLoading(loadedCards);

        final List<String> actualCards = mManager.mContextualCards.stream()
                .map(ContextualCard::getName)
                .collect(Collectors.toList());
        final List<String> expectedCards = Arrays.asList(TEST_SLICE_NAME);
        assertThat(actualCards).containsExactlyElementsIn(expectedCards);
    }

    @Test
    public void onWindowFocusChanged_hasFocusAndNoConditional_startMonitoringConditionCard() {
        final ContextualCardManager manager = spy(mManager);
        final ControllerRendererPool pool = spy(mManager.mControllerRendererPool);
        doReturn(pool).when(manager).getControllerRendererPool();
        final ConditionContextualCardController conditionController =
                pool.getController(mContext,
                        ContextualCard.CardType.CONDITIONAL);
        final OnStart controller = spy((OnStart) conditionController);
        doReturn(controller).when(pool).getController(mContext,
                ContextualCard.CardType.CONDITIONAL);

        manager.onWindowFocusChanged(true /* hasWindowFocus */);

        verify(controller, atLeast(1)).onStart();
    }

    @Test
    public void onWindowFocusChanged_hasFocusAndHasConditionals_startMonitoringConditionCard() {
        mManager.mContextualCards.add(new ConditionalContextualCard.Builder().build());
        mManager.mContextualCards.add(new ConditionFooterContextualCard.Builder().build());
        final ContextualCardManager manager = spy(mManager);
        final ControllerRendererPool pool = spy(mManager.mControllerRendererPool);
        doReturn(pool).when(manager).getControllerRendererPool();
        final ConditionContextualCardController conditionController =
                pool.getController(mContext,
                        ContextualCard.CardType.CONDITIONAL);
        final OnStart controller = spy((OnStart) conditionController);
        doReturn(controller).when(pool).getController(mContext,
                ContextualCard.CardType.CONDITIONAL);

        manager.onWindowFocusChanged(true /* hasWindowFocus */);

        verify(controller, atLeast(2)).onStart();
    }

    @Test
    public void onWindowFocusChanged_loseFocusAndHasConditionals_stopMonitoringConditionCard() {
        mManager.mContextualCards.add(new ConditionalContextualCard.Builder().build());
        mManager.mContextualCards.add(new ConditionFooterContextualCard.Builder().build());
        final ContextualCardManager manager = spy(mManager);
        final ControllerRendererPool pool = spy(mManager.mControllerRendererPool);
        doReturn(pool).when(manager).getControllerRendererPool();
        final ConditionContextualCardController conditionController =
                pool.getController(mContext,
                        ContextualCard.CardType.CONDITIONAL);
        final OnStop controller = spy((OnStop) conditionController);
        doReturn(controller).when(pool).getController(mContext,
                ContextualCard.CardType.CONDITIONAL);

        manager.onWindowFocusChanged(false /* hasWindowFocus */);

        verify(controller, atLeast(2)).onStop();
    }

    @Test
    public void onWindowFocusChanged_loseFocusAndNoConditional_stopMonitoringConditionCard() {
        final ContextualCardManager manager = spy(mManager);
        final ControllerRendererPool pool = spy(mManager.mControllerRendererPool);
        doReturn(pool).when(manager).getControllerRendererPool();
        final ConditionContextualCardController conditionController =
                pool.getController(mContext,
                        ContextualCard.CardType.CONDITIONAL);
        final OnStop controller = spy((OnStop) conditionController);
        doReturn(controller).when(pool).getController(mContext,
                ContextualCard.CardType.CONDITIONAL);

        manager.onWindowFocusChanged(false /* hasWindowFocus */);

        verify(controller, atLeast(1)).onStop();
    }

    @Test
    public void getCardsWithViewType_noSuggestionCards_shouldNotHaveHalfCards() {
        final List<Integer> categories = Arrays.asList(
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE,
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE,
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE,
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE,
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE
        );
        final List<ContextualCard> noSuggestionCards = buildCategoriedCards(getContextualCardList(),
                categories);

        final List<ContextualCard> result = mManager.getCardsWithViewType(noSuggestionCards);

        assertThat(result).hasSize(5);
        for (ContextualCard card : result) {
            assertThat(card.getViewType()).isEqualTo(VIEW_TYPE_FULL_WIDTH);
        }
    }

    @Test
    public void getCardsWithViewType_oneSuggestionCards_shouldNotHaveHalfCards() {
        final List<Integer> categories = Arrays.asList(
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE,
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE,
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE,
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE,
                ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE
        );
        final List<ContextualCard> oneSuggestionCards = buildCategoriedCards(
                getContextualCardList(), categories);

        final List<ContextualCard> result = mManager.getCardsWithViewType(oneSuggestionCards);

        assertThat(result).hasSize(5);
        for (ContextualCard card : result) {
            assertThat(card.getViewType()).isEqualTo(VIEW_TYPE_FULL_WIDTH);
        }
    }

    @Test
    public void getCardsWithViewType_twoConsecutiveSuggestionCards_shouldHaveTwoHalfCards() {
        final List<Integer> categories = Arrays.asList(
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE,
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE,
                ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE,
                ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE,
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE
        );
        final List<ContextualCard> twoConsecutiveSuggestionCards = buildCategoriedCards(
                getContextualCardList(), categories);
        final List<Integer> expectedValues = Arrays.asList(VIEW_TYPE_FULL_WIDTH,
                VIEW_TYPE_FULL_WIDTH, VIEW_TYPE_HALF_WIDTH, VIEW_TYPE_HALF_WIDTH,
                VIEW_TYPE_FULL_WIDTH);

        final List<ContextualCard> result = mManager.getCardsWithViewType(
                twoConsecutiveSuggestionCards);

        assertThat(result).hasSize(5);
        for (int i = 0; i < result.size(); i++) {
            assertThat(result.get(i).getViewType()).isEqualTo(expectedValues.get(i));
        }
    }

    @Test
    public void getCardsWithViewType_twoNonConsecutiveSuggestionCards_shouldNotHaveHalfCards() {
        final List<Integer> categories = Arrays.asList(
                ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE,
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE,
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE,
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE,
                ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE
        );
        final List<ContextualCard> twoNonConsecutiveSuggestionCards = buildCategoriedCards(
                getContextualCardList(), categories);

        final List<ContextualCard> result = mManager.getCardsWithViewType(
                twoNonConsecutiveSuggestionCards);

        assertThat(result).hasSize(5);
        for (ContextualCard card : result) {
            assertThat(card.getViewType()).isEqualTo(VIEW_TYPE_FULL_WIDTH);
        }
    }

    @Test
    public void getCardsWithViewType_threeConsecutiveSuggestionCards_shouldHaveTwoHalfCards() {
        final List<Integer> categories = Arrays.asList(
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE,
                ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE,
                ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE,
                ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE,
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE
        );
        final List<ContextualCard> threeConsecutiveSuggestionCards = buildCategoriedCards(
                getContextualCardList(), categories);
        final List<Integer> expectedValues = Arrays.asList(VIEW_TYPE_FULL_WIDTH,
                VIEW_TYPE_HALF_WIDTH, VIEW_TYPE_HALF_WIDTH, VIEW_TYPE_FULL_WIDTH,
                VIEW_TYPE_FULL_WIDTH);

        final List<ContextualCard> result = mManager.getCardsWithViewType(
                threeConsecutiveSuggestionCards);

        assertThat(result).hasSize(5);
        for (int i = 0; i < result.size(); i++) {
            assertThat(result.get(i).getViewType()).isEqualTo(expectedValues.get(i));
        }
    }

    @Test
    public void getCardsWithViewType_fourConsecutiveSuggestionCards_shouldHaveFourHalfCards() {
        final List<Integer> categories = Arrays.asList(
                ContextualCardProto.ContextualCard.Category.IMPORTANT_VALUE,
                ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE,
                ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE,
                ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE,
                ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE
        );
        final List<ContextualCard> fourConsecutiveSuggestionCards = buildCategoriedCards(
                getContextualCardList(), categories);
        final List<Integer> expectedValues = Arrays.asList(VIEW_TYPE_FULL_WIDTH,
                VIEW_TYPE_HALF_WIDTH, VIEW_TYPE_HALF_WIDTH, VIEW_TYPE_HALF_WIDTH,
                VIEW_TYPE_HALF_WIDTH);

        final List<ContextualCard> result = mManager.getCardsWithViewType(
                fourConsecutiveSuggestionCards);

        assertThat(result).hasSize(5);
        for (int i = 0; i < result.size(); i++) {
            assertThat(result.get(i).getViewType()).isEqualTo(expectedValues.get(i));
        }
    }

    @Test
    public void getCardsWithViewType_hasOneStickySlice_shouldHaveOneStickyCard() {
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(buildContextualCard(CustomSliceRegistry.FACE_ENROLL_SLICE_URI.toString()));
        cards.add(buildContextualCard(CustomSliceRegistry.LOW_STORAGE_SLICE_URI.toString()));
        final List<Integer> categories = Arrays.asList(
                ContextualCardProto.ContextualCard.Category.STICKY_VALUE,
                ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE
        );
        final List<ContextualCard> cardListWithWifi = buildCategoriedCards(cards, categories);

        final List<ContextualCard> result = mManager.getCardsWithViewType(cardListWithWifi);

        assertThat(result).hasSize(cards.size());
        assertThat(result.get(0).getViewType()).isEqualTo(VIEW_TYPE_STICKY);
        assertThat(result.get(1).getViewType()).isEqualTo(VIEW_TYPE_FULL_WIDTH);
    }

    @Test
    public void getCardsToKeep_hasSavedCard_shouldResetSavedCards() {
        final List<String> savedCardNames = new ArrayList<>();
        savedCardNames.add(TEST_SLICE_NAME);
        mManager.mSavedCards = savedCardNames;

        mManager.getCardsToKeep(getContextualCardList());

        assertThat(mManager.mSavedCards).isNull();
    }

    private ContextualCard buildContextualCard(String sliceUri) {
        return new ContextualCard.Builder()
                .setName(TEST_SLICE_NAME)
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(sliceUri))
                .setViewType(VIEW_TYPE_FULL_WIDTH)
                .build();
    }

    private ContextualCard buildContextualCard(Uri uri, int category, double rankingScore) {
        return new ContextualCard.Builder()
                .setName(uri.toString())
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(uri)
                .setViewType(VIEW_TYPE_FULL_WIDTH)
                .setCategory(category)
                .setRankingScore(rankingScore)
                .build();
    }

    private List<ContextualCard> buildCategoriedCards(List<ContextualCard> cards,
            List<Integer> categories) {
        final List<ContextualCard> result = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            result.add(cards.get(i).mutate().setCategory(categories.get(i)).build());
        }
        return result;
    }

    private List<ContextualCard> getContextualCardList() {
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(new ContextualCard.Builder()
                .setName("test_low_storage")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.LOW_STORAGE_SLICE_URI)
                .setViewType(VIEW_TYPE_FULL_WIDTH)
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_flashlight")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(
                        "content://com.android.settings.test.slices/action/flashlight"))
                .setViewType(VIEW_TYPE_FULL_WIDTH)
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_dark_theme")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.DARK_THEME_SLICE_URI)
                .setViewType(VIEW_TYPE_FULL_WIDTH)
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_gesture")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(
                        "content://com.android.settings.test.slices/action/gesture_pick_up"))
                .setViewType(VIEW_TYPE_FULL_WIDTH)
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_face_enroll")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(CustomSliceRegistry.FACE_ENROLL_SLICE_URI)
                .setViewType(VIEW_TYPE_FULL_WIDTH)
                .build());
        return cards;
    }
}
