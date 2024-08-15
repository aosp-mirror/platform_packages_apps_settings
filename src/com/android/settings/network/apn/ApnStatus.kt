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
    val carrierEnabled: Boolean = true,
    val networkType: Long = 0,
    val edited: Int = Telephony.Carriers.USER_EDITED,
    val userEditable: Int = 1,
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
        // Copy network type into lingering network type.
        Telephony.Carriers.LINGERING_NETWORK_TYPE_BITMASK to networkType,
        Telephony.Carriers.CARRIER_ENABLED to carrierEnabled,
        Telephony.Carriers.EDITED_STATUS to Telephony.Carriers.USER_EDITED,
    )

    fun getContentValues(context: Context) = ContentValues().apply {
        if (newApn) context.getApnIdMap(subId).forEach(::putObject)
        getContentValueMap(context).forEach(::putObject)
    }

    fun isFieldEnabled(vararg fieldName: String): Boolean =
        !customizedConfig.readOnlyApn &&
            fieldName.all { it !in customizedConfig.readOnlyApnFields }
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
    val errorMsg: String? = when {
        apnData.name.isEmpty() -> context.resources.getString(R.string.error_name_empty)
        apnData.apn.isEmpty() -> context.resources.getString(R.string.error_apn_empty)
        apnData.apnType.isEmpty() -> context.resources.getString(R.string.error_apn_type_empty)
        else -> validateMMSC(true, apnData.mmsc, context) ?: isItemExist(apnData, context)
    }
    return errorMsg?.also { Log.d(TAG, "APN data not valid, reason: $it") }
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
        return apnDataInit.copy(
            customizedConfig = apnDataInit.customizedConfig.copy(readOnlyApn = true)
        )
    }
    val readOnlyApnFields = apnDataInit.customizedConfig.readOnlyApnFields
    if (readOnlyApnFields.isNotEmpty()) {
        Log.d(TAG, "disableInit: readOnlyApnFields $readOnlyApnFields)")
    }
    return apnDataInit
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
