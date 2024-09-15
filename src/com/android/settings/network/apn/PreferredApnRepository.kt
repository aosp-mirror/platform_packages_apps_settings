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

package com.android.settings.network.apn

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import com.android.settingslib.spaprivileged.database.contentChangeFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreferredApnRepository(private val context: Context, private val subId: Int) {
    private val contentResolver = context.contentResolver
    private val preferredApnUri =
        Uri.withAppendedPath(Telephony.Carriers.PREFERRED_APN_URI, "$subId")

    /** TODO: Move this to UI layer, when UI layer migrated to Kotlin. */
    fun restorePreferredApn(lifecycleOwner: LifecycleOwner, onRestored: () -> Unit) {
        lifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                restorePreferredApn()
            }
            onRestored()
        }
    }

    fun restorePreferredApn() {
        contentResolver.delete(
            Uri.withAppendedPath(RestorePreferredApnUri, "subId/$subId"), null, null
        )
    }

    fun setPreferredApn(apnId: String) {
        val values = ContentValues().apply {
            put(ApnSettings.APN_ID, apnId)
        }
        contentResolver.update(preferredApnUri, values, null, null)
    }

    /** TODO: Move this to UI layer, when UI layer migrated to Kotlin. */
    fun collectPreferredApn(lifecycleOwner: LifecycleOwner, action: (String?) -> Unit) {
        preferredApnFlow().collectLatestWithLifecycle(lifecycleOwner, action = action)
    }

    fun preferredApnFlow(): Flow<String?> = context.contentChangeFlow(preferredApnUri).map {
        contentResolver.query(
            preferredApnUri,
            arrayOf(Telephony.Carriers._ID),
            null,
            null,
            Telephony.Carriers.DEFAULT_SORT_ORDER,
        ).use { cursor ->
            if (cursor?.moveToNext() == true) {
                cursor.getString(cursor.getColumnIndex(Telephony.Carriers._ID))
            } else {
                null
            }.also { Log.d(TAG, "[$subId] preferred APN: $it") }
        }
    }.conflate().flowOn(Dispatchers.Default)

    companion object {
        private const val TAG = "PreferredApnRepository"

        private const val RESTORE_PREFERRED_APN = "content://telephony/carriers/restore"

        @JvmStatic
        val RestorePreferredApnUri: Uri = Uri.parse(RESTORE_PREFERRED_APN)
    }
}
