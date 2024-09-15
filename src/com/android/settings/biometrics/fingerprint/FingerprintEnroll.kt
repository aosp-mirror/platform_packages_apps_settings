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

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory

/**
 * Default class for handling fingerprint enrollment, designed to launch a subsequent activity and
 * forward the result, then finish itself.
 */
open class FingerprintEnroll: AppCompatActivity() {

    /** Inner class representing enrolling fingerprint enrollment in SetupWizard environment */
    class SetupActivity : FingerprintEnroll() {
        override val nextActivityClass: Class<*>
            get() = enrollActivityProvider.setup
    }

    /** Inner class representing enrolling fingerprint enrollment from FingerprintSettings */
    class InternalActivity : FingerprintEnroll() {
        override val nextActivityClass: Class<*>
        get() = enrollActivityProvider.internal
    }

    /**
     * The class of the next activity to launch. This is open to allow subclasses to provide their
     * own behavior. Defaults to the default activity class provided by the
     * enrollActivityClassProvider.
     */
    open val nextActivityClass: Class<*>
        get() = enrollActivityProvider.default

    protected val enrollActivityProvider: FingerprintEnrollActivityClassProvider
        get() = featureFactory.fingerprintFeatureProvider.enrollActivityClassProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         *  Logs the next activity to be launched, creates an intent for that activity,
         *  adds flags to forward the result, includes any existing extras from the current intent,
         *  starts the new activity and then finishes the current one
         */
        Log.d("FingerprintEnroll", "forward to $nextActivityClass")
        val nextIntent = Intent(this, nextActivityClass)
        nextIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        nextIntent.putExtras(intent)
        startActivity(nextIntent)
        finish()
    }
}