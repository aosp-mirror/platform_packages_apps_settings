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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.List;
import java.util.function.Function;

@RunWith(RobolectricTestRunner.class)
public class CircularIconSetTest {

    @Mock private Function<Integer, Drawable> mDrawableLoader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        CircularIconSet.sExecutorService = MoreExecutors.newDirectExecutorService();
        when(mDrawableLoader.apply(anyInt())).thenReturn(new ColorDrawable(Color.BLACK));
    }

    @Test
    public void equals_sameItems_true() {
        CircularIconSet<Integer> items1 = new CircularIconSet<>(ImmutableList.of(1, 2),
                num -> new ColorDrawable(Color.BLUE));
        CircularIconSet<Integer> items2 = new CircularIconSet<>(ImmutableList.of(1, 2),
                num -> new ColorDrawable(Color.GREEN));

        assertThat(items1.hasSameItemsAs(items2)).isTrue();
    }

    @Test
    public void equals_differentTypes_false() {
        CircularIconSet<Integer> items1 = new CircularIconSet<>(ImmutableList.of(1, 2),
                num -> new ColorDrawable(Color.BLUE));
        CircularIconSet<String> items2 = new CircularIconSet<>(ImmutableList.of("a", "b"),
                str -> new ColorDrawable(Color.GREEN));

        assertThat(items1.hasSameItemsAs(items2)).isFalse();
    }

    @Test
    public void equals_differentItems_false() {
        CircularIconSet<String> items1 = new CircularIconSet<>(ImmutableList.of("a", "b"),
                str -> new ColorDrawable(Color.GREEN));
        CircularIconSet<String> items2 = new CircularIconSet<>(ImmutableList.of("a", "b", "c"),
                str -> new ColorDrawable(Color.GREEN));

        assertThat(items1.hasSameItemsAs(items2)).isFalse();
    }

    @Test
    public void getIcons_loadsAllIcons() {
        CircularIconSet<Integer> set = new CircularIconSet<>(ImmutableList.of(1, 2, 3),
                mDrawableLoader);

        List<ListenableFuture<Drawable>> iconFutures = set.getIcons();

        assertThat(iconFutures).hasSize(3);
        verify(mDrawableLoader).apply(1);
        verify(mDrawableLoader).apply(2);
        verify(mDrawableLoader).apply(3);
    }

    @Test
    public void getIcons_loadsRequestedIcons() {
        CircularIconSet<Integer> set = new CircularIconSet<>(ImmutableList.of(1, 2, 3, 4, 5),
                mDrawableLoader);

        List<ListenableFuture<Drawable>> iconFutures = set.getIcons(2);

        assertThat(iconFutures).hasSize(2);
        verify(mDrawableLoader).apply(1);
        verify(mDrawableLoader).apply(2);
        verifyNoMoreInteractions(mDrawableLoader);
    }

    @Test
    public void getIcons_cachesIcons() {
        CircularIconSet<Integer> set = new CircularIconSet<>(ImmutableList.of(1, 2, 3, 4, 5),
                mDrawableLoader);

        List<ListenableFuture<Drawable>> iconFutures = set.getIcons(2);
        assertThat(iconFutures).hasSize(2);
        verify(mDrawableLoader).apply(1);
        verify(mDrawableLoader).apply(2);
        verifyNoMoreInteractions(mDrawableLoader);

        List<ListenableFuture<Drawable>> iconFuturesAgain = set.getIcons(3);
        assertThat(iconFuturesAgain).hasSize(3);
        verify(mDrawableLoader).apply(3);
        verifyNoMoreInteractions(mDrawableLoader);
    }
}
