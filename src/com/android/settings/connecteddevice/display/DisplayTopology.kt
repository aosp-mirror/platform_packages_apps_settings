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

import com.android.settings.R

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF

import androidx.preference.Preference

import java.util.Locale

import kotlin.math.max
import kotlin.math.min

/**
 * Contains the parameters needed for transforming global display coordinates to and from topology
 * pane coordinates. This is necessary for implementing an interactive display topology pane. The
 * pane allows dragging and dropping display blocks into place to define the topology. Conversion to
 * pane coordinates is necessary when rendering the original topology. Conversion in the other
 * direction, to display coordinates, is necessary for resolve a drag position to display space.
 *
 * The topology pane coordinates are integral and represent the relative position from the upper-
 * left corner of the pane. It uses a scale optimized for showing all displays with minimal or no
 * scrolling. The display coordinates are floating point and the origin can be in any position. In
 * practice the origin will be the upper-left coordinate of the primary display.
 *
 * @param paneWidth width of the pane in view coordinates
 * @param minEdgeLength the smallest length permitted of a display block. This should be set based
 *                      on accessibility requirements, but also accounting for padding that appears
 *                      around each button.
 * @param maxBlockRatio the highest allowed ratio of block size to display size. For instance, a
 *                      value of 0.05 means the block will at most be 1/20 the size of the display
 *                      it represents. This limit may be breached to account for minEdgeLength,
 *                      which is considered an a11y requirement.
 * @param displaysPos the absolute topology coordinates for each display in the topology.
 */
class TopologyScale(
        paneWidth : Int, minEdgeLength : Int, maxBlockRatio : Float,
        displaysPos : Collection<RectF>) {
    /** Scale of block sizes to real-world display sizes. Should be less than 1. */
    val blockRatio : Float

    /** Height of topology pane needed to allow all display blocks to appear with some padding. */
    val paneHeight : Int

    /** Pane's X view coordinate that corresponds with topology's X=0 coordinate. */
    val originPaneX : Int

    /** Pane's Y view coordinate that corresponds with topology's Y=0 coordinate. */
    val originPaneY : Int

    init {
        val displayBounds = RectF(
                Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
        var smallestDisplayDim = Float.MAX_VALUE
        var biggestDisplayHeight = Float.MIN_VALUE

        // displayBounds is the smallest rect encompassing all displays, in display space.
        // smallestDisplayDim is the size of the smallest display edge, in display space.
        for (pos in displaysPos) {
            displayBounds.union(pos)
            smallestDisplayDim = minOf(smallestDisplayDim, pos.height(), pos.width())
            biggestDisplayHeight = max(biggestDisplayHeight, pos.height())
        }

        // Set height according to the width and the aspect ratio of the display bounds limitted by
        // maxBlockRatio. It prevents blocks from being too large, which would make dragging and
        // dropping awkward.
        val rawBlockRatio = min(maxBlockRatio, paneWidth.toFloat() * 0.6f / displayBounds.width())

        // If the `ratio` is set too low because one of the displays will have an edge less than
        // minEdgeLength(dp) long, increase it such that the smallest edge is that long.
        blockRatio = max(minEdgeLength.toFloat() / smallestDisplayDim, rawBlockRatio).toFloat()

        // Essentially, we just set the pane height based on the pre-determined pane width and the
        // aspect ratio of the display bounds. But we may need to increase it slightly to achieve
        // 20% padding above and below the display bounds - this is where the 0.6 comes from.
        val rawPaneHeight = max(
                paneWidth.toDouble() / displayBounds.width() * displayBounds.height(),
                displayBounds.height() * blockRatio / 0.6)

        // It is easy for the aspect ratio to result in an excessively tall pane, since the width is
        // pre-determined and may be considerably wider than necessary. So we prevent the height
        // from growing too large here, by limiting vertical padding to the size of the tallest
        // display. This improves results for very tall display bounds.
        paneHeight = min(
                rawPaneHeight.toInt(),
                (blockRatio * (displayBounds.height() + biggestDisplayHeight * 2f)).toInt())

        // Set originPaneXY (the location of 0,0 in display space in the pane's coordinate system)
        // such that the display bounds rect is centered in the pane.
        // It is unlikely that either of these coordinates will be negative since blockRatio has
        // been chosen to allow 20% padding around each side of the display blocks. However, the
        // a11y requirement applied above (minEdgeLength / smallestDisplayDim) may cause the blocks
        // to not fit. This should be rare in practice, and can be worked around by moving the
        // settings UI to a larger display.
        val blockMostLeft = (paneWidth - displayBounds.width() * blockRatio) / 2
        val blockMostTop = (paneHeight - displayBounds.height() * blockRatio) / 2

        originPaneX = (blockMostLeft - displayBounds.left * blockRatio).toInt()
        originPaneY = (blockMostTop - displayBounds.top * blockRatio).toInt()
    }

    /** Transforms coordinates in view pane space to display space. */
    fun paneToDisplayCoor(panePos : Point) : PointF {
        return PointF(
                (panePos.x - originPaneX).toFloat() / blockRatio,
                (panePos.y - originPaneY).toFloat() / blockRatio)
    }

    /** Transforms coordinates in display space to view pane space. */
    fun displayToPaneCoor(displayPos : PointF) : Point {
        return Point(
                (displayPos.x * blockRatio).toInt() + originPaneX,
                (displayPos.y * blockRatio).toInt() + originPaneY)
    }

    override fun toString() : String {
        return String.format(
                Locale.ROOT,
                "{TopoScale blockRatio=%f originPaneXY=%d,%d paneHeight=%d}",
                blockRatio, originPaneX, originPaneY, paneHeight)
    }
}

const val PREFERENCE_KEY = "display_topology_preference"

/**
 * DisplayTopologyPreference allows the user to change the display topology
 * when there is one or more extended display attached.
 */
class DisplayTopologyPreference(context : Context) : Preference(context) {
    init {
        layoutResource = R.layout.display_topology_preference

        // Prevent highlight when hovering with mouse.
        isSelectable = false

        key = PREFERENCE_KEY
    }
}
