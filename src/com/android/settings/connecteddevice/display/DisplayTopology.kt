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

import android.app.WallpaperManager
import com.android.settings.R

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayTopology
import android.hardware.display.DisplayTopology.TreeNode.POSITION_BOTTOM
import android.hardware.display.DisplayTopology.TreeNode.POSITION_LEFT
import android.hardware.display.DisplayTopology.TreeNode.POSITION_RIGHT
import android.hardware.display.DisplayTopology.TreeNode.POSITION_TOP
import android.util.Log
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView

import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

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

/** dp of padding on each side of a display block. */
const val BLOCK_PADDING = 2

/**
 * DisplayTopologyPreference allows the user to change the display topology
 * when there is one or more extended display attached.
 */
class DisplayTopologyPreference(context : Context)
        : Preference(context), ViewTreeObserver.OnGlobalLayoutListener {
    @VisibleForTesting lateinit var mPaneContent : FrameLayout
    @VisibleForTesting lateinit var mPaneHolder : FrameLayout
    @VisibleForTesting lateinit var mTopologyHint : TextView

    @VisibleForTesting var injector : Injector

    /**
     * This is needed to prevent a repopulation of the pane causing another
     * relayout and vice-versa ad infinitum.
     */
    private var mPaneNeedsRefresh = false

    init {
        layoutResource = R.layout.display_topology_preference

        // Prevent highlight when hovering with mouse.
        isSelectable = false

        key = PREFERENCE_KEY

        injector = Injector()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val newPane = holder.findViewById(R.id.display_topology_pane_content) as FrameLayout
        if (this::mPaneContent.isInitialized) {
            if (newPane == mPaneContent) {
                return
            }
            mPaneContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
        mPaneContent = newPane
        mPaneHolder = holder.itemView as FrameLayout
        mTopologyHint = holder.findViewById(R.id.topology_hint) as TextView
        mPaneContent.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onAttached() {
        // We don't know if topology changes happened when we were detached, as it is impossible to
        // listen at that time (we must remove listeners when detaching). Setting this flag makes
        // the following onGlobalLayout call refresh the pane.
        mPaneNeedsRefresh = true
    }

    override fun onGlobalLayout() {
        if (mPaneNeedsRefresh) {
            mPaneNeedsRefresh = false
            refreshPane()
        }
    }

    open class Injector {
        open fun displayTopology(context : Context) : DisplayTopology? {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            return displayManager.displayTopology
        }

        open fun wallpaper(context : Context) : Drawable {
            return WallpaperManager.getInstance(context).drawable ?: ColorDrawable(Color.BLACK)
        }
    }

    private fun calcAbsRects(
            dest : MutableMap<Int, RectF>, n : DisplayTopology.TreeNode, x : Float, y : Float) {
        dest.put(n.displayId, RectF(x, y, x + n.width, y + n.height))

        for (c in n.children) {
            val (xoff, yoff) = when (c.position) {
                POSITION_LEFT -> Pair(-c.width, +c.offset)
                POSITION_RIGHT -> Pair(+n.width, +c.offset)
                POSITION_TOP -> Pair(+c.offset, -c.height)
                POSITION_BOTTOM -> Pair(+c.offset, +n.height)
                else -> throw IllegalStateException("invalid position for display: ${c}")
            }
            calcAbsRects(dest, c, x + xoff, y + yoff)
        }
    }

    private fun refreshPane() {
        mPaneContent.removeAllViews()

        val root = injector.displayTopology(context)?.root
        if (root == null) {
            // This occurs when no topology is active.
            // TODO(b/352648432): show main display or mirrored displays rather than an empty pane.
            mTopologyHint.text = ""
            return
        }
        mTopologyHint.text = context.getString(R.string.external_display_topology_hint)

        val blocksPos = buildMap { calcAbsRects(this, root, x = 0f, y = 0f) }

        val scaling = TopologyScale(
                mPaneContent.width, minEdgeLength = 60, maxBlockRatio = 0.12f, blocksPos.values)
        mPaneHolder.layoutParams.let {
            if (it.height != scaling.paneHeight) {
                it.height = scaling.paneHeight
                mPaneHolder.layoutParams = it
            }
        }
        val wallpaper = injector.wallpaper(context)
        blocksPos.values.forEach { p ->
            Button(context).apply {
                isScrollContainer = false
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                background = wallpaper
                val topLeft = scaling.displayToPaneCoor(PointF(p.left, p.top))
                val bottomRight = scaling.displayToPaneCoor(PointF(p.right, p.bottom))

                mPaneContent.addView(this)

                val layout = layoutParams
                layout.width = bottomRight.x - topLeft.x - BLOCK_PADDING * 2
                layout.height = bottomRight.y - topLeft.y - BLOCK_PADDING * 2
                layoutParams = layout
                x = (topLeft.x + BLOCK_PADDING).toFloat()
                y = (topLeft.y + BLOCK_PADDING).toFloat()
            }
        }
    }
}
