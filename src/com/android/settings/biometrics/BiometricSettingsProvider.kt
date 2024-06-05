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

package com.android.settings.biometrics

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.android.settings.flags.Flags

class BiometricSettingsProvider : ContentProvider() {
  companion object {
    const val GET_SUW_FACE_ENABLED = "getSuwFaceEnabled"
    const val SUW_FACE_ENABLED = "suw_face_enabled"
  }

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
    throw UnsupportedOperationException("query operation not supported currently.")
  }

  override fun getType(uri: Uri): String? {
    throw UnsupportedOperationException("getType not supported")
  }

  override fun insert(uri: Uri, values: ContentValues?): Uri? {
    throw UnsupportedOperationException("insert not supported")
  }

  override fun query(
    uri: Uri,
    projection: Array<out String>?,
    selection: String?,
    selectionArgs: Array<out String>?,
    sortOrder: String?
  ): Cursor? {
    throw UnsupportedOperationException("query not supported")
  }

  override fun update(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<out String>?
  ): Int {
    throw UnsupportedOperationException("update not supported")
  }

  override fun onCreate(): Boolean = true

  override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
    val bundle = Bundle()
    if (Flags.biometricSettingsProvider()) {
      if (GET_SUW_FACE_ENABLED == method) {
        val faceEnabled =
          requireContext()
            .resources
            .getBoolean(com.android.settings.R.bool.config_suw_support_face_enroll)
        bundle.putBoolean(SUW_FACE_ENABLED, faceEnabled)
      }
    }
    return bundle
  }
}
