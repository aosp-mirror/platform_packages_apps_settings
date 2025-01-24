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

package com.android.settings.biometrics.fingerprint

import com.android.settings.R

/**
 * Provide features for FingerprintSettings page.
 */
open class FingerprintSettingsFeatureProvider {
    /**
     * Get the description shown in the FingerprintSetting page.
     */
    open fun getSettingPageDescription(): Int {
        return 0
    }

    /**
     * Get the learn more description shown in the footer of the FingerprintSetting page.
     */
    open fun getSettingPageFooterLearnMoreDescription(): Int {
        return R.string.security_settings_fingerprint_settings_footer_learn_more
    }

    companion object {
        @JvmStatic
        val instance = FingerprintSettingsFeatureProvider()
    }
}