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

package com.android.settings.connecteddevice.display

import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

fun assertPointF(x: Float, y: Float, delta: Float, actual: PointF) {
    assertEquals(x, actual.x, delta)
    assertEquals(y, actual.y, delta)
}

@RunWith(RobolectricTestRunner::class)
class TopologyScaleTest {
    @Test
    fun oneDisplay4to3Aspect() {
        val scale = TopologyScale(
                /* paneWidth= */ 640,
                minEdgeLength = 48, maxBlockRatio = 0.05f,
                listOf(RectF(0f, 0f, 640f, 480f)))

        // blockRatio is higher than 0.05 in order to make the smallest display edge (480 dp) 48dp
        // in the pane.
        assertEquals(
                "{TopologyScale blockRatio=0.100000 originPaneXY=288.0,48.0 paneHeight=144.0}",
                "" + scale)

        assertPointF(352f, 96f, 0.001f, scale.displayToPaneCoor(640f, 480f))
        assertPointF(320f, 72f, 0.001f, scale.displayToPaneCoor(320f, 240f))
        assertPointF(640f, 480f, 0.001f, scale.paneToDisplayCoor(352f, 96f))
    }

    @Test
    fun twoUnalignedDisplays() {
        val scale = TopologyScale(
                /* paneWidth= */ 300,
                minEdgeLength = 48, maxBlockRatio = 0.05f,
                listOf(RectF(0f, 0f, 1920f, 1200f), RectF(1920f, -300f, 3840f, 900f)))

        assertEquals(
                "{TopologyScale blockRatio=0.046875 originPaneXY=60.0,37.5 paneHeight=117.2}",
                "" + scale)

        assertPointF(78.75f, 56.25f, 0.001f, scale.displayToPaneCoor(400f, 400f))
        assertPointF(41.25f, 37.5f, 0.001f, scale.displayToPaneCoor(-400f, 0f))
        assertPointF(-384f, 96f, 0.001f, scale.paneToDisplayCoor(42f, 42f))
    }

    @Test
    fun twoDisplaysBlockRatioBumpedForGarSizeMinimumHorizontal() {
        val scale = TopologyScale(
                /* paneWidth= */ 192,
                minEdgeLength = 48, maxBlockRatio = 0.05f,
                listOf(RectF(0f, 0f, 240f, 320f), RectF(-240f, -320f, 0f, 0f)))

        // blockRatio is higher than 0.05 in order to make the smallest display edge (240 dp) 48dp
        // in the pane.
        assertEquals(
                "{TopologyScale blockRatio=0.200000 originPaneXY=96.0,128.0 paneHeight=256.0}",
                "" + scale)

        assertPointF(192f, 256f, 0.001f, scale.displayToPaneCoor(480f, 640f))
        assertPointF(96f, 64f, 0.001f, scale.displayToPaneCoor(0f, -320f))
        assertPointF(220f, -430f, 0.001f, scale.paneToDisplayCoor(140f, 42f))
    }

    @Test
    fun paneVerticalPaddingLimitedByTallestDisplay() {
        val scale = TopologyScale(
                /* paneWidth= */ 300,
                minEdgeLength = 48, maxBlockRatio = 0.05f,
                listOf(
                        RectF(0f, 0f, 640f, 480f),
                        RectF(0f, 480f, 640f, 960f),
                        RectF(0f, 960f, 640f, 1440f),
                        RectF(0f, 1440f, 640f, 1920f),
                        RectF(0f, 1920f, 640f, 2400f),
                        RectF(0f, 2400f, 640f, 2880f)))

        assertEquals(
                "{TopologyScale blockRatio=0.100000 originPaneXY=118.0,48.0 paneHeight=384.0}",
                "" + scale)
        assertPointF(150f, 48f, 0.001f, scale.displayToPaneCoor(320f, 0f))
        assertPointF(-180f, 2880f, 0.001f, scale.paneToDisplayCoor(100f, 336f))
    }

    @Test
    fun limitedByCustomMaxBlockRatio() {
        val scale = TopologyScale(
                /* paneWidth= */ 300,
                minEdgeLength = 24, maxBlockRatio = 0.12f,
                listOf(
                        RectF(0f, 0f, 640f, 480f),
                        RectF(0f, 480f, 640f, 960f)))

        assertEquals(
                "{TopologyScale blockRatio=0.120000 originPaneXY=111.6,57.6 paneHeight=230.4}",
                "" + scale)
        assertPointF(150f, 57.6f, 0.001f, scale.displayToPaneCoor(320f, 0f))
        assertPointF(-96.6667f, 2320f, 0.001f, scale.paneToDisplayCoor(100f, 336f))
    }

    @Test
    fun largeCustomMinEdgeLength() {
        // minBlockEdgeLength/minDisplayEdgeLength = 80/480 = 1/6, so the block ratio will be 1/6
        val scale = TopologyScale(
                /* paneWidth= */ 300,
                minEdgeLength = 80, maxBlockRatio = 0.05f,
                listOf(
                        RectF(0f, 0f, 640f, 480f),
                        RectF(0f, 480f, 640f, 960f)))

        assertEquals(
                "{TopologyScale blockRatio=0.166667 originPaneXY=96.7,80.0 paneHeight=320.0}",
                "" + scale)
        assertPointF(150f, 80f, 0.001f, scale.displayToPaneCoor(320f, 0f))
        assertPointF(20f, 1536f, 0.001f, scale.paneToDisplayCoor(100f, 336f))
    }
}
