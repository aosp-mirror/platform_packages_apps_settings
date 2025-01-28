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
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Assert as many points as possible in a single assert since a tweak can change every assertion
// in a small way and we don't want to re-compile and re-test for *every* individual float.
fun assertPointF(comparisons: List<Pair<PointF, PointF>>, delta: Float) {
    val errors = StringBuilder()
    comparisons.forEachIndexed {i, (a, b) ->
        if (abs(b.x - a.x) > delta) {
            errors.append("x value at index $i - ${a.x} != ${b.x}\n")
        }
        if (abs(b.y - a.y) > delta) {
            errors.append("y value at index $i - ${a.y} != ${b.y}\n")
        }
    }
    assertEquals("", errors.toString())
}

@RunWith(RobolectricTestRunner::class)
class TopologyScaleTest {
    @Test
    fun oneDisplay4to3Aspect() {
        val scale = TopologyScale(
                /* paneWidth= */ 640, minPaneHeight = 0f,
                minEdgeLength = 48f, maxEdgeLength = 64f,
                listOf(RectF(0f, 0f, 640f, 480f)))

        // blockRatio is is set in order to make the smallest display edge (480 dp) 48dp
        // in the pane.
        assertEquals(
                "{TopologyScale blockRatio=0.100000 originPaneXY=288.0,72.0 paneHeight=192.0}",
                "" + scale)

        assertPointF(
            listOf(
                PointF(352f, 120f) to scale.displayToPaneCoor(640f, 480f),
                PointF(320f, 96f) to scale.displayToPaneCoor(320f, 240f),
                PointF(640f, 240f) to scale.paneToDisplayCoor(352f, 96f),
            ),
            0.001f)

        // Same as original scale but made taller with minPaneHeight.
        // The paneHeight and origin coordinates are changed but the block ratio is the same.
        val taller = TopologyScale(
                /* paneWidth= */ 640, minPaneHeight = 155.0f,
                minEdgeLength = 48f, maxEdgeLength = 64f,
                listOf(RectF(0f, 0f, 640f, 480f)))

        assertEquals(
                "{TopologyScale blockRatio=0.100000 originPaneXY=288.0,72.0 paneHeight=192.0}",
                "" + taller)
    }

    @Test
    fun twoUnalignedDisplays() {
        val scale = TopologyScale(
                /* paneWidth= */ 300, minPaneHeight = 0f,
                minEdgeLength = 48f, maxEdgeLength = 96f,
                listOf(RectF(0f, 0f, 1920f, 1200f), RectF(1920f, -300f, 3840f, 900f)))

        assertEquals(
                "{TopologyScale blockRatio=0.046875 originPaneXY=60.0,86.1 paneHeight=214.3}",
                "" + scale)

        assertPointF(
            listOf(
                PointF(78.75f, 104.8125f) to scale.displayToPaneCoor(400f, 400f),
                PointF(41.25f, 86.0625f) to scale.displayToPaneCoor(-400f, 0f),
                PointF(-384f, -940f) to scale.paneToDisplayCoor(42f, 42f),
            ), 0.001f)
    }

    @Test
    fun twoDisplaysBlockRatioBumpedForGarSizeMinimumHorizontal() {
        val scale = TopologyScale(
                /* paneWidth= */ 192, minPaneHeight = 0f,
                minEdgeLength = 48f, maxEdgeLength = 64f,
                listOf(RectF(0f, 0f, 240f, 320f), RectF(-240f, -320f, 0f, 0f)))

        // blockRatio is higher than 0.05 in order to make the smallest display edge (240 dp) 48dp
        // in the pane.
        assertEquals(
                "{TopologyScale blockRatio=0.200000 originPaneXY=96.0,136.0 paneHeight=272.0}",
                "" + scale)

        assertPointF(
            listOf(
                PointF(192f, 264f) to scale.displayToPaneCoor(480f, 640f),
                PointF(96f, 72f) to scale.displayToPaneCoor(0f, -320f),
                PointF(220f, -470f) to scale.paneToDisplayCoor(140f, 42f),
            ), 0.001f)
    }

    @Test
    fun paneVerticalPaddingSetByMinEdgeLength() {
        val scale = TopologyScale(
                /* paneWidth= */ 300, minPaneHeight = 0f,
                minEdgeLength = 48f, maxEdgeLength = 80f,
                listOf(
                        RectF(0f, 0f, 640f, 480f),
                        RectF(0f, 480f, 640f, 960f),
                        RectF(0f, 960f, 640f, 1440f),
                        RectF(0f, 1440f, 640f, 1920f),
                        RectF(0f, 1920f, 640f, 2400f),
                        RectF(0f, 2400f, 640f, 2880f)))

        assertEquals(
                "{TopologyScale blockRatio=0.125000 originPaneXY=110.0,72.0 paneHeight=504.0}",
                "" + scale)
        assertPointF(
            listOf(
                PointF(150f, 72f) to scale.displayToPaneCoor(320f, 0f),
                PointF(-80f, 2112f) to scale.paneToDisplayCoor(100f, 336f),
            ), 0.001f)
    }

    @Test
    fun limitedByCustomMaxBlockRatio() {
        val scale = TopologyScale(
                /* paneWidth= */ 300, minPaneHeight = 0f,
                minEdgeLength = 24f, maxEdgeLength = 77f,
                listOf(
                        RectF(0f, 0f, 640f, 480f),
                        RectF(0f, 480f, 640f, 960f)))

        assertEquals(
                "{TopologyScale blockRatio=0.120312 originPaneXY=111.5,36.0 paneHeight=187.5}",
                "" + scale)
        assertPointF(
            listOf(
                PointF(150f, 36.0f) to scale.displayToPaneCoor(320f, 0f),
                PointF(-95.58442f, 2493.5066f) to scale.paneToDisplayCoor(100f, 336f),
            ), 0.001f)
    }

    @Test
    fun largeCustomMinEdgeLength() {
        // minBlockEdgeLength/minDisplayEdgeLength = 80/480 = 1/6, so the block ratio will be 1/6
        val scale = TopologyScale(
                /* paneWidth= */ 300, minPaneHeight = 0f,
                minEdgeLength = 80f, maxEdgeLength = 100f,
                listOf(
                        RectF(0f, 0f, 640f, 480f),
                        RectF(0f, 480f, 640f, 960f)))

        assertEquals(
                "{TopologyScale blockRatio=0.166667 originPaneXY=96.7,120.0 paneHeight=400.0}",
                "" + scale)
        assertPointF(
            listOf(
                PointF(150f, 120f) to scale.displayToPaneCoor(320f, 0f),
                PointF(20f, 1296f) to scale.paneToDisplayCoor(100f, 336f),
            ), 0.001f)
    }
}
