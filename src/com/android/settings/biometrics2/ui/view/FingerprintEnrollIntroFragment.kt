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

import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyResources.Strings.Settings.FINGERPRINT_UNLOCK_DISABLED
import android.content.Context
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
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.settings.R
import com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus
import com.android.settings.biometrics2.ui.model.FingerprintEnrollable.FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX
import com.android.settings.biometrics2.ui.model.FingerprintEnrollable.FINGERPRINT_ENROLLABLE_OK
import com.android.settings.biometrics2.ui.model.FingerprintEnrollable.FINGERPRINT_ENROLLABLE_UNKNOWN
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.template.RequireScrollMixin
import com.google.android.setupdesign.util.DeviceHelper
import com.google.android.setupdesign.util.DynamicColorPalette
import com.google.android.setupdesign.util.DynamicColorPalette.ColorType.ACCENT
import java.util.function.Supplier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fingerprint intro onboarding page fragment implementation
 */
class FingerprintEnrollIntroFragment : Fragment() {

    private val viewModelProvider: ViewModelProvider
        get() = ViewModelProvider(requireActivity())

    private var _viewModel: FingerprintEnrollIntroViewModel? = null
    private val viewModel: FingerprintEnrollIntroViewModel
        get() = _viewModel!!

    private var introView: GlifLayout? = null

    private var primaryFooterButton: FooterButton? = null

    private var secondaryFooterButton: FooterButton? = null

    private val onNextClickListener =
        View.OnClickListener { _: View? ->
            activity?.lifecycleScope?.let {
                viewModel.onNextButtonClick(it)
            }
        }

    private val onSkipOrCancelClickListener =
        View.OnClickListener { _: View? ->
            activity?.lifecycleScope?.let {
                viewModel.onSkipOrCancelButtonClick(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        introView = inflater.inflate(
            R.layout.fingerprint_enroll_introduction,
            container,
            false
        ) as GlifLayout
        return introView!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().bindFingerprintEnrollIntroView(
            view = introView!!,
            canAssumeUdfps = viewModel.canAssumeUdfps,
            isBiometricUnlockDisabledByAdmin = viewModel.isBiometricUnlockDisabledByAdmin,
            isParentalConsentRequired = viewModel.isParentalConsentRequired,
            descriptionDisabledByAdminSupplier = { getDescriptionDisabledByAdmin(view.context) }
        )
    }

    override fun onStart() {
        val context: Context = requireContext()
        val footerBarMixin: FooterBarMixin = footerBarMixin
        viewModel.updateEnrollableStatus(lifecycleScope)
        initPrimaryFooterButton(context, footerBarMixin)
        initSecondaryFooterButton(context, footerBarMixin)
        collectPageStatusFlowIfNeed()
        super.onStart()
    }

    private fun initPrimaryFooterButton(
        context: Context,
        footerBarMixin: FooterBarMixin
    ) {
        if (footerBarMixin.primaryButton != null) {
            return
        }
        primaryFooterButton = FooterButton.Builder(context)
            .setText(R.string.security_settings_fingerprint_enroll_introduction_agree)
            .setButtonType(FooterButton.ButtonType.OPT_IN)
            .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
            .build()
            .also {
                it.setOnClickListener(onNextClickListener)
                footerBarMixin.primaryButton = it
            }
    }

    private fun initSecondaryFooterButton(
        context: Context,
        footerBarMixin: FooterBarMixin
    ) {
        if (footerBarMixin.secondaryButton != null) {
            return
        }
        secondaryFooterButton = FooterButton.Builder(context)
            .setText(
                if (viewModel.request.isAfterSuwOrSuwSuggestedAction)
                    R.string.security_settings_fingerprint_enroll_introduction_cancel
                else
                    R.string.security_settings_fingerprint_enroll_introduction_no_thanks
            )
            .setButtonType(FooterButton.ButtonType.NEXT)
            .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
            .build()
            .also {
                it.setOnClickListener(onSkipOrCancelClickListener)
                footerBarMixin.setSecondaryButton(it, true /* usePrimaryStyle */)
            }
    }

    private fun collectPageStatusFlowIfNeed() {
        lifecycleScope.launch {
            val status = viewModel.pageStatusFlow.first()
            Log.d(TAG, "collectPageStatusFlowIfNeed status:$status")
            if (status.hasScrollToBottom()
                || status.enrollableStatus === FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX
            ) {
                // Update once and do not requireScrollWithButton() again when page has
                // scrolled to bottom or User has enrolled at least a fingerprint, because if
                // we requireScrollWithButton() again, primary button will become "More" after
                // scrolling.
                updateFooterButtons(status)
            } else {
                introView!!.getMixin(RequireScrollMixin::class.java).let {
                    it.requireScrollWithButton(
                        requireActivity(),
                        primaryFooterButton!!,
                        moreButtonTextRes,
                        onNextClickListener
                    )
                    it.setOnRequireScrollStateChangedListener { scrollNeeded: Boolean ->
                        viewModel.setHasScrolledToBottom(!scrollNeeded, lifecycleScope)
                    }
                }
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.pageStatusFlow.collect(
                        this@FingerprintEnrollIntroFragment::updateFooterButtons
                    )
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        _viewModel = viewModelProvider[FingerprintEnrollIntroViewModel::class.java]
        super.onAttach(context)
    }

    private val footerBarMixin: FooterBarMixin
        get() = introView!!.getMixin(FooterBarMixin::class.java)

    private fun getDescriptionDisabledByAdmin(context: Context): String? {
        val defaultStrId: Int =
            R.string.security_settings_fingerprint_enroll_introduction_message_unlock_disabled
        val devicePolicyManager: DevicePolicyManager =
            checkNotNull(requireActivity().getSystemService(DevicePolicyManager::class.java))

        return devicePolicyManager.resources.getString(FINGERPRINT_UNLOCK_DISABLED) {
            context.getString(defaultStrId)
        }
    }

    private fun updateFooterButtons(status: FingerprintEnrollIntroStatus) {
        if (DEBUG) {
            Log.d(TAG, "updateFooterButtons($status)")
        }
        primaryFooterButton!!.setText(
            context,
            if (status.enrollableStatus === FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX)
                R.string.done
            else if (status.hasScrollToBottom())
                R.string.security_settings_fingerprint_enroll_introduction_agree
            else
                moreButtonTextRes
        )
        secondaryFooterButton!!.visibility =
            if (status.hasScrollToBottom()
                && status.enrollableStatus !== FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX
                )
                View.VISIBLE
            else
                View.INVISIBLE

        view!!.requireViewById<TextView>(R.id.error_text).let {
            when (status.enrollableStatus) {
                FINGERPRINT_ENROLLABLE_OK -> {
                    it.text = null
                    it.visibility = View.GONE
                }

                FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX -> {
                    it.setText(R.string.fingerprint_intro_error_max)
                    it.visibility = View.VISIBLE
                }

                FINGERPRINT_ENROLLABLE_UNKNOWN -> {}
            }
        }
    }

    @get:StringRes
    private val moreButtonTextRes: Int
        get() = R.string.security_settings_face_enroll_introduction_more

    companion object {
        private const val TAG = "FingerprintEnrollIntroFragment"
        private const val DEBUG = false
    }
}

fun FragmentActivity.bindFingerprintEnrollIntroView(
    view: GlifLayout,
    canAssumeUdfps: Boolean,
    isBiometricUnlockDisabledByAdmin: Boolean,
    isParentalConsentRequired: Boolean,
    descriptionDisabledByAdminSupplier: Supplier<String?>
) {
    val context = view.context

    val iconFingerprint = view.findViewById<ImageView>(R.id.icon_fingerprint)!!
    val iconDeviceLocked = view.findViewById<ImageView>(R.id.icon_device_locked)!!
    val iconTrashCan = view.findViewById<ImageView>(R.id.icon_trash_can)!!
    val iconInfo = view.findViewById<ImageView>(R.id.icon_info)!!
    val iconShield = view.findViewById<ImageView>(R.id.icon_shield)!!
    val iconLink = view.findViewById<ImageView>(R.id.icon_link)!!
    val footerMessage6 = view.findViewById<TextView>(R.id.footer_message_6)!!

    PorterDuffColorFilter(
        DynamicColorPalette.getColor(context, ACCENT),
        PorterDuff.Mode.SRC_IN
    ).let { colorFilter ->
        iconFingerprint.drawable.colorFilter = colorFilter
        iconDeviceLocked.drawable.colorFilter = colorFilter
        iconTrashCan.drawable.colorFilter = colorFilter
        iconInfo.drawable.colorFilter = colorFilter
        iconShield.drawable.colorFilter = colorFilter
        iconLink.drawable.colorFilter = colorFilter
    }

    view.findViewById<TextView>(R.id.footer_learn_more)!!.let { learnMore ->
        learnMore.movementMethod = LinkMovementMethod.getInstance()
        val footerLinkStr: String = context.getString(
            R.string.security_settings_fingerprint_v2_enroll_introduction_message_learn_more,
            Html.FROM_HTML_MODE_LEGACY
        )
        learnMore.text = Html.fromHtml(footerLinkStr)
    }

    if (canAssumeUdfps) {
        footerMessage6.visibility = View.VISIBLE
        iconShield.visibility = View.VISIBLE
    } else {
        footerMessage6.visibility = View.GONE
        iconShield.visibility = View.GONE
    }
    val glifLayoutHelper = GlifLayoutHelper(this, view)
    if (isBiometricUnlockDisabledByAdmin && !isParentalConsentRequired) {
        glifLayoutHelper.setHeaderText(
            R.string.security_settings_fingerprint_enroll_introduction_title_unlock_disabled
        )
        glifLayoutHelper.setDescriptionText(descriptionDisabledByAdminSupplier.get())
    } else {
        glifLayoutHelper.setHeaderText(
            R.string.security_settings_fingerprint_enroll_introduction_title
        )
        glifLayoutHelper.setDescriptionText(
            getString(
                R.string.security_settings_fingerprint_enroll_introduction_v3_message,
                DeviceHelper.getDeviceName(context)
            )
        )
    }

    view.findViewById<ScrollView>(com.google.android.setupdesign.R.id.sud_scroll_view)
        ?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
}
