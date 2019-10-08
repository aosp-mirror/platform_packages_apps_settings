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

import static com.android.settings.homepage.contextualcards.slices.SliceContextualCardRenderer.VIEW_TYPE_HALF_WIDTH;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.graphics.drawable.IconCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.Slice;
import androidx.slice.SliceProvider;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.slices.SliceHalfCardRendererHelper.HalfCardViewHolder;
import com.android.settings.intelligence.ContextualCardProto;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SliceHalfCardRendererHelperTest {

    private static final Uri TEST_SLICE_URI = Uri.parse("content://test/test");

    private Activity mActivity;
    private SliceHalfCardRendererHelper mHelper;

    @Before
    public void setUp() {
        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        mActivity = Robolectric.buildActivity(Activity.class).create().get();
        mActivity.setTheme(R.style.Theme_Settings_Home);
        mHelper = new SliceHalfCardRendererHelper(mActivity);
    }

    @Test
    public void createViewHolder_shouldAlwaysReturnCustomViewHolder() {
        final RecyclerView.ViewHolder viewHolder = getHalfCardViewHolder();

        assertThat(viewHolder).isInstanceOf(HalfCardViewHolder.class);
    }

    @Test
    public void bindView_shouldSetTitle() {
        final RecyclerView.ViewHolder viewHolder = getHalfCardViewHolder();

        mHelper.bindView(viewHolder, buildContextualCard(), buildSlice());

        assertThat(((HalfCardViewHolder) viewHolder).title.getText()).isEqualTo("test_title");
    }

    private RecyclerView.ViewHolder getHalfCardViewHolder() {
        final RecyclerView recyclerView = new RecyclerView(mActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        final View view = LayoutInflater.from(mActivity).inflate(VIEW_TYPE_HALF_WIDTH, recyclerView,
                false);

        return mHelper.createViewHolder(view);
    }

    private ContextualCard buildContextualCard() {
        return new ContextualCard.Builder()
                .setName("test_name")
                .setCategory(ContextualCardProto.ContextualCard.Category.SUGGESTION_VALUE)
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(TEST_SLICE_URI)
                .setViewType(VIEW_TYPE_HALF_WIDTH)
                .build();
    }

    private Slice buildSlice() {
        final String title = "test_title";
        final IconCompat icon = IconCompat.createWithResource(mActivity, R.drawable.empty_icon);
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                mActivity,
                title.hashCode() /* requestCode */,
                new Intent("test action"),
                0  /* flags */);
        final SliceAction action
                = SliceAction.createDeeplink(pendingIntent, icon, ListBuilder.SMALL_IMAGE, title);
        return new ListBuilder(mActivity, TEST_SLICE_URI, ListBuilder.INFINITY)
                .addRow(new ListBuilder.RowBuilder()
                        .addEndItem(icon, ListBuilder.ICON_IMAGE)
                        .setTitle(title)
                        .setPrimaryAction(action))
                .build();
    }
}
