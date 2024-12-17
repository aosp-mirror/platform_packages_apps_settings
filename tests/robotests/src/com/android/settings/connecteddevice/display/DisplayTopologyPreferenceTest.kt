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

import android.hardware.display.DisplayTopology.TreeNode.POSITION_LEFT

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayTopology
import android.view.View
import android.widget.FrameLayout
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider

import com.android.settings.R
import com.google.common.truth.Truth.assertThat

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DisplayTopologyPreferenceTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preference = DisplayTopologyPreference(context)
    val injector = TestInjector()
    val rootView = View.inflate(context, preference.layoutResource, /*parent=*/ null)
    val holder = PreferenceViewHolder.createInstanceForTests(rootView)
    val wallpaper = ColorDrawable(Color.MAGENTA)

    init {
        preference.injector = injector
        injector.systemWallpaper = wallpaper
        preference.onBindViewHolder(holder)
    }

    class TestInjector : DisplayTopologyPreference.Injector() {
        var topology : DisplayTopology? = null
        var systemWallpaper : Drawable? = null

        override fun displayTopology(context : Context) : DisplayTopology? { return topology }

        override fun wallpaper(context : Context) : Drawable { return systemWallpaper!! }
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

    @Test
    fun twoDisplaysGenerateBlocks() {
        val child = DisplayTopology.TreeNode(
                /* displayId= */ 42, /* width= */ 100f, /* height= */ 80f,
                POSITION_LEFT, /* offset= */ 42f)
        val root = DisplayTopology.TreeNode(
                /* displayId= */ 0, /* width= */ 200f, /* height= */ 160f,
                POSITION_LEFT, /* offset= */ 0f)
        root.addChild(child)
        injector.topology = DisplayTopology(root, /*primaryDisplayId=*/ 0)

        // This layoutParams needs to be non-null for the global layout handler.
        preference.mPaneHolder.layoutParams = FrameLayout.LayoutParams(
                /* width= */ 640, /* height= */ 480)

        // Force pane width to have a reasonable value (hundreds of dp) so the TopologyScale is
        // calculated reasonably.
        preference.mPaneContent.left = 0
        preference.mPaneContent.right = 640

        preference.onAttached()
        preference.onGlobalLayout()

        assertThat(preference.mPaneContent.childCount).isEqualTo(2)
        val block0 = preference.mPaneContent.getChildAt(0)
        val block1 = preference.mPaneContent.getChildAt(1)

        // Block of child display is on the left.
        val (childBlock, rootBlock) = if (block0.x < block1.x)
                listOf(block0, block1)
        else
                listOf(block1, block0)

        // After accounting for padding, child should be half the length of root in each dimension.
        assertThat(childBlock.layoutParams.width + BLOCK_PADDING)
                .isEqualTo(rootBlock.layoutParams.width / 2)
        assertThat(childBlock.layoutParams.height + BLOCK_PADDING)
                .isEqualTo(rootBlock.layoutParams.height / 2)
        assertThat(childBlock.y).isGreaterThan(rootBlock.y)
        assertThat(block0.background).isEqualTo(wallpaper)
        assertThat(block1.background).isEqualTo(wallpaper)
        assertThat(rootBlock.x - BLOCK_PADDING * 2)
                .isEqualTo(childBlock.x + childBlock.layoutParams.width)

        assertThat(preference.mTopologyHint.text)
                .isEqualTo(context.getString(R.string.external_display_topology_hint))
    }
}
