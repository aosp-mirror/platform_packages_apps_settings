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
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.android.settings.R
import com.android.settingslib.utils.ThreadUtils
import java.util.Locale

val Projection = arrayOf(
    Telephony.Carriers._ID,  // 0
    Telephony.Carriers.NAME,  // 1
    Telephony.Carriers.APN,  // 2
    Telephony.Carriers.PROXY,  // 3
    Telephony.Carriers.PORT,  // 4
    Telephony.Carriers.USER,  // 5
    Telephony.Carriers.SERVER,  // 6
    Telephony.Carriers.PASSWORD,  // 7
    Telephony.Carriers.MMSC,  // 8
    Telephony.Carriers.MMSPROXY,  // 9
    Telephony.Carriers.MMSPORT,  // 10
    Telephony.Carriers.AUTH_TYPE,  // 11
    Telephony.Carriers.TYPE,  // 12
    Telephony.Carriers.PROTOCOL,  // 13
    Telephony.Carriers.CARRIER_ENABLED,  // 14
    Telephony.Carriers.NETWORK_TYPE_BITMASK, // 15
    Telephony.Carriers.ROAMING_PROTOCOL,  // 16
    Telephony.Carriers.EDITED_STATUS,  // 17
    Telephony.Carriers.USER_EDITABLE,  // 18
)

private const val TAG = "ApnRepository"

/**
 * Query apn related information based on uri.
 * @param uri URI data used for query.
 *
 * @return Stored apn related information.
 */
fun getApnDataFromUri(uri: Uri, context: Context): ApnData {
    var apnData = ApnData()
    val contentResolver = context.contentResolver

    contentResolver.query(
        uri,
        Projection,
        null /* selection */,
        null /* selectionArgs */,
        null /* sortOrder */
    ).use { cursor ->
        if (cursor != null && cursor.moveToFirst()) {
            apnData = ApnData(
                id = cursor.getInt(Telephony.Carriers._ID),
                name = cursor.getString(Telephony.Carriers.NAME),
                apn = cursor.getString(Telephony.Carriers.APN),
                proxy = cursor.getString(Telephony.Carriers.PROXY),
                port = cursor.getString(Telephony.Carriers.PORT),
                userName = cursor.getString(Telephony.Carriers.USER),
                passWord = cursor.getString(Telephony.Carriers.PASSWORD),
                server = cursor.getString(Telephony.Carriers.SERVER),
                mmsc = cursor.getString(Telephony.Carriers.MMSC),
                mmsProxy = cursor.getString(Telephony.Carriers.MMSPROXY),
                mmsPort = cursor.getString(Telephony.Carriers.MMSPORT),
                authType = cursor.getInt(Telephony.Carriers.AUTH_TYPE),
                apnType = cursor.getString(Telephony.Carriers.TYPE),
                apnProtocol = context.convertProtocol2Options(
                    cursor.getString(Telephony.Carriers.PROTOCOL)
                ),
                apnRoaming = context.convertProtocol2Options(
                    cursor.getString(Telephony.Carriers.ROAMING_PROTOCOL)
                ),
                apnEnable = cursor.getInt(Telephony.Carriers.CARRIER_ENABLED) == 1,
                networkType = cursor.getLong(Telephony.Carriers.NETWORK_TYPE_BITMASK),
                edited = cursor.getInt(Telephony.Carriers.EDITED_STATUS),
                userEditable = cursor.getInt(Telephony.Carriers.USER_EDITABLE),
            )
        }
    }
    if (apnData.name == "") {
        Log.d(TAG, "Can't get apnData from Uri $uri")
    }
    return apnData
}

private fun Cursor.getString(columnName: String) = getString(getColumnIndexOrThrow(columnName))
private fun Cursor.getInt(columnName: String) = getInt(getColumnIndexOrThrow(columnName))
private fun Cursor.getLong(columnName: String) = getLong(getColumnIndexOrThrow(columnName))

/**
 * Returns The UI choice index corresponding to the given raw value of the protocol preference
 * (e.g., "IPV4V6").
 * If unknown, return -1.
 */
private fun Context.convertProtocol2Options(protocol: String): Int {
    var normalizedProtocol = protocol.uppercase(Locale.getDefault())
    if (normalizedProtocol == "IPV4") normalizedProtocol = "IP"
    return resources.getStringArray(R.array.apn_protocol_values).indexOf(normalizedProtocol)
}

fun Context.convertOptions2Protocol(protocolIndex: Int): String =
    resources.getStringArray(R.array.apn_protocol_values).getOrElse(protocolIndex) { "" }

fun updateApnDataToDatabase(
    newApn: Boolean,
    values: ContentValues,
    context: Context,
    uriInit: Uri
) {
    ThreadUtils.postOnBackgroundThread {
        if (newApn) {
            Log.d(TAG, "Adding an new APN to the database $uriInit $values")
            val newUri = context.contentResolver.insert(uriInit, values)
            if (newUri == null) {
                Log.e(TAG, "Can't add a new apn to database $uriInit")
            }
        } else {
            Log.d(TAG, "Updating an existing APN to the database $uriInit $values")
            context.contentResolver.update(
                uriInit, values, null /* where */, null /* selection Args */
            )
        }
    }
}

/** Not allowing add duplicated items, if the values of the following keys are all identical. */
private val NonDuplicatedKeys = setOf(
    Telephony.Carriers.APN,
    Telephony.Carriers.PROXY,
    Telephony.Carriers.PORT,
    Telephony.Carriers.MMSC,
    Telephony.Carriers.MMSPROXY,
    Telephony.Carriers.MMSPORT,
    Telephony.Carriers.PROTOCOL,
    Telephony.Carriers.ROAMING_PROTOCOL,
)

fun isItemExist(apnData: ApnData, context: Context): String? {
    val selectionMap = apnData.getContentValueMap(context).filterKeys { it in NonDuplicatedKeys }
        .mapKeys { "${it.key} = ?" }
        .toMutableMap()
    if (apnData.id != -1) selectionMap += "${Telephony.Carriers._ID} != ?" to apnData.id
    val list = selectionMap.entries.toList()
    val selection = list.joinToString(" AND ") { it.key }
    val selectionArgs: Array<String> = list.map { it.value.toString() }.toTypedArray()
    context.contentResolver.query(
        Uri.withAppendedPath(Telephony.Carriers.SIM_APN_URI, apnData.subId.toString()),
        /* projection = */ emptyArray(),
        selection,
        selectionArgs,
        /* sortOrder = */ null,
    )?.use { cursor ->
        if (cursor.count > 0) {
            return context.resources.getString(R.string.error_duplicate_apn_entry)
        }
    }
    return null
}

fun Context.getApnIdMap(subId: Int): Map<String, Any> {
    val subInfo = getSystemService(SubscriptionManager::class.java)!!
        .getActiveSubscriptionInfo(subId)
    val carrierId = subInfo.carrierId
    return if (carrierId != TelephonyManager.UNKNOWN_CARRIER_ID) {
        mapOf(Telephony.Carriers.CARRIER_ID to carrierId)
    } else {
        mapOf(Telephony.Carriers.NUMERIC to subInfo.mccString + subInfo.mncString)
    }.also { Log.d(TAG, "[$subId] New APN item with id: $it") }
}
