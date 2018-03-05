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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Canvas;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowCanvas;

@RunWith(SettingsRobolectricTestRunner.class)
public class LinearColorBarTest {

    private static final int HEIGHT = 100;
    private static final int WIDTH = 100;

    private Context mContext;
    private LinearColorBar mLinearColorBar;
    private Canvas mCanvas;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mLinearColorBar = spy(new LinearColorBar(mContext, null /* attrs */));
        when(mLinearColorBar.getHeight()).thenReturn(HEIGHT);
        when(mLinearColorBar.getWidth()).thenReturn(WIDTH);
        mLinearColorBar.setRatios(0.2f, 0.4f, 0.4f);
        mLinearColorBar.setColors(1, 2, 3);
        mCanvas = new Canvas();
    }

    @Test
    public void draw_ltr_showStartFromLeft() {
        mLinearColorBar.onDraw(mCanvas);

        final ShadowCanvas shadowCanvas = Shadows.shadowOf(mCanvas);
        assertThat(shadowCanvas.getRectPaintHistoryCount()).isEqualTo(3);

        // 3 boxes, takes space of 20%, 40%, 40% of the the WIDTH correspondingly.
        assertThat(shadowCanvas.getDrawnRect(0).left).isWithin(0.01f).of(0);
        assertThat(shadowCanvas.getDrawnRect(1).left).isWithin(0.01f).of(20);
        assertThat(shadowCanvas.getDrawnRect(2).left).isWithin(0.01f).of(60);
    }

    @Test
    public void draw_rtl_showStartFromRight() {
        when(mLinearColorBar.isLayoutRtl()).thenReturn(true);

        mLinearColorBar.onDraw(mCanvas);

        final ShadowCanvas shadowCanvas = Shadows.shadowOf(mCanvas);
        assertThat(shadowCanvas.getRectPaintHistoryCount()).isEqualTo(3);

        // 3 boxes, takes space of 20%, 40%, 40% of the the WIDTH correspondingly.
        assertThat(shadowCanvas.getDrawnRect(0).right).isWithin(0.01f).of(100);
        assertThat(shadowCanvas.getDrawnRect(1).right).isWithin(0.01f).of(80);
        assertThat(shadowCanvas.getDrawnRect(2).right).isWithin(0.01f).of(40);
    }
}
