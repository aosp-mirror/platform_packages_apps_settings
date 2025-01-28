/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.settings.supervision

import android.app.supervision.SupervisionManager
import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SupervisionMainSwitchPreferenceTest {
    private val preference = SupervisionMainSwitchPreference()

    private val mockSupervisionManager = mock<SupervisionManager>()

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val context =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any =
                when (name) {
                    Context.SUPERVISION_SERVICE -> mockSupervisionManager
                    else -> super.getSystemService(name)
                }
        }

    @Test
    fun checked_supervisionEnabled_returnTrue() {
        setSupervisionEnabled(true)

        assertThat(getMainSwitchPreference().isChecked).isTrue()
    }

    @Test
    fun checked_supervisionDisabled_returnFalse() {
        setSupervisionEnabled(false)

        assertThat(getMainSwitchPreference().isChecked).isFalse()
    }

    @Test
    fun toggleOn() {
        setSupervisionEnabled(false)
        val widget = getMainSwitchPreference()

        assertThat(widget.isChecked).isFalse()

        widget.performClick()

        assertThat(widget.isChecked).isTrue()
        verify(mockSupervisionManager).setSupervisionEnabled(true)
    }

    @Test
    fun toggleOff() {
        setSupervisionEnabled(true)
        val widget = getMainSwitchPreference()

        assertThat(widget.isChecked).isTrue()

        widget.performClick()

        assertThat(widget.isChecked).isFalse()
        verify(mockSupervisionManager).setSupervisionEnabled(false)
    }

    private fun getMainSwitchPreference(): MainSwitchPreference =
        preference.createAndBindWidget(context)

    private fun setSupervisionEnabled(enabled: Boolean) =
        mockSupervisionManager.stub { on { isSupervisionEnabled } doReturn enabled }
}
