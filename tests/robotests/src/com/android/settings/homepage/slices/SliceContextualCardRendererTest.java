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

package com.android.settings.homepage.slices;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.homepage.ContextualCard;
import com.android.settings.homepage.PersonalSettingsFragment;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class SliceContextualCardRendererTest {

    private Context mContext;
    private SliceContextualCardRenderer mRenderer;
    private LifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycleOwner = new PersonalSettingsFragment();
        mRenderer = new SliceContextualCardRenderer(mContext, mLifecycleOwner);
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

    private RecyclerView.ViewHolder getSliceViewHolder() {
        final int viewType = mRenderer.getViewType();
        final RecyclerView recyclerView = new RecyclerView(mContext);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        final View view = LayoutInflater.from(mContext).inflate(viewType, recyclerView, false);
        final RecyclerView.ViewHolder viewHolder = mRenderer.createViewHolder(view);

        return viewHolder;
    }

    private ContextualCard buildContextualCard(String sliceUri) {
        return new ContextualCard.Builder()
                .setName("test_name")
                .setSliceUri(Uri.parse(sliceUri))
                .build();
    }
}
