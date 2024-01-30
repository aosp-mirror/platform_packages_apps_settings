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

import static com.android.settings.homepage.contextualcards.slices.SliceContextualCardRenderer.VIEW_TYPE_FULL_WIDTH;
import static com.android.settings.homepage.contextualcards.slices.SliceContextualCardRenderer.VIEW_TYPE_STICKY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCardsFragment;
import com.android.settings.homepage.contextualcards.ControllerRendererPool;

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
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @Test
    public void bindView_invalidScheme_sliceShouldBeNull() {
        final Uri invalidUri = Uri.parse("contet://com.android.settings.slices/action/flashlight");
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();

        mRenderer.bindView(viewHolder, buildContextualCard(invalidUri));

        assertThat(
                ((SliceFullCardRendererHelper.SliceViewHolder) viewHolder).sliceView.getSlice())
                .isNull();
    }

    @Ignore("b/313598030")
    @Test
    public void bindView_viewTypeFullWidth_shouldSetCachedSlice() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();

        mRenderer.bindView(viewHolder, buildContextualCard(TEST_SLICE_URI));

        assertThat(
                ((SliceFullCardRendererHelper.SliceViewHolder) viewHolder).sliceView.getSlice())
                .isNotNull();
    }

    @Ignore("b/313598030")
    @Test
    public void bindView_viewTypeSticky_shouldSetCachedSlice() {
        final RecyclerView.ViewHolder viewHolder = spy(getStickyViewHolder());
        doReturn(VIEW_TYPE_STICKY).when(viewHolder).getItemViewType();

        mRenderer.bindView(viewHolder, buildContextualCard(TEST_SLICE_URI));

        assertThat(
                ((SliceFullCardRendererHelper.SliceViewHolder) viewHolder).sliceView.getSlice())
                .isNotNull();
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
    public void bindView_isPendingDismiss_shouldShowDismissalView() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final View dismissalView = viewHolder.itemView.findViewById(R.id.dismissal_view);
        final ContextualCard card = buildContextualCard(
                TEST_SLICE_URI).mutate().setIsPendingDismiss(true).build();

        mRenderer.bindView(viewHolder, card);

        assertThat(dismissalView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindView_isPendingDismiss_shouldAddViewHolderToSet() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final ContextualCard card = buildContextualCard(
                TEST_SLICE_URI).mutate().setIsPendingDismiss(true).build();

        mRenderer.bindView(viewHolder, card);

        assertThat(mRenderer.mFlippedCardSet).contains(viewHolder);
    }

    @Test
    public void bindView_beforeSuccessfulSliceBinding_shouldHideSwipeBackground() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final ContextualCard card = buildContextualCard(TEST_SLICE_URI);
        final View swipeBg = viewHolder.itemView.findViewById(R.id.dismissal_swipe_background);

        mRenderer.bindView(viewHolder, card);

        assertThat(swipeBg.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindView_reuseViewHolder_shouldHideSwipeBackgroundBeforeSliceBinding() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final ContextualCard card = buildContextualCard(TEST_SLICE_URI);
        final View swipeBg = viewHolder.itemView.findViewById(R.id.dismissal_swipe_background);
        swipeBg.setVisibility(View.VISIBLE);
        mRenderer.mSliceLiveDataMap.put(TEST_SLICE_URI, mSliceLiveData);

        mRenderer.bindView(viewHolder, card);

        assertThat(swipeBg.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void viewClick_keepCard_shouldShowSlice() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final View sliceView = viewHolder.itemView.findViewById(R.id.slice_view);
        final View dismissalView = viewHolder.itemView.findViewById(R.id.dismissal_view);
        final Button btnKeep = viewHolder.itemView.findViewById(R.id.keep);
        final ContextualCard card = buildContextualCard(
                TEST_SLICE_URI).mutate().setIsPendingDismiss(true).build();
        mRenderer.bindView(viewHolder, card);

        btnKeep.performClick();

        assertThat(dismissalView.getVisibility()).isEqualTo(View.GONE);
        assertThat(sliceView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void viewClick_keepCard_shouldRemoveViewHolderFromSet() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final Button btnKeep = viewHolder.itemView.findViewById(R.id.keep);
        final ContextualCard card = buildContextualCard(
                TEST_SLICE_URI).mutate().setIsPendingDismiss(true).build();
        mRenderer.bindView(viewHolder, card);
        assertThat(mRenderer.mFlippedCardSet).contains(viewHolder);

        btnKeep.performClick();

        assertThat(mRenderer.mFlippedCardSet).doesNotContain(viewHolder);
    }

    @Test
    public void viewClick_removeCard_shouldRemoveViewHolderFromSet() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final Button btnRemove = viewHolder.itemView.findViewById(R.id.remove);
        final ContextualCard card = buildContextualCard(
                TEST_SLICE_URI).mutate().setIsPendingDismiss(true).build();
        mRenderer.bindView(viewHolder, card);
        assertThat(mRenderer.mFlippedCardSet).contains(viewHolder);
        doReturn(mController).when(mControllerRendererPool).getController(mActivity,
                ContextualCard.CardType.SLICE);

        btnRemove.performClick();

        assertThat(mRenderer.mFlippedCardSet).doesNotContain(viewHolder);
    }

    @Test
    public void viewClick_removeCard_sliceLiveDataShouldRemoveObservers() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final Button btnRemove = viewHolder.itemView.findViewById(R.id.remove);
        final ContextualCard contextualCard = buildContextualCard(TEST_SLICE_URI);
        mRenderer.mSliceLiveDataMap.put(TEST_SLICE_URI, mSliceLiveData);
        mRenderer.bindView(viewHolder, contextualCard);
        doReturn(mController).when(mControllerRendererPool).getController(mActivity,
                ContextualCard.CardType.SLICE);

        btnRemove.performClick();

        assertThat(mRenderer.mSliceLiveDataMap.get(TEST_SLICE_URI).hasObservers()).isFalse();
    }

    @Test
    public void onStop_cardIsInDismissalView_shouldResetToSliceView() {
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final View sliceView = viewHolder.itemView.findViewById(R.id.slice_view);
        final View dismissalView = viewHolder.itemView.findViewById(R.id.dismissal_view);
        final ContextualCard card = buildContextualCard(
                TEST_SLICE_URI).mutate().setIsPendingDismiss(true).build();
        mRenderer.bindView(viewHolder, card);
        assertThat(mRenderer.mFlippedCardSet).contains(viewHolder);

        mRenderer.onStop();

        assertThat(sliceView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(dismissalView.getVisibility()).isEqualTo(View.GONE);
    }

    private RecyclerView.ViewHolder getSliceViewHolder() {
        final RecyclerView recyclerView = new RecyclerView(mActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        final View view = LayoutInflater.from(mActivity).inflate(VIEW_TYPE_FULL_WIDTH, recyclerView,
                false);

        return mRenderer.createViewHolder(view, VIEW_TYPE_FULL_WIDTH);
    }

    private RecyclerView.ViewHolder getStickyViewHolder() {
        final RecyclerView recyclerView = new RecyclerView(mActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        final View view = LayoutInflater.from(mActivity).inflate(VIEW_TYPE_STICKY, recyclerView,
                false);

        return mRenderer.createViewHolder(view, VIEW_TYPE_STICKY);
    }

    private ContextualCard buildContextualCard(Uri sliceUri) {
        final Slice slice = buildSlice();
        return new ContextualCard.Builder()
                .setName("test_name")
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(sliceUri)
                .setViewType(VIEW_TYPE_FULL_WIDTH)
                .setSlice(slice)
                .build();
    }

    private Slice buildSlice() {
        final String title = "test_title";
        final IconCompat icon = IconCompat.createWithResource(mActivity, R.drawable.empty_icon);
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                mActivity,
                title.hashCode() /* requestCode */,
                new Intent("test action"),
                PendingIntent.FLAG_IMMUTABLE);
        final SliceAction action
                = SliceAction.createDeeplink(pendingIntent, icon, ListBuilder.SMALL_IMAGE, title);
        return new ListBuilder(mActivity, TEST_SLICE_URI, ListBuilder.INFINITY)
                .addRow(new ListBuilder.RowBuilder()
                        .addEndItem(icon, ListBuilder.ICON_IMAGE)
                        .setTitle(title)
                        .setPrimaryAction(action))
                .addAction(SliceAction.createToggle(pendingIntent, null /* actionTitle */, true))
                .build();
    }
}
