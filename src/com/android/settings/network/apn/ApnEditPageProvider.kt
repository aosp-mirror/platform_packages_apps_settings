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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.android.settings.R
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.stateOf
import com.android.settingslib.spa.widget.editor.SettingsExposedDropdownMenuBox
import com.android.settingslib.spa.widget.editor.SettingsExposedDropdownMenuCheckBox
import com.android.settingslib.spa.widget.editor.SettingsOutlinedTextField
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import java.util.Base64

const val URI_TYPE = "uriType"
const val URI = "uri"
const val SUB_ID = "subId"
const val MVNO_TYPE = "mvnoType"
const val MVNO_MATCH_DATA = "mvnoMatchData"
const val EDIT_URL = "editUrl"

object ApnEditPageProvider : SettingsPageProvider {

    override val name = "Apn"
    const val TAG = "ApnPageProvider"

    override val parameter = listOf(
        navArgument(URI_TYPE) { type = NavType.StringType },
        navArgument(URI) { type = NavType.StringType },
        navArgument(SUB_ID) { type = NavType.IntType },
        navArgument(MVNO_TYPE) { type = NavType.StringType },
        navArgument(MVNO_MATCH_DATA) { type = NavType.StringType },
    )

    @Composable
    override fun Page(arguments: Bundle?) {
        val apnDataInit = ApnData()
        val apnDataCur = remember {
            mutableStateOf(apnDataInit)
        }
        ApnPage(apnDataCur)
    }

    fun getRoute(
        uriType: String,
        uri: Uri,
        subId: Int,
        mMvnoType: String,
        mMvnoMatchData: String
    ): String = "${name}/$uriType/${
        Base64.getUrlEncoder().encodeToString(uri.toString().toByteArray())
    }/$subId/$mMvnoType/$mMvnoMatchData"
}

@Composable
fun ApnPage(apnDataCur: MutableState<ApnData>) {
    var apnData by apnDataCur
    val context = LocalContext.current
    val authTypeOptions = stringArrayResource(R.array.apn_auth_entries).toList()
    val apnProtocolOptions = stringArrayResource(R.array.apn_protocol_entries).toList()
    val bearerOptionsAll = stringArrayResource(R.array.bearer_entries)
    val bearerOptions = bearerOptionsAll.drop(1).toList()
    val bearerEmptyVal = bearerOptionsAll[0]
    val mvnoTypeOptions = stringArrayResource(R.array.mvno_type_entries).toList()
    val bearerSelectedOptionsState = remember {
        getBearerSelectedOptionsState(apnData.bearer, apnData.bearerBitmask, context)
    }
    RegularScaffold(
        title = stringResource(id = R.string.apn_edit),
    ) {
        Column() {
            SettingsOutlinedTextField(
                apnData.name,
                stringResource(R.string.apn_name),
                enabled = apnData.nameEnabled
            ) { apnData = apnData.copy(name = it) }
            SettingsOutlinedTextField(
                apnData.apn,
                stringResource(R.string.apn_apn),
                enabled = apnData.apnEnabled
            ) { apnData = apnData.copy(apn = it) }
            SettingsOutlinedTextField(
                apnData.proxy,
                stringResource(R.string.apn_http_proxy),
                enabled = apnData.proxyEnabled
            ) { apnData = apnData.copy(proxy = it) }
            SettingsOutlinedTextField(
                apnData.port,
                stringResource(R.string.apn_http_port),
                enabled = apnData.portEnabled
            ) { apnData = apnData.copy(port = it) }
            SettingsOutlinedTextField(
                apnData.userName,
                stringResource(R.string.apn_user),
                enabled = apnData.userNameEnabled
            ) { apnData = apnData.copy(userName = it) }
            // TODO: password
            SettingsOutlinedTextField(
                apnData.server,
                stringResource(R.string.apn_server),
                enabled = apnData.serverEnabled
            ) { apnData = apnData.copy(server = it) }
            SettingsOutlinedTextField(
                apnData.mmsc,
                stringResource(R.string.apn_mmsc),
                enabled = apnData.mmscEnabled
            ) { apnData = apnData.copy(mmsc = it) }
            SettingsOutlinedTextField(
                apnData.mmsProxy,
                stringResource(R.string.apn_mms_proxy),
                enabled = apnData.mmsProxyEnabled
            ) { apnData = apnData.copy(mmsProxy = it) }
            SettingsOutlinedTextField(
                apnData.mmsPort,
                stringResource(R.string.apn_mms_port),
                enabled = apnData.mmsPortEnabled
            ) { apnData = apnData.copy(mmsPort = it) }
            SettingsOutlinedTextField(
                apnData.mcc,
                stringResource(R.string.apn_mcc),
                enabled = apnData.mccEnabled
            ) { apnData = apnData.copy(mcc = it) }
            SettingsOutlinedTextField(
                apnData.mnc,
                stringResource(R.string.apn_mnc),
                enabled = apnData.mncEnabled
            ) { apnData = apnData.copy(mnc = it) }
            // Warning: apnProtocol, apnRoaming, mvnoType string2Int
            SettingsExposedDropdownMenuBox(
                stringResource(R.string.apn_auth_type),
                authTypeOptions,
                apnData.authType,
                apnData.authTypeEnabled,
            ) { apnData = apnData.copy(authType = it) }
            SettingsOutlinedTextField(
                apnData.apnType,
                stringResource(R.string.apn_type),
                enabled = apnData.apnTypeEnabled
            ) { apnData = apnData.copy(apn = it) } // TODO: updateApnType
            SettingsExposedDropdownMenuBox(
                stringResource(R.string.apn_protocol),
                apnProtocolOptions,
                apnData.apnProtocol,
                apnData.apnProtocolEnabled
            ) { apnData = apnData.copy(apnProtocol = it) }
            SettingsExposedDropdownMenuBox(
                stringResource(R.string.apn_roaming_protocol),
                apnProtocolOptions,
                apnData.apnRoaming,
                apnData.apnRoamingEnabled
            ) { apnData = apnData.copy(apnRoaming = it) }
            SwitchPreference(
                object : SwitchPreferenceModel {
                    override val title = context.resources.getString(R.string.carrier_enabled)
                    override val changeable =
                        stateOf(apnData.apnEnableEnabled)
                    override val checked =
                        stateOf(apnData.apnEnable)
                    override val onCheckedChange = { newChecked: Boolean ->
                        apnData = apnData.copy(apnEnable = newChecked)
                    }
                }
            )
            SettingsExposedDropdownMenuCheckBox(
                stringResource(R.string.bearer),
                bearerOptions,
                bearerSelectedOptionsState,
                bearerEmptyVal,
                apnData.bearerEnabled
            ) {}
            SettingsExposedDropdownMenuBox(
                stringResource(R.string.mvno_type),
                mvnoTypeOptions,
                apnData.mvnoType,
                apnData.mvnoTypeEnabled
            ) {
                apnData = apnData.copy(mvnoType = it)
            } // TODO: mvnoDescription
            SettingsOutlinedTextField(
                apnData.mvnoValue,
                stringResource(R.string.mvno_match_data),
                enabled = apnData.mvnoValueEnabled
            ) { apnData = apnData.copy(mvnoValue = it) }
        }
    }
}