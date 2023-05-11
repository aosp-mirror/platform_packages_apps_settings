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

package com.android.settings.notetask.shortcut

import android.app.Activity
import android.app.role.RoleManager
import android.app.role.RoleManager.ROLE_NOTES
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.PersistableBundle
import android.os.UserHandle
import androidx.activity.ComponentActivity
import androidx.core.content.getSystemService
import com.android.settings.R

/**
 * Activity responsible for create a shortcut for notes action. If the shortcut is enabled, a new
 * shortcut will appear in the widget picker. If the shortcut is selected, the Activity here will be
 * launched, creating a new shortcut for [CreateNoteTaskShortcutActivity], and will finish.
 *
 * IMPORTANT! The shortcut package name and class should be synchronized with SystemUI controller:
 * [com.android.systemui.notetask.NoteTaskController#SETTINGS_CREATE_NOTE_TASK_SHORTCUT_COMPONENT].
 *
 * Changing the package name or class is a breaking change.
 *
 * @see <a
 *   href="https://developer.android.com/develop/ui/views/launch/shortcuts/creating-shortcuts#custom-pinned">Creating
 *   a custom shortcut activity</a>
 */
internal class CreateNoteTaskShortcutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val roleManager = requireNotNull(getSystemService<RoleManager>())
        val shortcutManager = requireNotNull(getSystemService<ShortcutManager>())

        super.onCreate(savedInstanceState)

        val shortcutInfo = roleManager.createNoteShortcutInfoAsUser(context = this, user)
        val shortcutIntent = shortcutManager.createShortcutResultIntent(shortcutInfo)
        setResult(Activity.RESULT_OK, shortcutIntent)

        finish()
    }

    private companion object {

        private const val SHORTCUT_ID = "note_task_shortcut_id"
        private const val EXTRA_SHORTCUT_BADGE_OVERRIDE_PACKAGE =
                "extra_shortcut_badge_override_package"
        private const val ACTION_LAUNCH_NOTE_TASK = "com.android.systemui.action.LAUNCH_NOTE_TASK"

        private fun RoleManager.createNoteShortcutInfoAsUser(
                context: Context,
                user: UserHandle,
        ): ShortcutInfo? {
            val systemUiComponent = context.getSystemUiComponent() ?: return null

            val extras = PersistableBundle()
            getDefaultRoleHolderAsUser(ROLE_NOTES, user)?.let { packageName ->
                // Set custom app badge using the icon from ROLES_NOTES default app.
                extras.putString(EXTRA_SHORTCUT_BADGE_OVERRIDE_PACKAGE, packageName)
            }

            val icon = Icon.createWithResource(context, R.drawable.ic_note_task_shortcut_widget)

            val intent = Intent(ACTION_LAUNCH_NOTE_TASK).apply {
                setPackage(systemUiComponent.packageName)
            }

            // Creates a System UI context. That will let the ownership with SystemUI and allows it
            // to perform updates such as enabling or updating the badge override package.
            val systemUiContext = context.createPackageContext(
                    systemUiComponent.packageName,
                    /* flags */ 0,
            )

            return ShortcutInfo.Builder(systemUiContext, SHORTCUT_ID)
                    .setIntent(intent)
                    .setShortLabel(context.getString(R.string.note_task_shortcut_label))
                    .setLongLived(true)
                    .setIcon(icon)
                    .setExtras(extras)
                    .build()
        }

        private fun RoleManager.getDefaultRoleHolderAsUser(
                role: String,
                user: UserHandle,
        ): String? = getRoleHoldersAsUser(role, user).firstOrNull()

        private fun Context.getSystemUiComponent(): ComponentName? {
            val flattenName = getString(
                    com.android.internal.R.string.config_systemUIServiceComponent)
            check(flattenName.isNotEmpty()) {
                "No 'com.android.internal.R.string.config_systemUIServiceComponent' resource"
            }
            return try {
                ComponentName.unflattenFromString(flattenName)
            } catch (e: RuntimeException) {
                val message = "Invalid component name defined by 'com.android.internal.R.string." +
                        "config_systemUIServiceComponent' resource: $flattenName"
                throw IllegalStateException(message, e)
            }
        }
    }
}
