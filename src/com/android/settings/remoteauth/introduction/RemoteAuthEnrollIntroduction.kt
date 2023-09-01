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

package com.android.settings.remoteauth.introduction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import com.android.settings.R
import com.android.settings.remoteauth.RemoteAuthEnrollBase
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.template.RequireScrollMixin

/**
 * Provides introductory info about remote authenticator unlock.
 */
class RemoteAuthEnrollIntroduction :
    RemoteAuthEnrollBase(
        layoutResId = R.layout.remote_auth_enroll_introduction,
        glifLayoutId = R.id.setup_wizard_layout,
    ) {
    private val navController by lazy { findNavController(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        viewGroup: ViewGroup?,
        savedInstanceArgs: Bundle?
    ) =
        super.onCreateView(inflater, viewGroup, savedInstanceArgs).also {
            initializeRequireScrollMixin(it)
        }


    override fun initializePrimaryFooterButton(): FooterButton {
        return FooterButton.Builder(context!!)
            .setText(R.string.security_settings_remoteauth_enroll_introduction_agree)
            .setListener(::onPrimaryFooterButtonClick)
            .setButtonType(FooterButton.ButtonType.OPT_IN)
            .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
            .build()
    }

    override fun initializeSecondaryFooterButton(): FooterButton {
        return FooterButton.Builder(context!!)
            .setText(R.string.security_settings_remoteauth_enroll_introduction_disagree)
            .setListener(::onSecondaryFooterButtonClick)
            .setButtonType(FooterButton.ButtonType.NEXT)
            .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
            .build()
    }

    private fun onPrimaryFooterButtonClick(view: View) {
        navController.navigate(R.id.action_introduction_to_enrolling)
    }

    private fun onSecondaryFooterButtonClick(view: View) {
        navController.navigateUp()
    }

    private fun initializeRequireScrollMixin(view: View) {
        val layout = checkNotNull(getGlifLayout(view))
        secondaryFooterButton?.visibility = View.INVISIBLE
        val requireScrollMixin = layout.getMixin(RequireScrollMixin::class.java)
        requireScrollMixin.requireScrollWithButton(
            requireContext(),
            primaryFooterButton,
            R.string.security_settings_remoteauth_enroll_introduction_more,
            ::onPrimaryFooterButtonClick
        )
        requireScrollMixin.setOnRequireScrollStateChangedListener { scrollNeeded ->
            if (scrollNeeded) {
                primaryFooterButton.setText(
                    requireContext(),
                    R.string.security_settings_remoteauth_enroll_introduction_more
                )
            } else {
                primaryFooterButton.setText(
                    requireContext(),
                    R.string.security_settings_remoteauth_enroll_introduction_agree
                )
                secondaryFooterButton?.visibility = View.VISIBLE
            }
        }
    }

    private companion object {
        const val TAG = "RemoteAuthEnrollIntro"
    }
}
