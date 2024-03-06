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
import android.provider.Telephony
import android.util.Log
import com.android.settings.R
import com.android.settingslib.utils.ThreadUtils
import java.util.Locale

const val NAME_INDEX = 1
const val APN_INDEX = 2
const val PROXY_INDEX = 3
const val PORT_INDEX = 4
const val USER_INDEX = 5
const val SERVER_INDEX = 6
const val PASSWORD_INDEX = 7
const val MMSC_INDEX = 8
const val MMSPROXY_INDEX = 9
const val MMSPORT_INDEX = 10
const val AUTH_TYPE_INDEX = 11
const val TYPE_INDEX = 12
const val PROTOCOL_INDEX = 13
const val CARRIER_ENABLED_INDEX = 14
const val NETWORK_TYPE_INDEX = 15
const val ROAMING_PROTOCOL_INDEX = 16
const val EDITED_INDEX = 17
const val USER_EDITABLE_INDEX = 18
const val CARRIER_ID_INDEX = 19

val sProjection = arrayOf(
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
    Telephony.Carriers.CARRIER_ID // 19
)

const val TAG = "ApnRepository"

/**
 * Query apn related information based on uri.
 * @param uri URI data used for query.
 *
 * @return Stored apn related information.
 */
fun getApnDataFromUri(uri: Uri, context: Context): ApnData {
    var apnData = ApnData()
    val contentResolver = context.contentResolver
    val apnProtocolOptions = context.resources.getStringArray(R.array.apn_protocol_entries).toList()

    contentResolver.query(
        uri,
        sProjection,
        null /* selection */,
        null /* selectionArgs */,
        null /* sortOrder */
    ).use { cursor ->
        if (cursor != null && cursor.moveToFirst()) {
            val name = cursor.getString(NAME_INDEX)
            val apn = cursor.getString(APN_INDEX)
            val proxy = cursor.getString(PROXY_INDEX)
            val port = cursor.getString(PORT_INDEX)
            val userName = cursor.getString(USER_INDEX)
            val server = cursor.getString(SERVER_INDEX)
            val passWord = cursor.getString(PASSWORD_INDEX)
            val mmsc = cursor.getString(MMSC_INDEX)
            val mmsProxy = cursor.getString(MMSPROXY_INDEX)
            val mmsPort = cursor.getString(MMSPORT_INDEX)
            val authType = cursor.getInt(AUTH_TYPE_INDEX)
            val apnType = cursor.getString(TYPE_INDEX)
            val apnProtocol = convertProtocol2Options(cursor.getString(PROTOCOL_INDEX), context)
            val apnRoaming =
                convertProtocol2Options(cursor.getString(ROAMING_PROTOCOL_INDEX), context)
            val apnEnable = cursor.getInt(CARRIER_ENABLED_INDEX) == 1
            val networkType = cursor.getLong(NETWORK_TYPE_INDEX)

            val edited = cursor.getInt(EDITED_INDEX)
            val userEditable = cursor.getInt(USER_EDITABLE_INDEX)
            val carrierId = cursor.getInt(CARRIER_ID_INDEX)

            apnData = apnData.copy(
                name = name,
                apn = apn,
                proxy = proxy,
                port = port,
                userName = userName,
                passWord = passWord,
                server = server,
                mmsc = mmsc,
                mmsProxy = mmsProxy,
                mmsPort = mmsPort,
                authType = authType,
                apnType = apnType,
                apnProtocol = apnProtocolOptions.indexOf(apnProtocol),
                apnRoaming = apnProtocolOptions.indexOf(apnRoaming),
                apnEnable = apnEnable,
                networkType = networkType,
                edited = edited,
                userEditable = userEditable,
                carrierId = carrierId
            )
        }
    }
    if (apnData.name == "") {
        Log.d(TAG, "Can't get apnData from Uri $uri")
    }
    return apnData
}

/**
 * Returns The UI choice (e.g., "IPv4/IPv6") corresponding to the given
 * raw value of the protocol preference (e.g., "IPV4V6"). If unknown,
 * return null.
 *
 * @return UI choice
 */
private fun convertProtocol2Options(raw: String, context: Context): String {
    val apnProtocolOptions = context.resources.getStringArray(R.array.apn_protocol_entries).toList()
    val apnProtocolValues = context.resources.getStringArray(R.array.apn_protocol_values).toList()

    var uRaw = raw.uppercase(Locale.getDefault())
    uRaw = if (uRaw == "IPV4") "IP" else uRaw
    val protocolIndex = apnProtocolValues.indexOf(uRaw)
    return if (protocolIndex == -1) {
        ""
    } else {
        try {
            apnProtocolOptions[protocolIndex]
        } catch (e: ArrayIndexOutOfBoundsException) {
            ""
        }
    }
}

fun convertOptions2Protocol(protocolIndex: Int, context: Context): String {
    val apnProtocolValues = context.resources.getStringArray(R.array.apn_protocol_values).toList()

    return if (protocolIndex == -1) {
        ""
    } else {
        try {
            apnProtocolValues[protocolIndex]
        } catch (e: ArrayIndexOutOfBoundsException) {
            ""
        }
    }
}

fun updateApnDataToDatabase(newApn: Boolean, values: ContentValues, context: Context, uriInit: Uri) {
    ThreadUtils.postOnBackgroundThread {
        if (newApn) {
            // Add a new apn to the database
            val newUri = context.contentResolver.insert(uriInit, values)
            if (newUri == null) {
                Log.e(TAG, "Can't add a new apn to database $uriInit")
            }
        } else {
            // Update the existing apn
            context.contentResolver.update(
                uriInit, values, null /* where */, null /* selection Args */
            )
        }
    }
}