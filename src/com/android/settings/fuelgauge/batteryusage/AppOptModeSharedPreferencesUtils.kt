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

package com.android.settings.fuelgauge.batteryusage

import android.content.Context
import android.content.SharedPreferences
import android.util.ArrayMap
import android.util.Base64
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action
import com.android.settings.fuelgauge.BatteryOptimizeUtils
import com.android.settings.fuelgauge.BatteryUtils
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory

/** A util to store and update app optimization mode expiration event data. */
object AppOptModeSharedPreferencesUtils {
    private const val TAG: String = "AppOptModeSharedPreferencesUtils"
    private const val SHARED_PREFS_FILE: String = "app_optimization_mode_shared_prefs"

    @VisibleForTesting const val UNLIMITED_EXPIRE_TIME: Long = -1L

    private val appOptimizationModeLock = Any()
    private val defaultInstance = AppOptimizationModeEvent.getDefaultInstance()

    /** Returns all app optimization mode events for log. */
    @JvmStatic
    fun getAllEvents(context: Context): List<AppOptimizationModeEvent> =
        synchronized(appOptimizationModeLock) { getAppOptModeEventsMap(context).values.toList() }

    /** Removes all app optimization mode events. */
    @JvmStatic
    fun clearAll(context: Context) =
        synchronized(appOptimizationModeLock) {
            getSharedPreferences(context).edit().clear().apply()
        }

    /** Updates the app optimization mode event data. */
    @JvmStatic
    fun updateAppOptModeExpiration(
        context: Context,
        uids: List<Int>,
        packageNames: List<String>,
        optimizationModes: List<Int>,
        expirationTimes: LongArray,
    ) =
        // The internal fun with an additional lambda parameter is used to
        // 1) get true BatteryOptimizeUtils in production environment
        // 2) get fake BatteryOptimizeUtils for testing environment
        updateAppOptModeExpirationInternal(
            context,
            uids,
            packageNames,
            optimizationModes,
            expirationTimes,
        ) { uid: Int, packageName: String ->
            BatteryOptimizeUtils(context, uid, packageName)
        }

    /** Resets the app optimization mode event data since the query timestamp. */
    @JvmStatic
    fun resetExpiredAppOptModeBeforeTimestamp(context: Context, queryTimestampMs: Long) =
        synchronized(appOptimizationModeLock) {
            val forceExpireEnabled =
                featureFactory.powerUsageFeatureProvider.isForceExpireAppOptimizationModeEnabled
            val eventsMap = getAppOptModeEventsMap(context)
            val expirationUids = ArrayList<Int>(eventsMap.size)
            for ((uid, event) in eventsMap) {
                // Not reset the mode if forceExpireEnabled is false and not expired.
                if (!forceExpireEnabled && event.expirationTime > queryTimestampMs) {
                    continue
                }
                updateBatteryOptimizationMode(
                    context,
                    event.uid,
                    event.packageName,
                    event.resetOptimizationMode,
                    Action.EXPIRATION_RESET,
                )
                expirationUids.add(uid)
            }
            // Remove the expired AppOptimizationModeEvent data from storage
            clearSharedPreferences(context, expirationUids)
        }

    /** Deletes all app optimization mode event data with a specific uid. */
    @JvmStatic
    fun deleteAppOptimizationModeEventByUid(context: Context, uid: Int) =
        synchronized(appOptimizationModeLock) { clearSharedPreferences(context, listOf(uid)) }

    @VisibleForTesting
    fun updateAppOptModeExpirationInternal(
        context: Context,
        uids: List<Int>,
        packageNames: List<String>,
        optimizationModes: List<Int>,
        expirationTimes: LongArray,
        getBatteryOptimizeUtils: (Int, String) -> BatteryOptimizeUtils,
    ) =
        synchronized(appOptimizationModeLock) {
            val restrictedModeOverwriteEnabled =
                featureFactory.powerUsageFeatureProvider.isRestrictedModeOverwriteEnabled
            val eventsMap = getAppOptModeEventsMap(context)
            val expirationEvents: MutableMap<Int, AppOptimizationModeEvent> = ArrayMap()
            for (i in uids.indices) {
                val uid = uids[i]
                val packageName = packageNames[i]
                val optimizationMode = optimizationModes[i]
                if (
                    !restrictedModeOverwriteEnabled &&
                        optimizationMode == BatteryOptimizeUtils.MODE_RESTRICTED
                ) {
                    // Unable to set restricted mode due to flag protection.
                    Log.w(TAG, "setOptimizationMode($packageName) into restricted ignored")
                    continue
                }
                val originalOptMode: Int =
                    updateBatteryOptimizationMode(
                        context,
                        uid,
                        packageName,
                        optimizationMode,
                        Action.EXTERNAL_UPDATE,
                        getBatteryOptimizeUtils(uid, packageName),
                    )
                if (originalOptMode == BatteryOptimizeUtils.MODE_UNKNOWN) {
                    continue
                }
                // Make sure the reset mode is consistent with the expiration event in storage.
                val resetOptMode = eventsMap[uid]?.resetOptimizationMode ?: originalOptMode
                val expireTimeMs: Long = expirationTimes[i]
                if (expireTimeMs != UNLIMITED_EXPIRE_TIME) {
                    Log.d(
                        TAG,
                        "setOptimizationMode($packageName) from $originalOptMode " +
                            "to $optimizationMode with expiration time $expireTimeMs",
                    )
                    expirationEvents[uid] =
                        AppOptimizationModeEvent.newBuilder()
                            .setUid(uid)
                            .setPackageName(packageName)
                            .setResetOptimizationMode(resetOptMode)
                            .setExpirationTime(expireTimeMs)
                            .build()
                }
            }

            // Append and update the AppOptimizationModeEvent.
            if (expirationEvents.isNotEmpty()) {
                updateSharedPreferences(context, expirationEvents)
            }
        }

    @VisibleForTesting
    fun updateBatteryOptimizationMode(
        context: Context,
        uid: Int,
        packageName: String,
        optimizationMode: Int,
        action: Action,
        batteryOptimizeUtils: BatteryOptimizeUtils =
            BatteryOptimizeUtils(context, uid, packageName),
    ): Int {
        if (!batteryOptimizeUtils.isOptimizeModeMutable) {
            Log.w(TAG, "Fail to update immutable optimization mode for: $packageName")
            return BatteryOptimizeUtils.MODE_UNKNOWN
        }
        val currentOptMode = batteryOptimizeUtils.appOptimizationMode
        batteryOptimizeUtils.setAppUsageState(optimizationMode, action)
        Log.d(
            TAG,
            "setAppUsageState($packageName) to $optimizationMode with action = ${action.name}",
        )
        return currentOptMode
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(
            SHARED_PREFS_FILE,
            Context.MODE_PRIVATE,
        )
    }

    private fun getAppOptModeEventsMap(context: Context): ArrayMap<Int, AppOptimizationModeEvent> {
        val sharedPreferences = getSharedPreferences(context)
        val allKeys = sharedPreferences.all?.keys ?: emptySet()
        if (allKeys.isEmpty()) {
            return ArrayMap()
        }
        val eventsMap = ArrayMap<Int, AppOptimizationModeEvent>(allKeys.size)
        for (key in allKeys) {
            sharedPreferences.getString(key, null)?.let {
                eventsMap[key.toInt()] = deserializeAppOptimizationModeEvent(it)
            }
        }
        return eventsMap
    }

    private fun updateSharedPreferences(
        context: Context,
        eventsMap: Map<Int, AppOptimizationModeEvent>,
    ) {
        val sharedPreferences = getSharedPreferences(context)
        sharedPreferences.edit().run {
            for ((uid, event) in eventsMap) {
                putString(uid.toString(), serializeAppOptimizationModeEvent(event))
            }
            apply()
        }
    }

    private fun clearSharedPreferences(context: Context, uids: List<Int>) {
        val sharedPreferences = getSharedPreferences(context)
        sharedPreferences.edit().run {
            for (uid in uids) {
                remove(uid.toString())
            }
            apply()
        }
    }

    private fun serializeAppOptimizationModeEvent(event: AppOptimizationModeEvent): String {
        return Base64.encodeToString(event.toByteArray(), Base64.DEFAULT)
    }

    private fun deserializeAppOptimizationModeEvent(
        encodedProtoString: String,
    ): AppOptimizationModeEvent {
        return BatteryUtils.parseProtoFromString(encodedProtoString, defaultInstance)
    }
}
