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

package com.android.settings.spa.app.appinfo

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.Utils
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.framework.common.asUser
import com.android.settingslib.spaprivileged.model.app.userHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Composable
fun InstantAppDomainsPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    if (!app.isInstantApp) return

    val presenter = remember { InstantAppDomainsPresenter(context, app) }
    var openDialog by rememberSaveable { mutableStateOf(false) }

    val summary by presenter.summaryFlow.collectAsStateWithLifecycle(
        initialValue = stringResource(R.string.summary_placeholder),
    )
    Preference(object : PreferenceModel {
        override val title = stringResource(R.string.app_launch_supported_domain_urls_title)
        override val summary = { summary }
        override val onClick = { openDialog = true }
    })

    val domainsState = presenter.domainsFlow.collectAsStateWithLifecycle(initialValue = emptySet())
    if (openDialog) {
        Dialog(domainsState) {
            openDialog = false
        }
    }
}

@Composable
private fun Dialog(domainsState: State<Set<String>>, onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        title = {
            Text(stringResource(R.string.app_launch_supported_domain_urls_title))
        },
        text = {
            Column {
                domainsState.value.forEach { domain ->
                    Text(
                        text = domain,
                        modifier = Modifier.padding(vertical = SettingsDimension.itemPaddingAround),
                    )
                }
            }
        },
    )
}

private class InstantAppDomainsPresenter(
    private val context: Context,
    private val app: ApplicationInfo,
) {
    private val userContext = context.asUser(app.userHandle)
    private val userPackageManager = userContext.packageManager

    val domainsFlow = flow {
        emit(Utils.getHandledDomains(userPackageManager, app.packageName))
    }.flowOn(Dispatchers.IO)

    val summaryFlow = domainsFlow.map { entries ->
        when (entries.size) {
            0 -> context.getString(R.string.domain_urls_summary_none)
            1 -> context.getString(R.string.domain_urls_summary_one, entries.first())
            else -> context.getString(R.string.domain_urls_summary_some, entries.first())
        }
    }.flowOn(Dispatchers.IO)
}
