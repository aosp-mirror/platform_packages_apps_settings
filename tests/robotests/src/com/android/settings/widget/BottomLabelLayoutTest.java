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

import android.view.View;
import android.widget.LinearLayout;
import android.widget.Space;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BottomLabelLayoutTest {

    private BottomLabelLayout mBottomLabelLayout;
    private Space mSpace;

    @Before
    public void setUp() {
        mBottomLabelLayout = new BottomLabelLayout(RuntimeEnvironment.application, null);
        mBottomLabelLayout.setOrientation(LinearLayout.HORIZONTAL);

        mSpace = new Space(RuntimeEnvironment.application);
        mSpace.setId(R.id.spacer);
        mBottomLabelLayout.addView(mSpace);
    }

    @Test
    public void testSetStacked_stackedTrue_layoutVertical() {
        mBottomLabelLayout.setStacked(true);

        assertThat(mBottomLabelLayout.getOrientation()).isEqualTo(LinearLayout.VERTICAL);
        assertThat(mSpace.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testSetStacked_stackedFalse_layoutHorizontal() {
        mBottomLabelLayout.setStacked(false);

        assertThat(mBottomLabelLayout.getOrientation()).isEqualTo(LinearLayout.HORIZONTAL);
        assertThat(mSpace.getVisibility()).isEqualTo(View.VISIBLE);
    }
}
