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
package com.android.settings.biometrics2.ui.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.R
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout

/**
 * Fragment explaining the under-display fingerprint sensor location for fingerprint enrollment.
 * It interacts with Primary button, and LottieAnimationView.
 * <pre>
 * | Has                 | UDFPS | SFPS | Other (Rear FPS) |
 * |---------------------|-------|------|------------------|
 * | Primary button      | Yes   | No   | No               |
 * | Illustration Lottie | Yes   | Yes  | No               |
 * | Animation           | No    | No   | Depend on layout |
 * | Progress ViewModel  | No    | Yes  | Yes              |
 * | Orientation detect  | No    | Yes  | No               |
 * | Foldable detect     | No    | Yes  | No               |
 * </pre>
 */
class FingerprintEnrollFindUdfpsFragment : Fragment() {

    private var _viewModel: FingerprintEnrollFindSensorViewModel? = null
    private val mViewModel: FingerprintEnrollFindSensorViewModel
        get() = _viewModel!!

    private var findUdfpsView: GlifLayout? = null

    private val mOnSkipClickListener =
        View.OnClickListener { _: View? -> mViewModel.onSkipButtonClick() }

    private val mOnStartClickListener =
        View.OnClickListener { _: View? -> mViewModel.onStartButtonClick() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = (inflater.inflate(
        R.layout.udfps_enroll_find_sensor_layout,
        container,
        false
    ) as GlifLayout).also {
        findUdfpsView = it
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().bindFingerprintEnrollFindUdfpsView(
            view = findUdfpsView!!,
            isAccessibilityEnabled = mViewModel.isAccessibilityEnabled,
            onSkipClickListener = mOnSkipClickListener,
            onStartClickListener = mOnStartClickListener
        )
    }

    override fun onAttach(context: Context) {
        _viewModel = ViewModelProvider(requireActivity())[
            FingerprintEnrollFindSensorViewModel::class.java
        ]
        super.onAttach(context)
    }
}

fun FragmentActivity.bindFingerprintEnrollFindUdfpsView(
    view: GlifLayout,
    isAccessibilityEnabled: Boolean,
    onSkipClickListener: View.OnClickListener,
    onStartClickListener: View.OnClickListener,
) {
    GlifLayoutHelper(this, view).let { helper ->
        helper.setHeaderText(R.string.security_settings_udfps_enroll_find_sensor_title)
        helper.setDescriptionText(
            getText(R.string.security_settings_udfps_enroll_find_sensor_message)
        )
    }

    view.getMixin(FooterBarMixin::class.java)!!.let {
        it.secondaryButton = FooterButton.Builder(this)
            .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
            .setButtonType(FooterButton.ButtonType.SKIP)
            .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
            .build()
        it.secondaryButton.setOnClickListener(onSkipClickListener)

        it.primaryButton = FooterButton.Builder(this)
            .setText(R.string.security_settings_udfps_enroll_find_sensor_start_button)
            .setButtonType(FooterButton.ButtonType.NEXT)
            .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
            .build()
        it.primaryButton.setOnClickListener(onStartClickListener)
    }

    view.findViewById<LottieAnimationView>(R.id.illustration_lottie)!!.let {
        it.setOnClickListener(onStartClickListener)
        if (isAccessibilityEnabled) {
            it.setAnimation(R.raw.udfps_edu_a11y_lottie)
        }
    }
}