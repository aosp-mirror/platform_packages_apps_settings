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

import android.graphics.PointF
import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TopologyClampTest {
    @Test
    fun clampToSides() {
        val start = RectF(6f, 0f, 16f, 10f)
        val clamp1 = clampPosition(listOf(RectF(0f, 0f, 10f, 10f)), start)
        assertEquals(RectF(10f, 0f, 20f, 10f), clamp1)

        val clamp2 = clampPosition(listOf(RectF(18f, 0f, 28f, 10f)), start)
        assertEquals(RectF(8f, 0f, 18f, 10f), clamp2)
    }

    @Test
    fun clampToTopOrBottom() {
        val start = RectF(0f, 6f, 10f, 16f)
        val clamp1 = clampPosition(listOf(RectF(0f, 0f, 10f, 10f)), start)
        assertEquals(RectF(0f, 10f, 10f, 20f), clamp1)

        val clamp2 = clampPosition(listOf(RectF(0f, 18f, 10f, 28f)), start)
        assertEquals(RectF(0f, 8f, 10f, 18f), clamp2)
    }

    @Test
    fun clampToCloserSide() {
        // Shift one pixel right.
        val start = RectF(9f, 8f, 19f, 18f)
        val clamp1 = clampPosition(listOf(RectF(0f, 0f, 10f, 10f)), start)
        assertEquals(RectF(10f, 8f, 20f, 18f), clamp1)

        // Shift two pixels down.
        start.set(7f, 8f, 17f, 18f)
        val clamp2 = clampPosition(listOf(RectF(0f, 0f, 10f, 10f)), start)
        assertEquals(RectF(7f, 10f, 17f, 20f), clamp2)

        // Shift three pixels left.
        start.set(-7f, -6f, 3f, 4f);
        val s3 = clampPosition(listOf(RectF(0f, 0f, 10f, 10f)), start)
        assertEquals(RectF(-10f, -6f, 0f, 4f), s3)
    }

    @Test
    fun clampToCloserDisplayInCorner() {
        val start = RectF(9f, 6f, 19f, 16f)
        val clamp1 = clampPosition(listOf(RectF(0f, 0f, 8f, 8f), RectF(8f, 0f, 16f, 4f)), start)
        assertEquals(RectF(8f, 6f, 18f, 16f), clamp1)

        start.set(10f, 5f, 20f, 15f)
        val clamp2 = clampPosition(listOf(RectF(0f, 0f, 8f, 8f), RectF(8f, 0f, 16f, 4f)), start)
        assertEquals(RectF(10f, 4f, 20f, 14f), clamp2)
    }

    @Test
    fun clampToSecondDisplayToAvoidOverlap() {
        val start = RectF(8f, 3f, 18f, 13f)
        val clamp = clampPosition(listOf(RectF(0f, 0f, 8f, 8f), RectF(8f, 0f, 16f, 4f)), start)
        assertEquals(RectF(8f, 4f, 18f, 14f), clamp)
    }

    @Test
    fun clampToInnerCorner() {
        val start = RectF(4f, 4f, 14f, 14f)
        val clamp = clampPosition(listOf(RectF(5f, 0f, 10f, 5f), RectF(0f, 5f, 5f, 10f)), start)
        assertEquals(RectF(5f, 5f, 15f, 15f), clamp)
    }

    @Test
    fun mustBeAdjacent() {
        val start = RectF(9f, 10f, 14f, 15f)

        // Have candidate X, Y pair that is not adjacent to any display.
        val clamp = clampPosition(listOf(RectF(5f, 0f, 10f, 5f), RectF(0f, 5f, 5f, 10f)), start)
        assertEquals(RectF(5f, 10f, 10f, 15f), clamp)
    }

    @Test
    fun mustNotIntersect() {
        // 1 and 2 are attached with 1/3 of their respective sides. Attempt to drag the other
        // display to 1's lower-right corner. It should be forced to the right side of 2.
        //111
        //111
        //111
        //  222
        //  222
        //  222

        val start = RectF(30f, 30f, 60f, 60f)
        val clamp = clampPosition(listOf(RectF(0f, 0f, 30f, 30f), RectF(20f, 30f, 50f, 60f)), start)
        assertEquals(RectF(50f, 30f, 80f, 60f), clamp)
    }

    @Test
    fun attachingToTwoRectsAtOnce() {
        // 2 is being dragged and starts out overlapping 0 and 1, then it is
        // clamped to the right side of 0 and the bottom of 1 at the same time.
        //
        //00
        //002
        //  2
        // 11
        // 11

        val clamp = clampPosition(
                listOf(RectF(0f, 0f, 20f, 20f), RectF(10f, 30f, 30f, 50f)),
                RectF(10f, 11f, 20f, 31f))

        assertEquals(RectF(20f, 10f, 30f, 30f), clamp)
    }

    @Test
    fun attachingToTwoRectsAtOnceAxisSwapped() {
        // Same as previous but with x and y swapped.

        val clamp = clampPosition(
                listOf(RectF(0f, 0f, 20f, 20f), RectF(30f, 10f, 50f, 30f)),
                RectF(11f, 10f, 31f, 20f))

        assertEquals(RectF(10f, 20f, 30f, 30f), clamp)
    }
}
