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
import com.android.settings.R
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFinishViewModel
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout

/**
 * Fragment which concludes fingerprint enrollment.
 */
class FingerprintEnrollFinishFragment : Fragment() {

    private var _viewModel: FingerprintEnrollFinishViewModel? = null
    private val viewModel: FingerprintEnrollFinishViewModel
        get() = _viewModel!!

    private val addButtonClickListener =
        View.OnClickListener { _: View? -> viewModel.onAddButtonClick() }

    private val nextButtonClickListener =
        View.OnClickListener { _: View? -> viewModel.onNextButtonClick() }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        _viewModel = ViewModelProvider(requireActivity())[
            FingerprintEnrollFinishViewModel::class.java
        ]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) : View = (inflater.inflate(
            if (viewModel.canAssumeSfps())
                R.layout.sfps_enroll_finish
            else
                R.layout.fingerprint_enroll_finish,
            container,
            false
        ) as GlifLayout).also {
            requireActivity().bindFingerprintEnrollFinishFragment(
                view = it,
                isSuw = viewModel.request.isSuw,
                canAssumeSfps = viewModel.canAssumeSfps(),
                isAnotherFingerprintEnrollable = viewModel.isAnotherFingerprintEnrollable,
                nextButtonClickListener = nextButtonClickListener,
                addButtonClickListener = addButtonClickListener
            )
        }
}

fun FragmentActivity.bindFingerprintEnrollFinishFragment(
    view: GlifLayout,
    isSuw: Boolean,
    canAssumeSfps: Boolean,
    isAnotherFingerprintEnrollable: Boolean,
    nextButtonClickListener: View.OnClickListener,
    addButtonClickListener: View.OnClickListener
) {
    GlifLayoutHelper(this, view).apply {
        setHeaderText(R.string.security_settings_fingerprint_enroll_finish_title)
        setDescriptionText(
            getString(
                if (canAssumeSfps && isAnotherFingerprintEnrollable)
                    R.string.security_settings_fingerprint_enroll_finish_v2_add_fingerprint_message
                else
                    R.string.security_settings_fingerprint_enroll_finish_v2_message
            )
        )
    }

    view.getMixin(FooterBarMixin::class.java).also { footer ->
        footer.primaryButton = FooterButton.Builder(this)
            .setText(
                if (isSuw)
                    R.string.next_label
                else
                    R.string.security_settings_fingerprint_enroll_done
            )
            .setListener(nextButtonClickListener)
            .setButtonType(FooterButton.ButtonType.NEXT)
            .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
            .build()
        if (isAnotherFingerprintEnrollable) {
            footer.secondaryButton = FooterButton.Builder(this)
                .setText(R.string.fingerprint_enroll_button_add)
                .setListener(addButtonClickListener)
                .setButtonType(FooterButton.ButtonType.SKIP)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                .build()
        }
    }

}
