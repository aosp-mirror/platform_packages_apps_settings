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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

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
        mOneIconWidth = res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_diameter)
                + res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_margin_between);
    }

    private void bindAndMeasureViewHolder(int viewWidth) {
        bindViewHolder();
        measureViewHolder(viewWidth);
    }

    private void bindViewHolder() {
        View preferenceView = LayoutInflater.from(mContext).inflate(mPreference.getLayoutResource(),
                null);
        mIconContainer = checkNotNull(preferenceView.findViewById(R.id.circles_container));
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(preferenceView);
        mPreference.onBindViewHolder(holder);
    }

    private void measureViewHolder(int viewWidth) {
        checkState(mIconContainer != null, "Call bindViewHolder() first!");
        mIconContainer.measure(makeMeasureSpec(viewWidth, View.MeasureSpec.EXACTLY),
                makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        mIconContainer.getViewTreeObserver().dispatchOnGlobalLayout();
    }

    @Test
    public void displayIcons_loadsIcons() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);

        bindAndMeasureViewHolder(VIEW_WIDTH);
        mPreference.displayIcons(iconSet);

        assertThat(mPreference.getIcons()).hasSize(2);
        assertThat(((ColorDrawable) mPreference.getIcons().get(0)).getColor()).isEqualTo(1);
        assertThat(((ColorDrawable) mPreference.getIcons().get(1)).getColor()).isEqualTo(2);
        assertThat(mPreference.getPlusText()).isNull();
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
        int fittingCircles = width / mOneIconWidth;
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(
                IntStream.range(0, fittingCircles).boxed().toList(),
                ColorDrawable::new);

        bindAndMeasureViewHolder(width);
        mPreference.displayIcons(iconSet);

        assertThat(mPreference.getIcons()).hasSize(fittingCircles);
        assertThat(mPreference.getIcons()).containsExactlyElementsIn(
                Futures.allAsList(iconSet.getIcons()).get()).inOrder();
        assertThat(mPreference.getPlusText()).isNull();

    }

    @Test
    public void displayIcons_tooManyIcons_loadsFirstNAndPlusIcon() throws Exception {
        int width = 300;
        int fittingCircles = width / mOneIconWidth;
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(
                IntStream.range(0, fittingCircles + 5).boxed().toList(),
                ColorDrawable::new);

        bindAndMeasureViewHolder(width);
        mPreference.displayIcons(iconSet);

        // N-1 icons, plus (+6) text.
        assertThat(mPreference.getIcons()).hasSize(fittingCircles - 1);
        assertThat(mPreference.getIcons()).containsExactlyElementsIn(
                        Futures.allAsList(iconSet.getIcons(fittingCircles - 1)).get())
                .inOrder();
        assertThat(mPreference.getPlusText()).isEqualTo("+6");
    }

    @Test
    public void displayIcons_teenyTinySpace_showsPlusIcon_noCrash() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);

        bindAndMeasureViewHolder(1);
        mPreference.displayIcons(iconSet);

        assertThat(mPreference.getIcons()).isEmpty();
        assertThat(mPreference.getPlusText()).isEqualTo("+2");
    }

    @Test
    public void displayIcons_beforeBind_loadsIconsOnBindAndMeasure() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                ColorDrawable::new);

        mPreference.displayIcons(iconSet);
        assertThat(mPreference.getIcons()).isEmpty(); // Hold...

        bindViewHolder();
        assertThat(mPreference.getIcons()).isEmpty(); // Hooooold...

        measureViewHolder(VIEW_WIDTH);
        assertThat(mPreference.getIcons()).hasSize(3);
    }

    @Test
    public void displayIcons_beforeMeasure_loadsIconsOnMeasure() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                ColorDrawable::new);
        bindViewHolder();

        mPreference.displayIcons(iconSet);
        assertThat(mPreference.getIcons()).isEmpty();

        measureViewHolder(VIEW_WIDTH);
        assertThat(mPreference.getIcons()).hasSize(3);
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
        assertThat(mPreference.getIcons()).hasSize(3);
        mPreference.displayIcons(twoIcons);
        assertThat(mPreference.getIcons()).hasSize(2);
        mPreference.displayIcons(fourIcons);
        assertThat(mPreference.getIcons()).hasSize(4);
    }

    @Test
    public void displayIcons_sameSet_doesNotReloadIcons() {
        CircularIconSet<Integer> one = new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                ColorDrawable::new);
        CircularIconSet<Integer> same = Mockito.spy(new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                ColorDrawable::new));
        when(same.getIcons()).thenThrow(new RuntimeException("Shouldn't be called!"));

        bindAndMeasureViewHolder(VIEW_WIDTH);

        mPreference.displayIcons(one);
        mPreference.displayIcons(same); // if no exception, wasn't called.
    }
}
