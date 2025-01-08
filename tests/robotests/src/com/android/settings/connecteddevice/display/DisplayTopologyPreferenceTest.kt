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

import android.hardware.display.DisplayTopology.TreeNode.POSITION_BOTTOM
import android.hardware.display.DisplayTopology.TreeNode.POSITION_LEFT
import android.hardware.display.DisplayTopology.TreeNode.POSITION_TOP

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayTopology
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.view.MotionEventBuilder

import com.android.settings.R
import com.google.common.truth.Truth.assertThat

import java.util.function.Consumer

import kotlin.math.abs

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DisplayTopologyPreferenceTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preference = DisplayTopologyPreference(context)
    val injector = TestInjector(context)
    val rootView = View.inflate(context, preference.layoutResource, /*parent=*/ null)
    val holder = PreferenceViewHolder.createInstanceForTests(rootView)
    val wallpaper = ColorDrawable(Color.MAGENTA)

    init {
        preference.injector = injector
        injector.systemWallpaper = wallpaper
        preference.onBindViewHolder(holder)
    }

    class TestInjector(context : Context) : DisplayTopologyPreference.Injector(context) {
        var topology: DisplayTopology? = null
        var systemWallpaper: Drawable? = null
        var topologyListener: Consumer<DisplayTopology>? = null

        override var displayTopology : DisplayTopology?
            get() = topology
            set(value) { topology = value }

        override val wallpaper : Drawable
            get() = systemWallpaper!!

        override fun registerTopologyListener(listener: Consumer<DisplayTopology>) {
            if (topologyListener != null) {
                throw IllegalStateException(
                        "already have a listener registered: ${topologyListener}")
            }
            topologyListener = listener
        }

        override fun unregisterTopologyListener(listener: Consumer<DisplayTopology>) {
            if (topologyListener != listener) {
                throw IllegalStateException("no such listener registered: ${listener}")
            }
            topologyListener = null
        }
    }

    @Test
    fun disabledTopology() {
        preference.onAttached()
        preference.onGlobalLayout()

        assertThat(preference.mPaneContent.childCount).isEqualTo(0)
        // TODO(b/352648432): update test when we show the main display even when
        // a topology is not active.
        assertThat(preference.mTopologyHint.text).isEqualTo("")
    }

    private fun getPaneChildren(): List<DisplayBlock> =
        (0..preference.mPaneContent.childCount-1)
                .map { preference.mPaneContent.getChildAt(it) as DisplayBlock }
                .toList()

    fun twoDisplayTopology(childPosition: Int, childOffset: Float): DisplayTopology {
        val primaryId = 1

        val child = DisplayTopology.TreeNode(
                /* displayId= */ 42, /* width= */ 100f, /* height= */ 80f,
                childPosition, childOffset)
        val root = DisplayTopology.TreeNode(
                primaryId, /* width= */ 200f, /* height= */ 160f, POSITION_LEFT, /* offset= */ 0f)
        root.addChild(child)

        return DisplayTopology(root, primaryId)
    }

    /** Uses the topology in the injector to populate and prepare the pane for interaction. */
    fun preparePane() {
        // This layoutParams needs to be non-null for the global layout handler.
        preference.mPaneHolder.layoutParams = FrameLayout.LayoutParams(
                /* width= */ 640, /* height= */ 480)

        // Force pane width to have a reasonable value (hundreds of dp) so the TopologyScale is
        // calculated reasonably.
        preference.mPaneContent.left = 0
        preference.mPaneContent.right = 640

        preference.onAttached()
        preference.onGlobalLayout()
    }

    /**
     * Sets up a simple topology in the pane with two displays. Returns the left-hand display and
     * right-hand display in order in a list. The right-hand display is the root.
     */
    fun setupTwoDisplays(childPosition: Int = POSITION_LEFT, childOffset: Float = 42f):
            List<DisplayBlock> {
        injector.topology = twoDisplayTopology(childPosition, childOffset)

        preparePane()

        val paneChildren = getPaneChildren()
        assertThat(paneChildren).hasSize(2)

        // Block of child display is on the left.
        return if (paneChildren[0].x < paneChildren[1].x)
                paneChildren
        else
                paneChildren.reversed()
    }

    @Test
    fun twoDisplaysGenerateBlocks() {
        val (childBlock, rootBlock) = setupTwoDisplays()

        // After accounting for padding, child should be half the length of root in each dimension.
        assertThat(childBlock.layoutParams.width + BLOCK_PADDING)
                .isEqualTo(rootBlock.layoutParams.width / 2)
        assertThat(childBlock.layoutParams.height + BLOCK_PADDING)
                .isEqualTo(rootBlock.layoutParams.height / 2)
        assertThat(childBlock.y).isGreaterThan(rootBlock.y)
        assertThat(childBlock.background).isEqualTo(wallpaper)
        assertThat(rootBlock.background).isEqualTo(wallpaper)
        assertThat(rootBlock.x - BLOCK_PADDING * 2)
                .isEqualTo(childBlock.x + childBlock.layoutParams.width)

        assertThat(preference.mTopologyHint.text)
                .isEqualTo(context.getString(R.string.external_display_topology_hint))
    }

    @Test
    fun dragDisplayDownward() {
        val (leftBlock, rightBlock) = setupTwoDisplays()

        val downEvent = MotionEventBuilder.newBuilder()
                .setPointer(0f, 0f)
                .setAction(MotionEvent.ACTION_DOWN)
                .build()

        // Move the left block half of its height downward. This is 40 pixels in display
        // coordinates. The original offset is 42, so the new offset will be 42 + 40.
        val moveEvent = MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(0f, leftBlock.layoutParams.height / 2f + BLOCK_PADDING)
                .build()
        val upEvent = MotionEventBuilder.newBuilder().setAction(MotionEvent.ACTION_UP).build()

        leftBlock.dispatchTouchEvent(downEvent)
        leftBlock.dispatchTouchEvent(moveEvent)
        leftBlock.dispatchTouchEvent(upEvent)

        val rootChildren = injector.topology!!.root!!.children
        assertThat(rootChildren).hasSize(1)
        val child = rootChildren[0]
        assertThat(child.position).isEqualTo(POSITION_LEFT)
        assertThat(child.offset).isWithin(1f).of(82f)
    }

    @Test
    fun dragRootDisplayToNewSide() {
        val (leftBlock, rightBlock) = setupTwoDisplays()

        val downEvent = MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_DOWN)
                .setPointer(0f, 0f)
                .build()

        // Move the right block left and upward. We won't move it into exactly the correct position,
        // relying on the clamp algorithm to choose the correct side and offset.
        val moveEvent = MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(
                        -leftBlock.layoutParams.width - 2f * BLOCK_PADDING,
                        -leftBlock.layoutParams.height / 2f)
                .build()

        val upEvent = MotionEventBuilder.newBuilder().setAction(MotionEvent.ACTION_UP).build()

        assertThat(leftBlock.y).isGreaterThan(rightBlock.y)

        rightBlock.dispatchTouchEvent(downEvent)
        rightBlock.dispatchTouchEvent(moveEvent)
        rightBlock.dispatchTouchEvent(upEvent)

        val rootChildren = injector.topology!!.root!!.children
        assertThat(rootChildren).hasSize(1)
        val child = rootChildren[0]
        assertThat(child.position).isEqualTo(POSITION_BOTTOM)
        assertThat(child.offset).isWithin(1f).of(0f)

        // After rearranging blocks, the original block views should still be present.
        val paneChildren = getPaneChildren()
        assertThat(paneChildren.indexOf(leftBlock)).isNotEqualTo(-1)
        assertThat(paneChildren.indexOf(rightBlock)).isNotEqualTo(-1)

        // Left edge of both blocks should be aligned after dragging.
        assertThat(paneChildren[0].x)
                .isWithin(1f)
                .of(paneChildren[1].x)
    }

    @Test
    fun keepOriginalViewsWhenAddingMore() {
        setupTwoDisplays()
        val childrenBefore = getPaneChildren()
        injector.topology!!.addDisplay(/* displayId= */ 101, 320f, 240f)
        preference.refreshPane()
        val childrenAfter = getPaneChildren()

        assertThat(childrenBefore).hasSize(2)
        assertThat(childrenAfter).hasSize(3)
        assertThat(childrenAfter.subList(0, 2)).isEqualTo(childrenBefore)
    }

    @Test
    fun applyNewTopologyViaListenerUpdate() {
        setupTwoDisplays()
        val newTopology = injector.topology!!.copy()
        newTopology.addDisplay(/* displayId= */ 8008, /* width= */ 300f, /* height= */ 320f)

        injector.topology = newTopology
        injector.topologyListener!!.accept(newTopology)

        assertThat(preference.mTimesReceivedSameTopology).isEqualTo(0)
        val paneChildren = getPaneChildren()
        assertThat(paneChildren).hasSize(3)

        // Look for a display with the same unusual aspect ratio as the one we've added.
        val expectedAspectRatio = 300f/320f
        assertThat(paneChildren
                .map { (it.layoutParams.width.toFloat() + BLOCK_PADDING*2) /
                        (it.layoutParams.height.toFloat() + BLOCK_PADDING*2) }
                .filter { abs(it - expectedAspectRatio) < 0.001f }
        ).hasSize(1)
    }

    @Test
    fun ignoreListenerUpdateOfUnchangedTopology() {
        injector.topology = twoDisplayTopology(POSITION_TOP, /* offset= */ 12.0f)
        preparePane()

        assertThat(preference.mTimesReceivedSameTopology).isEqualTo(0)
        injector.topology = twoDisplayTopology(POSITION_TOP, /* offset= */ 12.1f)
        injector.topologyListener!!.accept(injector.topology!!)

        assertThat(preference.mTimesReceivedSameTopology).isEqualTo(1)
    }

    @Test
    fun updatedTopologyCancelsDragIfNonTrivialChange() {
        val (leftBlock, rightBlock) = setupTwoDisplays(POSITION_LEFT, /* childOffset= */ 42f)

        assertThat(leftBlock.unpaddedY).isWithin(0.01f).of(142.17f)

        leftBlock.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_DOWN)
                .setPointer(0f, 0f)
                .build())
        leftBlock.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(0f, 30f)
                .build())
        assertThat(leftBlock.unpaddedY).isWithin(0.01f).of(172.17f)

        // Offset is only different by 0.5 dp, so the drag will not cancel.
        injector.topology = twoDisplayTopology(POSITION_LEFT, /* childOffset= */ 41.5f)
        injector.topologyListener!!.accept(injector.topology!!)

        assertThat(leftBlock.unpaddedY).isWithin(0.01f).of(172.17f)
        // Move block farther downward.
        leftBlock.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(0f, 50f)
                .build())
        assertThat(leftBlock.unpaddedY).isWithin(0.01f).of(192.17f)

        injector.topology = twoDisplayTopology(POSITION_LEFT, /* childOffset= */ 20f)
        injector.topologyListener!!.accept(injector.topology!!)

        assertThat(leftBlock.unpaddedY).isWithin(0.01f).of(125.67f)
        // Another move in the opposite direction should not move the left block.
        leftBlock.dispatchTouchEvent(MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(0f, -20f)
                .build())
        assertThat(leftBlock.unpaddedY).isWithin(0.01f).of(125.67f)
    }
}
