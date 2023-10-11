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
import java.util.Arrays
import java.util.Locale

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
    val authType: Int = -1,
    val apnType: String = "",
    val apnProtocol: Int = -1,
    val apnRoaming: Int = -1,
    val apnEnable: Boolean = true,
    val networkType: Long = 0,
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
    val authTypeEnabled: Boolean = true,
    val apnTypeEnabled: Boolean = true,
    val apnProtocolEnabled: Boolean = true,
    val apnRoamingEnabled: Boolean = true,
    val apnEnableEnabled: Boolean = true,
    val networkTypeEnabled: Boolean = true,
    val newApn: Boolean = false,
    val subId: Int = -1,
    val customizedConfig: CustomizedConfig = CustomizedConfig()
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

    if (!uriInit.isPathPrefixMatch(Telephony.Carriers.CONTENT_URI)) {
        Log.e(TAG, "Insert request not for carrier table. Uri: $uriInit")
        return ApnData() //TODO: finish
    }

    var apnDataInit = when (uriType) {
        EDIT_URL -> getApnDataFromUri(uriInit, context)
        INSERT_URL -> ApnData()
        else -> ApnData() //TODO: finish
    }

    if (uriType == INSERT_URL) {
        apnDataInit = apnDataInit.copy(newApn = true)
    }

    apnDataInit = apnDataInit.copy(subId = subId)
    val configManager =
        context.getSystemService(Context.CARRIER_CONFIG_SERVICE) as CarrierConfigManager
    apnDataInit =
        apnDataInit.copy(customizedConfig = getCarrierCustomizedConfig(apnDataInit, configManager))

    apnDataInit = apnDataInit.copy(
        apnEnableEnabled =
        context.resources.getBoolean(R.bool.config_allow_edit_carrier_enabled)
    )
    // TODO: mIsCarrierIdApn
    disableInit(apnDataInit)
    return apnDataInit
}

/**
 * Initialize CustomizedConfig information through subId.
 * @param subId subId information obtained from arguments.
 *
 * @return Initialized CustomizedConfig information.
 */
fun getCarrierCustomizedConfig(
    apnInit: ApnData,
    configManager: CarrierConfigManager
): CustomizedConfig {
    val b = configManager.getConfigForSubId(
        apnInit.subId,
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

fun disableInit(apnDataInit : ApnData): ApnData {
    var apnData = apnDataInit
    val isUserEdited = apnDataInit.edited == Telephony.Carriers.USER_EDITED
    Log.d(TAG, "disableInit: EDITED $isUserEdited")
    // if it's not a USER_EDITED apn, check if it's read-only
    if (!isUserEdited && (apnDataInit.userEditable == 0
            || apnTypesMatch(apnDataInit.customizedConfig.readOnlyApnTypes, apnDataInit.apnType))
    ) {
        Log.d(TAG, "disableInit: read-only APN")
        apnData = apnDataInit.copy(customizedConfig = apnDataInit.customizedConfig.copy(readOnlyApn = true))
        apnData = disableAllFields(apnData)
    } else if (!ArrayUtils.isEmpty(apnData.customizedConfig.readOnlyApnFields)) {
        Log.d(TAG, "disableInit: mReadOnlyApnFields ${apnData.customizedConfig.readOnlyApnFields.joinToString(", ")})")
        apnData = disableFields(apnData.customizedConfig.readOnlyApnFields, apnData)
    }
    return apnData
}

/**
 * Disables all fields so that user cannot modify the APN
 */
private fun disableAllFields(apnDataInit : ApnData): ApnData {
    var apnData = apnDataInit
    apnData = apnData.copy(nameEnabled = false)
    apnData = apnData.copy(apnEnabled = false)
    apnData = apnData.copy(proxyEnabled = false)
    apnData = apnData.copy(portEnabled = false)
    apnData = apnData.copy(userNameEnabled = false)
    apnData = apnData.copy(passWordEnabled = false)
    apnData = apnData.copy(serverEnabled = false)
    apnData = apnData.copy(mmscEnabled = false)
    apnData = apnData.copy(mmsProxyEnabled = false)
    apnData = apnData.copy(mmsPortEnabled = false)
    apnData = apnData.copy(authTypeEnabled = false)
    apnData = apnData.copy(apnTypeEnabled = false)
    apnData = apnData.copy(apnProtocolEnabled = false)
    apnData = apnData.copy(apnRoamingEnabled = false)
    apnData = apnData.copy(apnEnableEnabled = false)
    apnData = apnData.copy(networkTypeEnabled = false)
    return apnData
}

/**
 * Disables given fields so that user cannot modify them
 *
 * @param apnFields fields to be disabled
 */
private fun disableFields(apnFields: List<String>, apnDataInit : ApnData): ApnData {
    var apnData = apnDataInit
    for (apnField in apnFields) {
        apnData = disableByFieldName(apnField, apnDataInit)
    }
    return apnData
}

private fun disableByFieldName(apnField: String, apnDataInit : ApnData): ApnData {
    var apnData = apnDataInit
    when (apnField) {
        Telephony.Carriers.NAME -> apnData = apnData.copy(nameEnabled = false)
        Telephony.Carriers.APN -> apnData = apnData.copy(apnEnabled = false)
        Telephony.Carriers.PROXY -> apnData = apnData.copy(proxyEnabled = false)
        Telephony.Carriers.PORT -> apnData = apnData.copy(portEnabled = false)
        Telephony.Carriers.USER -> apnData = apnData.copy(userNameEnabled = false)
        Telephony.Carriers.SERVER -> apnData = apnData.copy(serverEnabled = false)
        Telephony.Carriers.PASSWORD -> apnData = apnData.copy(passWordEnabled = false)
        Telephony.Carriers.MMSPROXY -> apnData = apnData.copy(mmsProxyEnabled = false)
        Telephony.Carriers.MMSPORT -> apnData = apnData.copy(mmsPortEnabled = false)
        Telephony.Carriers.MMSC -> apnData = apnData.copy(mmscEnabled = false)
        Telephony.Carriers.TYPE -> apnData = apnData.copy(apnTypeEnabled = false)
        Telephony.Carriers.AUTH_TYPE -> apnData = apnData.copy(authTypeEnabled = false)
        Telephony.Carriers.PROTOCOL -> apnData = apnData.copy(apnProtocolEnabled = false)
        Telephony.Carriers.ROAMING_PROTOCOL -> apnData = apnData.copy(apnRoamingEnabled = false)
        Telephony.Carriers.CARRIER_ENABLED -> apnData = apnData.copy(apnEnableEnabled = false)
        Telephony.Carriers.BEARER, Telephony.Carriers.BEARER_BITMASK -> apnData = apnData.copy(networkTypeEnabled =
            false)
    }
    return apnData
}

private fun apnTypesMatch(apnTypesArray: List<String>, apnTypesCur: String?): Boolean {
    if (ArrayUtils.isEmpty(apnTypesArray)) {
        return false
    }
    val apnTypesArrayLowerCase = arrayOfNulls<String>(
        apnTypesArray.size
    )
    for (i in apnTypesArray.indices) {
        apnTypesArrayLowerCase[i] = apnTypesArray[i].lowercase(Locale.getDefault())
    }
    if (hasAllApns(apnTypesArrayLowerCase) || TextUtils.isEmpty(apnTypesCur)) {
        return true
    }
    val apnTypesList: List<*> = listOf(*apnTypesArrayLowerCase)
    val apnTypesArrayCur =
        apnTypesCur!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    for (apn in apnTypesArrayCur) {
        if (apnTypesList.contains(apn.trim { it <= ' ' }.lowercase(Locale.getDefault()))) {
            Log.d(TAG, "apnTypesMatch: true because match found for " + apn.trim { it <= ' ' })
            return true
        }
    }
    Log.d(TAG, "apnTypesMatch: false")
    return false
}

fun hasAllApns(apnTypes: Array<String?>): Boolean {
    if (ArrayUtils.isEmpty(apnTypes)) {
        return false
    }
    val apnList: List<*> = Arrays.asList(*apnTypes)
    if (apnList.contains(ApnEditor.APN_TYPE_ALL)) {
        Log.d(TAG, "hasAllApns: true because apnList.contains(APN_TYPE_ALL)")
        return true
    }
    for (apn in ApnEditor.APN_TYPES) {
        if (!apnList.contains(apn)) {
            return false
        }
    }
    Log.d(TAG, "hasAllApns: true")
    return true
}