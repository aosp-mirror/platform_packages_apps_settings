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

package com.android.settings.homepage.contextualcards.slices;

import static com.android.settings.homepage.contextualcards.slices.SliceContextualCardRenderer.VIEW_TYPE_DEFERRED_SETUP;
import static com.android.settings.homepage.contextualcards.slices.SliceContextualCardRenderer.VIEW_TYPE_FULL_WIDTH;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ViewFlipper;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.Slice;
import androidx.slice.widget.SliceView;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCardsFragment;
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
public class SliceContextualCardRendererTest {

    private static final Uri TEST_SLICE_URI = Uri.parse("content://test/test");

    @Mock
    private LiveData<Slice> mSliceLiveData;
    @Mock
    private ControllerRendererPool mControllerRendererPool;
    @Mock
    private SliceContextualCardController mController;

    private Activity mActivity;
    private SliceContextualCardRenderer mRenderer;
    private LifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final ActivityController<Activity> activityController = Robolectric.buildActivity(
                Activity.class);
        mActivity = activityController.get();
        mActivity.setTheme(R.style.Theme_Settings_Home);
        activityController.create();
        mLifecycleOwner = new ContextualCardsFragment();
        mRenderer = new SliceContextualCardRenderer(mActivity, mLifecycleOwner,
                mControllerRendererPool);
    }

    @Test
    public void bindView_invalidScheme_sliceShouldBeNull() {
        final Uri sliceUri = Uri.parse("contet://com.android.settings.slices/action/flashlight");
        RecyclerView.ViewHolder viewHolder = getSliceViewHolder();

        mRenderer.bindView(viewHolder, buildContextualCard(sliceUri));

        assertThat(
                ((SliceFullCardRendererHelper.SliceViewHolder) viewHolder).sliceView.getSlice())
                .isNull();
    }

    @Test
    public void bindView_newSliceLiveData_shouldAddDataToMap() {
        mRenderer.bindView(getSliceViewHolder(), buildContextualCard(TEST_SLICE_URI));

        assertThat(mRenderer.mSliceLiveDataMap.size()).isEqualTo(1);
    }

    @Test
    public void bindView_sliceLiveDataShouldObserveSliceView() {
        mRenderer.bindView(getSliceViewHolder(), buildContextualCard(TEST_SLICE_URI));

        assertThat(mRenderer.mSliceLiveDataMap.get(TEST_SLICE_URI).hasObservers()).isTrue();
    }

    @Test
    public void bindView_sliceLiveDataShouldRemoveObservers() {
        mRenderer.mSliceLiveDataMap.put(TEST_SLICE_URI, mSliceLiveData);

        mRenderer.bindView(getSliceViewHolder(), buildContextualCard(TEST_SLICE_URI));

        verify(mSliceLiveData).removeObservers(mLifecycleOwner);
    }

    @Test
    public void longClick_shouldFlipCard() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final View card = viewHolder.itemView.findViewById(R.id.slice_view);
        final ViewFlipper viewFlipper = viewHolder.itemView.findViewById(R.id.view_flipper);
        final View dismissalView = viewHolder.itemView.findViewById(R.id.dismissal_view);
        mRenderer.bindView(viewHolder, buildContextualCard(TEST_SLICE_URI));

        card.performLongClick();

        assertThat(viewFlipper.getCurrentView()).isEqualTo(dismissalView);
    }

    @Test
    public void longClick_deferredSetupCard_shouldNotBeClickable() {
        final RecyclerView.ViewHolder viewHolder = getDeferredSetupViewHolder();
        final View contentView = viewHolder.itemView.findViewById(R.id.content);
        mRenderer.bindView(viewHolder, buildContextualCard(TEST_SLICE_URI));

        assertThat(contentView.isLongClickable()).isFalse();
    }

    @Test
    public void longClick_shouldAddViewHolderToSet() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final View card = viewHolder.itemView.findViewById(R.id.slice_view);
        mRenderer.bindView(viewHolder, buildContextualCard(TEST_SLICE_URI));

        card.performLongClick();

        assertThat(mRenderer.mFlippedCardSet).contains(viewHolder);
    }

    @Test
    public void viewClick_keepCard_shouldFlipBackToSlice() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final View card = viewHolder.itemView.findViewById(R.id.slice_view);
        final Button btnKeep = viewHolder.itemView.findViewById(R.id.keep);
        final ViewFlipper viewFlipper = viewHolder.itemView.findViewById(R.id.view_flipper);
        mRenderer.bindView(viewHolder, buildContextualCard(TEST_SLICE_URI));

        card.performLongClick();
        btnKeep.performClick();

        assertThat(viewFlipper.getCurrentView()).isInstanceOf(SliceView.class);
    }

    @Test
    public void viewClick_keepCard_shouldRemoveViewHolderFromSet() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final View card = viewHolder.itemView.findViewById(R.id.slice_view);
        final Button btnKeep = viewHolder.itemView.findViewById(R.id.keep);
        mRenderer.bindView(viewHolder, buildContextualCard(TEST_SLICE_URI));

        card.performLongClick();
        btnKeep.performClick();

        assertThat(mRenderer.mFlippedCardSet).doesNotContain(viewHolder);
    }

    @Test
    public void viewClick_removeCard_shouldRemoveViewHolderFromSet() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final View card = viewHolder.itemView.findViewById(R.id.slice_view);
        final Button btnRemove = viewHolder.itemView.findViewById(R.id.remove);
        final ContextualCard contextualCard = buildContextualCard(TEST_SLICE_URI);
        mRenderer.bindView(viewHolder, contextualCard);
        doReturn(mController).when(mControllerRendererPool).getController(mActivity,
                ContextualCard.CardType.SLICE);

        card.performLongClick();
        btnRemove.performClick();

        assertThat(mRenderer.mFlippedCardSet).doesNotContain(viewHolder);
    }

    @Test
    public void viewClick_removeCard_sliceLiveDataShouldRemoveObservers() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final View card = viewHolder.itemView.findViewById(R.id.slice_view);
        final Button btnRemove = viewHolder.itemView.findViewById(R.id.remove);
        final ContextualCard contextualCard = buildContextualCard(TEST_SLICE_URI);
        mRenderer.mSliceLiveDataMap.put(TEST_SLICE_URI, mSliceLiveData);
        mRenderer.bindView(viewHolder, contextualCard);
        doReturn(mController).when(mControllerRendererPool).getController(mActivity,
                ContextualCard.CardType.SLICE);

        card.performLongClick();
        btnRemove.performClick();

        assertThat(mRenderer.mSliceLiveDataMap.get(TEST_SLICE_URI).hasObservers()).isFalse();
    }

    @Test
    public void onStop_cardIsFlipped_shouldFlipBack() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final View card = viewHolder.itemView.findViewById(R.id.slice_view);
        final ViewFlipper viewFlipper = viewHolder.itemView.findViewById(R.id.view_flipper);
        mRenderer.bindView(viewHolder, buildContextualCard(TEST_SLICE_URI));

        card.performLongClick();
        mRenderer.onStop();

        assertThat(viewFlipper.getCurrentView()).isInstanceOf(SliceView.class);
    }

    private RecyclerView.ViewHolder getSliceViewHolder() {
        final RecyclerView recyclerView = new RecyclerView(mActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        final View view = LayoutInflater.from(mActivity).inflate(VIEW_TYPE_FULL_WIDTH, recyclerView,
                false);

        return mRenderer.createViewHolder(view, VIEW_TYPE_FULL_WIDTH);
    }

    private RecyclerView.ViewHolder getDeferredSetupViewHolder() {
        final RecyclerView recyclerView = new RecyclerView(mActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        final View view = LayoutInflater.from(mActivity).inflate(VIEW_TYPE_DEFERRED_SETUP,
                recyclerView, false);
        final RecyclerView.ViewHolder viewHolder = spy(
                mRenderer.createViewHolder(view, VIEW_TYPE_DEFERRED_SETUP));
        doReturn(VIEW_TYPE_DEFERRED_SETUP).when(viewHolder).getItemViewType();

        return viewHolder;
    }

    private ContextualCard buildContextualCard(Uri sliceUri) {
        return new ContextualCard.Builder()
                .setName("test_name")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(sliceUri)
                .setViewType(VIEW_TYPE_FULL_WIDTH)
                .build();
    }
}
