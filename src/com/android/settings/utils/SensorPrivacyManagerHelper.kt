/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.utils

import android.content.Context
import android.hardware.SensorPrivacyManager
import android.hardware.SensorPrivacyManager.OnSensorPrivacyChangedListener.SensorPrivacyChangedParams
import android.hardware.SensorPrivacyManager.Sources.SETTINGS
import android.util.Log
import java.util.concurrent.Executor

/**
 * A class to help with calls to the sensor privacy manager. This class caches state when needed and
 * multiplexes multiple listeners to a minimal set of binder calls.
 *
 * If you are not a test use [SensorPrivacyManagerHelper.getInstance]
 */
// This class uses `open` a lot for mockito
open class SensorPrivacyManagerHelper(context: Context) :
        SensorPrivacyManager.OnSensorPrivacyChangedListener {
    private val sensorPrivacyManager: SensorPrivacyManager
    private val cache: MutableMap<Pair<Int, Int>, Boolean> = mutableMapOf()
    private val callbacks: MutableMap<Pair<Int, Int>, MutableSet<Pair<Callback, Executor>>> =
            mutableMapOf()
    private val lock = Any()

    /**
     * Callback for when the state of the sensor privacy changes.
     */
    interface Callback {
        /**
         * Method invoked when the sensor privacy changes.
         * @param sensor The sensor which changed
         * @param blocked If the sensor is blocked
         */
        fun onSensorPrivacyChanged(toggleType: Int, sensor: Int, blocked: Boolean)
    }

    init {
        sensorPrivacyManager = context.getSystemService(SensorPrivacyManager::class.java)!!

        sensorPrivacyManager.addSensorPrivacyListener(context.mainExecutor, this)
    }

    /**
     * Checks if the given toggle is supported on this device
     * @param sensor The sensor to check
     * @return whether the toggle for the sensor is supported on this device.
     */
    open fun supportsSensorToggle(sensor: Int): Boolean {
        return sensorPrivacyManager.supportsSensorToggle(sensor)
    }

    @JvmOverloads
    open fun isSensorBlocked(toggleType: Int = TOGGLE_TYPE_ANY, sensor: Int): Boolean {
        synchronized(lock) {
            if (toggleType == TOGGLE_TYPE_ANY) {
                return isSensorBlocked(TOGGLE_TYPE_SOFTWARE, sensor) ||
                        isSensorBlocked(TOGGLE_TYPE_HARDWARE, sensor)
            }
            return cache.getOrPut(toggleType to sensor) {
                sensorPrivacyManager.isSensorPrivacyEnabled(toggleType, sensor)
            }
        }
    }

    open fun setSensorBlocked(sensor: Int, blocked: Boolean) {
        sensorPrivacyManager.setSensorPrivacy(SETTINGS, sensor, blocked)
    }

    open fun addSensorBlockedListener(executor: Executor?, callback: Callback?) {
        // Not using defaults for mockito
        addSensorBlockedListener(SENSOR_ANY, executor, callback)
    }

    open fun addSensorBlockedListener(sensor: Int, executor: Executor?, callback: Callback?) {
        // Not using defaults for mockito
        addSensorBlockedListener(TOGGLE_TYPE_ANY, sensor, executor, callback)
    }

    open fun addSensorBlockedListener(toggleType: Int, sensor: Int,
                                      executor: Executor?, callback: Callback?) {
        // Note: executor and callback should be nonnull, but we want to use mockito
        if (toggleType == TOGGLE_TYPE_ANY) {
            addSensorBlockedListener(TOGGLE_TYPE_SOFTWARE, sensor, executor, callback)
            addSensorBlockedListener(TOGGLE_TYPE_HARDWARE, sensor, executor, callback)
            return
        }

        if (sensor == SENSOR_ANY) {
            addSensorBlockedListener(toggleType, SENSOR_MICROPHONE, executor, callback)
            addSensorBlockedListener(toggleType, SENSOR_CAMERA, executor, callback)
            return
        }

        synchronized(lock) {
            callbacks.getOrPut(toggleType to sensor) { mutableSetOf() }
                    .add(callback!! to executor!!)
        }
    }

    open fun removeSensorBlockedListener(callback: Callback) {
        val keysToRemove = mutableListOf<Pair<Int, Int>>()
        synchronized(lock) {
            callbacks.forEach { entry ->
                entry.value.removeIf {
                    it.first == callback
                }

                if (entry.value.isEmpty()) {
                    keysToRemove.add(entry.key)
                }
            }

            keysToRemove.forEach {
                callbacks.remove(it)
            }
        }
    }

    companion object {
        const val TOGGLE_TYPE_SOFTWARE = SensorPrivacyManager.TOGGLE_TYPE_SOFTWARE
        const val TOGGLE_TYPE_HARDWARE = SensorPrivacyManager.TOGGLE_TYPE_HARDWARE
        const val SENSOR_MICROPHONE = SensorPrivacyManager.Sensors.MICROPHONE
        const val SENSOR_CAMERA = SensorPrivacyManager.Sensors.CAMERA

        private const val TOGGLE_TYPE_ANY = -1
        private const val SENSOR_ANY = -1
        private var sInstance: SensorPrivacyManagerHelper? = null

        /**
         * Gets the singleton instance
         * @param context The context which is needed if the instance hasn't been created
         * @return the instance
         */
        @JvmStatic
        fun getInstance(context: Context): SensorPrivacyManagerHelper? {
            if (sInstance == null) {
                sInstance = SensorPrivacyManagerHelper(context)
            }
            return sInstance
        }
    }

    override fun onSensorPrivacyChanged(sensor: Int, enabled: Boolean) {
        // ignored
    }

    override fun onSensorPrivacyChanged(params: SensorPrivacyChangedParams) {
        var changed: Boolean
        synchronized(lock) {
            changed = cache.put(params.toggleType to params.sensor, params.isEnabled) !=
                    params.isEnabled

            if (changed) {
                callbacks[params.toggleType to params.sensor]?.forEach {
                    it.second.execute {
                        it.first.onSensorPrivacyChanged(params.toggleType, params.sensor,
                                params.isEnabled)
                    }
                }
            }
        }
    }
}
