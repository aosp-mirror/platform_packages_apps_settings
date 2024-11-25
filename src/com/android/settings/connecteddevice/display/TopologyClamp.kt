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

import android.graphics.RectF
import kotlin.math.hypot

// Unfortunately, in the world of IEEE 32-bit floats, A + X - X is not always == A
// For example: A = 1075.4271f
//              C = 1249.2203f
// For example: - A - 173.79326f = - C
// However:     - C + A = - 173.79321f
// So we need to keep track of how the movingDisplay block is attaching to otherDisplays throughout
// the calculations below. We cannot use the rect.left with its width as a proxy for rect.right. We
// have to save the "inner" or attached side and use the width or height to calculate the "external"
// side.

/** A potential X position for the display to clamp at. */
private class XCoor(
    val left : Float, val right : Float,

    /**
     * If present, the position of the display being attached to. If absent, indicates the X
     * position is derived from the exact drag position.
     */
    val attaching : RectF?,
)

/** A potential Y position for the display to clamp at. */
private class YCoor(
    val top : Float, val bottom : Float,

    /**
     * If present, the position of the display being attached to. If absent, indicates the Y
     * position is derived from the exact drag position.
     */
    val attaching : RectF?,
)

/**
 * Finds the optimal clamp position assuming the user has dragged the block to `movingDisplay`.
 *
 * @param otherDisplays positions of the stationary displays (every one not being dragged)
 * @param movingDisplay the position the user is current holding the block during a drag
 *
 * @return the clamp position as a RectF, whose dimensions will match that of `movingDisplay`
 */
fun clampPosition(otherDisplays : Iterable<RectF>, movingDisplay : RectF) : RectF {
    val xCoors = otherDisplays.flatMap {
        listOf(
            // Attaching to left edge of `it`
            XCoor(it.left - movingDisplay.width(), it.left, it),
            // Attaching to right edge of `it`
            XCoor(it.right, it.right + movingDisplay.width(), it),
        )
    }.plusElement(XCoor(movingDisplay.left, movingDisplay.right, null))

    val yCoors = otherDisplays.flatMap {
        listOf(
            // Attaching to the top edge of `it`
            YCoor(it.top - movingDisplay.height(), it.top, it),
            // Attaching to the bottom edge of `it`
            YCoor(it.bottom, it.bottom + movingDisplay.height(), it),
        )
    }.plusElement(YCoor(movingDisplay.top, movingDisplay.bottom, null))

    class Cand(val x : XCoor, val y : YCoor)

    val candidateGrid = xCoors.flatMap { x -> yCoors.map { y -> Cand(x, y) }}
    val hasAttachInRange = candidateGrid.filter {
        if (it.x.attaching != null) {
            // Attaching to a vertical (left or right) edge. The y range of dragging and
            // stationary blocks must overlap.
            it.y.top <= it.x.attaching.bottom && it.y.bottom >= it.x.attaching.top
        } else if (it.y.attaching != null) {
            // Attaching to a horizontal (top or bottom) edge. The x range of dragging and
            // stationary blocks must overlap.
            it.x.left <= it.y.attaching.right && it.x.right >= it.y.attaching.left
        } else {
            // Not attaching to another display's edge at all, so not a valid clamp position.
            false
        }
    }
    // Clamp positions closest to the user's drag position are best. Sort by increasing distance
    // from it, so the best will be first.
    val prioritized = hasAttachInRange.sortedBy {
        hypot(it.x.left - movingDisplay.left, it.y.top - movingDisplay.top)
    }
    val notIntersectingAny = prioritized.asSequence()
        .map { RectF(it.x.left, it.y.top, it.x.right, it.y.bottom) }
        .filter { p -> otherDisplays.all { !RectF.intersects(p, it) } }

    // Note we return a copy of `movingDisplay` if there is no valid clamp position, which will only
    // happen if `otherDisplays` is empty or has no valid rectangles. It may not be wise to rely on
    // this behavior.
    return notIntersectingAny.firstOrNull() ?: RectF(movingDisplay)
}
