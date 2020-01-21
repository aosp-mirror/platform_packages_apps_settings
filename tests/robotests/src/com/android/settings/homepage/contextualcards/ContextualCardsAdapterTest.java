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
 * limitations under the License.
 */

package com.android.settings.homepage.contextualcards;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.net.Uri;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.conditional.ConditionContextualCardRenderer;
import com.android.settings.homepage.contextualcards.conditional.ConditionContextualCardRenderer.ConditionalCardHolder;
import com.android.settings.homepage.contextualcards.conditional.ConditionalContextualCard;
import com.android.settings.homepage.contextualcards.slices.SliceContextualCardRenderer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class ContextualCardsAdapterTest {

    private static final Uri TEST_SLICE_URI = Uri.parse("content://test/test");
    private static final String TEST_SLICE_NAME = "test_name";

    private Activity mActivity;
    private ContextualCardManager mManager;
    private ContextualCardsAdapter mAdapter;

    @Before
    public void setUp() {
        final ActivityController<Activity> activityController = Robolectric.buildActivity(
                Activity.class);
        mActivity = activityController.get();
        mActivity.setTheme(R.style.Theme_Settings_Home);
        activityController.create();
        final ContextualCardsFragment fragment = new ContextualCardsFragment();
        mManager = new ContextualCardManager(mActivity, fragment.getSettingsLifecycle(),
                null /* bundle */);
        mAdapter = new ContextualCardsAdapter(mActivity, fragment, mManager);
    }

    @Test
    public void getItemViewType_sliceFullWidth_shouldReturnSliceFullWidthViewType() {
        mAdapter.mContextualCards.addAll(getContextualCardList());

        final int viewType = mAdapter.getItemViewType(1);

        assertThat(viewType).isEqualTo(SliceContextualCardRenderer.VIEW_TYPE_FULL_WIDTH);
    }

    @Test
    public void getItemCount_cardList_shouldReturnListSize() {
        final List<ContextualCard> cards = getContextualCardList();
        mAdapter.mContextualCards.addAll(cards);

        final int count = mAdapter.getItemCount();

        assertThat(count).isEqualTo(cards.size());
    }

    @Test
    public void onCreateViewHolder_conditionalCard_shouldReturnConditionalCardHolder() {
        final RecyclerView recyclerView = new RecyclerView(mActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));

        final RecyclerView.ViewHolder holder = mAdapter.onCreateViewHolder(recyclerView,
                ConditionContextualCardRenderer.VIEW_TYPE_FULL_WIDTH);

        assertThat(holder).isInstanceOf(ConditionalCardHolder.class);
    }

    @Test
    public void onBindViewHolder_conditionalCard_shouldSetTestTitle() {
        mAdapter.mContextualCards.add(buildConditionContextualCard());
        final RecyclerView recyclerView = new RecyclerView(mActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        final View view = LayoutInflater.from(mActivity)
                .inflate(ConditionContextualCardRenderer.VIEW_TYPE_FULL_WIDTH, recyclerView, false);
        final ConditionalCardHolder holder = new ConditionalCardHolder(view);

        mAdapter.onBindViewHolder(holder, 0);

        assertThat(holder.title.getText()).isEqualTo("test_title");
    }

    @Test
    public void onContextualCardUpdated_emptyList_shouldClearCardList() {
        mAdapter.mContextualCards.addAll(getContextualCardList());
        final Map<Integer, List<ContextualCard>> cardsToUpdate = new ArrayMap<>();
        final List<ContextualCard> newCardList = new ArrayList<>();
        cardsToUpdate.put(ContextualCard.CardType.DEFAULT, newCardList);

        mAdapter.onContextualCardUpdated(cardsToUpdate);

        assertThat(mAdapter.mContextualCards).isEmpty();
    }

    @Test
    public void onContextualCardUpdated_newCardList_shouldUpdateCardList() {
        mAdapter.mContextualCards.addAll(getContextualCardList());
        final Map<Integer, List<ContextualCard>> cardsToUpdate = new ArrayMap<>();
        final List<ContextualCard> newCardList = new ArrayList<>();
        newCardList.add(buildContextualCard(TEST_SLICE_URI));
        cardsToUpdate.put(ContextualCard.CardType.DEFAULT, newCardList);

        mAdapter.onContextualCardUpdated(cardsToUpdate);

        assertThat(mAdapter.mContextualCards).isEqualTo(newCardList);
    }

    @Test
    public void onSwiped_shouldSetIsPendingDismissToTrue() {
        mAdapter.mContextualCards.addAll(getContextualCardList());
        assertThat(mAdapter.mContextualCards.get(0).isPendingDismiss()).isFalse();

        mAdapter.onSwiped(0);

        assertThat(mAdapter.mContextualCards.get(0).isPendingDismiss()).isTrue();
    }

    private List<ContextualCard> getContextualCardList() {
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(new ContextualCard.Builder()
                .setName("test_name")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(TEST_SLICE_URI)
                .setViewType(SliceContextualCardRenderer.VIEW_TYPE_HALF_WIDTH)
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("test_name_1")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(TEST_SLICE_URI)
                .setViewType(SliceContextualCardRenderer.VIEW_TYPE_FULL_WIDTH)
                .build());
        return cards;
    }

    private ContextualCard buildContextualCard(Uri sliceUri) {
        return new ContextualCard.Builder()
                .setName(TEST_SLICE_NAME)
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(sliceUri)
                .setViewType(SliceContextualCardRenderer.VIEW_TYPE_FULL_WIDTH)
                .build();
    }

    private ContextualCard buildConditionContextualCard() {
        return new ConditionalContextualCard.Builder()
                .setConditionId(123)
                .setName("test_name")
                .setTitleText("test_title")
                .setViewType(ConditionContextualCardRenderer.VIEW_TYPE_FULL_WIDTH)
                .build();
    }
}
