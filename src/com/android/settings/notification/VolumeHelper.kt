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

import android.app.NotificationManager
import android.app.NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS
import android.app.NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA
import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.*
import android.provider.Settings.Global.ZEN_MODE_ALARMS
import android.provider.Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS
import android.provider.Settings.Global.ZEN_MODE_NO_INTERRUPTIONS
import android.service.notification.ZenModeConfig

class VolumeHelper {
    companion object {
        fun isMuted(context: Context, streamType: Int): Boolean {
            val audioManager = context.getSystemService(AudioManager::class.java)
            return audioManager.isStreamMute(streamType) && !isZenMuted(context, streamType)
        }

        fun isZenMuted(context: Context, streamType: Int): Boolean {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val zenMode = notificationManager.getZenMode()
            val notificationPolicy = notificationManager.getConsolidatedNotificationPolicy()
            val isAllowAlarms =
                (notificationPolicy.priorityCategories and PRIORITY_CATEGORY_ALARMS) != 0
            val isAllowMedia =
                (notificationPolicy.priorityCategories and PRIORITY_CATEGORY_MEDIA) != 0
            val isAllowRinger =
                !ZenModeConfig.areAllPriorityOnlyRingerSoundsMuted(notificationPolicy)
            return isNotificationOrRingStream(streamType)
                    && zenMode == ZEN_MODE_ALARMS || zenMode == ZEN_MODE_NO_INTERRUPTIONS
                    || (zenMode == ZEN_MODE_IMPORTANT_INTERRUPTIONS
                    && (!isAllowRinger && isNotificationOrRingStream(streamType)
                    || !isAllowMedia && isMediaStream(streamType)
                    || !isAllowAlarms && isAlarmStream(streamType)))
        }

        private fun isNotificationOrRingStream(streamType: Int) =
            streamType == STREAM_RING || streamType == STREAM_NOTIFICATION

        private fun isAlarmStream(streamType: Int) = streamType == STREAM_ALARM

        private fun isMediaStream(streamType: Int) = streamType == STREAM_MUSIC
    }
}