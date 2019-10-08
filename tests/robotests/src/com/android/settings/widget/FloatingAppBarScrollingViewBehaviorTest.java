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

package com.android.settings.widget;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.android.settings.R;

import com.google.android.material.appbar.AppBarLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class FloatingAppBarScrollingViewBehaviorTest {

    private FloatingAppBarScrollingViewBehavior mScrollingViewBehavior;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mScrollingViewBehavior = new FloatingAppBarScrollingViewBehavior(mContext,
                Robolectric.buildAttributeSet().build());
    }

    @Test
    public void shouldHeaderOverlapScrollingChild_returnTrue() {
        assertThat(mScrollingViewBehavior.shouldHeaderOverlapScrollingChild()).isTrue();
    }

    @Test
    public void setAppBarLayoutTransparent_backgroundDefaultAsWhite_shouldBeTransparent() {
        mContext.setTheme(R.style.Theme_Settings_Home);
        final AppBarLayout appBarLayout = new AppBarLayout(mContext);
        appBarLayout.setBackgroundColor(Color.WHITE);
        mScrollingViewBehavior.setAppBarLayoutTransparent(appBarLayout);
        assertThat(((ColorDrawable) appBarLayout.getBackground()).getColor()).isEqualTo(
                Color.TRANSPARENT);
    }
}
