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
    private CircularIconsView mContainer;

    private int mOneIconWidth;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        CircularIconSet.sExecutorService = MoreExecutors.newDirectExecutorService();
        mPreference = new TestableCircularIconsPreference(mContext);
        // Tests should call bindAndLayoutViewHolder() so that icons can be added.

        Resources res = mContext.getResources();
        mOneIconWidth = res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_diameter)
                + res.getDimensionPixelSize(R.dimen.zen_mode_circular_icon_margin_between);
    }

    private void bindAndLayoutViewHolder(int viewWidth) {
        bindViewHolder();
        layoutViewHolder(viewWidth);
    }

    private void bindViewHolder() {
        View preferenceView = LayoutInflater.from(mContext).inflate(mPreference.getLayoutResource(),
                null);
        mContainer = checkNotNull(preferenceView.findViewById(R.id.circles_container));
        mContainer.setUiExecutor(MoreExecutors.directExecutor());
        PreferenceViewHolder viewHolder = PreferenceViewHolder.createInstanceForTests(
                preferenceView);
        mPreference.onBindViewHolder(viewHolder);
    }

    private void layoutViewHolder(int viewWidth) {
        checkState(mContainer != null, "Call bindViewHolder() first!");
        mContainer.measure(makeMeasureSpec(viewWidth, View.MeasureSpec.EXACTLY),
                makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        mContainer.layout(0, 0, viewWidth, 1000);
    }

    @Test
    public void setIcons_loadsIcons() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);

        bindAndLayoutViewHolder(VIEW_WIDTH);
        mPreference.setIcons(iconSet);

        assertThat(getDrawables(mContainer)).hasSize(2);
        assertThat(((ColorDrawable) getDrawables(mContainer).get(0)).getColor()).isEqualTo(1);
        assertThat(((ColorDrawable) getDrawables(mContainer).get(1)).getColor()).isEqualTo(2);
        assertThat(getPlusText(mContainer)).isNull();
    }

    @Test
    public void setIcons_noIcons_hidesRow() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(),
                ColorDrawable::new);

        bindAndLayoutViewHolder(VIEW_WIDTH);
        mPreference.setIcons(iconSet);

        assertThat(mContainer.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void setIcons_exactlyMaxIcons_loadsAllIcons() throws Exception {
        int width = 300;
        int fittingCircles = width / mOneIconWidth;
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(
                IntStream.range(0, fittingCircles).boxed().toList(),
                ColorDrawable::new);

        bindAndLayoutViewHolder(width);
        mPreference.setIcons(iconSet);

        assertThat(getDrawables(mContainer)).hasSize(fittingCircles);
        assertThat(getDrawables(mContainer)).containsExactlyElementsIn(
                Futures.allAsList(iconSet.getIcons()).get()).inOrder();
        assertThat(getPlusText(mContainer)).isNull();
    }

    @Test
    public void setIcons_tooManyIcons_loadsFirstNAndPlusIcon() throws Exception {
        int width = 300;
        int fittingCircles = width / mOneIconWidth;
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(
                IntStream.range(0, fittingCircles + 5).boxed().toList(),
                ColorDrawable::new);

        bindAndLayoutViewHolder(width);
        mPreference.setIcons(iconSet);

        // N-1 icons, plus (+6) text.
        assertThat(getDrawables(mContainer)).hasSize(fittingCircles - 1);
        assertThat(getDrawables(mContainer)).containsExactlyElementsIn(
                        Futures.allAsList(iconSet.getIcons(fittingCircles - 1)).get())
                .inOrder();
        assertThat(getPlusText(mContainer)).isEqualTo("+6");
    }

    @Test
    public void setIcons_teenyTinySpace_showsPlusIcon_noCrash() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);

        bindAndLayoutViewHolder(1);
        mPreference.setIcons(iconSet);

        assertThat(getDrawables(mContainer)).isEmpty();
        assertThat(getPlusText(mContainer)).isEqualTo("+2");
    }

    @Test
    public void setIcons_beforeBind_loadsIconsOnBindAndMeasure() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                ColorDrawable::new);

        mPreference.setIcons(iconSet);
        assertThat(mContainer).isNull(); // Hold...

        bindViewHolder();
        assertThat(getDrawables(mContainer)).hasSize(0); // Hooooold...

        layoutViewHolder(VIEW_WIDTH);
        assertThat(getDrawables(mContainer)).hasSize(3);
    }

    @Test
    public void setIcons_beforeMeasure_loadsIconsOnMeasure() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                ColorDrawable::new);
        bindViewHolder();

        mPreference.setIcons(iconSet);
        assertThat(getDrawables(mContainer)).hasSize(0);

        layoutViewHolder(VIEW_WIDTH);
        assertThat(getDrawables(mContainer)).hasSize(3);
    }

    @Test
    public void setIcons_calledAgain_reloadsIcons() {
        CircularIconSet<Integer> threeIcons = new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                ColorDrawable::new);
        CircularIconSet<Integer> twoIcons = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);
        CircularIconSet<Integer> fourIcons = new CircularIconSet<>(ImmutableList.of(1, 2, 3, 4),
                ColorDrawable::new);
        bindAndLayoutViewHolder(VIEW_WIDTH);

        mPreference.setIcons(threeIcons);
        assertThat(getDrawables(mContainer)).hasSize(3);

        mPreference.setIcons(twoIcons);
        assertThat(getDrawables(mContainer)).hasSize(2);

        mPreference.setIcons(fourIcons);
        assertThat(getDrawables(mContainer)).hasSize(4);
    }

    @Test
    public void setIcons_sameSet_doesNotReloadIcons() {
        CircularIconSet<Integer> one = new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                ColorDrawable::new);
        CircularIconSet<Integer> same = Mockito.spy(new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                ColorDrawable::new));
        when(same.getIcons()).thenThrow(new RuntimeException("Shouldn't be called!"));

        bindAndLayoutViewHolder(VIEW_WIDTH);

        mPreference.setIcons(one);

        mPreference.setIcons(same); // if no exception, wasn't called.
    }

    @Test
    public void sizeChanged_reloadsIconsIfDifferentFit() {
        CircularIconSet<Integer> largeIconSet = new CircularIconSet<>(
                IntStream.range(0, 100).boxed().toList(),
                ColorDrawable::new);
        mPreference.setIcons(largeIconSet);

        // Base space -> some icons
        int firstWidth = 600;
        int firstFittingCircles = firstWidth / mOneIconWidth;
        bindAndLayoutViewHolder(firstWidth);

        assertThat(getDrawables(mContainer)).hasSize(firstFittingCircles - 1);
        assertThat(getPlusText(mContainer)).isEqualTo("+" + (100 - (firstFittingCircles - 1)));

        // More space -> more icons
        int secondWidth = 1000;
        int secondFittingCircles = secondWidth / mOneIconWidth;
        assertThat(secondFittingCircles).isGreaterThan(firstFittingCircles);
        bindAndLayoutViewHolder(secondWidth);

        assertThat(getDrawables(mContainer)).hasSize(secondFittingCircles - 1);
        assertThat(getPlusText(mContainer)).isEqualTo("+" + (100 - (secondFittingCircles - 1)));

        // Less space -> fewer icons
        int thirdWidth = 600;
        int thirdFittingCircles = thirdWidth / mOneIconWidth;
        bindAndLayoutViewHolder(thirdWidth);

        assertThat(getDrawables(mContainer)).hasSize(thirdFittingCircles - 1);
        assertThat(getPlusText(mContainer)).isEqualTo("+" + (100 - (thirdFittingCircles - 1)));
    }


    @Test
    public void onBindViewHolder_withDifferentView_reloadsIconsCorrectly() {
        View preferenceViewOne = LayoutInflater.from(mContext).inflate(
                mPreference.getLayoutResource(), null);
        CircularIconsView containerOne = preferenceViewOne.findViewById(R.id.circles_container);
        containerOne.setUiExecutor(MoreExecutors.directExecutor());
        PreferenceViewHolder viewHolderOne = PreferenceViewHolder.createInstanceForTests(
                preferenceViewOne);
        containerOne.measure(makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));

        View preferenceViewTwo = LayoutInflater.from(mContext).inflate(
                mPreference.getLayoutResource(), null);
        CircularIconsView containerTwo = preferenceViewTwo.findViewById(R.id.circles_container);
        containerTwo.setUiExecutor(MoreExecutors.directExecutor());
        PreferenceViewHolder viewHolderTwo = PreferenceViewHolder.createInstanceForTests(
                preferenceViewTwo);
        containerTwo.measure(makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));

        CircularIconSet<Integer> iconSetOne = new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                ColorDrawable::new);
        CircularIconSet<Integer> iconSetTwo = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);

        mPreference.onBindViewHolder(viewHolderOne);
        mPreference.setIcons(iconSetOne);
        assertThat(getDrawables(containerOne)).hasSize(3);

        mPreference.onBindViewHolder(viewHolderTwo);
        assertThat(getDrawables(containerTwo)).hasSize(3);

        mPreference.setIcons(iconSetTwo);

        // The second view is updated and the first view is unaffected.
        assertThat(getDrawables(containerTwo)).hasSize(2);
        assertThat(getDrawables(containerOne)).hasSize(3);
    }

    @Test
    public void setEnabled_afterSetIcons_showsEnabledOrDisabledImages() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);
        bindAndLayoutViewHolder(VIEW_WIDTH);
        mPreference.setIcons(iconSet);
        assertThat(getViews(mContainer)).hasSize(2);

        mPreference.setEnabled(false);
        assertThat(getViews(mContainer).get(0).getAlpha()).isLessThan(1f);

        mPreference.setEnabled(true);
        assertThat(getViews(mContainer).get(0).getAlpha()).isEqualTo(1f);
    }

    @Test
    public void setEnabled_beforeSetIcons_showsEnabledOrDisabledImages() {
        CircularIconSet<Integer> iconSet = new CircularIconSet<>(ImmutableList.of(1, 2),
                ColorDrawable::new);

        mPreference.setEnabled(false);
        bindAndLayoutViewHolder(VIEW_WIDTH);
        mPreference.setIcons(iconSet);

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

    private static List<Drawable> getDrawables(ViewGroup container) {
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
