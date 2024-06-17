/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network.telephony

import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import androidx.core.os.persistableBundleOf

/**
 * Gets the configuration values of the specified config keys applied.
 */
fun CarrierConfigManager.safeGetConfig(
    keys: List<String>,
    subId: Int = SubscriptionManager.getDefaultSubscriptionId(),
): PersistableBundle = try {
    getConfigForSubId(subId, *keys.toTypedArray())
} catch (e: IllegalStateException) {
    // The CarrierConfigLoader (the service implemented the CarrierConfigManager) hasn't been
    // initialized yet. This may occurs during very early phase of phone booting up or when Phone
    // process has been restarted.
    // Settings should not assume Carrier config loader (and any other system services as well) are
    // always available. If not available, use default value instead.
    persistableBundleOf()
}
