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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ControllerRendererPool;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class LegacySuggestionContextualCardRendererTest {
    @Mock
    private ControllerRendererPool mControllerRendererPool;
    @Mock
    private LegacySuggestionContextualCardController mController;
    private Activity mActivity;
    private LegacySuggestionContextualCardRenderer mRenderer;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final ActivityController<Activity> activityController = Robolectric.buildActivity(
                Activity.class);
        mActivity = activityController.get();
        mActivity.setTheme(R.style.Theme_Settings_Home);
        activityController.create();

        mRenderer = new LegacySuggestionContextualCardRenderer(mActivity, mControllerRendererPool);
    }

    @Test
    public void bindView_shouldSetListener() {
        final RecyclerView recyclerView = new RecyclerView(mActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        final ContextualCard card = buildContextualCard();
        final View cardView = LayoutInflater.from(mActivity).inflate(card.getViewType(),
                recyclerView, false);
        final RecyclerView.ViewHolder viewHolder = mRenderer.createViewHolder(cardView,
                card.getViewType());
        when(mControllerRendererPool.getController(mActivity,
                ContextualCard.CardType.LEGACY_SUGGESTION)).thenReturn(mController);

        mRenderer.bindView(viewHolder, buildContextualCard());

        assertThat(cardView).isNotNull();
        assertThat(cardView.hasOnClickListeners()).isTrue();
    }

    @Test
    public void bindView_closeButton_shouldSetListener() {
        final RecyclerView recyclerView = new RecyclerView(mActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        final ContextualCard card = buildContextualCard();
        final View cardView = LayoutInflater.from(mActivity).inflate(card.getViewType(),
                recyclerView, false);
        final RecyclerView.ViewHolder viewHolder = mRenderer.createViewHolder(cardView,
                card.getViewType());
        final View closeButton = viewHolder.itemView.findViewById(R.id.close_button);
        when(mControllerRendererPool.getController(mActivity,
                ContextualCard.CardType.LEGACY_SUGGESTION)).thenReturn(mController);

        mRenderer.bindView(viewHolder, buildContextualCard());

        assertThat(closeButton).isNotNull();
        assertThat(closeButton.hasOnClickListeners()).isTrue();
    }

    @Test
    public void viewClick_shouldInvokeControllerPrimaryClick() {
        final RecyclerView recyclerView = new RecyclerView(mActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        final ContextualCard card = buildContextualCard();
        final View cardView = LayoutInflater.from(mActivity).inflate(card.getViewType(),
                recyclerView, false);
        final RecyclerView.ViewHolder viewHolder = mRenderer.createViewHolder(cardView,
                card.getViewType());
        when(mControllerRendererPool.getController(mActivity,
                ContextualCard.CardType.LEGACY_SUGGESTION)).thenReturn(mController);

        mRenderer.bindView(viewHolder, buildContextualCard());

        assertThat(cardView).isNotNull();
        cardView.performClick();

        verify(mController).onPrimaryClick(any(ContextualCard.class));
    }

    @Test
    public void viewClick_closeButton_shouldInvokeControllerDismissClick() {
        final RecyclerView recyclerView = new RecyclerView(mActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        final ContextualCard card = buildContextualCard();
        final View cardView = LayoutInflater.from(mActivity).inflate(card.getViewType(),
                recyclerView, false);
        final RecyclerView.ViewHolder viewHolder = mRenderer.createViewHolder(cardView,
                card.getViewType());
        final View closeButton = viewHolder.itemView.findViewById(R.id.close_button);
        when(mControllerRendererPool.getController(mActivity,
                ContextualCard.CardType.LEGACY_SUGGESTION)).thenReturn(mController);

        mRenderer.bindView(viewHolder, buildContextualCard());

        assertThat(closeButton).isNotNull();
        closeButton.performClick();

        verify(mController).onDismissed(any(ContextualCard.class));
    }

    private ContextualCard buildContextualCard() {
        return new LegacySuggestionContextualCard.Builder()
                .setName("test_name")
                .setTitleText("test_title")
                .setSummaryText("test_summary")
                .setIconDrawable(mActivity.getDrawable(R.drawable.ic_do_not_disturb_on_24dp))
                .setViewType(LegacySuggestionContextualCardRenderer.VIEW_TYPE)
                .build();
    }
}
