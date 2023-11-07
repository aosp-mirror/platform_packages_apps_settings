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

package com.android.settings.spa.app.appcompat

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_ACTIVITIES
import android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_UNSET
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.applications.appcompat.UserAspectRatioManager
import com.android.settingslib.spa.framework.common.SettingsEntryBuilder
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.framework.common.createSettingsPage
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.framework.compose.rememberContext
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.util.asyncMap
import com.android.settingslib.spa.framework.util.filterItem
import com.android.settingslib.spa.widget.illustration.Illustration
import com.android.settingslib.spa.widget.illustration.IllustrationModel
import com.android.settingslib.spa.widget.illustration.ResourceType
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.ui.SettingsBody
import com.android.settingslib.spa.widget.ui.SpinnerOption
import com.android.settingslib.spaprivileged.model.app.AppListModel
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.model.app.userId
import com.android.settingslib.spaprivileged.template.app.AppList
import com.android.settingslib.spaprivileged.template.app.AppListInput
import com.android.settingslib.spaprivileged.template.app.AppListItem
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.spaprivileged.template.app.AppListPage
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

object UserAspectRatioAppsPageProvider : SettingsPageProvider {
    override val name = "UserAspectRatioAppsPage"
    private val owner = createSettingsPage()

    override fun isEnabled(arguments: Bundle?): Boolean =
        UserAspectRatioManager.isFeatureEnabled(SpaEnvironmentFactory.instance.appContext)

    @Composable
    override fun Page(arguments: Bundle?) =
        UserAspectRatioAppList()

    @Composable
    @VisibleForTesting
    fun EntryItem() {
        val summary = getSummary()
        Preference(object : PreferenceModel {
            override val title = stringResource(R.string.aspect_ratio_experimental_title)
            override val summary = { summary }
            override val onClick = navigator(name)
        })
    }

    @VisibleForTesting
    fun buildInjectEntry() = SettingsEntryBuilder
        .createInject(owner)
        .setSearchDataFn { null }
        .setUiLayoutFn { EntryItem() }

    @Composable
    @VisibleForTesting
    fun getSummary(): String = stringResource(R.string.aspect_ratio_summary_text, Build.MODEL)
}

@Composable
fun UserAspectRatioAppList(
    appList: @Composable AppListInput<UserAspectRatioAppListItemModel>.() -> Unit
    = { AppList() },
) {
    AppListPage(
        title = stringResource(R.string.aspect_ratio_experimental_title),
        listModel = rememberContext(::UserAspectRatioAppListModel),
        appList = appList,
        header = {
            Box(Modifier.padding(SettingsDimension.itemPadding)) {
                SettingsBody(stringResource(R.string.aspect_ratio_main_summary_text, Build.MODEL))
            }
            Illustration(object : IllustrationModel {
                override val resId = R.raw.user_aspect_ratio_education
                override val resourceType = ResourceType.LOTTIE
            })
        },
        noMoreOptions = true,
    )
}

data class UserAspectRatioAppListItemModel(
    override val app: ApplicationInfo,
    val userOverride: Int,
    val suggested: Boolean,
    val canDisplay: Boolean,
) : AppRecord

class UserAspectRatioAppListModel(private val context: Context)
    : AppListModel<UserAspectRatioAppListItemModel> {

    private val packageManager = context.packageManager
    private val userAspectRatioManager = UserAspectRatioManager(context)

    override fun getSpinnerOptions(
        recordList: List<UserAspectRatioAppListItemModel>
    ): List<SpinnerOption> {
        val hasSuggested = recordList.any { it.suggested }
        val hasOverride = recordList.any { it.userOverride != USER_MIN_ASPECT_RATIO_UNSET }
        val options = mutableListOf(SpinnerItem.All)
        // Add suggested filter first as default
        if (hasSuggested) options.add(0, SpinnerItem.Suggested)
        if (hasOverride) options += SpinnerItem.Overridden
        return options.map {
            SpinnerOption(
                id = it.ordinal,
                text = context.getString(it.stringResId),
            )
        }
    }

    @Composable
    override fun AppListItemModel<UserAspectRatioAppListItemModel>.AppItem() {
        val app = record.app
        AppListItem(
            onClick = {
                navigateToAppAspectRatioSettings(
                    context,
                    app,
                    SettingsEnums.USER_ASPECT_RATIO_APP_LIST_SETTINGS
                )
            }
        )
    }

    override fun transform(userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>) =
        userIdFlow.combine(appListFlow) { uid, appList ->
            appList.asyncMap { app ->
                UserAspectRatioAppListItemModel(
                    app = app,
                    suggested = !app.isSystemApp && getPackageAndActivityInfo(
                                    app)?.isFixedOrientationOrAspectRatio() == true,
                    userOverride = userAspectRatioManager.getUserMinAspectRatioValue(
                                    app.packageName, uid),
                    canDisplay = userAspectRatioManager.canDisplayAspectRatioUi(app),
                )
            }
        }

    override fun filter(
        userIdFlow: Flow<Int>,
        option: Int,
        recordListFlow: Flow<List<UserAspectRatioAppListItemModel>>
    ): Flow<List<UserAspectRatioAppListItemModel>> = recordListFlow.filterItem(
        when (SpinnerItem.entries.getOrNull(option)) {
            SpinnerItem.Suggested -> ({ it.canDisplay && it.suggested })
            SpinnerItem.Overridden -> ({ it.userOverride != USER_MIN_ASPECT_RATIO_UNSET })
            else -> ({ it.canDisplay })
        }
    )

    @Composable
    override fun getSummary(option: Int, record: UserAspectRatioAppListItemModel): () -> String {
        val summary by remember(record.userOverride) {
            flow {
                emit(userAspectRatioManager.getUserMinAspectRatioEntry(record.userOverride,
                    record.app.packageName))
            }.flowOn(Dispatchers.IO)
        }.collectAsStateWithLifecycle(initialValue = stringResource(R.string.summary_placeholder))
        return { summary }
    }

    private fun getPackageAndActivityInfo(app: ApplicationInfo): PackageInfo? = try {
        packageManager.getPackageInfoAsUser(app.packageName, GET_ACTIVITIES_FLAGS, app.userId)
    } catch (e: Exception) {
        // Query PackageManager.getPackageInfoAsUser() with GET_ACTIVITIES_FLAGS could cause
        // exception sometimes. Since we reply on this flag to retrieve the Picture In Picture
        // packages, we need to catch the exception to alleviate the impact before PackageManager
        // fixing this issue or provide a better api.
        Log.e(TAG, "Exception while getPackageInfoAsUser", e)
        null
    }

    companion object {
        private const val TAG = "AspectRatioAppsListModel"
        private fun PackageInfo.isFixedOrientationOrAspectRatio() =
            activities?.any { a -> a.isFixedOrientation || a.hasFixedAspectRatio() } ?: false
        private val GET_ACTIVITIES_FLAGS =
            PackageManager.PackageInfoFlags.of(GET_ACTIVITIES.toLong())
    }
}

private enum class SpinnerItem(val stringResId: Int) {
    Suggested(R.string.user_aspect_ratio_suggested_apps_label),
    All(R.string.filter_all_apps),
    Overridden(R.string.user_aspect_ratio_changed_apps_label)
}