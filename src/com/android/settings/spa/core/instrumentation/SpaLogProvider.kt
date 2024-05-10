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

package com.android.settings.spa.core.instrumentation

import android.app.settings.SettingsEnums
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.android.settings.core.instrumentation.ElapsedTimeUtils
import com.android.settings.core.instrumentation.SettingsStatsLog
import com.android.settingslib.spa.framework.common.LOG_DATA_SESSION_NAME
import com.android.settingslib.spa.framework.common.LogCategory
import com.android.settingslib.spa.framework.common.LogEvent
import com.android.settingslib.spa.framework.common.SpaLogger
import com.android.settingslib.spa.framework.util.SESSION_BROWSE
import com.android.settingslib.spa.framework.util.SESSION_EXTERNAL
import com.android.settingslib.spa.framework.util.SESSION_SEARCH
import com.android.settingslib.spa.framework.util.SESSION_SLICE
import com.android.settingslib.spa.framework.util.SESSION_UNKNOWN

/**
 * To receive the events from spa framework and logging the these events.
 */
object SpaLogProvider : SpaLogger {
    private val dataModel = MetricsDataModel()

    override fun event(id: String, event: LogEvent, category: LogCategory, extraData: Bundle) {
        when(event) {
            LogEvent.PAGE_ENTER, LogEvent.PAGE_LEAVE ->
                write(SpaLogData(id, event, extraData, dataModel))
            else -> return  //TODO(b/253979024): Will be implemented in subsequent CLs.
        }
    }

    private fun write(data: SpaLogData) {
        with(data) {
            SettingsStatsLog.write(
                SettingsStatsLog.SETTINGS_SPA_REPORTED /* atomName */,
                getSessionType(),
                getPageId(),
                getTarget(),
                getAction(),
                getKey(),
                getValue(),
                getPreValue(),
                getElapsedTime()
            )
        }
    }
}

@VisibleForTesting
class SpaLogData(val id: String, val event: LogEvent,
                         val extraData: Bundle, val dataModel: MetricsDataModel) {

    fun getSessionType(): Int {
        if (!extraData.containsKey(LOG_DATA_SESSION_NAME)) {
            return SettingsEnums.SESSION_UNKNOWN
        }
        val sessionSource = extraData.getString(LOG_DATA_SESSION_NAME)
        return when(sessionSource) {
            SESSION_BROWSE -> SettingsEnums.SESSION_BROWSE
            SESSION_SEARCH -> SettingsEnums.SESSION_SEARCH
            SESSION_SLICE -> SettingsEnums.SESSION_SLICE_TYPE
            SESSION_EXTERNAL -> SettingsEnums.SESSION_EXTERNAL
            else -> SettingsEnums.SESSION_UNKNOWN
        }
    }

    fun getPageId(): String {
        return when(event) {
            LogEvent.PAGE_ENTER, LogEvent.PAGE_LEAVE -> id
            else -> getPageIdByEntryId(id)
        }
    }

    //TODO(b/253979024): Will be implemented in subsequent CLs.
    fun getTarget(): String? {
        return null
    }

    fun getAction(): Int = when (event) {
        LogEvent.PAGE_ENTER -> SettingsEnums.PAGE_VISIBLE
        LogEvent.PAGE_LEAVE -> SettingsEnums.PAGE_HIDE
        LogEvent.ENTRY_CLICK -> SettingsEnums.ACTION_SETTINGS_TILE_CLICK
        LogEvent.ENTRY_SWITCH -> SettingsEnums.ACTION_SETTINGS_PREFERENCE_CHANGE
    }

    //TODO(b/253979024): Will be implemented in subsequent CLs.
    fun getKey(): String? {
        return null
    }

    fun getValue(): String? {
        when(event) {
            LogEvent.PAGE_ENTER -> dataModel.addTimeStamp(
                PageTimeStamp(id, System.currentTimeMillis()))
            LogEvent.PAGE_LEAVE -> return dataModel.getPageDuration(id)
            else -> {} //TODO(b/253979024): Will be implemented in subsequent CLs.
        }
        return null
    }

    //TODO(b/253979024): Will be implemented in subsequent CLs.
    fun getPreValue(): String? {
        return null
    }

    fun getElapsedTime(): Long {
        return ElapsedTimeUtils.getElapsedTime(System.currentTimeMillis())
    }

    //TODO(b/253979024): Will be implemented in subsequent CLs. Remove @Suppress when done.
    private fun getPageIdByEntryId(@Suppress("UNUSED_PARAMETER") id: String): String {
        return ""
    }
}

/**
 * The buffer is keeping the time stamp while spa page entering.
 */
data class PageTimeStamp(val pageId: String, val timeStamp: Long)
