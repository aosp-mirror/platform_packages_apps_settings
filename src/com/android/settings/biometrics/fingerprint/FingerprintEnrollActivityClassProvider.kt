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

package com.android.settings.biometrics.fingerprint

import android.app.Activity

open class FingerprintEnrollActivityClassProvider {

    open val default: Class<out Activity>
        get() = FingerprintEnrollIntroduction::class.java
    open val setup: Class<out Activity>
        get() = SetupFingerprintEnrollIntroduction::class.java
    open val internal: Class<out Activity>
        get() = FingerprintEnrollIntroductionInternal::class.java

    companion object {
        @JvmStatic
        val instance = FingerprintEnrollActivityClassProvider()
    }
}
