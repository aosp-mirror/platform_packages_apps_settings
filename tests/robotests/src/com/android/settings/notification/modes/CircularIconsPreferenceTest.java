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
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@RunWith(RobolectricTestRunner.class)
public class CircularIconsPreferenceTest {

    private static final int VIEW_WIDTH = 800;

    private Context mContext;
    private CircularIconsPreference mPreference;
    private PreferenceViewHolder mViewHolder;
    private ViewGroup mContainer;

    private int mOneIconWidth;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        CircularIconSet.sExecutorService = MoreExecutors.newDirectExecutorService();
        mPreference = new TestableCircularIconsPreference(mContext);
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
        mContainer = checkNotNull(preferenceView.findViewById(R.id.circles_container));
        mViewHolder = PreferenceViewHolder.createInstanceForTests(preferenceView);
        mPreference.onBindViewHolder(mViewHolder);
    }

    private void measureViewHolder(int viewWidth) {
        checkState(mContainer != null, "Call bindViewHolder() first!");
        mContainer.measure(makeMeasureSpec(viewWidth, View.MeasureSpec.EXACTLY),
                makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        mContainer.getViewTreeObserver().dispatchOnGlobalLayout();
    }

    @Test
    public void displayIcons_loadsIcons() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);

        bindAndMeasureViewHolder(VIEW_WIDTH);
        mPreference.displayIcons(iconSet);

        assertThat(getIcons(mContainer)).hasSize(2);
        assertThat(((ColorDrawable) getIcons(mContainer).get(0)).getColor()).isEqualTo(1);
        assertThat(((ColorDrawable) getIcons(mContainer).get(1)).getColor()).isEqualTo(2);
        assertThat(getPlusText(mContainer)).isNull();
    }

    @Test
    public void displayIcons_noIcons_hidesRow() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(),
                ColorDrawable::new);

        bindAndMeasureViewHolder(VIEW_WIDTH);
        mPreference.displayIcons(iconSet);

        assertThat(mContainer.getVisibility()).isEqualTo(View.GONE);
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

        assertThat(getIcons(mContainer)).hasSize(fittingCircles);
        assertThat(getIcons(mContainer)).containsExactlyElementsIn(
                Futures.allAsList(iconSet.getIcons()).get()).inOrder();
        assertThat(getPlusText(mContainer)).isNull();

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
        assertThat(getIcons(mContainer)).hasSize(fittingCircles - 1);
        assertThat(getIcons(mContainer)).containsExactlyElementsIn(
                        Futures.allAsList(iconSet.getIcons(fittingCircles - 1)).get())
                .inOrder();
        assertThat(getPlusText(mContainer)).isEqualTo("+6");
    }

    @Test
    public void displayIcons_teenyTinySpace_showsPlusIcon_noCrash() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);

        bindAndMeasureViewHolder(1);
        mPreference.displayIcons(iconSet);

        assertThat(getIcons(mContainer)).isEmpty();
        assertThat(getPlusText(mContainer)).isEqualTo("+2");
    }

    @Test
    public void displayIcons_beforeBind_loadsIconsOnBindAndMeasure() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                ColorDrawable::new);

        mPreference.displayIcons(iconSet);
        assertThat(mPreference.getLoadedIcons()).isNull(); // Hold...

        bindViewHolder();
        assertThat(mPreference.getLoadedIcons()).isNull(); // Hooooold...

        measureViewHolder(VIEW_WIDTH);
        assertThat(mPreference.getLoadedIcons().icons()).hasSize(3);
        assertThat(getIcons(mContainer)).hasSize(3);
    }

    @Test
    public void displayIcons_beforeMeasure_loadsIconsOnMeasure() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                ColorDrawable::new);
        bindViewHolder();

        mPreference.displayIcons(iconSet);
        assertThat(mPreference.getLoadedIcons()).isNull();

        measureViewHolder(VIEW_WIDTH);
        assertThat(getIcons(mContainer)).hasSize(3);
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
        assertThat(mPreference.getLoadedIcons()).isNotNull();
        assertThat(getIcons(mContainer)).hasSize(3);

        mPreference.displayIcons(twoIcons);
        assertThat(mPreference.getLoadedIcons()).isNotNull();
        assertThat(getIcons(mContainer)).hasSize(2);

        mPreference.displayIcons(fourIcons);
        assertThat(mPreference.getLoadedIcons()).isNotNull();
        assertThat(getIcons(mContainer)).hasSize(4);
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

    @Test
    public void onBindViewHolder_withDifferentView_reloadsIconsCorrectly() {
        View preferenceViewOne = LayoutInflater.from(mContext).inflate(
                mPreference.getLayoutResource(), null);
        ViewGroup containerOne = preferenceViewOne.findViewById(R.id.circles_container);
        PreferenceViewHolder viewHolderOne = PreferenceViewHolder.createInstanceForTests(
                preferenceViewOne);
        containerOne.measure(makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));

        View preferenceViewTwo = LayoutInflater.from(mContext).inflate(
                mPreference.getLayoutResource(), null);
        ViewGroup containerTwo = preferenceViewTwo.findViewById(R.id.circles_container);
        PreferenceViewHolder viewHolderTwo = PreferenceViewHolder.createInstanceForTests(
                preferenceViewTwo);
        containerTwo.measure(makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));

        CircularIconSet<Integer> iconSetOne = new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                ColorDrawable::new);
        CircularIconSet<Integer> iconSetTwo = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);

        mPreference.onBindViewHolder(viewHolderOne);
        mPreference.displayIcons(iconSetOne);
        assertThat(getIcons(containerOne)).hasSize(3);

        mPreference.onBindViewHolder(viewHolderTwo);
        assertThat(getIcons(containerTwo)).hasSize(3);

        mPreference.displayIcons(iconSetTwo);

        // The second view is updated and the first view is unaffected.
        assertThat(getIcons(containerTwo)).hasSize(2);
        assertThat(getIcons(containerOne)).hasSize(3);
    }

    @Test
    public void setEnabled_afterDisplayIcons_showsEnabledOrDisabledImages() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);
        bindAndMeasureViewHolder(VIEW_WIDTH);
        mPreference.displayIcons(iconSet);
        assertThat(getViews(mContainer)).hasSize(2);

        mPreference.setEnabled(false);
        assertThat(getViews(mContainer).get(0).getAlpha()).isLessThan(1f);

        mPreference.setEnabled(true);
        assertThat(getViews(mContainer).get(0).getAlpha()).isEqualTo(1f);
    }

    @Test
    public void setEnabled_beforeDisplayIcons_showsEnabledOrDisabledImages() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);

        mPreference.setEnabled(false);
        bindAndMeasureViewHolder(VIEW_WIDTH);
        mPreference.displayIcons(iconSet);

        assertThat(getViews(mContainer)).hasSize(2);
        assertThat(getViews(mContainer).get(0).getAlpha()).isLessThan(1f);
    }

    private static List<View> getViews(ViewGroup container) {
        ArrayList<View> views = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            views.add(container.getChildAt(i));
        }
        return views;
    }

    private static List<Drawable> getIcons(ViewGroup container) {
        ArrayList<Drawable> drawables = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            if (container.getChildAt(i) instanceof ImageView imageView) {
                drawables.add(imageView.getDrawable());

            }
        }
        return drawables;
    }

    @Nullable
    private static String getPlusText(ViewGroup container) {
        View lastChild = container.getChildAt(container.getChildCount() - 1);
        if (lastChild instanceof TextView tv) {
            return tv.getText() != null ? tv.getText().toString() : null;
        } else {
            return null;
        }
    }
}
