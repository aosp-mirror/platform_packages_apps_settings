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

import static com.google.common.truth.Truth.assertThat;

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
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

@RunWith(SettingsRobolectricTestRunner.class)
public class SliceContextualCardRendererTest {

    @Mock
    private LiveData<Slice> mSliceLiveData;
    @Mock
    private ControllerRendererPool mControllerRendererPool;

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
    public void bindView_shouldSetScrollableToFalse() {
        final String sliceUri = "content://com.android.settings.slices/action/flashlight";
        RecyclerView.ViewHolder viewHolder = getSliceViewHolder();

        mRenderer.bindView(viewHolder, buildContextualCard(sliceUri));

        assertThat(
                ((SliceContextualCardRenderer.SliceViewHolder) viewHolder).sliceView.isScrollable
                        ()).isFalse();
    }

    @Test
    public void bindView_invalidScheme_sliceShouldBeNull() {
        final String sliceUri = "contet://com.android.settings.slices/action/flashlight";
        RecyclerView.ViewHolder viewHolder = getSliceViewHolder();

        mRenderer.bindView(viewHolder, buildContextualCard(sliceUri));

        assertThat(
                ((SliceContextualCardRenderer.SliceViewHolder) viewHolder).sliceView.getSlice())
                .isNull();
    }

    @Test
    public void bindView_newSliceLiveData_shouldAddDataToMap() {
        final String sliceUri = "content://com.android.settings.slices/action/flashlight";

        mRenderer.bindView(getSliceViewHolder(), buildContextualCard(sliceUri));

        assertThat(mRenderer.mSliceLiveDataMap.size()).isEqualTo(1);
    }

    @Test
    public void bindView_sliceLiveDataShouldObserveSliceView() {
        final String sliceUri = "content://com.android.settings.slices/action/flashlight";

        mRenderer.bindView(getSliceViewHolder(), buildContextualCard(sliceUri));

        assertThat(mRenderer.mSliceLiveDataMap.get(sliceUri).hasObservers()).isTrue();
    }

    @Test
    public void bindView_sliceLiveDataShouldRemoveObservers() {
        final String sliceUri = "content://com.android.settings.slices/action/flashlight";
        mRenderer.mSliceLiveDataMap.put(sliceUri, mSliceLiveData);

        mRenderer.bindView(getSliceViewHolder(), buildContextualCard(sliceUri));

        verify(mSliceLiveData).removeObservers(mLifecycleOwner);
    }

    @Test
    public void longClick_shouldFlipCard() {
        final String sliceUri = "content://com.android.settings.slices/action/flashlight";
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final View card = viewHolder.itemView.findViewById(R.id.slice_view);
        final ViewFlipper viewFlipper = viewHolder.itemView.findViewById(R.id.viewFlipper);
        final View dismissalView = viewHolder.itemView.findViewById(R.id.dismissal_view);
        mRenderer.bindView(viewHolder, buildContextualCard(sliceUri));

        assertThat(card).isNotNull();
        card.performLongClick();

        assertThat(viewFlipper.getCurrentView()).isEqualTo(dismissalView);
    }

    @Test
    public void viewClick_keepCard_shouldFlipBackToSlice() {
        final String sliceUri = "content://com.android.settings.slices/action/flashlight";
        final RecyclerView.ViewHolder viewHolder = getSliceViewHolder();
        final View card = viewHolder.itemView.findViewById(R.id.slice_view);
        final Button btnKeep = viewHolder.itemView.findViewById(R.id.keep);
        final ViewFlipper viewFlipper = viewHolder.itemView.findViewById(R.id.viewFlipper);
        mRenderer.bindView(viewHolder, buildContextualCard(sliceUri));

        assertThat(card).isNotNull();
        card.performLongClick();
        assertThat(btnKeep).isNotNull();
        btnKeep.performClick();

        assertThat(viewFlipper.getCurrentView()).isInstanceOf(SliceView.class);
    }

    private RecyclerView.ViewHolder getSliceViewHolder() {
        final int viewType = mRenderer.getViewType(false /* isHalfWidth */);
        final RecyclerView recyclerView = new RecyclerView(mActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        final View view = LayoutInflater.from(mActivity).inflate(viewType, recyclerView, false);

        return mRenderer.createViewHolder(view);
    }

    private ContextualCard buildContextualCard(String sliceUri) {
        return new ContextualCard.Builder()
                .setName("test_name")
                .setSliceUri(Uri.parse(sliceUri))
                .build();
    }
}
