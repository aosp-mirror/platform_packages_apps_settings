/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.spa.app.specialaccess

import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.MODE_ERRORED
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.settingslib.spaprivileged.framework.common.alarmManager
import com.android.settingslib.spaprivileged.framework.common.appOpsManager
import com.android.settingslib.spaprivileged.model.app.userId

class AlarmsAndRemindersController(
    context: Context,
    private val app: ApplicationInfo,
) {
    private val alarmManager = context.alarmManager
    private val appOpsManager = context.appOpsManager

    val isAllowed: LiveData<Boolean>
        get() = _allowed

    fun setAllowed(allowed: Boolean) {
        val mode = if (allowed) MODE_ALLOWED else MODE_ERRORED
        appOpsManager.setUidMode(AppOpsManager.OP_SCHEDULE_EXACT_ALARM, app.uid, mode)
        _allowed.postValue(allowed)
    }

    private val _allowed = object : MutableLiveData<Boolean>() {
        override fun onActive() {
            postValue(alarmManager.hasScheduleExactAlarm(app.packageName, app.userId))
        }
    }
}
