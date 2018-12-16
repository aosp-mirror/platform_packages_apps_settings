/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class AspectRatioFrameLayoutTest {

    private Context mContext;
    private AspectRatioFrameLayout mLayout;

    @Before
    public void setup() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void measure_squareAspectRatio_squeezeWidth() {
        mLayout = new AspectRatioFrameLayout(mContext);

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY);

        mLayout.measure(widthMeasureSpec, heightMeasureSpec);

        assertThat(mLayout.getMeasuredWidth()).isEqualTo(50);
        assertThat(mLayout.getMeasuredHeight()).isEqualTo(50);
    }

    @Test
    public void measure_squareAspectRatio_stretchWidth() {
        mLayout = new AspectRatioFrameLayout(mContext);

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);

        mLayout.measure(widthMeasureSpec, heightMeasureSpec);

        assertThat(mLayout.getMeasuredWidth()).isEqualTo(100);
        assertThat(mLayout.getMeasuredHeight()).isEqualTo(100);
    }

    @Test
    public void measure_squareAspectRatio_doNotStretch() {
        mLayout = new AspectRatioFrameLayout(mContext);

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);

        mLayout.measure(widthMeasureSpec, heightMeasureSpec);

        assertThat(mLayout.getMeasuredWidth()).isEqualTo(100);
        assertThat(mLayout.getMeasuredHeight()).isEqualTo(100);
    }

    @Test
    public void measure_rectangleAspectRatio_stretch() {
        mLayout = new AspectRatioFrameLayout(mContext);
        // Set aspect ratio to 2:1.
        ReflectionHelpers.setField(mLayout, "mAspectRatio", 2f);

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);

        mLayout.measure(widthMeasureSpec, heightMeasureSpec);

        // Should stretch width/height to 2:1 ratio
        assertThat(mLayout.getMeasuredWidth()).isEqualTo(200);
        assertThat(mLayout.getMeasuredHeight()).isEqualTo(100);
    }
}
