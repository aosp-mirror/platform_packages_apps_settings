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

package com.android.settings.remoteauth.finish

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.R
import com.android.settings.remoteauth.RemoteAuthEnrollBase
import com.android.settingslib.widget.LottieColorUtils
import com.google.android.setupcompat.template.FooterButton

/**
 * Displays the enrollment finish view.
 */
class RemoteAuthEnrollFinish :
    RemoteAuthEnrollBase(
        layoutResId = R.layout.remote_auth_enroll_finish,
        glifLayoutId = R.id.setup_wizard_layout,
    ) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LottieColorUtils.applyDynamicColors(
            requireContext(),
            view.findViewById<LottieAnimationView>(R.id.enroll_finish_animation)
        )
    }

    override fun initializePrimaryFooterButton(): FooterButton {
        return FooterButton.Builder(requireContext())
            .setText(R.string.security_settings_remoteauth_enroll_finish_btn_next)
            .setListener(this::onPrimaryFooterButtonClick)
            .setButtonType(FooterButton.ButtonType.NEXT)
            .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
            .build()
    }

    override fun initializeSecondaryFooterButton(): FooterButton? = null

    fun onPrimaryFooterButtonClick(view: View) {
        findNavController(this).navigate(R.id.action_finish_to_settings)
    }

    private companion object {
        const val TAG = "RemoteAuthEnrollFinish"
    }
}