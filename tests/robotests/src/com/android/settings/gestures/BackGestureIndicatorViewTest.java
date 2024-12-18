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

package com.android.settings.gestures;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.widget.ImageView;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class BackGestureIndicatorViewTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private Context mContext;

    private BackGestureIndicatorDrawable mLeftDrawable;
    private BackGestureIndicatorDrawable mRightDrawable;

    private BackGestureIndicatorView mView;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mView = new BackGestureIndicatorView(mContext);

        mLeftDrawable = (BackGestureIndicatorDrawable) ((ImageView) mView.findViewById(
                R.id.indicator_left)).getDrawable();
        mRightDrawable = (BackGestureIndicatorDrawable) ((ImageView) mView.findViewById(
                R.id.indicator_right)).getDrawable();
    }

    @Test
    public void testSetIndicatoreWidth() {
        mView.setIndicatorWidth(25, true);
        mView.setIndicatorWidth(52, false);
        ShadowLooper.idleMainLooper();

        assertEquals(25, mLeftDrawable.getWidth());
        assertEquals(52, mRightDrawable.getWidth());
    }
}
