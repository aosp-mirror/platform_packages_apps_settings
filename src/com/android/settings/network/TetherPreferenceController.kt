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

import android.content.Context
import android.net.TetheringManager
import android.os.UserHandle
import android.os.UserManager
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.TetherUtil
import com.android.settingslib.Utils
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TetherPreferenceController
@JvmOverloads
constructor(
    context: Context,
    key: String,
    private val tetheredRepository: TetheredRepository = TetheredRepository(context),
) : BasePreferenceController(context, key) {

    private val tetheringManager = mContext.getSystemService(TetheringManager::class.java)!!

    private var preference: Preference? = null

    private val isTetherAvailableFlow =
        flow { emit(TetherUtil.isTetherAvailable(mContext)) }
            .distinctUntilChanged()
            .conflate()
            .flowOn(Dispatchers.Default)

    /**
     * Always returns available here to avoid ANR.
     * - Actual UI visibility is handled in [onViewCreated].
     * - Search visibility is handled in [updateNonIndexableKeys].
     */
    override fun getAvailabilityStatus() = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        isTetherAvailableFlow.collectLatestWithLifecycle(viewLifecycleOwner) {
            preference?.isVisible = it
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                getTitleResId()?.let { preference?.setTitle(it) }
            }
        }

        tetheredRepository.tetheredTypesFlow().collectLatestWithLifecycle(viewLifecycleOwner) {
            preference?.setSummary(getSummaryResId(it))
        }
    }

    private suspend fun getTitleResId(): Int? = withContext(Dispatchers.Default) {
        if (isTetherConfigDisallowed(mContext)) null
        else Utils.getTetheringLabel(tetheringManager)
    }

    @VisibleForTesting
    @StringRes
    fun getSummaryResId(tetheredTypes: Set<Int>): Int {
        val hotSpotOn = TetheringManager.TETHERING_WIFI in tetheredTypes
        val tetherOn = tetheredTypes.any { it != TetheringManager.TETHERING_WIFI }
        return when {
            hotSpotOn && tetherOn -> R.string.tether_settings_summary_hotspot_on_tether_on
            hotSpotOn -> R.string.tether_settings_summary_hotspot_on_tether_off
            tetherOn -> R.string.tether_settings_summary_hotspot_off_tether_on
            else -> R.string.tether_preference_summary_off
        }
    }

    override fun updateNonIndexableKeys(keys: MutableList<String>) {
        if (!TetherUtil.isTetherAvailable(mContext)) {
            keys += preferenceKey
        }
    }

    companion object {
        @JvmStatic
        fun isTetherConfigDisallowed(context: Context?): Boolean =
            RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                context, UserManager.DISALLOW_CONFIG_TETHERING, UserHandle.myUserId()
            ) != null
    }
}
