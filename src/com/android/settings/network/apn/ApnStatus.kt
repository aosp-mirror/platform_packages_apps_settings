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

import android.provider.Telephony
import android.telephony.TelephonyManager

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
    val bearer: Int = 0,
    val mvnoType: Int = -1,
    var mvnoValue: String = "",
    val bearerBitmask: Int = 0,
    val edited: Int = Telephony.Carriers.USER_EDITED,
    val userEditable: Int = 1,
    val carrierId: Int = TelephonyManager.UNKNOWN_CARRIER_ID
) {
    var nameEnabled = true
    var apnEnabled = true
    var proxyEnabled = true
    var portEnabled = true
    var userNameEnabled = true
    var passWordEnabled = true
    var serverEnabled = true
    var mmscEnabled = true
    var mmsProxyEnabled = true
    var mmsPortEnabled = true
    var mccEnabled = true
    var mncEnabled = true
    var authTypeEnabled = true
    var apnTypeEnabled = true
    var apnProtocolEnabled = true
    var apnRoamingEnabled = true
    var apnEnableEnabled = true
    var bearerEnabled = true
    var mvnoTypeEnabled = true
    var mvnoValueEnabled = false
}