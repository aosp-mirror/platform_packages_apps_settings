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

import android.content.Context
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import java.util.concurrent.ConcurrentHashMap
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

class CarrierConfigRepository(private val context: Context) {

    private val carrierConfigManager: CarrierConfigManager? =
        context.getSystemService(CarrierConfigManager::class.java)

    private enum class KeyType {
        BOOLEAN,
        INT,
        STRING
    }

    interface CarrierConfigAccessor {
        fun getBoolean(key: String): Boolean

        fun getInt(key: String): Int

        fun getString(key: String): String?
    }

    private class Accessor(private val cache: ConfigCache) : CarrierConfigAccessor {
        private val keysToRetrieve = mutableMapOf<String, KeyType>()

        override fun getBoolean(key: String): Boolean {
            check(key.endsWith("_bool")) { "Boolean key should ends with _bool" }
            val value = cache[key]
            return if (value == null) {
                keysToRetrieve += key to KeyType.BOOLEAN
                DefaultConfig.getBoolean(key)
            } else {
                check(value is BooleanConfigValue) { "Boolean value type wrong" }
                value.value
            }
        }

        override fun getInt(key: String): Int {
            check(key.endsWith("_int")) { "Int key should ends with _int" }
            val value = cache[key]
            return if (value == null) {
                keysToRetrieve += key to KeyType.INT
                DefaultConfig.getInt(key)
            } else {
                check(value is IntConfigValue) { "Int value type wrong" }
                value.value
            }
        }

        override fun getString(key: String): String? {
            check(key.endsWith("_string")) { "String key should ends with _string" }
            val value = cache[key]
            return if (value == null) {
                keysToRetrieve += key to KeyType.STRING
                DefaultConfig.getString(key)
            } else {
                check(value is StringConfigValue) { "String value type wrong" }
                value.value
            }
        }

        fun getKeysToRetrieve(): Map<String, KeyType> = keysToRetrieve
    }

    /**
     * Gets the configuration values for the given [subId].
     *
     * Configuration values could be accessed in [block]. Note: [block] could be called multiple
     * times, so it should be pure function without side effort.
     */
    fun <T> transformConfig(subId: Int, block: CarrierConfigAccessor.() -> T): T {
        val perSubCache = getPerSubCache(subId)
        val accessor = Accessor(perSubCache)
        val result = accessor.block()
        val keysToRetrieve = accessor.getKeysToRetrieve()
        // If all keys found in the first pass, no need to collect again
        if (keysToRetrieve.isEmpty()) return result

        perSubCache.update(subId, keysToRetrieve)

        return accessor.block()
    }

    /** Gets the configuration boolean for the given [subId] and [key]. */
    fun getBoolean(subId: Int, key: String): Boolean = transformConfig(subId) { getBoolean(key) }

    /** Gets the configuration int for the given [subId] and [key]. */
    fun getInt(subId: Int, key: String): Int = transformConfig(subId) { getInt(key) }

    /** Gets the configuration string for the given [subId] and [key]. */
    fun getString(subId: Int, key: String): String? = transformConfig(subId) { getString(key) }

    private fun ConfigCache.update(subId: Int, keysToRetrieve: Map<String, KeyType>) {
        val config = safeGetConfig(subId, keysToRetrieve.keys) ?: return
        for ((key, type) in keysToRetrieve) {
            when (type) {
                KeyType.BOOLEAN -> this[key] = BooleanConfigValue(config.getBoolean(key))
                KeyType.INT -> this[key] = IntConfigValue(config.getInt(key))
                KeyType.STRING -> this[key] = StringConfigValue(config.getString(key))
            }
        }
    }

    /** Gets the configuration values of the specified config keys applied. */
    private fun safeGetConfig(subId: Int, keys: Collection<String>): PersistableBundle? {
        if (carrierConfigManager == null || !SubscriptionManager.isValidSubscriptionId(subId)) {
            return null
        }
        tryRegisterListener(context)
        return try {
            carrierConfigManager.getConfigForSubId(subId, *keys.toTypedArray())
        } catch (e: Exception) {
            Log.e(TAG, "safeGetConfig: exception", e)
            // The CarrierConfigLoader (the service implemented the CarrierConfigManager) hasn't
            // been initialized yet. This may occurs during very early phase of phone booting up
            // or when Phone process has been restarted.
            // Settings should not assume Carrier config loader (and any other system services
            // as well) are always available. If not available, use default value instead.
            null
        }
    }

    companion object {
        private const val TAG = "CarrierConfigRepository"

        private val DefaultConfig = CarrierConfigManager.getDefaultConfig()

        /** Cache of config values for each subscription. */
        private val Cache = ConcurrentHashMap<Int, ConfigCache>()

        private fun getPerSubCache(subId: Int) =
            Cache.computeIfAbsent(subId) { ConcurrentHashMap() }

        /** To make sure the registerCarrierConfigChangeListener is only called once. */
        private val ListenerRegistered = atomic(false)

        private fun tryRegisterListener(context: Context) {
            if (ListenerRegistered.compareAndSet(expect = false, update = true)) {
                val carrierConfigManager =
                    context.applicationContext.getSystemService(CarrierConfigManager::class.java)
                if (carrierConfigManager != null) {
                    carrierConfigManager.registerCarrierConfigChangeListener()
                } else {
                    ListenerRegistered.getAndSet(false)
                }
            }
        }

        private fun CarrierConfigManager.registerCarrierConfigChangeListener() {
            val executor = Dispatchers.Default.asExecutor()
            registerCarrierConfigChangeListener(executor) { _, subId, _, _ ->
                Log.d(TAG, "[$subId] onCarrierConfigChanged")
                Cache.remove(subId)
            }
        }

        @VisibleForTesting
        fun resetForTest() {
            Cache.clear()
            ListenerRegistered.getAndSet(false)
        }

        @VisibleForTesting
        fun setBooleanForTest(subId: Int, key: String, value: Boolean) {
            check(key.endsWith("_bool")) { "Boolean key should ends with _bool" }
            getPerSubCache(subId)[key] = BooleanConfigValue(value)
        }

        @VisibleForTesting
        fun setIntForTest(subId: Int, key: String, value: Int) {
            check(key.endsWith("_int")) { "Int key should ends with _int" }
            getPerSubCache(subId)[key] = IntConfigValue(value)
        }

        @VisibleForTesting
        fun setStringForTest(subId: Int, key: String, value: String?) {
            check(key.endsWith("_string")) { "String key should ends with _string" }
            getPerSubCache(subId)[key] = StringConfigValue(value)
        }
    }
}

private sealed interface ConfigValue

private data class BooleanConfigValue(val value: Boolean) : ConfigValue

private data class IntConfigValue(val value: Int) : ConfigValue

private data class StringConfigValue(val value: String?) : ConfigValue

private typealias ConfigCache = ConcurrentHashMap<String, ConfigValue>
