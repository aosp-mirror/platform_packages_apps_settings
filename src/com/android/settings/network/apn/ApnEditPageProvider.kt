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

package com.android.settings.network.apn

import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.android.settings.R
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.editor.SettingsDropdownBox
import com.android.settingslib.spa.widget.editor.SettingsOutlinedTextField
import com.android.settingslib.spa.widget.editor.SettingsTextFieldPassword
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.scaffold.MoreOptionsAction
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import java.util.Base64

const val URI_TYPE = "uriType"
const val URI = "uri"
const val SUB_ID = "subId"
const val EDIT_URL = "editUrl"
const val INSERT_URL = "insertUrl"

object ApnEditPageProvider : SettingsPageProvider {

    override val name = "ApnEdit"
    const val TAG = "ApnEditPageProvider"

    override val parameter = listOf(
        navArgument(URI_TYPE) { type = NavType.StringType },
        navArgument(URI) { type = NavType.StringType },
        navArgument(SUB_ID) { type = NavType.IntType },
    )

    @Composable
    override fun Page(arguments: Bundle?) {
        val uriString = arguments!!.getString(URI)
        val uriInit = Uri.parse(String(Base64.getDecoder().decode(uriString)))
        val subId = arguments.getInt(SUB_ID)
        val apnDataInit = getApnDataInit(arguments, LocalContext.current, uriInit, subId) ?: return
        val apnDataCur = remember {
            mutableStateOf(apnDataInit)
        }
        ApnPage(apnDataInit, apnDataCur, uriInit)
    }

    fun getRoute(
        uriType: String,
        uri: Uri,
        subId: Int
    ): String = "${name}/$uriType/${
        Base64.getUrlEncoder().encodeToString(uri.toString().toByteArray())
    }/$subId"
}

@Composable
fun ApnPage(apnDataInit: ApnData, apnDataCur: MutableState<ApnData>, uriInit: Uri) {
    var apnData by apnDataCur
    val context = LocalContext.current
    val authTypeOptions = stringArrayResource(R.array.apn_auth_entries).toList()
    val apnProtocolOptions = stringArrayResource(R.array.apn_protocol_entries).toList()
    var apnTypeMmsSelected by remember { mutableStateOf(false) }
    val navController = LocalNavController.current
    var valid: String?
    RegularScaffold(
        title = if (apnDataInit.newApn) stringResource(id = R.string.apn_add) else stringResource(id = R.string.apn_edit),
        actions = {
            if (!apnData.customizedConfig.readOnlyApn) {
                Button(onClick = {
                    valid = validateAndSaveApnData(
                        apnDataInit,
                        apnData,
                        context,
                        uriInit
                    )
                    if (valid == null) navController.navigateBack()
                    else if (!apnData.validEnabled) apnData = apnData.copy(validEnabled = true)
                }) { Text(text = stringResource(id = R.string.save)) }
            }
            if (!apnData.newApn && !apnData.customizedConfig.readOnlyApn
                && apnData.customizedConfig.isAddApnAllowed
            ) {
                MoreOptionsAction {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_delete)) },
                        onClick = {
                            deleteApn(uriInit, context)
                            navController.navigateBack()
                        })
                }
            }
        },
    ) {
        Column {
            if (apnData.validEnabled) {
                valid = validateApnData(apnData, context)
                valid?.let {
                    Text(
                        text = it,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SettingsDimension.menuFieldPadding),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            SettingsOutlinedTextField(
                value = apnData.name,
                label = stringResource(R.string.apn_name),
                enabled = apnData.nameEnabled,
                errorMessage = validateName(apnData.validEnabled, apnData.name, context)
            ) { apnData = apnData.copy(name = it) }
            SettingsOutlinedTextField(
                value = apnData.apn,
                label = stringResource(R.string.apn_apn),
                enabled = apnData.apnEnabled,
                errorMessage = validateAPN(apnData.validEnabled, apnData.apn, context)
            ) { apnData = apnData.copy(apn = it) }
            SettingsOutlinedTextField(
                value = apnData.proxy,
                label = stringResource(R.string.apn_http_proxy),
                enabled = apnData.proxyEnabled
            ) { apnData = apnData.copy(proxy = it) }
            SettingsOutlinedTextField(
                value = apnData.port,
                label = stringResource(R.string.apn_http_port),
                enabled = apnData.portEnabled
            ) { apnData = apnData.copy(port = it) }
            SettingsOutlinedTextField(
                value = apnData.userName,
                label = stringResource(R.string.apn_user),
                enabled = apnData.userNameEnabled
            ) { apnData = apnData.copy(userName = it) }
            SettingsTextFieldPassword(
                value = apnData.passWord,
                label = stringResource(R.string.apn_password),
                enabled = apnData.passWordEnabled
            ) { apnData = apnData.copy(passWord = it) }
            SettingsOutlinedTextField(
                value = apnData.server,
                label = stringResource(R.string.apn_server),
                enabled = apnData.serverEnabled
            ) { apnData = apnData.copy(server = it) }
            ApnTypeCheckBox(
                apnData = apnData,
                onTypeChanged = { apnData = apnData.copy(apnType = it) },
                onMmsSelectedChanged = { apnTypeMmsSelected = it },
            )
            if (apnTypeMmsSelected) {
                SettingsOutlinedTextField(
                    value = apnData.mmsc,
                    label = stringResource(R.string.apn_mmsc),
                    errorMessage = validateMMSC(apnData.validEnabled, apnData.mmsc, context),
                    enabled = apnData.mmscEnabled
                ) { apnData = apnData.copy(mmsc = it) }
                SettingsOutlinedTextField(
                    value = apnData.mmsProxy,
                    label = stringResource(R.string.apn_mms_proxy),
                    enabled = apnData.mmsProxyEnabled
                ) { apnData = apnData.copy(mmsProxy = it) }
                SettingsOutlinedTextField(
                    value = apnData.mmsPort,
                    label = stringResource(R.string.apn_mms_port),
                    enabled = apnData.mmsPortEnabled
                ) { apnData = apnData.copy(mmsPort = it) }
            }
            SettingsDropdownBox(
                label = stringResource(R.string.apn_auth_type),
                options = authTypeOptions,
                selectedOptionIndex = apnData.authType,
                enabled = apnData.authTypeEnabled,
            ) { apnData = apnData.copy(authType = it) }
            SettingsDropdownBox(
                label = stringResource(R.string.apn_protocol),
                options = apnProtocolOptions,
                selectedOptionIndex = apnData.apnProtocol,
                enabled = apnData.apnProtocolEnabled
            ) { apnData = apnData.copy(apnProtocol = it) }
            SettingsDropdownBox(
                label = stringResource(R.string.apn_roaming_protocol),
                options = apnProtocolOptions,
                selectedOptionIndex = apnData.apnRoaming,
                enabled = apnData.apnRoamingEnabled
            ) { apnData = apnData.copy(apnRoaming = it) }
            ApnNetworkTypeCheckBox(apnData) { apnData = apnData.copy(networkType = it) }
            SwitchPreference(
                object : SwitchPreferenceModel {
                    override val title = context.resources.getString(R.string.carrier_enabled)
                    override val changeable = { apnData.apnEnableEnabled }
                    override val checked = { apnData.apnEnable }
                    override val onCheckedChange = { newChecked: Boolean ->
                        apnData = apnData.copy(apnEnable = newChecked)
                    }
                }
            )
        }
    }
}