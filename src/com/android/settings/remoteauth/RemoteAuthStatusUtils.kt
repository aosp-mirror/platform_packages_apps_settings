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

package com.android.settings.remoteauth

import android.content.Context
import com.android.settings.R

/**
 * Utilities for remoteauth details shared between Security Settings and Safety Center.
 */
object RemoteAuthStatusUtils  {
    /**
     * Returns the summary of remote auth settings entity.
     */
    fun getSummary(context: Context): String {
        // TODO(b/290768873): Update text based on if authenticator is enrolled.
        return context.resources.getString(R.string.security_settings_remoteauth_preference_summary)
    }

    /**
     * Returns the class name of the Settings page corresponding to remote auth settings.
     */
    fun getSettingsClassName() = RemoteAuthActivityInternal::class.java.name

}