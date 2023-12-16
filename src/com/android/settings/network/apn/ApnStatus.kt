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
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.android.internal.util.ArrayUtils
import com.android.settings.R
import com.android.settings.network.apn.ApnNetworkTypes.getNetworkType
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
    val validEnabled: Boolean = false,
    val customizedConfig: CustomizedConfig = CustomizedConfig()
) {
    fun getContentValues(context: Context): ContentValues {
        val values = ContentValues()
        values.put(Telephony.Carriers.NAME, name)
        values.put(Telephony.Carriers.APN, apn)
        values.put(Telephony.Carriers.PROXY, proxy)
        values.put(Telephony.Carriers.PORT, port)
        values.put(Telephony.Carriers.MMSPROXY, mmsProxy)
        values.put(Telephony.Carriers.MMSPORT, mmsPort)
        values.put(Telephony.Carriers.USER, userName)
        values.put(Telephony.Carriers.SERVER, server)
        values.put(Telephony.Carriers.PASSWORD, passWord)
        values.put(Telephony.Carriers.MMSC, mmsc)
        values.put(Telephony.Carriers.AUTH_TYPE, authType)
        values.put(Telephony.Carriers.PROTOCOL, convertOptions2Protocol(apnProtocol, context))
        values.put(
            Telephony.Carriers.ROAMING_PROTOCOL,
            convertOptions2Protocol(apnRoaming, context)
        )
        values.put(Telephony.Carriers.TYPE, apnType)
        values.put(Telephony.Carriers.NETWORK_TYPE_BITMASK, networkType)
        values.put(Telephony.Carriers.CARRIER_ENABLED, apnEnable)
        values.put(Telephony.Carriers.EDITED_STATUS, Telephony.Carriers.USER_EDITED)
        if (newApn) {
            val simCarrierId =
                context.getSystemService(TelephonyManager::class.java)!!
                    .createForSubscriptionId(subId)
                    .getSimCarrierId()
            values.put(Telephony.Carriers.CARRIER_ID, simCarrierId)
        }
        return values
    }
}

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
 * APN types for data connections.  These are usage categories for an APN
 * entry.  One APN entry may support multiple APN types, eg, a single APN
 * may service regular internet traffic ("default") as well as MMS-specific
 * connections.<br></br>
 * APN_TYPE_ALL is a special type to indicate that this APN entry can
 * service all data connections.
 */
const val APN_TYPE_ALL = "*"

/** APN type for default data traffic  */
const val APN_TYPE_DEFAULT = "default"

/** APN type for MMS traffic  */
const val APN_TYPE_MMS = "mms"

/** APN type for SUPL assisted GPS  */
const val APN_TYPE_SUPL = "supl"

/** APN type for DUN traffic  */
const val APN_TYPE_DUN = "dun"

/** APN type for HiPri traffic  */
const val APN_TYPE_HIPRI = "hipri"

/** APN type for FOTA  */
const val APN_TYPE_FOTA = "fota"

/** APN type for IMS  */
const val APN_TYPE_IMS = "ims"

/** APN type for CBS  */
const val APN_TYPE_CBS = "cbs"

/** APN type for IA Initial Attach APN  */
const val APN_TYPE_IA = "ia"

/** APN type for Emergency PDN. This is not an IA apn, but is used
 * for access to carrier services in an emergency call situation.  */
const val APN_TYPE_EMERGENCY = "emergency"

/** APN type for Mission Critical Services  */
const val APN_TYPE_MCX = "mcx"

/** APN type for XCAP  */
const val APN_TYPE_XCAP = "xcap"
val APN_TYPES = arrayOf(
    APN_TYPE_DEFAULT,
    APN_TYPE_MMS,
    APN_TYPE_SUPL,
    APN_TYPE_DUN,
    APN_TYPE_HIPRI,
    APN_TYPE_FOTA,
    APN_TYPE_IMS,
    APN_TYPE_CBS,
    APN_TYPE_IA,
    APN_TYPE_EMERGENCY,
    APN_TYPE_MCX,
    APN_TYPE_XCAP
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
 * Validates the apn data and save it to the database if it's valid.
 * A dialog with error message will be displayed if the APN data is invalid.
 *
 * @return true if there is no error
 */
fun validateAndSaveApnData(
    apnDataInit: ApnData,
    apnData: ApnData,
    context: Context,
    uriInit: Uri,
    networkTypeSelectedOptionsState: SnapshotStateList<Int>
): Boolean {
    // Nothing to do if it's a read only APN
    if (apnData.customizedConfig.readOnlyApn) {
        return true
    }
    val errorMsg = validateApnData(apnData, context)
    if (errorMsg != null) {
        return false
    }
    val newApnData = apnData.copy(networkType = getNetworkType(networkTypeSelectedOptionsState))
    if (newApnData.newApn || (newApnData != apnDataInit)) {
        Log.d(TAG, "[validateAndSaveApnData] newApnData.networkType: ${newApnData.networkType}")
        updateApnDataToDatabase(
            newApnData.newApn,
            newApnData.getContentValues(context),
            context,
            uriInit
        )
    }
    return true
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
        validateMMSC(apnData.validEnabled, apnData.mmsc, context)
    }
    if (errorMsg == null) {
        errorMsg = validateAPNType(
            apnData.validEnabled,
            apnData.apnType,
            apnData.customizedConfig.readOnlyApnTypes,
            context
        )
    }
    return errorMsg
}

private fun getUserEnteredApnType(apnType: String, readOnlyApnTypes: List<String>): String {
    // if user has not specified a type, map it to "ALL APN TYPES THAT ARE NOT READ-ONLY"
    // but if user enter empty type, map it just for default
    var userEnteredApnType = apnType
    if (userEnteredApnType != "") userEnteredApnType =
        userEnteredApnType.trim { it <= ' ' }
    if (TextUtils.isEmpty(userEnteredApnType) || APN_TYPE_ALL == userEnteredApnType) {
        userEnteredApnType = getEditableApnType(readOnlyApnTypes)
    }
    Log.d(
        TAG, "getUserEnteredApnType: changed apn type to editable apn types: "
            + userEnteredApnType
    )
    return userEnteredApnType
}

private fun getEditableApnType(readOnlyApnTypes: List<String>): String {
    val editableApnTypes = StringBuilder()
    var first = true
    for (apnType in APN_TYPES) {
        // add APN type if it is not read-only and is not wild-cardable
        if (!readOnlyApnTypes.contains(apnType)
            && apnType != APN_TYPE_IA
            && apnType != APN_TYPE_EMERGENCY
            && apnType != APN_TYPE_MCX
            && apnType != APN_TYPE_IMS
        ) {
            if (first) {
                first = false
            } else {
                editableApnTypes.append(",")
            }
            editableApnTypes.append(apnType)
        }
    }
    return editableApnTypes.toString()
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

fun disableInit(apnDataInit: ApnData): ApnData {
    var apnData = apnDataInit
    val isUserEdited = apnDataInit.edited == Telephony.Carriers.USER_EDITED
    Log.d(TAG, "disableInit: EDITED $isUserEdited")
    // if it's not a USER_EDITED apn, check if it's read-only
    if (!isUserEdited && (apnDataInit.userEditable == 0
            || apnTypesMatch(apnDataInit.customizedConfig.readOnlyApnTypes, apnDataInit.apnType))
    ) {
        Log.d(TAG, "disableInit: read-only APN")
        apnData =
            apnDataInit.copy(customizedConfig = apnDataInit.customizedConfig.copy(readOnlyApn = true))
        apnData = disableAllFields(apnData)
    } else if (!ArrayUtils.isEmpty(apnData.customizedConfig.readOnlyApnFields)) {
        Log.d(
            TAG,
            "disableInit: mReadOnlyApnFields ${
                apnData.customizedConfig.readOnlyApnFields.joinToString(", ")
            })"
        )
        apnData = disableFields(apnData.customizedConfig.readOnlyApnFields, apnData)
    }
    return apnData
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

private fun apnTypesMatch(apnTypeList: List<String>, apnType: String): Boolean {
    val normalizeApnTypeList = apnTypeList.map(::normalizeApnType)
    return hasAllApns(normalizeApnTypeList) ||
        apnType.split(",").map(::normalizeApnType).all { it in normalizeApnTypeList }
}

fun hasAllApns(apnTypes: List<String>): Boolean {
    if (APN_TYPE_ALL in apnTypes) {
        Log.d(TAG, "hasAllApns: true because apnTypes.contains(APN_TYPE_ALL)")
        return true
    }
    return APN_TYPES.all { it in apnTypes }
}

private fun normalizeApnType(apnType: String): String =
    apnType.trim().lowercase(Locale.getDefault())

fun updateApnType(apnData: ApnData): String {
    return if (apnData.apnType == "" && apnData.customizedConfig.defaultApnTypes.isNotEmpty())
        getEditableApnType(apnData)
    else
        apnData.apnType
}

private fun getEditableApnType(apnData: ApnData): String {
    val customizedConfig = apnData.customizedConfig
    return customizedConfig.defaultApnTypes.filterNot { apnType ->
        customizedConfig.readOnlyApnTypes.contains(apnType) || apnType in listOf(
            APN_TYPE_IA,
            APN_TYPE_EMERGENCY,
            APN_TYPE_MCX,
            APN_TYPE_IMS,
        )
    }.joinToString()
}

fun deleteApn(uri: Uri, context: Context) {
    val contentResolver = context.contentResolver
    contentResolver.delete(uri, null, null)
}

fun validateMMSC(validEnabled: Boolean, mmsc: String, context: Context): String? {
    return if (validEnabled && !mmsc.matches(Regex("^https?:\\/\\/.+")))
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

fun validateAPNType(
    validEnabled: Boolean,
    apnType: String,
    readOnlyApnTypes: List<String>,
    context: Context
): String? {
    // if carrier does not allow editing certain apn types, make sure type does not include those
    if (validEnabled && !ArrayUtils.isEmpty(readOnlyApnTypes)
        && apnTypesMatch(
            readOnlyApnTypes,
            getUserEnteredApnType(apnType, readOnlyApnTypes)
        )
    ) {
        return String.format(
            context.resources.getString(R.string.error_adding_apn_type),
            readOnlyApnTypes.joinToString(", ")
        )
    }
    return null
}