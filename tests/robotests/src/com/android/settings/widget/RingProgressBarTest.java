/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.view.View.MeasureSpec;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class RingProgressBarTest {

    private Context mContext = RuntimeEnvironment.application;

    private RingProgressBar mProgressBar;

    @Before
    public void setUp() {
        mProgressBar = new RingProgressBar(mContext);
    }

    @Test
    public void testMeasurePortrait() {
        mProgressBar.measure(
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(200, MeasureSpec.EXACTLY));
        assertEquals(100, mProgressBar.getMeasuredHeight());
        assertEquals(100, mProgressBar.getMeasuredWidth());
    }

    @Test
    public void testMeasureLandscape() {
        mProgressBar.measure(
                MeasureSpec.makeMeasureSpec(200, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY));
        assertEquals(100, mProgressBar.getMeasuredHeight());
        assertEquals(100, mProgressBar.getMeasuredWidth());
    }

    @Test
    public void testDefaultAttributes() {
        assertFalse(mProgressBar.isIndeterminate());
        assertEquals(0, mProgressBar.getProgress());
        assertEquals(10000, mProgressBar.getMax());
    }
}
