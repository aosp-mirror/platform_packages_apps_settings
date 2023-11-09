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

import android.annotation.NonNull
import android.annotation.StringRes
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.android.settings.R
import com.android.settings.biometrics.fingerprint2.shared.model.Unicorn
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollNavigationViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintEnrollViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintGatekeeperViewModel
import com.android.settings.biometrics.fingerprint2.ui.enrollment.viewmodel.FingerprintScrollViewModel
import com.android.systemui.biometrics.shared.model.FingerprintSensorType
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.template.RequireScrollMixin
import com.google.android.setupdesign.util.DynamicColorPalette
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val TAG = "FingerprintEnrollmentIntroV2Fragment"

/** This class represents the customizable text for FingerprintEnrollIntroduction. */
private data class TextModel(
  @StringRes val footerMessageTwo: Int,
  @StringRes val footerMessageThree: Int,
  @StringRes val footerMessageFour: Int,
  @StringRes val footerMessageFive: Int,
  @StringRes val footerMessageSix: Int,
  @StringRes val negativeButton: Int,
  @StringRes val footerTitleOne: Int,
  @StringRes val footerTitleTwo: Int,
  @StringRes val headerText: Int,
  @StringRes val descriptionText: Int,
)

/**
 * The introduction fragment that is used to inform the user the basics of what a fingerprint sensor
 * is and how it will be used.
 *
 * The main gaols of this page are
 * 1. Inform the user what the fingerprint sensor is and does
 * 2. How the data will be stored
 * 3. How the user can access and remove their data
 */
class FingerprintEnrollIntroV2Fragment() : Fragment(R.layout.fingerprint_v2_enroll_introduction) {

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

  private lateinit var footerBarMixin: FooterBarMixin
  private lateinit var textModel: TextModel

  // Note that the ViewModels cannot be requested before the onCreate call
  private val navigationViewModel: FingerprintEnrollNavigationViewModel by lazy {
    viewModelProvider[FingerprintEnrollNavigationViewModel::class.java]
  }
  private val fingerprintViewModel: FingerprintEnrollViewModel by lazy {
    viewModelProvider[FingerprintEnrollViewModel::class.java]
  }
  private val fingerprintScrollViewModel: FingerprintScrollViewModel by lazy {
    viewModelProvider[FingerprintScrollViewModel::class.java]
  }
  private val gateKeeperViewModel: FingerprintGatekeeperViewModel by lazy {
    viewModelProvider[FingerprintGatekeeperViewModel::class.java]
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? =
    super.onCreateView(inflater, container, savedInstanceState).also { theView ->
      val view = theView!!

      viewLifecycleOwner.lifecycleScope.launch {
        combine(
            navigationViewModel.fingerprintFlow,
            fingerprintViewModel.sensorType,
          ) { enrollType, sensorType ->
            Pair(enrollType, sensorType)
          }
          .collect { (enrollType, sensorType) ->
            textModel =
              when (enrollType) {
                Unicorn -> getUnicornTextModel()
                else -> getNormalTextModel()
              }

            setupFooterBarAndScrollView(view)

            val layout = view as GlifLayout

            layout.setHeaderText(textModel.headerText)
            layout.setDescriptionText(textModel.descriptionText)

            // Set color filter for the following icons.
            val colorFilter = getIconColorFilter()
            listOf(
                R.id.icon_fingerprint,
                R.id.icon_device_locked,
                R.id.icon_trash_can,
                R.id.icon_info,
                R.id.icon_shield,
                R.id.icon_link
              )
              .forEach { icon ->
                view.requireViewById<ImageView>(icon).drawable.colorFilter = colorFilter
              }

            // Set the text for the footer text views.
            listOf(
                R.id.footer_message_2 to textModel.footerMessageTwo,
                R.id.footer_message_3 to textModel.footerMessageThree,
                R.id.footer_message_4 to textModel.footerMessageFour,
                R.id.footer_message_5 to textModel.footerMessageFive,
                R.id.footer_message_6 to textModel.footerMessageSix,
              )
              .forEach { pair -> view.requireViewById<TextView>(pair.first).setText(pair.second) }

            setFooterLink(view)

            val iconShield: ImageView = view.requireViewById(R.id.icon_shield)
            val footerMessage6: TextView = view.requireViewById(R.id.footer_message_6)
            when (sensorType) {
              FingerprintSensorType.UDFPS_ULTRASONIC,
              FingerprintSensorType.UDFPS_OPTICAL -> {
                footerMessage6.visibility = View.VISIBLE
                iconShield.visibility = View.VISIBLE
              }
              else -> {
                footerMessage6.visibility = View.GONE
                iconShield.visibility = View.GONE
              }
            }

            view.requireViewById<TextView?>(R.id.footer_title_1).setText(textModel.footerTitleOne)
            view.requireViewById<TextView?>(R.id.footer_title_2).setText(textModel.footerTitleOne)
          }
      }
      return view
    }

  /**
   * TODO (b/305269201): This link isn't displaying for screenshot tests.
   */
  private fun setFooterLink(view: View) {
    val footerLink: TextView = view.requireViewById(R.id.footer_learn_more)
    footerLink.movementMethod = LinkMovementMethod.getInstance()
    footerLink.text =
      Html.fromHtml(
        getString(R.string.security_settings_fingerprint_v2_enroll_introduction_message_learn_more),
        Html.FROM_HTML_MODE_LEGACY
      )
  }

  private fun setupFooterBarAndScrollView(
    view: View,
  ) {
    val scrollView: ScrollView =
      view.requireViewById(com.google.android.setupdesign.R.id.sud_scroll_view)
    scrollView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    // Next button responsible for starting the next fragment.
    val onNextButtonClick: View.OnClickListener =
      View.OnClickListener {
        Log.d(TAG, "OnNextClicked")
        navigationViewModel.nextStep()
      }

    val layout: GlifLayout = view.findViewById(R.id.setup_wizard_layout)!!
    footerBarMixin = layout.getMixin(FooterBarMixin::class.java)
    footerBarMixin.primaryButton =
      FooterButton.Builder(requireContext())
        .setText(R.string.security_settings_face_enroll_introduction_more)
        .setListener(onNextButtonClick)
        .setButtonType(FooterButton.ButtonType.OPT_IN)
        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
        .build()

    footerBarMixin.setSecondaryButton(
      FooterButton.Builder(requireContext())
        .setText(textModel.negativeButton)
        .setListener({ Log.d(TAG, "prevClicked") })
        .setButtonType(FooterButton.ButtonType.NEXT)
        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
        .build(),
      true /* usePrimaryStyle */
    )

    val primaryButton = footerBarMixin.primaryButton
    val secondaryButton = footerBarMixin.secondaryButton

    secondaryButton.visibility = View.INVISIBLE

    val requireScrollMixin = layout.getMixin(RequireScrollMixin::class.java)
    requireScrollMixin.requireScrollWithButton(
      requireContext(),
      primaryButton,
      R.string.security_settings_face_enroll_introduction_more,
      onNextButtonClick
    )

    requireScrollMixin.setOnRequireScrollStateChangedListener { scrollNeeded: Boolean ->
      // Show secondary button once scroll is completed.
      if (!scrollNeeded) {
        fingerprintScrollViewModel.userConsented()
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      fingerprintScrollViewModel.hasReadConsentScreen.collect { consented ->
        if (consented) {
          primaryButton.setText(
            requireContext(),
            R.string.security_settings_fingerprint_enroll_introduction_agree
          )
          secondaryButton.visibility = View.VISIBLE
        } else {
          secondaryButton.visibility = View.INVISIBLE
        }
      }
    }

    footerBarMixin.getButtonContainer()?.setBackgroundColor(Color.TRANSPARENT)

    // I think I should remove this, and make the challenge a pre-requisite of launching
    // the flow. For instance if someone launches the activity with an invalid challenge, it
    // either 1) Fails or 2) Launched confirmDeviceCredential
    primaryButton.isEnabled = false
    viewLifecycleOwner.lifecycleScope.launch {
      gateKeeperViewModel.hasValidGatekeeperInfo.collect { primaryButton.isEnabled = it }
    }
  }

  private fun getNormalTextModel() =
    TextModel(
      R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_2,
      R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_3,
      R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_4,
      R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_5,
      R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_6,
      R.string.security_settings_fingerprint_enroll_introduction_no_thanks,
      R.string.security_settings_fingerprint_enroll_introduction_footer_title_1,
      R.string.security_settings_fingerprint_enroll_introduction_footer_title_2,
      R.string.security_settings_fingerprint_enroll_introduction_title,
      R.string.security_settings_fingerprint_enroll_introduction_v3_message,
    )

  private fun getUnicornTextModel() =
    TextModel(
      R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_consent_2,
      R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_consent_3,
      R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_consent_4,
      R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_consent_5,
      R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_consent_6,
      R.string.security_settings_fingerprint_enroll_introduction_no_thanks,
      R.string.security_settings_fingerprint_enroll_introduction_footer_title_consent_1,
      R.string.security_settings_fingerprint_enroll_introduction_footer_title_2,
      R.string.security_settings_fingerprint_enroll_consent_introduction_title,
      R.string.security_settings_fingerprint_enroll_introduction_v3_message,
    )

  @NonNull
  private fun getIconColorFilter(): PorterDuffColorFilter {
    return PorterDuffColorFilter(
      DynamicColorPalette.getColor(context, DynamicColorPalette.ColorType.ACCENT),
      PorterDuff.Mode.SRC_IN
    )
  }
}
