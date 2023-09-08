/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.ui

import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import com.android.settings.ui.testutils.SettingsTestUtils.SETTINGS_PACKAGE
import com.android.settings.ui.testutils.SettingsTestUtils.startMainActivityFromHomeScreen
import com.android.settings.ui.testutils.SettingsTestUtils.waitObject
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies basic functionality of the About Phone screen  */
@RunWith(AndroidJUnit4::class)
@SmallTest
class AboutPhoneSettingsTests {
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = startMainActivityFromHomeScreen(Settings.ACTION_DEVICE_INFO_SETTINGS)
    }

    @Test
    fun testAllMenuEntriesExist() {
        searchForItemsAndTakeAction(device)
    }

    /**
     * Removes items found in the view and optionally takes some action.
     */
    private fun removeItemsAndTakeAction(device: UiDevice, itemsLeftToFind: MutableList<String>) {
        val iterator = itemsLeftToFind.iterator()
        while (iterator.hasNext()) {
            val itemText = iterator.next()
            val item = device.waitObject(By.text(itemText))
            if (item != null) {
                iterator.remove()
            }
        }
    }

    /**
     * Searches for UI elements in the current view and optionally takes some action.
     *
     *
     * Will scroll down the screen until it has found all elements or reached the bottom.
     * This allows elements to be found and acted on even if they change order.
     */
    private fun searchForItemsAndTakeAction(device: UiDevice) {
        val itemsLeftToFind = resourceTexts.toMutableList()
        assertWithMessage("There must be at least one item to search for on the screen!")
            .that(itemsLeftToFind)
            .isNotEmpty()
        var canScrollDown = true
        while (canScrollDown && itemsLeftToFind.isNotEmpty()) {
            removeItemsAndTakeAction(device, itemsLeftToFind)

            // when we've finished searching the current view, scroll down
            val view = device.waitObject(By.res("$SETTINGS_PACKAGE:id/main_content"))
            canScrollDown = view?.scroll(Direction.DOWN, 1.0f) ?: false
        }
        // check the last items once we have reached the bottom of the view
        removeItemsAndTakeAction(device, itemsLeftToFind)
        assertWithMessage(
            "The following items were not found on the screen: "
                + itemsLeftToFind.joinToString(", ")
        )
            .that(itemsLeftToFind)
            .isEmpty()
    }

    companion object {
        // TODO: retrieve using name/ids from com.android.settings package
        private val resourceTexts = listOf(
            "Device name",
            "Legal information",
            "Regulatory labels"
        )
    }
}
