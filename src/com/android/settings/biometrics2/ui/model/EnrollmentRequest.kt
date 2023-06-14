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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.android.settings.SetupWizardUtils
import com.android.settings.biometrics.BiometricEnrollActivity.EXTRA_SKIP_INTRO
import com.google.android.setupcompat.util.WizardManagerHelper
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW

/**
 * Biometric enrollment generic intent data, which includes
 * 1. isSuw
 * 2. isAfterSuwOrSuwSuggestedAction
 * 3. theme
 * 4. isFromSettingsSummery
 * 5. isSkipIntro
 * 6. isSkipFindSensor
 * 7. a helper method, getSetupWizardExtras
 */
class EnrollmentRequest(
    intent: Intent,
    context: Context,
    isSetupActivity: Boolean
) {
    val isSuw: Boolean = isSetupActivity && WizardManagerHelper.isAnySetupWizard(intent)

    val isAfterSuwOrSuwSuggestedAction = (isSetupActivity
            && (WizardManagerHelper.isDeferredSetupWizard(intent)
            || WizardManagerHelper.isPortalSetupWizard(intent)
            || intent.getBooleanExtra(EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW, false)))

    private val _suwExtras = getSuwExtras(isSuw, intent)

    val isSkipIntro = intent.getBooleanExtra(EXTRA_SKIP_INTRO, false)

    val isSkipFindSensor = intent.getBooleanExtra(EXTRA_SKIP_FIND_SENSOR, false)

    val theme = SetupWizardUtils.getTheme(context, intent)

    val suwExtras: Bundle
        get() = Bundle(_suwExtras)

    /**
     * Returns a string representation of the object
     */
    override fun toString(): String {
        return (javaClass.simpleName + ":{isSuw:" + isSuw
                + ", isAfterSuwOrSuwSuggestedAction:" + isAfterSuwOrSuwSuggestedAction
                + "}")
    }

    companion object {
        const val EXTRA_SKIP_FIND_SENSOR = "skip_find_sensor"
        private fun getSuwExtras(isSuw: Boolean, intent: Intent): Bundle {
            val toIntent = Intent()
            if (isSuw) {
                SetupWizardUtils.copySetupExtras(intent, toIntent)
            }
            return toIntent.extras ?: Bundle()
        }
    }
}
