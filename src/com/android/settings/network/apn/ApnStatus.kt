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

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.telephony.CarrierConfigManager
import android.util.Log
import com.android.settings.R
import com.android.settings.network.apn.ApnTypes.getPreSelectedApnType

private const val TAG = "ApnStatus"

data class ApnData(
    val id: Int = -1,
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
    val validEnabled: Boolean = false,
    val customizedConfig: CustomizedConfig = CustomizedConfig()
) {
    fun getContentValueMap(context: Context): Map<String, Any> = mapOf(
        Telephony.Carriers.NAME to name,
        Telephony.Carriers.APN to apn,
        Telephony.Carriers.PROXY to proxy,
        Telephony.Carriers.PORT to port,
        Telephony.Carriers.USER to userName,
        Telephony.Carriers.SERVER to server,
        Telephony.Carriers.PASSWORD to passWord,
        Telephony.Carriers.MMSC to mmsc,
        Telephony.Carriers.MMSPROXY to mmsProxy,
        Telephony.Carriers.MMSPORT to mmsPort,
        Telephony.Carriers.AUTH_TYPE to authType,
        Telephony.Carriers.PROTOCOL to context.convertOptions2Protocol(apnProtocol),
        Telephony.Carriers.ROAMING_PROTOCOL to context.convertOptions2Protocol(apnRoaming),
        Telephony.Carriers.TYPE to apnType,
        Telephony.Carriers.NETWORK_TYPE_BITMASK to networkType,
        Telephony.Carriers.CARRIER_ENABLED to apnEnable,
        Telephony.Carriers.EDITED_STATUS to Telephony.Carriers.USER_EDITED,
    )

    fun getContentValues(context: Context) = ContentValues().apply {
        if (newApn) context.getApnIdMap(subId).forEach(::putObject)
        getContentValueMap(context).forEach(::putObject)
    }
}

data class CustomizedConfig(
    val readOnlyApn: Boolean = false,
    val isAddApnAllowed: Boolean = true,
    val readOnlyApnTypes: List<String> = emptyList(),
    val readOnlyApnFields: List<String> = emptyList(),
    val defaultApnTypes: List<String>? = null,
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
fun getApnDataInit(arguments: Bundle, context: Context, uriInit: Uri, subId: Int): ApnData? {
    val uriType = arguments.getString(URI_TYPE) ?: return null

    if (!uriInit.isPathPrefixMatch(Telephony.Carriers.CONTENT_URI)) {
        Log.e(TAG, "Insert request not for carrier table. Uri: $uriInit")
        return null
    }

    var apnDataInit = when (uriType) {
        EDIT_URL -> getApnDataFromUri(uriInit, context)
        INSERT_URL -> ApnData()
        else -> return null
    }

    if (uriType == INSERT_URL) {
        apnDataInit = apnDataInit.copy(newApn = true)
    }

    apnDataInit = apnDataInit.copy(subId = subId)
    val configManager =
        context.getSystemService(Context.CARRIER_CONFIG_SERVICE) as CarrierConfigManager
    apnDataInit =
        apnDataInit.copy(customizedConfig = getCarrierCustomizedConfig(apnDataInit, configManager))

    if (apnDataInit.newApn) {
        apnDataInit = apnDataInit.copy(
            apnType = getPreSelectedApnType(apnDataInit.customizedConfig)
        )
    }

    apnDataInit = apnDataInit.copy(
        apnEnableEnabled =
        context.resources.getBoolean(R.bool.config_allow_edit_carrier_enabled)
    )
    // TODO: mIsCarrierIdApn
    return disableInit(apnDataInit)
}

/**
 * Validates the apn data and save it to the database if it's valid.
 * A dialog with error message will be displayed if the APN data is invalid.
 *
 * @return true if there is no error
 */
fun validateAndSaveApnData(
    apnDataInit: ApnData,
    newApnData: ApnData,
    context: Context,
    uriInit: Uri
): String? {
    val errorMsg = validateApnData(newApnData, context)
    if (errorMsg != null) {
        return errorMsg
    }
    if (newApnData.newApn || (newApnData != apnDataInit)) {
        Log.d(TAG, "[validateAndSaveApnData] newApnData.networkType: ${newApnData.networkType}")
        updateApnDataToDatabase(
            newApnData.newApn,
            newApnData.getContentValues(context),
            context,
            uriInit
        )
    }
    return null
}

/**
 * Validates whether the apn data is valid.
 *
 * @return An error message if the apn data is invalid, otherwise return null.
 */
fun validateApnData(apnData: ApnData, context: Context): String? {
    var errorMsg: String?
    val name = apnData.name
    val apn = apnData.apn
    errorMsg = if (name == "") {
        context.resources.getString(R.string.error_name_empty)
    } else if (apn == "") {
        context.resources.getString(R.string.error_apn_empty)
    } else {
        validateMMSC(true, apnData.mmsc, context)
    }
    if (errorMsg == null) {
        errorMsg = isItemExist(apnData, context)
    }
    return errorMsg?.apply { Log.d(TAG, "APN data not valid, reason: $this") }
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
    fun log(message: String) {
        Log.d(TAG, "getCarrierCustomizedConfig: $message")
    }

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
        )?.toList() ?: emptyList(),
        readOnlyApnFields = b.getStringArray(
            CarrierConfigManager.KEY_READ_ONLY_APN_FIELDS_STRING_ARRAY
        )?.toList() ?: emptyList(),
        defaultApnTypes = b.getStringArray(
            CarrierConfigManager.KEY_APN_SETTINGS_DEFAULT_APN_TYPES_STRING_ARRAY
        )?.toList(),
        defaultApnProtocol = b.getString(
            CarrierConfigManager.Apn.KEY_SETTINGS_DEFAULT_PROTOCOL_STRING
        ) ?: "",
        defaultApnRoamingProtocol = b.getString(
            CarrierConfigManager.Apn.KEY_SETTINGS_DEFAULT_ROAMING_PROTOCOL_STRING
        ) ?: "",
        isAddApnAllowed = b.getBoolean(CarrierConfigManager.KEY_ALLOW_ADDING_APNS_BOOL),
    )
    if (customizedConfig.readOnlyApnTypes.isNotEmpty()) {
        log("read only APN type: " + customizedConfig.readOnlyApnTypes)
    }
    customizedConfig.defaultApnTypes?.takeIf { it.isNotEmpty() }?.let {
        log("default apn types: $it")
    }
    if (customizedConfig.defaultApnProtocol.isNotEmpty()) {
        log("default apn protocol: ${customizedConfig.defaultApnProtocol}")
    }
    if (customizedConfig.defaultApnRoamingProtocol.isNotEmpty()) {
        log("default apn roaming protocol: ${customizedConfig.defaultApnRoamingProtocol}")
    }
    if (!customizedConfig.isAddApnAllowed) {
        log("not allow to add new APN")
    }
    return customizedConfig
}

private fun ApnData.isReadOnly(): Boolean {
    Log.d(TAG, "isReadOnly: edited $edited")
    if (edited == Telephony.Carriers.USER_EDITED) return false
    // if it's not a USER_EDITED apn, check if it's read-only
    return userEditable == 0 ||
        ApnTypes.isApnTypeReadOnly(apnType, customizedConfig.readOnlyApnTypes)
}

fun disableInit(apnDataInit: ApnData): ApnData {
    if (apnDataInit.isReadOnly()) {
        Log.d(TAG, "disableInit: read-only APN")
        val apnData = apnDataInit.copy(
            customizedConfig = apnDataInit.customizedConfig.copy(readOnlyApn = true)
        )
        return disableAllFields(apnData)
    }
    val readOnlyApnFields = apnDataInit.customizedConfig.readOnlyApnFields
    if (readOnlyApnFields.isNotEmpty()) {
        Log.d(TAG, "disableInit: readOnlyApnFields $readOnlyApnFields)")
        return disableFields(readOnlyApnFields, apnDataInit)
    }
    return apnDataInit
}

/**
 * Disables all fields so that user cannot modify the APN
 */
private fun disableAllFields(apnDataInit: ApnData): ApnData {
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
private fun disableFields(apnFields: List<String>, apnDataInit: ApnData): ApnData {
    var apnData = apnDataInit
    for (apnField in apnFields) {
        apnData = disableByFieldName(apnField, apnDataInit)
    }
    return apnData
}

private fun disableByFieldName(apnField: String, apnDataInit: ApnData): ApnData {
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
        Telephony.Carriers.BEARER, Telephony.Carriers.BEARER_BITMASK,
        Telephony.Carriers.NETWORK_TYPE_BITMASK -> apnData = apnData.copy(
            networkTypeEnabled =
            false
        )
    }
    return apnData
}

fun deleteApn(uri: Uri, context: Context) {
    val contentResolver = context.contentResolver
    contentResolver.delete(uri, null, null)
}

fun validateMMSC(validEnabled: Boolean, mmsc: String, context: Context): String? {
    return if (validEnabled && mmsc != "" && !mmsc.matches(Regex("^https?:\\/\\/.+")))
        context.resources.getString(R.string.error_mmsc_valid)
    else null
}

fun validateName(validEnabled: Boolean, name: String, context: Context): String? {
    return if (validEnabled && (name == "")) context.resources.getString(R.string.error_name_empty)
    else null
}

fun validateAPN(validEnabled: Boolean, apn: String, context: Context): String? {
    return if (validEnabled && (apn == "")) context.resources.getString(R.string.error_apn_empty)
    else null
}
