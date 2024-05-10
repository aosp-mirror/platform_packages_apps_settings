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
package com.android.settings.biometrics2.ui.model

/** Biometric Enrollment progress */
class EnrollmentProgress(val steps: Int, val remaining: Int) {

    val isInitialStep: Boolean
        get() = steps == INITIAL_STEPS

    override fun toString(): String {
        return ("${javaClass.simpleName}@${Integer.toHexString(hashCode())}"
                + "{steps:$steps, remaining:$remaining}")
    }

    companion object {
        const val INITIAL_STEPS = -1
        const val INITIAL_REMAINING = 0
    }
}
