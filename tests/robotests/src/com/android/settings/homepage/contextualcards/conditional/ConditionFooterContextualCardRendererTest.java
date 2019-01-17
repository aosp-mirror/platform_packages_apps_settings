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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ControllerRendererPool;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ConditionFooterContextualCardRendererTest {

    @Mock
    private ControllerRendererPool mControllerRendererPool;
    @Mock
    private ConditionContextualCardController mController;
    private Context mContext;
    private ConditionFooterContextualCardRenderer mRenderer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mRenderer = new ConditionFooterContextualCardRenderer(mContext, mControllerRendererPool);
    }

    @Test
    public void bindView_shouldSetClickListener() {
        final RecyclerView recyclerView = new RecyclerView(mContext);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        final ContextualCard card = generateConditionFooterContextualCard();
        final View view = LayoutInflater.from(mContext).inflate(card.getViewType(), recyclerView,
                false);
        final RecyclerView.ViewHolder viewHolder = mRenderer.createViewHolder(view,
                card.getViewType());
        when(mControllerRendererPool.getController(mContext,
                ContextualCard.CardType.CONDITIONAL_FOOTER)).thenReturn(mController);

        mRenderer.bindView(viewHolder, card);

        assertThat(viewHolder.itemView).isNotNull();
        assertThat(viewHolder.itemView.hasOnClickListeners()).isTrue();
    }

    @Test
    public void bindView_clickView_shouldSetTrueToIsConditionExpanded() {
        final RecyclerView recyclerView = new RecyclerView(mContext);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        final ContextualCard card = generateConditionFooterContextualCard();
        final View view = LayoutInflater.from(mContext).inflate(card.getViewType(), recyclerView,
                false);
        final RecyclerView.ViewHolder viewHolder = mRenderer.createViewHolder(view,
                card.getViewType());
        when(mControllerRendererPool.getController(mContext,
                ContextualCard.CardType.CONDITIONAL_FOOTER)).thenReturn(mController);

        mRenderer.bindView(viewHolder, card);

        assertThat(viewHolder.itemView).isNotNull();
        viewHolder.itemView.performClick();

        verify(mController).setIsExpanded(false);
        verify(mController).onConditionsChanged();
    }

    private ContextualCard generateConditionFooterContextualCard() {
        return new ConditionFooterContextualCard.Builder()
                .setName("test_condition_footer")
                .setRankingScore(-9999.0)
                .setViewType(ConditionFooterContextualCardRenderer.VIEW_TYPE)
                .build();
    }
}
