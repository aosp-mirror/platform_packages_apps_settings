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

package com.android.settings.testutils

/**
 * Helper class to override system properties.
 *
 * [System.setProperty] changes the static state in the JVM, which is shared by all tests. Hence,
 * there is chance that test cases are dependent/interfered due to system property unexpectedly.
 * This helper class backs up the old properties when invoking [override] and restore the old
 * properties in [close] to avoid flaky testing.
 */
class SystemProperty(overrides: Map<String, String?> = mapOf()) : AutoCloseable {
    private val oldProperties = mutableMapOf<String, String?>()

    constructor(key: String, value: String?) : this(mapOf(key to value))

    init {
        override(overrides)
    }

    fun override(key: String, value: String?) = override(mapOf(key to value))

    fun override(overrides: Map<String, String?>) {
        // back up system properties for the overrides
        for (key in overrides.keys) {
            // only back up the oldest property
            if (!oldProperties.containsKey(key)) {
                oldProperties[key] = System.getProperty(key)
            }
        }
        overrides.overrideProperties()
    }

    override fun close() {
        // restore the backed up properties
        oldProperties.overrideProperties()
        oldProperties.clear()
    }

    private fun Map<String, String?>.overrideProperties() {
        for ((key, value) in this) {
            if (value != null) {
                System.setProperty(key, value)
            } else {
                System.clearProperty(key)
            }
        }
    }
}
