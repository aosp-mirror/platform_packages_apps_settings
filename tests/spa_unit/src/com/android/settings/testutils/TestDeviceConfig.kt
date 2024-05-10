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

package com.android.settings.testutils

import android.provider.DeviceConfig

/**
 * A util class used to override [DeviceConfig] value for testing purpose.
 */
class TestDeviceConfig(private val namespace: String, private val name: String) {
    private val initialValue = DeviceConfig.getProperty(namespace, name)

    /** Overrides the property value. */
    fun override(value: Boolean) {
        DeviceConfig.setProperty(namespace, name, value.toString(), false)
    }

    /** Resets the property to its initial value before the testing. */
    fun reset() {
        DeviceConfig.setProperty(namespace, name, initialValue, false)
    }
}
