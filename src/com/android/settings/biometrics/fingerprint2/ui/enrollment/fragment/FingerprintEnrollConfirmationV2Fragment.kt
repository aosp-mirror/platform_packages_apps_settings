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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollConfirmationViewModel
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import kotlinx.coroutines.launch

/**
 * A fragment to indicate that fingerprint enrollment has been completed.
 *
 * This page will display basic information about what a fingerprint can be used for and acts as the
 * final step of enrollment.
 */
class FingerprintEnrollConfirmationV2Fragment() :
  Fragment(R.layout.fingerprint_enroll_finish_base) {

  companion object {
    const val TAG = "FingerprintEnrollConfirmationV2Fragment"
  }

  /** Used for testing purposes */
  private var factory: ViewModelProvider.Factory? = null

  @VisibleForTesting
  constructor(theFactory: ViewModelProvider.Factory) : this() {
    factory = theFactory
  }

  private val viewModelProvider: ViewModelProvider by lazy {
    if (factory != null) {
      ViewModelProvider(requireActivity(), factory!!)
    } else {
      ViewModelProvider(requireActivity())
    }
  }

  private val viewModel: FingerprintEnrollConfirmationViewModel by lazy {
    viewModelProvider[FingerprintEnrollConfirmationViewModel::class.java]
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? =
    super.onCreateView(inflater, container, savedInstanceState).also { theView ->
      val mainView = theView!! as GlifLayout

      mainView.setHeaderText(R.string.security_settings_fingerprint_enroll_finish_title)
      mainView.setDescriptionText(R.string.security_settings_fingerprint_enroll_finish_v2_message)

      val mixin = mainView.getMixin(FooterBarMixin::class.java)
      viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
          viewModel.isAddAnotherButtonVisible.collect {
            mixin.secondaryButton =
              FooterButton.Builder(requireContext())
                .setText(R.string.fingerprint_enroll_button_add)
                .setListener { viewModel.onAddAnotherButtonClicked() }
                .setButtonType(FooterButton.ButtonType.SKIP)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                .build()
          }
        }
      }

      mixin.setPrimaryButton(
        FooterButton.Builder(requireContext())
          .setText(R.string.security_settings_fingerprint_enroll_done)
          .setListener(this::onNextButtonClick)
          .setButtonType(FooterButton.ButtonType.NEXT)
          .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
          .build()
      )
    }

  @Suppress("UNUSED_PARAMETER")
  private fun onNextButtonClick(view: View?) {
    viewModel.onNextButtonClicked()
  }
}
