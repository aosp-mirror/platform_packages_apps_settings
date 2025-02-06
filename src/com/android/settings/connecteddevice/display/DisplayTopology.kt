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
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayTopology
import android.hardware.display.DisplayTopology.TreeNode.POSITION_BOTTOM
import android.hardware.display.DisplayTopology.TreeNode.POSITION_LEFT
import android.hardware.display.DisplayTopology.TreeNode.POSITION_RIGHT
import android.hardware.display.DisplayTopology.TreeNode.POSITION_TOP
import android.util.DisplayMetrics
import android.util.Log
import android.view.DisplayInfo
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView

import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

import java.util.Locale
import java.util.function.Consumer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// These extension methods make calls to min and max chainable.
fun Float.atMost(n: Number): Float = min(this, n.toFloat())
fun Float.atLeast(n: Number): Float = max(this, n.toFloat())

/**
 * Contains the parameters needed for transforming global display coordinates to and from topology
 * pane coordinates. This is necessary for implementing an interactive display topology pane. The
 * pane allows dragging and dropping display blocks into place to define the topology. Conversion to
 * pane coordinates is necessary when rendering the original topology. Conversion in the other
 * direction, to display coordinates, is necessary for resolve a drag position to display space.
 *
 * The topology pane coordinates are physical pixels and represent the relative position from the
 * upper-left corner of the pane. It uses a scale optimized for showing all displays with minimal
 * or no scrolling. The display coordinates are floating point and the origin can be in any
 * position. In practice the origin will be the upper-left coordinate of the primary display.
 *
 * @param paneWidth width of the pane in view coordinates
 * @param minEdgeLength the smallest length permitted of a display block. This should be set based
 *                      on accessibility requirements, but also accounting for padding that appears
 *                      around each button.
 * @param maxEdgeLength the longest width or height permitted of a display block. This will limit
 *                      the amount of dragging and scrolling the user will need to do to set the
 *                      arrangement.
 * @param displaysPos the absolute topology coordinates for each display in the topology.
 */
class TopologyScale(
        paneWidth: Int, minEdgeLength: Float, maxEdgeLength: Float,
        displaysPos: Collection<RectF>) {
    /** Scale of block sizes to real-world display sizes. Should be less than 1. */
    val blockRatio: Float

    /** Height of topology pane needed to allow all display blocks to appear with some padding. */
    val paneHeight: Float

    /** Pane's X view coordinate that corresponds with topology's X=0 coordinate. */
    val originPaneX: Float

    /** Pane's Y view coordinate that corresponds with topology's Y=0 coordinate. */
    val originPaneY: Float

    init {
        val displayBounds = RectF(
                Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
        var smallestDisplayDim = Float.MAX_VALUE
        var biggestDisplayDim = Float.MIN_VALUE

        // displayBounds is the smallest rect encompassing all displays, in display space.
        // smallestDisplayDim is the size of the smallest display edge, in display space.
        for (pos in displaysPos) {
            displayBounds.union(pos)
            smallestDisplayDim = minOf(smallestDisplayDim, pos.height(), pos.width())
            biggestDisplayDim = maxOf(biggestDisplayDim, pos.height(), pos.width())
        }

        // Initialize blockRatio such that there is 20% padding on left and right sides of the
        // display bounds.
        blockRatio = (paneWidth * 0.6 / displayBounds.width()).toFloat()
                // If the `ratio` is set too high because one of the displays will have an edge
                // greater than maxEdgeLength(px) long, decrease it such that the largest edge is
                // that long.
                .atMost(maxEdgeLength / biggestDisplayDim)
                // Also do the opposite of the above, this latter step taking precedence for a11y
                // requirements.
                .atLeast(minEdgeLength / smallestDisplayDim)

        // A tall pane is likely to result in more scrolling. So we
        // prevent the height from growing too large here, by limiting vertical padding to
        // 1.5x of the minEdgeLength on each side. This keeps a comfortable amount of
        // padding without it resulting in too much deadspace.
        paneHeight = blockRatio * displayBounds.height() + minEdgeLength * 3f

        // Set originPaneXY (the location of 0,0 in display space in the pane's coordinate system)
        // such that the display bounds rect is centered in the pane.
        // It is unlikely that either of these coordinates will be negative since blockRatio has
        // been chosen to allow 20% padding around each side of the display blocks. However, the
        // a11y requirement applied above (minEdgeLength / smallestDisplayDim) may cause the blocks
        // to not fit. This should be rare in practice, and can be worked around by moving the
        // settings UI to a larger display.
        val blockMostLeft = (paneWidth - displayBounds.width() * blockRatio) / 2
        val blockMostTop = (paneHeight - displayBounds.height() * blockRatio) / 2

        originPaneX = blockMostLeft - displayBounds.left * blockRatio
        originPaneY = blockMostTop - displayBounds.top * blockRatio
    }

    /** Transforms coordinates in view pane space to display space. */
    fun paneToDisplayCoor(paneX: Float, paneY: Float): PointF {
        return PointF((paneX - originPaneX) / blockRatio, (paneY - originPaneY) / blockRatio)
    }

    /** Transforms coordinates in display space to view pane space. */
    fun displayToPaneCoor(displayX: Float, displayY: Float): PointF {
        return PointF(displayX * blockRatio + originPaneX, displayY * blockRatio + originPaneY)
    }

    override fun toString() : String {
        return String.format(
                Locale.ROOT,
                "{TopologyScale blockRatio=%f originPaneXY=%.1f,%.1f paneHeight=%.1f}",
                blockRatio, originPaneX, originPaneY, paneHeight)
    }
}

const val TOPOLOGY_PREFERENCE_KEY = "display_topology_preference"

/** Represents a draggable block in the topology pane. */
class DisplayBlock(context : Context) : Button(context) {
    @VisibleForTesting var mSelectedImage: Drawable = ColorDrawable(Color.BLACK)
    @VisibleForTesting var mUnselectedImage: Drawable = ColorDrawable(Color.BLACK)

    private val mSelectedBg = context.getDrawable(
            R.drawable.display_block_selection_marker_background)!!
    private val mUnselectedBg = context.getDrawable(
            R.drawable.display_block_unselected_background)!!
    private val mInsetPx = context.resources.getDimensionPixelSize(R.dimen.display_block_padding)

    init {
        isScrollContainer = false
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        // Prevents shadow from appearing around edge of button.
        stateListAnimator = null
    }

    /** Sets position of the block given unpadded coordinates. */
    fun place(topLeft: PointF) {
        x = topLeft.x
        y = topLeft.y
    }

    fun setWallpaper(wallpaper: Bitmap?) {
        val wallpaperDrawable = BitmapDrawable(context.resources, wallpaper ?: return)

        fun framedBy(bg: Drawable): Drawable =
            LayerDrawable(arrayOf(wallpaperDrawable, bg)).apply {
                setLayerInsetRelative(0, mInsetPx, mInsetPx, mInsetPx, mInsetPx)
            }
        mSelectedImage = framedBy(mSelectedBg)
        mUnselectedImage = framedBy(mUnselectedBg)
    }

    fun setHighlighted(value: Boolean) {
        background = if (value) mSelectedImage else mUnselectedImage
    }

    /** Sets position and size of the block given unpadded bounds. */
    fun placeAndSize(bounds : RectF, scale : TopologyScale) {
        val topLeft = scale.displayToPaneCoor(bounds.left, bounds.top)
        val bottomRight = scale.displayToPaneCoor(bounds.right, bounds.bottom)
        val layout = layoutParams
        layout.width = (bottomRight.x - topLeft.x).toInt()
        layout.height = (bottomRight.y - topLeft.y).toInt()
        layoutParams = layout
        place(topLeft)
    }
}

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

    private val mTopologyListener = Consumer<DisplayTopology> { applyTopology(it) }

    init {
        layoutResource = R.layout.display_topology_preference

        // Prevent highlight when hovering with mouse.
        isSelectable = false

        key = TOPOLOGY_PREFERENCE_KEY
        isPersistent = false

        injector = Injector(context)
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
        super.onAttached()
        // We don't know if topology changes happened when we were detached, as it is impossible to
        // listen at that time (we must remove listeners when detaching). Setting this flag makes
        // the following onGlobalLayout call refresh the pane.
        mPaneNeedsRefresh = true
        injector.registerTopologyListener(mTopologyListener)
    }

    override fun onDetached() {
        super.onDetached()
        injector.unregisterTopologyListener(mTopologyListener)
    }

    override fun onGlobalLayout() {
        if (mPaneNeedsRefresh) {
            mPaneNeedsRefresh = false
            refreshPane()
        }
    }

    open class Injector(val context : Context) {
        /**
         * Lazy property for Display Manager, to prevent eagerly getting the service in unit tests.
         */
        private val displayManager : DisplayManager by lazy {
            context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        }

        open var displayTopology : DisplayTopology?
            get() = displayManager.displayTopology
            set(value) { displayManager.displayTopology = value }

        open val wallpaper: Bitmap?
            get() = WallpaperManager.getInstance(context).bitmap

        open val densityDpi: Int
            get() {
                val info = DisplayInfo()
                return if (context.display.getDisplayInfo(info)) {
                    info.logicalDensityDpi
                } else {
                    DisplayMetrics.DENSITY_DEFAULT
                }
            }

        open fun registerTopologyListener(listener: Consumer<DisplayTopology>) {
            displayManager.registerTopologyListener(context.mainExecutor, listener)
        }

        open fun unregisterTopologyListener(listener: Consumer<DisplayTopology>) {
            displayManager.unregisterTopologyListener(listener)
        }
    }

    /**
     * Holds information about the current system topology.
     * @param positions list of displays comprised of the display ID and position
     */
    private data class TopologyInfo(
            val topology: DisplayTopology, val scaling: TopologyScale,
            val positions: List<Pair<Int, RectF>>)

    /**
     * Holds information about the current drag operation.
     * @param stationaryDisps ID and position of displays that are not moving
     * @param display View that is currently being dragged
     * @param displayId ID of display being dragged
     * @param displayWidth width of display being dragged in actual (not View) coordinates
     * @param displayHeight height of display being dragged in actual (not View) coordinates
     * @param dragOffsetX difference between event rawX coordinate and X of the display in the pane
     * @param dragOffsetY difference between event rawY coordinate and Y of the display in the pane
     */
    private data class BlockDrag(
            val stationaryDisps : List<Pair<Int, RectF>>,
            val display: DisplayBlock, val displayId: Int,
            val displayWidth: Float, val displayHeight: Float,
            val dragOffsetX: Float, val dragOffsetY: Float)

    private var mTopologyInfo : TopologyInfo? = null
    private var mDrag : BlockDrag? = null

    private fun sameDisplayPosition(a: RectF, b: RectF): Boolean {
        // Comparing in display coordinates, so a 1 pixel difference will be less than one dp in
        // pane coordinates. Canceling the drag and refreshing the pane will not change the apparent
        // position of displays in the pane.
        val EPSILON = 1f
        return EPSILON > abs(a.left - b.left) &&
                EPSILON > abs(a.right - b.right) &&
                EPSILON > abs(a.top - b.top) &&
                EPSILON > abs(a.bottom - b.bottom)
    }

    @VisibleForTesting fun refreshPane() {
        val topology = injector.displayTopology
        if (topology == null) {
            // This occurs when no topology is active.
            // TODO(b/352648432): show main display or mirrored displays rather than an empty pane.
            mTopologyHint.text = ""
            mPaneContent.removeAllViews()
            mTopologyInfo = null
            return
        }

        applyTopology(topology)
    }

    @VisibleForTesting var mTimesReceivedSameTopology = 0

    private fun applyTopology(topology: DisplayTopology) {
        mTopologyHint.text = context.getString(R.string.external_display_topology_hint)

        val oldBounds = mTopologyInfo?.positions
        val newBounds = buildList {
            val bounds = topology.absoluteBounds
            (0..bounds.size()-1).forEach {
                add(Pair(bounds.keyAt(it), bounds.valueAt(it)))
            }
        }

        if (oldBounds != null && oldBounds.size == newBounds.size &&
                oldBounds.zip(newBounds).all { (old, new) ->
                    old.first == new.first && sameDisplayPosition(old.second, new.second)
                }) {
            mTimesReceivedSameTopology++
            return
        }

        val recycleableBlocks = ArrayDeque<DisplayBlock>()
        for (i in 0..mPaneContent.childCount-1) {
            recycleableBlocks.add(mPaneContent.getChildAt(i) as DisplayBlock)
        }

        // This density is the density of the current display (showing the topology pane). It is
        // necessary to use this density here because the topology pane coordinates are in physical
        // pixels, and the display coordinates are in density-independent pixels.
        val dpi = injector.densityDpi
        val scaling = TopologyScale(
                mPaneContent.width,
                minEdgeLength = DisplayTopology.dpToPx(60f, dpi),
                maxEdgeLength = DisplayTopology.dpToPx(256f, dpi),
                newBounds.map { it.second }.toList())
        mPaneHolder.layoutParams.let {
            val newHeight = scaling.paneHeight.toInt()
            if (it.height != newHeight) {
                it.height = newHeight
                mPaneHolder.layoutParams = it
            }
        }

        var wallpaperBitmap : Bitmap? = null

        newBounds.forEach { (id, pos) ->
            val block = recycleableBlocks.removeFirstOrNull() ?: DisplayBlock(context).apply {
                if (wallpaperBitmap == null) {
                    wallpaperBitmap = injector.wallpaper
                }
                // We need a separate wallpaper Drawable for each display block, since each needs to
                // be drawn at a separate size.
                setWallpaper(wallpaperBitmap)

                mPaneContent.addView(this)
            }
            block.setHighlighted(false)

            block.placeAndSize(pos, scaling)
            block.setOnTouchListener { view, ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> onBlockTouchDown(id, pos, block, ev)
                    MotionEvent.ACTION_MOVE -> onBlockTouchMove(ev)
                    MotionEvent.ACTION_UP -> onBlockTouchUp()
                    else -> false
                }
            }
        }
        mPaneContent.removeViews(newBounds.size, recycleableBlocks.size)

        mTopologyInfo = TopologyInfo(topology, scaling, newBounds)

        // Cancel the drag if one is in progress.
        mDrag = null
    }

    private fun onBlockTouchDown(
            displayId: Int, displayPos: RectF, block: DisplayBlock, ev: MotionEvent): Boolean {
        val positions = (mTopologyInfo ?: return false).positions

        // Do not allow dragging for single-display topology, since there is nothing to clamp it to.
        if (positions.size <= 1) { return false }

        val stationaryDisps = positions.filter { it.first != displayId }

        mDrag?.display?.setHighlighted(false)
        block.setHighlighted(true)

        // We have to use rawX and rawY for the coordinates since the view receiving the event is
        // also the view that is moving. We need coordinates relative to something that isn't
        // moving, and the raw coordinates are relative to the screen.
        mDrag = BlockDrag(
                stationaryDisps.toList(), block, displayId, displayPos.width(), displayPos.height(),
                ev.rawX - block.x, ev.rawY - block.y)

        // Prevents a container of this view from intercepting the touch events in the case the
        // pointer moves outside of the display block or the pane.
        mPaneContent.requestDisallowInterceptTouchEvent(true)
        return true
    }

    private fun onBlockTouchMove(ev: MotionEvent): Boolean {
        val drag = mDrag ?: return false
        val topology = mTopologyInfo ?: return false
        val dispDragCoor = topology.scaling.paneToDisplayCoor(
                ev.rawX - drag.dragOffsetX, ev.rawY - drag.dragOffsetY)
        val dispDragRect = RectF(
                dispDragCoor.x, dispDragCoor.y,
                dispDragCoor.x + drag.displayWidth, dispDragCoor.y + drag.displayHeight)
        val snapRect = clampPosition(drag.stationaryDisps.map { it.second }, dispDragRect)

        drag.display.place(topology.scaling.displayToPaneCoor(snapRect.left, snapRect.top))

        return true
    }

    private fun onBlockTouchUp(): Boolean {
        val drag = mDrag ?: return false
        val topology = mTopologyInfo ?: return false
        mPaneContent.requestDisallowInterceptTouchEvent(false)
        drag.display.setHighlighted(false)

        val newCoor = topology.scaling.paneToDisplayCoor(
                drag.display.x, drag.display.y)
        val newTopology = topology.topology.copy()
        val newPositions = drag.stationaryDisps.map { (id, pos) -> id to PointF(pos.left, pos.top) }
                .plus(drag.displayId to newCoor)

        val arr = hashMapOf(*newPositions.toTypedArray())
        newTopology.rearrange(arr)
        injector.displayTopology = newTopology

        refreshPane()
        return true
    }
}
