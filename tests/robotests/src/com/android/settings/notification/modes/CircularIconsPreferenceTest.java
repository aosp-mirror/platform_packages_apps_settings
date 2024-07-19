/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.view.View.MeasureSpec.makeMeasureSpec;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;
import java.util.stream.IntStream;

@RunWith(RobolectricTestRunner.class)
public class CircularIconsPreferenceTest {

    private static final int VIEW_WIDTH = 800;

    private Context mContext;
    private CircularIconsPreference mPreference;
    private View mIconContainer;

    private int mOneIconWidth;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        CircularIconSet.sExecutorService = MoreExecutors.newDirectExecutorService();
        mPreference = new CircularIconsPreference(mContext, MoreExecutors.directExecutor());
        // Tests should call bindAndMeasureViewHolder() so that icons can be added.

        Resources res = mContext.getResources();
        mOneIconWidth = res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_size)
                + res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_margin_between);
    }

    private void bindAndMeasureViewHolder(int viewWidth) {
        View preferenceView = LayoutInflater.from(mContext).inflate(mPreference.getLayoutResource(),
                null);
        mIconContainer = checkNotNull(preferenceView.findViewById(R.id.circles_container));
        mIconContainer.measure(makeMeasureSpec(viewWidth, View.MeasureSpec.EXACTLY),
                makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(preferenceView);
        mPreference.onBindViewHolder(holder);
    }

    @Test
    public void displayIcons_loadsIcons() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);

        bindAndMeasureViewHolder(VIEW_WIDTH);
        mPreference.displayIcons(iconSet);

        assertThat(mPreference.getIconViews()).hasSize(2);
        assertThat(mPreference.getIconViews().get(0).getDrawable())
                .isInstanceOf(ColorDrawable.class);
        assertThat(((ColorDrawable) mPreference.getIconViews().get(0).getDrawable()).getColor())
                .isEqualTo(1);
        assertThat(((ColorDrawable) mPreference.getIconViews().get(1).getDrawable()).getColor())
                .isEqualTo(2);
        assertThat(mIconContainer.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void displayIcons_noIcons_hidesRow() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(),
                ColorDrawable::new);

        bindAndMeasureViewHolder(VIEW_WIDTH);
        mPreference.displayIcons(iconSet);

        assertThat(mIconContainer.getVisibility()).isEqualTo(View.GONE);
    }


    @Test
    public void displayIcons_exactlyMaxIcons_loadsAllIcons() throws Exception {
        int width = 300;
        int fittingIcons = width / mOneIconWidth;
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(
                IntStream.range(0, fittingIcons).boxed().toList(),
                ColorDrawable::new);

        bindAndMeasureViewHolder(width);
        mPreference.displayIcons(iconSet);

        List<Drawable> displayedDrawables = mPreference.getIconViews().stream()
                .map(ImageView::getDrawable).toList();
        assertThat(displayedDrawables).hasSize(fittingIcons);
        assertThat(displayedDrawables).containsExactlyElementsIn(
                Futures.allAsList(iconSet.getIcons()).get()).inOrder();
    }

    @Test
    public void displayIcons_tooManyIcons_loadsFirstNAndPlusIcon() throws Exception {
        int width = 300;
        int fittingIcons = width / mOneIconWidth;
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(
                IntStream.range(0, fittingIcons + 5).boxed().toList(),
                ColorDrawable::new);

        bindAndMeasureViewHolder(width);
        mPreference.displayIcons(iconSet);

        List<Drawable> displayedDrawables = mPreference.getIconViews().stream()
                .map(ImageView::getDrawable).toList();
        assertThat(displayedDrawables).hasSize(fittingIcons);
        // N-1 are actual icons, Nth icon is (+xx).
        assertThat(displayedDrawables.stream().limit(fittingIcons - 1).toList())
                .containsExactlyElementsIn(
                        Futures.allAsList(iconSet.getIcons(fittingIcons - 1)).get())
                .inOrder();
        // TODO: b/346551087 - Correctly verify the plus-6 icon, once we generate it properly.
        assertThat(((ColorDrawable) displayedDrawables.get(
                displayedDrawables.size() - 1)).getColor()).isEqualTo(Color.BLUE);
    }

    @Test
    public void displayIcons_teenyTinySpace_showsPlusIcon_noCrash() {
        int width = 1;
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);

        bindAndMeasureViewHolder(width);
        mPreference.displayIcons(iconSet);

        assertThat(mPreference.getIconViews()).hasSize(1);
        // TODO: b/346551087 - Correctly verify the plus-2 icon, once we generate it properly.
        assertThat(((ColorDrawable) mPreference.getIconViews().get(0).getDrawable()).getColor())
                .isEqualTo(Color.BLUE);
    }

    @Test
    public void displayIcons_beforeBind_loadsIconsOnBind() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                ColorDrawable::new);

        mPreference.displayIcons(iconSet);
        assertThat(mPreference.getIconViews()).isEmpty();

        bindAndMeasureViewHolder(VIEW_WIDTH);
        assertThat(mPreference.getIconViews()).hasSize(3);
    }

    @Test
    public void displayIcons_calledAgain_reloadsIcons() {
        CircularIconSet<Integer> threeIcons = new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                ColorDrawable::new);
        CircularIconSet<Integer> twoIcons = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);
        CircularIconSet<Integer> fourIcons = new CircularIconSet<>(ImmutableList.of(1, 2, 3, 4),
                ColorDrawable::new);
        bindAndMeasureViewHolder(VIEW_WIDTH);

        mPreference.displayIcons(threeIcons);
        assertThat(mPreference.getIconViews()).hasSize(3);
        mPreference.displayIcons(twoIcons);
        assertThat(mPreference.getIconViews()).hasSize(2);
        mPreference.displayIcons(fourIcons);
        assertThat(mPreference.getIconViews()).hasSize(4);
    }
}
