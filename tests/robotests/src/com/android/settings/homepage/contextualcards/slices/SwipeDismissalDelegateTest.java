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

package com.android.settings.homepage.contextualcards.slices;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.conditional.ConditionContextualCardRenderer;
import com.android.settings.homepage.contextualcards.conditional.ConditionContextualCardRenderer.ConditionalCardHolder;
import com.android.settings.homepage.contextualcards.slices.SliceFullCardRendererHelper.SliceViewHolder;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class SwipeDismissalDelegateTest {

    @Mock
    private SwipeDismissalDelegate.Listener mDismissalDelegateListener;

    private Activity mActivity;
    private RecyclerView mRecyclerView;
    private SwipeDismissalDelegate mDismissalDelegate;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final ActivityController<Activity> activityController = Robolectric.buildActivity(
                Activity.class);
        mActivity = activityController.get();
        mActivity.setTheme(R.style.Theme_Settings_Home);
        activityController.create();
        mRecyclerView = new RecyclerView(mActivity);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        mDismissalDelegate = new SwipeDismissalDelegate(mDismissalDelegateListener);
    }

    @Ignore("b/313598030")
    @Test
    public void getMovementFlags_conditionalViewHolder_shouldDisableSwipe() {
        assertThat(mDismissalDelegate.getMovementFlags(mRecyclerView, getConditionalViewHolder()))
                .isEqualTo(0);
    }

    @Ignore("b/313598030")
    @Test
    public void getMovementFlags_dismissalView_shouldDisableSwipe() {
        final RecyclerView.ViewHolder holder = getSliceViewHolder();
        holder.itemView.findViewById(R.id.dismissal_view).setVisibility(View.VISIBLE);

        assertThat(mDismissalDelegate.getMovementFlags(mRecyclerView, holder)).isEqualTo(0);
    }

    @Ignore("b/313598030")
    @Test
    public void getMovementFlags_SliceViewHolder_shouldEnableSwipe() {
        final RecyclerView.ViewHolder holder = getSliceViewHolder();
        holder.itemView.findViewById(R.id.dismissal_view).setVisibility(View.GONE);

        assertThat(mDismissalDelegate.getMovementFlags(mRecyclerView, getSliceViewHolder()))
                .isNotEqualTo(0);
    }

    @Ignore("b/313598030")
    @Test
    public void onSwipe_shouldNotifyListener() {
        mDismissalDelegate.onSwiped(getSliceViewHolder(), 1);

        verify(mDismissalDelegateListener).onSwiped(anyInt());
    }

    private RecyclerView.ViewHolder getSliceViewHolder() {
        final View view = LayoutInflater.from(mActivity)
                .inflate(SliceContextualCardRenderer.VIEW_TYPE_FULL_WIDTH, mRecyclerView, false);
        final RecyclerView.ViewHolder viewHolder = spy(new SliceViewHolder(view));
        doReturn(SliceContextualCardRenderer.VIEW_TYPE_FULL_WIDTH).when(
                viewHolder).getItemViewType();

        return viewHolder;
    }

    private RecyclerView.ViewHolder getConditionalViewHolder() {
        final View view = LayoutInflater.from(mActivity)
                .inflate(ConditionContextualCardRenderer.VIEW_TYPE_FULL_WIDTH, mRecyclerView,
                        false);
        final RecyclerView.ViewHolder viewHolder = spy(new ConditionalCardHolder(view));
        doReturn(ConditionContextualCardRenderer.VIEW_TYPE_FULL_WIDTH).when(
                viewHolder).getItemViewType();

        return viewHolder;
    }
}
