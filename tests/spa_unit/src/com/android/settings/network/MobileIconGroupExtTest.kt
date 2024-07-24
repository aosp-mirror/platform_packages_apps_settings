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

package com.android.settings.network

import android.text.Spanned
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MobileIconGroupExtTest {
    @Test
    fun maybeToHtml_withoutHtmlTag() {
        val actual = CONNECTED_5G.maybeToHtml()

        assertThat(actual).isSameInstanceAs(CONNECTED_5G)
    }

    @Test
    fun maybeToHtml_withHtmlTag() {
        val actual = CONNECTED_5GE.maybeToHtml()

        assertThat(actual).isInstanceOf(Spanned::class.java)
    }

    private companion object {
        private const val CONNECTED_5G = "Connected / 5G"
        private const val CONNECTED_5GE = "Connected / <i>5G <small>E</small></i>"
    }
}
