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

package com.android.settings.notification

import android.app.INotificationManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager.RINGER_MODE_NORMAL
import android.media.AudioManager.RINGER_MODE_SILENT
import android.media.AudioManager.RINGER_MODE_VIBRATE
import android.media.AudioManager.STREAM_RING
import android.os.ServiceManager
import android.os.UserManager
import android.os.Vibrator
import android.service.notification.NotificationListenerService.HINT_HOST_DISABLE_CALL_EFFECTS
import android.service.notification.NotificationListenerService.HINT_HOST_DISABLE_EFFECTS
import androidx.preference.Preference
import com.android.settings.PreferenceRestrictionMixin
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.RangeValue
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
open class SeparateRingVolumePreference :
    PreferenceMetadata,
    PreferenceBinding,
    PersistentPreference<Int>,
    RangeValue,
    PreferenceAvailabilityProvider,
    PreferenceIconProvider,
    PreferenceRestrictionMixin {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.separate_ring_volume_option_title

    override fun getIcon(context: Context) =
        when {
            VolumeHelper.isMuted(context, STREAM_RING) -> getMuteIcon(context)
            else -> R.drawable.ic_ring_volume
        }

    override fun isAvailable(context: Context) = !createAudioHelper(context).isSingleVolume

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_ADJUST_VOLUME)

    override fun storage(context: Context): KeyValueStore {
        val helper = createAudioHelper(context)
        return object : NoOpKeyedObservable<String>(), KeyValueStore {
            override fun contains(key: String) = key == KEY

            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> getValue(key: String, valueType: Class<T>) =
                helper.getStreamVolume(STREAM_RING) as T

            override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
                helper.setStreamVolume(STREAM_RING, value as Int)
            }
        }
    }

    override fun getReadPermit(context: Context, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, value: Int?, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getMinValue(context: Context) =
        createAudioHelper(context).getMinVolume(STREAM_RING)

    override fun getMaxValue(context: Context) =
        createAudioHelper(context).getMaxVolume(STREAM_RING)

    override fun createWidget(context: Context) = VolumeSeekBarPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as VolumeSeekBarPreference).apply {
            setStream(STREAM_RING)
            setMuteIcon(getMuteIcon(preference.context))
            setListener { updateContentDescription(this) }
            setSuppressionText(getSuppressionText(preference.context))
        }
    }

    open fun createAudioHelper(context: Context) = AudioHelper(context)

    private fun updateContentDescription(preference: VolumeSeekBarPreference) {
        val context = preference.context
        val ringerMode = getEffectiveRingerMode(context)
        when (ringerMode) {
            RINGER_MODE_VIBRATE ->
                preference.updateContentDescription(
                    context.getString(R.string.ringer_content_description_vibrate_mode)
                )
            RINGER_MODE_SILENT ->
                preference.updateContentDescription(
                    context.getString(R.string.ringer_content_description_silent_mode)
                )
            else -> preference.updateContentDescription(preference.title)
        }
    }

    fun getMuteIcon(context: Context): Int {
        val ringerMode = getEffectiveRingerMode(context)
        return when (ringerMode) {
            RINGER_MODE_NORMAL -> R.drawable.ic_ring_volume
            RINGER_MODE_VIBRATE -> R.drawable.ic_volume_ringer_vibrate
            else -> R.drawable.ic_ring_volume_off
        }
    }

    fun getEffectiveRingerMode(context: Context): Int {
        val hasVibrator = context.getSystemService(Vibrator::class.java)?.hasVibrator() ?: false
        val ringerMode = createAudioHelper(context).ringerModeInternal
        return when {
            !hasVibrator && ringerMode == RINGER_MODE_VIBRATE -> RINGER_MODE_SILENT
            else -> ringerMode
        }
    }

    private fun getSuppressionText(context: Context): String? {
        val suppressor = NotificationManager.from(context).getEffectsSuppressor()
        val notificationManager =
            INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE)
            )
        val hints = notificationManager.hintsFromListenerNoToken
        return when {
            hintsMatch(hints) -> SuppressorHelper.getSuppressionText(context, suppressor)
            else -> null
        }
    }

    private fun hintsMatch(hints: Int) =
        (hints and HINT_HOST_DISABLE_CALL_EFFECTS) != 0 ||
            (hints and HINT_HOST_DISABLE_EFFECTS) != 0

    companion object {
        const val KEY = "separate_ring_volume"
    }
}
// LINT.ThenChange(SeparateRingVolumePreferenceController.java)
