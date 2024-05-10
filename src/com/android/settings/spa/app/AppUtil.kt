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

package com.android.settings.spa.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.UserHandle

/**
 * Based on PackageManagerService design, and it looks like the suggested replacement in the
 * deprecate notes suggest that we use PackageInstaller.uninstall which does not guarantee a pop up
 * would open and depends on the calling application. Seems like further investigation is needed
 * before we can move over to the new API.
 */
@Suppress("DEPRECATION")
fun Context.startUninstallActivity(
    packageName: String,
    userHandle: UserHandle,
    forAllUsers: Boolean = false,
) {
    val packageUri = Uri.parse("package:$packageName")

    val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri).apply {
        putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, forAllUsers)
    }
    startActivityAsUser(intent, userHandle)
}