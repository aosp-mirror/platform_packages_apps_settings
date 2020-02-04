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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCard.CardType;
import com.android.settings.homepage.contextualcards.ContextualCardUpdateListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowSubscriptionManager;
import org.robolectric.shadows.ShadowTelephonyManager;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class ConditionContextualCardControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private ConditionManager mConditionManager;
    @Mock
    private ContextualCardUpdateListener mListener;
    private Context mContext;
    private ConditionContextualCardController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        // parameters required by CellularDataConditionController
        final ShadowSubscriptionManager shadowSubscriptionMgr = shadowOf(
                mContext.getSystemService(SubscriptionManager.class));
        shadowSubscriptionMgr.setDefaultDataSubscriptionId(SUB_ID);

        final TelephonyManager telephonyManager =
                spy(mContext.getSystemService(TelephonyManager.class));
        final ShadowTelephonyManager shadowTelephonyMgr = shadowOf(telephonyManager);
        shadowTelephonyMgr.setTelephonyManagerForSubscriptionId(SUB_ID, telephonyManager);

        mController = spy(new ConditionContextualCardController(mContext));
        ReflectionHelpers.setField(mController, "mConditionManager", mConditionManager);
    }

    @Test
    public void onStart_shouldStartMonitoring() {
        mController.onStart();

        verify(mConditionManager).startMonitoringStateChange();
    }

    @Test
    public void onStop_shouldStopMonitoring() {
        mController.onStop();

        verify(mConditionManager).stopMonitoringStateChange();
    }

    @Test
    public void onConditionsChanged_listenerIsSet_shouldUpdateData() {
        final ContextualCard fakeConditionalCard = new ConditionalContextualCard.Builder().build();
        final List<ContextualCard> conditionalCards = new ArrayList<>();
        conditionalCards.add(fakeConditionalCard);
        when(mConditionManager.getDisplayableCards()).thenReturn(conditionalCards);
        mController.setCardUpdateListener(mListener);

        mController.onConditionsChanged();

        verify(mListener).onContextualCardUpdated(any());
    }

    @Test
    public void onConditionsChanged_listenerNotSet_shouldNotUpdateData() {
        final ContextualCard fakeConditionalCard = new ConditionalContextualCard.Builder().build();
        final List<ContextualCard> conditionalCards = new ArrayList<>();
        conditionalCards.add(fakeConditionalCard);
        when(mConditionManager.getDisplayableCards()).thenReturn(conditionalCards);

        mController.onConditionsChanged();

        verify(mListener, never()).onContextualCardUpdated(any());
    }

    @Test
    public void getConditionalCards_hasEmptyConditionCards_shouldReturnThreeEmptyList() {
        final Map<Integer, List<ContextualCard>> conditionalCards =
                mController.buildConditionalCardsWithFooterOrHeader(generateConditionCards(0));

        assertThat(conditionalCards).hasSize(3);
        for (@CardType int cardType : conditionalCards.keySet()) {
            assertThat(conditionalCards.get(cardType)).isEmpty();
        }
    }

    @Test
    public void getConditionalCards_hasOneConditionCardAndExpanded_shouldGetOneFullWidthCard() {
        mController.setIsExpanded(true);
        final Map<Integer, List<ContextualCard>> conditionalCards =
                mController.buildConditionalCardsWithFooterOrHeader(generateConditionCards(1));

        assertThat(conditionalCards).hasSize(3);
        assertThat(conditionalCards.get(CardType.CONDITIONAL)).hasSize(1);
        assertThat(conditionalCards.get(CardType.CONDITIONAL).get(0).getViewType()).isEqualTo(
                ConditionContextualCardRenderer.VIEW_TYPE_FULL_WIDTH);
        assertThat(conditionalCards.get(CardType.CONDITIONAL_HEADER)).isEmpty();
        assertThat(conditionalCards.get(CardType.CONDITIONAL_FOOTER)).isNotEmpty();
    }

    @Test
    public void getConditionalCards_hasOneConditionCardAndCollapsed_shouldGetConditionalHeader() {
        mController.setIsExpanded(false);
        final Map<Integer, List<ContextualCard>> conditionalCards =
                mController.buildConditionalCardsWithFooterOrHeader(generateConditionCards(1));

        assertThat(conditionalCards).hasSize(3);
        assertThat(conditionalCards.get(CardType.CONDITIONAL)).isEmpty();
        assertThat(conditionalCards.get(CardType.CONDITIONAL_HEADER)).isNotEmpty();
        assertThat(conditionalCards.get(CardType.CONDITIONAL_FOOTER)).isEmpty();
    }

    @Test
    public void getConditionalCards_hasTwoConditionCardsAndExpanded_shouldGetTwoHalfWidthCards() {
        mController.setIsExpanded(true);
        final Map<Integer, List<ContextualCard>> conditionalCards =
                mController.buildConditionalCardsWithFooterOrHeader(generateConditionCards(2));

        assertThat(conditionalCards).hasSize(3);
        assertThat(conditionalCards.get(CardType.CONDITIONAL)).hasSize(2);
        for (ContextualCard card : conditionalCards.get(CardType.CONDITIONAL)) {
            assertThat(card.getViewType()).isEqualTo(
                    ConditionContextualCardRenderer.VIEW_TYPE_HALF_WIDTH);
        }
        assertThat(conditionalCards.get(CardType.CONDITIONAL_HEADER)).isEmpty();
        assertThat(conditionalCards.get(CardType.CONDITIONAL_FOOTER)).isNotEmpty();
    }

    @Test
    public void getConditionalCards_hasTwoConditionCardsAndCollapsed_shouldGetConditionalHeader() {
        mController.setIsExpanded(false);
        final Map<Integer, List<ContextualCard>> conditionalCards =
                mController.buildConditionalCardsWithFooterOrHeader(generateConditionCards(2));

        assertThat(conditionalCards).hasSize(3);
        assertThat(conditionalCards.get(CardType.CONDITIONAL)).isEmpty();
        assertThat(conditionalCards.get(CardType.CONDITIONAL_HEADER)).isNotEmpty();
        assertThat(conditionalCards.get(CardType.CONDITIONAL_FOOTER)).isEmpty();
    }

    @Test
    public void getConditionalCards_hasThreeCardsAndExpanded_shouldGetThreeCardsWithFooter() {
        mController.setIsExpanded(true);
        final Map<Integer, List<ContextualCard>> conditionalCards =
                mController.buildConditionalCardsWithFooterOrHeader(generateConditionCards(3));

        assertThat(conditionalCards).hasSize(3);
        assertThat(conditionalCards.get(CardType.CONDITIONAL)).hasSize(3);
        assertThat(conditionalCards.get(CardType.CONDITIONAL_HEADER)).isEmpty();
        assertThat(conditionalCards.get(CardType.CONDITIONAL_FOOTER)).isNotEmpty();
    }

    @Test
    public void getConditionalCards_hasThreeCardsAndCollapsed_shouldGetOneConditionalHeader() {
        mController.setIsExpanded(false);
        final Map<Integer, List<ContextualCard>> conditionalCards =
                mController.buildConditionalCardsWithFooterOrHeader(generateConditionCards(3));

        assertThat(conditionalCards).hasSize(3);
        assertThat(conditionalCards.get(CardType.CONDITIONAL)).isEmpty();
        assertThat(conditionalCards.get(CardType.CONDITIONAL_HEADER)).isNotEmpty();
        assertThat(conditionalCards.get(CardType.CONDITIONAL_FOOTER)).isEmpty();
    }

    private List<ContextualCard> generateConditionCards(int numberOfCondition) {
        final List<ContextualCard> conditionCards = new ArrayList<>();
        for (int i = 0; i < numberOfCondition; i++) {
            conditionCards.add(new ConditionalContextualCard.Builder()
                    .setConditionId(123 + i)
                    .setMetricsConstant(1)
                    .setActionText("test_action" + i)
                    .setName("test_name" + i)
                    .setTitleText("test_title" + i)
                    .setSummaryText("test_summary" + i)
                    .setViewType(ConditionContextualCardRenderer.VIEW_TYPE_HALF_WIDTH)
                    .build());
        }
        return conditionCards;
    }
}
