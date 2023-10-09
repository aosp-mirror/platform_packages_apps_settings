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
import android.provider.Telephony
import android.util.Log
import com.android.settings.R
import java.util.Locale

const val NAME_INDEX = 1
const val APN_INDEX = 2
const val PROXY_INDEX = 3
const val PORT_INDEX = 4
const val USER_INDEX = 5
const val SERVER_INDEX = 6
const val PASSWORD_INDEX = 7
const val MMSC_INDEX = 8
const val MCC_INDEX = 9
const val MNC_INDEX = 10
const val MMSPROXY_INDEX = 12
const val MMSPORT_INDEX = 13
const val AUTH_TYPE_INDEX = 14
const val TYPE_INDEX = 15
const val PROTOCOL_INDEX = 16
const val CARRIER_ENABLED_INDEX = 17
const val NETWORK_TYPE_INDEX = 18
const val ROAMING_PROTOCOL_INDEX = 19
const val MVNO_TYPE_INDEX = 20
const val MVNO_MATCH_DATA_INDEX = 21
const val EDITED_INDEX = 22
const val USER_EDITABLE_INDEX = 23
const val CARRIER_ID_INDEX = 24

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
    Telephony.Carriers.MCC,  // 9
    Telephony.Carriers.MNC,  // 10
    Telephony.Carriers.NUMERIC,  // 11
    Telephony.Carriers.MMSPROXY,  // 12
    Telephony.Carriers.MMSPORT,  // 13
    Telephony.Carriers.AUTH_TYPE,  // 14
    Telephony.Carriers.TYPE,  // 15
    Telephony.Carriers.PROTOCOL,  // 16
    Telephony.Carriers.CARRIER_ENABLED,  // 17
    Telephony.Carriers.NETWORK_TYPE_BITMASK, // 18
    Telephony.Carriers.ROAMING_PROTOCOL,  // 19
    Telephony.Carriers.MVNO_TYPE,  // 20
    Telephony.Carriers.MVNO_MATCH_DATA,  // 21
    Telephony.Carriers.EDITED_STATUS,  // 22
    Telephony.Carriers.USER_EDITABLE,  // 23
    Telephony.Carriers.CARRIER_ID // 24
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
    val mvnoTypeOptions = context.resources.getStringArray(R.array.mvno_type_entries).toList()

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
            val mcc = cursor.getString(MCC_INDEX)
            val mnc = cursor.getString(MNC_INDEX)
            val mmsProxy = cursor.getString(MMSPROXY_INDEX)
            val mmsPort = cursor.getString(MMSPORT_INDEX)
            val authType = cursor.getInt(AUTH_TYPE_INDEX)
            val apnType = cursor.getString(TYPE_INDEX)
            val apnProtocol = convertProtocol2Options(cursor.getString(PROTOCOL_INDEX), context)
            val apnRoaming =
                convertProtocol2Options(cursor.getString(ROAMING_PROTOCOL_INDEX), context)
            val apnEnable = cursor.getInt(CARRIER_ENABLED_INDEX) == 1
            val networkType = cursor.getLong(NETWORK_TYPE_INDEX)
            val mvnoType = cursor.getString(MVNO_TYPE_INDEX)
            val mvnoValue = cursor.getString(MVNO_MATCH_DATA_INDEX)

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
                mcc = mcc,
                mnc = mnc,
                authType = authType,
                apnType = apnType,
                apnProtocol = apnProtocolOptions.indexOf(apnProtocol),
                apnRoaming = apnProtocolOptions.indexOf(apnRoaming),
                apnEnable = apnEnable,
                networkType = networkType,
                mvnoType = mvnoTypeOptions.indexOf(mvnoType),
                mvnoValue = mvnoValue,
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
