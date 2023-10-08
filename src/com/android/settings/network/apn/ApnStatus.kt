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

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.telephony.CarrierConfigManager
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import com.android.internal.util.ArrayUtils
import com.android.settings.R

data class ApnData(
    val name: String = "",
    val apn: String = "",
    val proxy: String = "",
    val port: String = "",
    val userName: String = "",
    val passWord: String = "",
    val server: String = "",
    val mmsc: String = "",
    val mmsProxy: String = "",
    val mmsPort: String = "",
    val mcc: String = "",
    val mnc: String = "",
    val authType: Int = -1,
    val apnType: String = "",
    val apnProtocol: Int = -1,
    val apnRoaming: Int = -1,
    val apnEnable: Boolean = true,
    val networkType: Long = 0,
    val mvnoType: Int = -1,
    var mvnoValue: String = "",
    val edited: Int = Telephony.Carriers.USER_EDITED,
    val userEditable: Int = 1,
    val carrierId: Int = TelephonyManager.UNKNOWN_CARRIER_ID,
    val nameEnabled: Boolean = true,
    val apnEnabled: Boolean = true,
    val proxyEnabled: Boolean = true,
    val portEnabled: Boolean = true,
    val userNameEnabled: Boolean = true,
    val passWordEnabled: Boolean = true,
    val serverEnabled: Boolean = true,
    val mmscEnabled: Boolean = true,
    val mmsProxyEnabled: Boolean = true,
    val mmsPortEnabled: Boolean = true,
    val mccEnabled: Boolean = true,
    val mncEnabled: Boolean = true,
    val authTypeEnabled: Boolean = true,
    val apnTypeEnabled: Boolean = true,
    val apnProtocolEnabled: Boolean = true,
    val apnRoamingEnabled: Boolean = true,
    val apnEnableEnabled: Boolean = true,
    val networkTypeEnabled: Boolean = true,
    val mvnoTypeEnabled: Boolean = true,
    val mvnoValueEnabled: Boolean = false,
    val newApn: Boolean = false,
)

data class CustomizedConfig(
    val newApn: Boolean = false,
    val readOnlyApn: Boolean = false,
    val isAddApnAllowed: Boolean = true,
    val readOnlyApnTypes: List<String> = emptyList(),
    val readOnlyApnFields: List<String> = emptyList(),
    val defaultApnTypes: List<String> = emptyList(),
    val defaultApnProtocol: String = "",
    val defaultApnRoamingProtocol: String = "",
)

/**
 * Initialize ApnData according to the arguments.
 * @param arguments The data passed in when the user calls PageProvider.
 * @param uriInit The decoded user incoming uri data in Page.
 * @param subId The subId obtained in arguments.
 *
 * @return Initialized CustomizedConfig information.
 */
fun getApnDataInit(arguments: Bundle, context: Context, uriInit: Uri, subId: Int): ApnData {

    val uriType = arguments.getString(URI_TYPE)!!
    val mvnoType = arguments.getString(MVNO_TYPE)
    val mvnoValue = arguments.getString(MVNO_MATCH_DATA)
    val mvnoTypeOptions = context.resources.getStringArray(R.array.mvno_type_entries).toList()

    val configManager =
        context.getSystemService(Context.CARRIER_CONFIG_SERVICE) as CarrierConfigManager
    getCarrierCustomizedConfig(configManager, subId)

    if (!uriInit.isPathPrefixMatch(Telephony.Carriers.CONTENT_URI)) {
        Log.e(TAG, "Insert request not for carrier table. Uri: $uriInit")
        return ApnData() //TODO: finish
    }

    var apnDataInit = when (uriType) {
        EDIT_URL -> getApnDataFromUri(uriInit, context)
        INSERT_URL -> ApnData(
            mvnoType = mvnoTypeOptions.indexOf(mvnoType!!),
            mvnoValue = mvnoValue!!
        )

        else -> ApnData() //TODO: finish
    }

    if (uriType == INSERT_URL) {
        apnDataInit = apnDataInit.copy(newApn = true)
    }

    // TODO: mvnoDescription
    apnDataInit = apnDataInit.copy(
        apnEnableEnabled =
        context.resources.getBoolean(R.bool.config_allow_edit_carrier_enabled)
    )
    // TODO: mIsCarrierIdApn & disableInit(apnDataInit)
    return apnDataInit
}

/**
 * Initialize CustomizedConfig information through subId.
 * @param subId subId information obtained from arguments.
 *
 * @return Initialized CustomizedConfig information.
 */
fun getCarrierCustomizedConfig(configManager: CarrierConfigManager, subId: Int): CustomizedConfig {
    val b = configManager.getConfigForSubId(
        subId,
        CarrierConfigManager.KEY_READ_ONLY_APN_TYPES_STRING_ARRAY,
        CarrierConfigManager.KEY_READ_ONLY_APN_FIELDS_STRING_ARRAY,
        CarrierConfigManager.KEY_APN_SETTINGS_DEFAULT_APN_TYPES_STRING_ARRAY,
        CarrierConfigManager.Apn.KEY_SETTINGS_DEFAULT_PROTOCOL_STRING,
        CarrierConfigManager.Apn.KEY_SETTINGS_DEFAULT_ROAMING_PROTOCOL_STRING,
        CarrierConfigManager.KEY_ALLOW_ADDING_APNS_BOOL
    )
    val customizedConfig = CustomizedConfig(
        readOnlyApnTypes = b.getStringArray(
            CarrierConfigManager.KEY_READ_ONLY_APN_TYPES_STRING_ARRAY
        )?.toList() ?: emptyList(), readOnlyApnFields = b.getStringArray(
            CarrierConfigManager.KEY_READ_ONLY_APN_FIELDS_STRING_ARRAY
        )?.toList() ?: emptyList(), defaultApnTypes = b.getStringArray(
            CarrierConfigManager.KEY_APN_SETTINGS_DEFAULT_APN_TYPES_STRING_ARRAY
        )?.toList() ?: emptyList(), defaultApnProtocol = b.getString(
            CarrierConfigManager.Apn.KEY_SETTINGS_DEFAULT_PROTOCOL_STRING
        ) ?: "", defaultApnRoamingProtocol = b.getString(
            CarrierConfigManager.Apn.KEY_SETTINGS_DEFAULT_ROAMING_PROTOCOL_STRING
        ) ?: "", isAddApnAllowed = b.getBoolean(CarrierConfigManager.KEY_ALLOW_ADDING_APNS_BOOL)
    )
    if (!ArrayUtils.isEmpty(customizedConfig.readOnlyApnTypes)) {
        Log.d(
            TAG,
            "getCarrierCustomizedConfig: read only APN type: " + customizedConfig.readOnlyApnTypes.joinToString(
                ", "
            )
        )
    }
    if (!ArrayUtils.isEmpty(customizedConfig.defaultApnTypes)) {
        Log.d(
            TAG,
            "getCarrierCustomizedConfig: default apn types: " + customizedConfig.defaultApnTypes.joinToString(
                ", "
            )
        )
    }
    if (!TextUtils.isEmpty(customizedConfig.defaultApnProtocol)) {
        Log.d(
            TAG,
            "getCarrierCustomizedConfig: default apn protocol: ${customizedConfig.defaultApnProtocol}"
        )
    }
    if (!TextUtils.isEmpty(customizedConfig.defaultApnRoamingProtocol)) {
        Log.d(
            TAG,
            "getCarrierCustomizedConfig: default apn roaming protocol: ${customizedConfig.defaultApnRoamingProtocol}"
        )
    }
    if (!customizedConfig.isAddApnAllowed) {
        Log.d(TAG, "getCarrierCustomizedConfig: not allow to add new APN")
    }
    return customizedConfig
}