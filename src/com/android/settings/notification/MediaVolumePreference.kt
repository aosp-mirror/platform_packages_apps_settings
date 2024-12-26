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
import android.content.Context
import android.content.pm.PackageManager.FEATURE_AUTOMOTIVE
import android.media.AudioManager.STREAM_MUSIC
import android.os.UserManager
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
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.RangeValue
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
open class MediaVolumePreference :
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
        get() = R.string.media_volume_option_title

    override fun getIcon(context: Context) =
        when {
            VolumeHelper.isMuted(context, STREAM_MUSIC) -> R.drawable.ic_media_stream_off
            else -> R.drawable.ic_media_stream
        }

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_media_volume)

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_ADJUST_VOLUME)

    override fun storage(context: Context): KeyValueStore {
        val helper = createAudioHelper(context)
        return object : NoOpKeyedObservable<String>(), KeyValueStore {
            override fun contains(key: String) = key == KEY

            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> getValue(key: String, valueType: Class<T>) =
                helper.getStreamVolume(STREAM_MUSIC) as T

            override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
                helper.setStreamVolume(STREAM_MUSIC, value as Int)
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
        createAudioHelper(context).getMinVolume(STREAM_MUSIC)

    override fun getMaxValue(context: Context) =
        createAudioHelper(context).getMaxVolume(STREAM_MUSIC)

    override fun createWidget(context: Context) = VolumeSeekBarPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as VolumeSeekBarPreference).apply {
            setStream(STREAM_MUSIC)
            setMuteIcon(R.drawable.ic_media_stream_off)
            setListener { updateContentDescription(this) }
        }
    }

    open fun createAudioHelper(context: Context) = AudioHelper(context)

    private fun updateContentDescription(preference: VolumeSeekBarPreference) {
        when {
            preference.isMuted ->
                preference.updateContentDescription(
                    preference.context.getString(
                        R.string.volume_content_description_silent_mode,
                        preference.title,
                    )
                )
            else -> preference.updateContentDescription(preference.title)
        }
    }

    companion object {
        const val KEY = "media_volume"
    }
}
// LINT.ThenChange(MediaVolumePreferenceController.java)
