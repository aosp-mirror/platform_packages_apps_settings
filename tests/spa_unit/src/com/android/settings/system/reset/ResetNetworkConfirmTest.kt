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

package com.android.settings.system.reset

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.fragment.app.testing.launchFragment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.ResetNetworkRequest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ResetNetworkConfirmTest {
    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {}

    private val scenario = launchFragment<ResetNetworkConfirm>()

    @Test
    fun resetNetworkData_notResetEsim() {
        scenario.recreate().onFragment { fragment ->
            fragment.resetNetworkRequest = ResetNetworkRequest(ResetNetworkRequest.RESET_NONE)

            runBlocking { fragment.onResetClicked() }

            verify(context, never()).getSystemService(any())
        }
    }

    @Test
    fun setSubtitle_eraseEsim() {
        scenario.onFragment { fragment ->
            fragment.resetNetworkRequest =
                ResetNetworkRequest(ResetNetworkRequest.RESET_NONE).apply {
                    setResetEsim(context.packageName)
                }

            val view = fragment.onCreateView(LayoutInflater.from(context), null, null)

            assertThat(view.requireViewById<TextView>(R.id.reset_network_confirm).text)
                .isEqualTo(context.getString(R.string.reset_network_final_desc_esim))
        }
    }

    @Test
    fun setSubtitle_notEraseEsim() {
        scenario.onFragment { fragment ->
            fragment.resetNetworkRequest = ResetNetworkRequest(ResetNetworkRequest.RESET_NONE)

            val view = fragment.onCreateView(LayoutInflater.from(context), null, null)

            assertThat(view.requireViewById<TextView>(R.id.reset_network_confirm).text)
                .isEqualTo(context.getString(R.string.reset_network_final_desc))
        }
    }
}
