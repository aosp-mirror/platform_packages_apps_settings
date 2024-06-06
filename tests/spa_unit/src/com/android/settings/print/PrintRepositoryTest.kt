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

package com.android.settings.print

import android.content.Context
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.graphics.drawable.Drawable
import android.print.PrintManager
import android.printservice.PrintServiceInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.spa.testutils.firstWithTimeoutOrNull
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy

@RunWith(AndroidJUnit4::class)
class PrintRepositoryTest {

    private val printServiceInfo = PrintServiceInfo(
        /* resolveInfo = */ ResolveInfo().apply { serviceInfo = MockServiceInfo },
        /* settingsActivityName = */ "",
        /* addPrintersActivityName = */ "",
        /* advancedPrintOptionsActivityName = */ "",
    )

    private val mockPrintManager = mock<PrintManager> {
        on { getPrintServices(PrintManager.ALL_SERVICES) } doReturn listOf(printServiceInfo)
    }

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(PrintManager::class.java) } doReturn mockPrintManager
    }

    private val repository = PrintRepository(context)

    @Test
    fun printServiceDisplayInfosFlow_title() = runBlocking {
        val displayInfo = repository.printServiceDisplayInfosFlow().firstWithTimeoutOrNull()!!
            .single()

        assertThat(displayInfo.title).isEqualTo(LABEL)
    }

    @Test
    fun printServiceDisplayInfosFlow_isEnabled() = runBlocking {
        printServiceInfo.setIsEnabled(true)

        val displayInfo = repository.printServiceDisplayInfosFlow().firstWithTimeoutOrNull()!!
            .single()

        assertThat(displayInfo.isEnabled).isTrue()
        assertThat(displayInfo.summary)
            .isEqualTo(context.getString(R.string.print_feature_state_on))
    }

    @Test
    fun printServiceDisplayInfosFlow_notEnabled() = runBlocking {
        printServiceInfo.setIsEnabled(false)

        val displayInfo = repository.printServiceDisplayInfosFlow().firstWithTimeoutOrNull()!!
            .single()

        assertThat(displayInfo.isEnabled).isFalse()
        assertThat(displayInfo.summary)
            .isEqualTo(context.getString(R.string.print_feature_state_off))
    }

    @Test
    fun printServiceDisplayInfosFlow_componentName() = runBlocking {
        val displayInfo = repository.printServiceDisplayInfosFlow().firstWithTimeoutOrNull()!!
            .single()

        assertThat(displayInfo.componentName).isEqualTo("$PACKAGE_NAME/$SERVICE_NAME")
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"
        const val SERVICE_NAME = "ServiceName"
        const val LABEL = "Label"
        val MockServiceInfo = mock<ServiceInfo> {
            on { loadLabel(any()) } doReturn LABEL
            on { loadIcon(any()) } doReturn mock<Drawable>()
        }.apply {
            packageName = PACKAGE_NAME
            name = SERVICE_NAME
        }
    }
}
