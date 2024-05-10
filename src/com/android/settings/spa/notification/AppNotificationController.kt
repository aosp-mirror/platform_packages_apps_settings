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

package com.android.settings.spa.notification

import android.content.pm.ApplicationInfo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class AppNotificationController(
    private val repository: AppNotificationRepository,
    private val app: ApplicationInfo,
) {
    val isEnabled: LiveData<Boolean>
        get() = _isEnabled

    fun getEnabled() = _isEnabled.get()

    fun setEnabled(enabled: Boolean) {
        if (repository.setEnabled(app, enabled)) {
            _isEnabled.postValue(enabled)
        }
    }

    private val _isEnabled = object : MutableLiveData<Boolean>() {
        override fun onActive() {
            postValue(repository.isEnabled(app))
        }

        override fun onInactive() {
        }

        fun get(): Boolean = value ?: repository.isEnabled(app).also {
            postValue(it)
        }
    }
}
