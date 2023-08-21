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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollmentNavigationViewModel

/**
 * A fragment that is used to educate the user about the fingerprint sensor on this device.
 *
 * The main goals of this page are
 * 1. Inform the user where the fingerprint sensor is on their device
 * 2. Explain to the user how the enrollment process shown by [FingerprintEnrollEnrollingV2Fragment]
 *    will work.
 */
class FingerprintEnrollFindSensorV2Fragment : Fragment(R.layout.fingerprint_v2_enroll_find_sensor) {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState == null) {
      val navigationViewModel =
        ViewModelProvider(requireActivity())[FingerprintEnrollmentNavigationViewModel::class.java]
    }
  }
}
