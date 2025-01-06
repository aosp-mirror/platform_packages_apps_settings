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

import android.Manifest.permission.MODIFY_AUDIO_SETTINGS
import android.Manifest.permission.MODIFY_AUDIO_SETTINGS_PRIVILEGED
import android.app.INotificationManager
import android.app.NotificationManager
import android.app.NotificationManager.ACTION_EFFECTS_SUPPRESSOR_CHANGED
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.FEATURE_AUTOMOTIVE
import android.media.AudioManager
import android.media.AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION
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
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.datastore.and
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.RangeValue
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
open class SeparateRingVolumePreference :
    PreferenceMetadata,
    PreferenceBinding,
    PersistentPreference<Int>,
    RangeValue,
    PreferenceAvailabilityProvider,
    PreferenceIconProvider,
    PreferenceLifecycleProvider,
    PreferenceRestrictionMixin {

    private var broadcastReceiver: BroadcastReceiver? = null

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.separate_ring_volume_option_title

    override fun getIcon(context: Context) = context.getIconRes()

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

    override fun getReadPermissions(context: Context) = Permissions.EMPTY

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context): Permissions? {
        var permissions = Permissions.allOf(MODIFY_AUDIO_SETTINGS)
        if (context.packageManager.hasSystemFeature(FEATURE_AUTOMOTIVE)) {
            permissions = permissions and MODIFY_AUDIO_SETTINGS_PRIVILEGED
        }
        return permissions
    }

    override fun getWritePermit(context: Context, value: Int?, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun getMinValue(context: Context) =
        createAudioHelper(context).getMinVolume(STREAM_RING)

    override fun getMaxValue(context: Context) =
        createAudioHelper(context).getMaxVolume(STREAM_RING)

    override fun createWidget(context: Context) = VolumeSeekBarPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as VolumeSeekBarPreference).apply {
            setStream(STREAM_RING)
            setMuteIcon(context.getIconRes())
            updateContentDescription(context.getContentDescription())
            setListener { updateContentDescription(context.getContentDescription()) }
            setSuppressionText(context.getSuppressionText())
        }
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context, intent: Intent) {
                    context.notifyPreferenceChange(KEY)
                }
            }
        context.registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(ACTION_EFFECTS_SUPPRESSOR_CHANGED)
                addAction(INTERNAL_RINGER_MODE_CHANGED_ACTION)
            },
        )
        broadcastReceiver = receiver
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        super.onStop(context)
        broadcastReceiver?.let { context.unregisterReceiver(it) }
    }

    open fun createAudioHelper(context: Context) = AudioHelper(context)

    companion object {
        const val KEY = "separate_ring_volume"
    }
}

fun Context.getContentDescription() =
    when (getEffectiveRingerMode()) {
        RINGER_MODE_VIBRATE -> getString(R.string.ringer_content_description_vibrate_mode)
        RINGER_MODE_SILENT -> getString(R.string.ringer_content_description_silent_mode)
        else -> getString(R.string.separate_ring_volume_option_title)
    }

fun Context.getIconRes() =
    when (getEffectiveRingerMode()) {
        RINGER_MODE_NORMAL -> R.drawable.ic_ring_volume
        RINGER_MODE_VIBRATE -> R.drawable.ic_volume_ringer_vibrate
        else -> R.drawable.ic_ring_volume_off
    }

fun Context.getEffectiveRingerMode(): Int {
    val hasVibrator = getSystemService(Vibrator::class.java)?.hasVibrator() ?: false
    val ringerMode =
        getSystemService(AudioManager::class.java)?.getRingerModeInternal() ?: RINGER_MODE_NORMAL
    return when {
        !hasVibrator && ringerMode == RINGER_MODE_VIBRATE -> RINGER_MODE_SILENT
        else -> ringerMode
    }
}

fun Context.getSuppressionText(): String? {
    val suppressor = NotificationManager.from(this).getEffectsSuppressor()
    val hints =
        INotificationManager.Stub.asInterface(ServiceManager.getService(NOTIFICATION_SERVICE))
            ?.hintsFromListenerNoToken ?: 0
    return when {
        (hints and HINT_HOST_DISABLE_CALL_EFFECTS) != 0 ||
            (hints and HINT_HOST_DISABLE_EFFECTS) != 0 ->
            SuppressorHelper.getSuppressionText(this, suppressor)
        else -> null
    }
}
// LINT.ThenChange(SeparateRingVolumePreferenceController.java)
