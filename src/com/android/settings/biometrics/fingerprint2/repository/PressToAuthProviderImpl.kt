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

package com.android.settings.biometrics.fingerprint2.repository

import android.content.Context
import android.provider.Settings
import com.android.settings.biometrics.fingerprint2.shared.data.repository.PressToAuthProvider

class PressToAuthProviderImpl(val context: Context) : PressToAuthProvider {
  override val isEnabled: Boolean
    get() {
      var toReturn: Int =
        Settings.Secure.getIntForUser(
          context.contentResolver,
          Settings.Secure.SFPS_PERFORMANT_AUTH_ENABLED,
          -1,
          context.userId,
        )
      if (toReturn == -1) {
        toReturn =
          if (
            context.resources.getBoolean(com.android.internal.R.bool.config_performantAuthDefault)
          ) {
            1
          } else {
            0
          }
        Settings.Secure.putIntForUser(
          context.contentResolver,
          Settings.Secure.SFPS_PERFORMANT_AUTH_ENABLED,
          toReturn,
          context.userId
        )
      }
      return (toReturn == 1)
    }
}
