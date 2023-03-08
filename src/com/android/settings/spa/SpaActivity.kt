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

package com.android.settings.spa

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.RemoteException
import android.os.UserHandle
import android.util.Log
import com.android.settingslib.spa.framework.BrowseActivity
import com.android.settingslib.spa.framework.util.SESSION_BROWSE
import com.android.settingslib.spa.framework.util.SESSION_EXTERNAL
import com.android.settingslib.spa.framework.util.appendSpaParams

class SpaActivity : BrowseActivity() {
    companion object {
        private const val TAG = "SpaActivity"
        @JvmStatic
        fun Context.startSpaActivity(destination: String) {
            val intent = Intent(this, SpaActivity::class.java)
                .appendSpaParams(destination = destination)
            if (isLaunchedFromInternal()) {
                intent.appendSpaParams(sessionName = SESSION_BROWSE)
            } else {
                intent.appendSpaParams(sessionName = SESSION_EXTERNAL)
            }
            startActivity(intent)
        }

        @JvmStatic
        fun Context.startSpaActivityForApp(destinationPrefix: String, intent: Intent): Boolean {
            val packageName = intent.data?.schemeSpecificPart ?: return false
            startSpaActivity("$destinationPrefix/$packageName/${UserHandle.myUserId()}")
            return true
        }

        fun Context.isLaunchedFromInternal(): Boolean {
            var pkg: String? = null
            try {
                pkg = ActivityManager.getService().getLaunchedFromPackage(getActivityToken())
            } catch (e: RemoteException) {
                Log.v(TAG, "Could not talk to activity manager.", e)
            }
            return applicationContext.packageName == pkg
        }
    }
}
