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
package com.android.settings.network.tether

import android.net.TetheringManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.ShadowConnectivityManager
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal
import com.android.settingslib.Utils
import com.android.settingslib.preference.CatalystScreenTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowConnectivityManager::class, ShadowRestrictedLockUtilsInternal::class,
    ShadowTetheringManager::class])
class TetherScreenTest : CatalystScreenTestCase() {
    override val preferenceScreenCreator = TetherScreen()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_TETHER_SETTINGS

    // TODO: Remove override (See b/368359963#comment7)
    override fun migration() {}

    @Before
    fun setUp() {
        ShadowConnectivityManager.getShadow().setTetheringSupported(true)
    }

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(TetherScreen.KEY)
    }

    @Test
    fun getPreferenceTitle_tetherConfigDisallowed_shouldShowAll() {
        ShadowRestrictedLockUtilsInternal.setRestricted(true)

        assertThat(preferenceScreenCreator.getPreferenceTitle(appContext)).isEqualTo(
                appContext.getString(R.string.tether_settings_title_all))
    }

    @Test
    fun getPreferenceTitle_tetherConfigAllowed_shouldShowTetheringLabel() {
        ShadowRestrictedLockUtilsInternal.setRestricted(false)
        val tm = appContext.getSystemService(TetheringManager::class.java)

        assertThat(preferenceScreenCreator.getPreferenceTitle(appContext)).isEqualTo(
                appContext.getText(Utils.getTetheringLabel(tm)))
    }

    @Test
    fun isAvailable_tetherIsAvailable_shouldReturnTrue() {
        ShadowRestrictedLockUtilsInternal.setRestricted(false)

        assertThat(preferenceScreenCreator.isAvailable(appContext)).isTrue()
    }

    @Test
    fun isAvailable_tetherIsUnavailable_shouldReturnFalse() {
        ShadowRestrictedLockUtilsInternal.setRestricted(true)

        assertThat(preferenceScreenCreator.isAvailable(appContext)).isFalse()
    }
}

@Implements(TetheringManager::class)
class ShadowTetheringManager {
    private val emptyArray = arrayOf<String>()

    @Implementation
    fun getTetheredIfaces() = emptyArray

    @Implementation
    fun getTetherableIfaces() = emptyArray

    @Implementation
    fun getTetherableWifiRegexs() = emptyArray

    @Implementation
    fun getTetherableUsbRegexs() = emptyArray

    @Implementation
    fun getTetherableBluetoothRegexs() = emptyArray
}
