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
public class ConditionContextualCardRendererTest {

    @Mock
    private ControllerRendererPool mControllerRendererPool;
    @Mock
    private ConditionContextualCardController mController;
    private Activity mActivity;
    private ConditionContextualCardRenderer mRenderer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final ActivityController<Activity> activityController = Robolectric.buildActivity(
                Activity.class);
        mActivity = activityController.get();
        mActivity.setTheme(R.style.Theme_Settings_Home);
        activityController.create();
        mRenderer = new ConditionContextualCardRenderer(mActivity, mControllerRendererPool);
    }

    @Test
    public void bindView_shouldSetListener() {
        final RecyclerView recyclerView = new RecyclerView(mActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        final ContextualCard card = buildConditionContextualCard();
        final View view = LayoutInflater.from(mActivity).inflate(card.getViewType(), recyclerView,
                false);
        final RecyclerView.ViewHolder viewHolder = mRenderer.createViewHolder(view,
                card.getViewType());
        final View cardView = view.findViewById(R.id.content);
        when(mControllerRendererPool.getController(mActivity,
                ContextualCard.CardType.CONDITIONAL)).thenReturn(mController);

        mRenderer.bindView(viewHolder, card);

        assertThat(cardView).isNotNull();
        assertThat(cardView.hasOnClickListeners()).isTrue();
    }

    @Test
    public void viewClick_shouldInvokeControllerPrimaryClick() {
        final RecyclerView recyclerView = new RecyclerView(mActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        final ContextualCard card = buildConditionContextualCard();
        final View view = LayoutInflater.from(mActivity).inflate(card.getViewType(), recyclerView,
                false);
        final RecyclerView.ViewHolder viewHolder = mRenderer.createViewHolder(view,
                card.getViewType());
        final View cardView = view.findViewById(R.id.content);
        when(mControllerRendererPool.getController(mActivity,
                ContextualCard.CardType.CONDITIONAL)).thenReturn(mController);

        mRenderer.bindView(viewHolder, card);

        assertThat(cardView).isNotNull();
        cardView.performClick();

        verify(mController).onPrimaryClick(any(ContextualCard.class));
    }

    private ContextualCard buildConditionContextualCard() {
        return new ConditionalContextualCard.Builder()
                .setConditionId(123)
                .setMetricsConstant(1)
                .setActionText("test_action")
                .setName("test_name")
                .setTitleText("test_title")
                .setSummaryText("test_summary")
                .setIconDrawable(mActivity.getDrawable(R.drawable.ic_do_not_disturb_on_24dp))
                .setViewType(ConditionContextualCardRenderer.VIEW_TYPE_FULL_WIDTH)
                .build();
    }
}
