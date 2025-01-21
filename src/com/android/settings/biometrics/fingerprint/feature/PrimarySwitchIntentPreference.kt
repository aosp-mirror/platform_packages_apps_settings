/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.settings.biometrics.fingerprint.feature

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import androidx.preference.PreferenceViewHolder
import com.android.settingslib.PrimarySwitchPreference
import com.android.settingslib.core.instrumentation.SettingsJankMonitor.detectToggleJank

/**
 * Supports launching a new activity through Intent for PrimarySwitchPreference
 */
abstract class PrimarySwitchIntentPreference : PrimarySwitchPreference {

    constructor(
        context: Context?, attrs: AttributeSet?,
        defStyleAttr: Int, defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?) : super(context)

    open fun forceUpdate() {}
    abstract fun getLaunchedIntent(token: ByteArray): Intent
}
