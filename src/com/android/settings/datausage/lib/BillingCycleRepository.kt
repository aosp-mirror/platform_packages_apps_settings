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

package com.android.settings.datausage.lib

import android.content.Context
import android.os.INetworkManagementService
import android.os.ServiceManager
import android.util.Log
import androidx.annotation.OpenForTesting
import com.android.settings.network.telephony.MobileDataRepository
import com.android.settingslib.spaprivileged.framework.common.userManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@OpenForTesting
open class BillingCycleRepository @JvmOverloads constructor(
    context: Context,
    private val networkService: INetworkManagementService =
        INetworkManagementService.Stub.asInterface(
            ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE)
        ),
    private val mobileDataRepository: MobileDataRepository = MobileDataRepository(context),
) {
    private val userManager = context.userManager

    fun isModifiableFlow(subId: Int): Flow<Boolean> =
        mobileDataRepository.isMobileDataEnabledFlow(subId).map { mobileDataEnabled ->
            mobileDataEnabled && isBandwidthControlEnabled() && userManager.isAdminUser
        }.conflate().flowOn(Dispatchers.Default)

    open fun isBandwidthControlEnabled(): Boolean = try {
        networkService.isBandwidthControlEnabled
    } catch (e: Exception) {
        Log.w(TAG, "problem talking with INetworkManagementService: ", e)
        false
    }

    companion object {
        private const val TAG = "BillingCycleRepository"
    }
}
