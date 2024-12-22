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

package com.android.settings.network.telephony

import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.ims.ImsManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.TwoStatePreference
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController
import com.android.settings.network.ims.VolteQueryImsState
import com.android.settings.network.ims.VtQueryImsState
import com.android.settings.network.telephony.Enhanced4gBasePreferenceController.On4gLteUpdateListener
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchItem
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchResult
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Preference controller for "Video Calling" */
class VideoCallingPreferenceController
@JvmOverloads
constructor(
    context: Context,
    key: String,
    private val callStateRepository: CallStateRepository = CallStateRepository(context),
) : TogglePreferenceController(context, key), On4gLteUpdateListener {

    private var subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    private var preference: TwoStatePreference? = null
    private var callingPreferenceCategoryController: CallingPreferenceCategoryController? = null
    private val repository = VideoCallingRepository(context)

    private var videoCallEditable = false
    private var isInCall = false

    /** Init instance of VideoCallingPreferenceController. */
    fun init(
        subId: Int,
        callingPreferenceCategoryController: CallingPreferenceCategoryController?,
    ): VideoCallingPreferenceController {
        this.subId = subId
        this.callingPreferenceCategoryController = callingPreferenceCategoryController

        return this
    }

    // Availability is controlled in onViewCreated() and VideoCallingSearchItem.
    override fun getAvailabilityStatus() = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        repository.isVideoCallReadyFlow(subId).collectLatestWithLifecycle(viewLifecycleOwner) {
            isReady ->
            preference?.isVisible = isReady
            callingPreferenceCategoryController?.updateChildVisible(preferenceKey, isReady)
        }
        callStateRepository.callStateFlow(subId).collectLatestWithLifecycle(viewLifecycleOwner) {
            callState ->
            isInCall = callState != TelephonyManager.CALL_STATE_IDLE
            updatePreference()
        }
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        videoCallEditable =
            queryVoLteState(subId).isEnabledByUser && queryImsState(subId).isAllowUserControl
        updatePreference()
    }

    private fun updatePreference() {
        preference?.isEnabled = videoCallEditable && !isInCall
        preference?.isChecked = videoCallEditable && isChecked
    }

    override fun getSliceHighlightMenuRes() = NO_RES

    override fun setChecked(isChecked: Boolean): Boolean {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false
        }
        val imsMmTelManager = ImsManager(mContext).getImsMmTelManager(subId)
        try {
            imsMmTelManager.isVtSettingEnabled = isChecked
            return true
        } catch (exception: IllegalArgumentException) {
            Log.w(TAG, "[$subId] Unable to set VT status $isChecked", exception)
        }
        return false
    }

    override fun isChecked(): Boolean = queryImsState(subId).isEnabledByUser

    override fun on4gLteUpdated() {
        preference?.let { updateState(it) }
    }

    @VisibleForTesting fun queryImsState(subId: Int) = VtQueryImsState(mContext, subId)

    @VisibleForTesting fun queryVoLteState(subId: Int) = VolteQueryImsState(mContext, subId)

    companion object {
        private const val TAG = "VideoCallingPreferenceController"

        class VideoCallingSearchItem(private val context: Context) :
            MobileNetworkSettingsSearchItem {
            private val repository = VideoCallingRepository(context)

            private fun isAvailable(subId: Int): Boolean = runBlocking {
                repository.isVideoCallReadyFlow(subId).first()
            }

            override fun getSearchResult(subId: Int): MobileNetworkSettingsSearchResult? {
                if (!isAvailable(subId)) return null
                return MobileNetworkSettingsSearchResult(
                    key = "video_calling_key",
                    title = context.getString(R.string.video_calling_settings_title),
                )
            }
        }
    }
}
