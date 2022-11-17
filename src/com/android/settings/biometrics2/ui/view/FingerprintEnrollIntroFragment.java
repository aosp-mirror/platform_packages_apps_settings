/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics2.ui.view;

import static android.app.admin.DevicePolicyResources.Strings.Settings.FINGERPRINT_UNLOCK_DISABLED;

import static com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus.FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX;
import static com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus.FINGERPRINT_ENROLLABLE_OK;
import static com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus.FINGERPRINT_ENROLLABLE_UNKNOWN;

import static com.google.android.setupdesign.util.DynamicColorPalette.ColorType.ACCENT;

import android.app.Activity;
import android.app.admin.DevicePolicyResourcesManager;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.settings.R;
import com.android.settings.biometrics2.ui.model.FingerprintEnrollIntroStatus;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollIntroViewModel;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.template.RequireScrollMixin;
import com.google.android.setupdesign.util.DynamicColorPalette;

/**
 * Fingerprint intro onboarding page fragment implementation
 */
public class FingerprintEnrollIntroFragment extends Fragment {

    private static final String TAG = "FingerprintEnrollIntroFragment";

    @NonNull private final ViewModelProvider mViewModelProvider;
    @Nullable private final DevicePolicyResourcesManager mDevicePolicyMgrRes;

    private FingerprintEnrollIntroViewModel mViewModel = null;

    private View mView = null;
    private FooterButton mPrimaryFooterButton = null;
    private FooterButton mSecondaryFooterButton = null;
    private ImageView mIconShield = null;
    private TextView mFooterMessage6 = null;
    @Nullable private PorterDuffColorFilter mIconColorFilter;

    public FingerprintEnrollIntroFragment(
            @NonNull ViewModelProvider viewModelProvider,
            @Nullable DevicePolicyResourcesManager devicePolicyMgrRes) {
        super();
        mViewModelProvider = viewModelProvider;
        mDevicePolicyMgrRes = devicePolicyMgrRes;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        final Context context = inflater.getContext();
        mView = inflater.inflate(R.layout.fingerprint_enroll_introduction, container, false);

        final ImageView iconFingerprint = mView.findViewById(R.id.icon_fingerprint);
        final ImageView iconDeviceLocked = mView.findViewById(R.id.icon_device_locked);
        final ImageView iconTrashCan = mView.findViewById(R.id.icon_trash_can);
        final ImageView iconInfo = mView.findViewById(R.id.icon_info);
        mIconShield = mView.findViewById(R.id.icon_shield);
        final ImageView iconLink = mView.findViewById(R.id.icon_link);
        iconFingerprint.getDrawable().setColorFilter(getIconColorFilter(context));
        iconDeviceLocked.getDrawable().setColorFilter(getIconColorFilter(context));
        iconTrashCan.getDrawable().setColorFilter(getIconColorFilter(context));
        iconInfo.getDrawable().setColorFilter(getIconColorFilter(context));
        mIconShield.getDrawable().setColorFilter(getIconColorFilter(context));
        iconLink.getDrawable().setColorFilter(getIconColorFilter(context));

        final TextView footerMessage2 = mView.findViewById(R.id.footer_message_2);
        final TextView footerMessage3 = mView.findViewById(R.id.footer_message_3);
        final TextView footerMessage4 = mView.findViewById(R.id.footer_message_4);
        final TextView footerMessage5 = mView.findViewById(R.id.footer_message_5);
        mFooterMessage6 = mView.findViewById(R.id.footer_message_6);
        footerMessage2.setText(
                R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_2);
        footerMessage3.setText(
                R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_3);
        footerMessage4.setText(
                R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_4);
        footerMessage5.setText(
                R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_5);
        mFooterMessage6.setText(
                R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_6);

        final TextView footerTitle1 = mView.findViewById(R.id.footer_title_1);
        final TextView footerTitle2 = mView.findViewById(R.id.footer_title_2);
        footerTitle1.setText(
                R.string.security_settings_fingerprint_enroll_introduction_footer_title_1);
        footerTitle2.setText(
                R.string.security_settings_fingerprint_enroll_introduction_footer_title_2);

        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Context context = view.getContext();

        final TextView footerLink = mView.findViewById(R.id.footer_learn_more);
        footerLink.setMovementMethod(LinkMovementMethod.getInstance());
        final String footerLinkStr = getContext().getString(
                R.string.security_settings_fingerprint_v2_enroll_introduction_message_learn_more,
                Html.FROM_HTML_MODE_LEGACY);
        footerLink.setText(Html.fromHtml(footerLinkStr));

        // footer buttons
        mPrimaryFooterButton = new FooterButton.Builder(context)
                .setText(R.string.security_settings_fingerprint_enroll_introduction_agree)
                .setListener(mViewModel::onNextButtonClick)
                .setButtonType(FooterButton.ButtonType.OPT_IN)
                .setTheme(R.style.SudGlifButton_Primary)
                .build();
        mSecondaryFooterButton = new FooterButton.Builder(context)
                .setListener(mViewModel::onSkipOrCancelButtonClick)
                .setButtonType(FooterButton.ButtonType.NEXT)
                .setTheme(R.style.SudGlifButton_Primary)
                .build();
        getFooterBarMixin().setPrimaryButton(mPrimaryFooterButton);
        getFooterBarMixin().setSecondaryButton(mSecondaryFooterButton, true /* usePrimaryStyle */);

        if (mViewModel.canAssumeUdfps()) {
            mFooterMessage6.setVisibility(View.VISIBLE);
            mIconShield.setVisibility(View.VISIBLE);
        } else {
            mFooterMessage6.setVisibility(View.GONE);
            mIconShield.setVisibility(View.GONE);
        }
        mSecondaryFooterButton.setText(getContext(),
                mViewModel.getEnrollmentRequest().isAfterSuwOrSuwSuggestedAction()
                ? R.string.security_settings_fingerprint_enroll_introduction_cancel
                : R.string.security_settings_fingerprint_enroll_introduction_no_thanks);

        if (mViewModel.isBiometricUnlockDisabledByAdmin()
                && !mViewModel.isParentalConsentRequired()) {
            setHeaderText(
                    getActivity(),
                    R.string.security_settings_fingerprint_enroll_introduction_title_unlock_disabled
            );
            getLayout().setDescriptionText(getDescriptionDisabledByAdmin(context));
        } else {
            setHeaderText(getActivity(),
                    R.string.security_settings_fingerprint_enroll_introduction_title);
        }

        mViewModel.getPageStatusLiveData().observe(this, this::updateFooterButtons);

        final RequireScrollMixin requireScrollMixin = getLayout()
                .getMixin(RequireScrollMixin.class);
        requireScrollMixin.requireScrollWithButton(getActivity(), mPrimaryFooterButton,
                getMoreButtonTextRes(), mViewModel::onNextButtonClick);
        requireScrollMixin.setOnRequireScrollStateChangedListener(scrollNeeded -> {
            if (!scrollNeeded) {
                mViewModel.setHasScrolledToBottom();
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        mViewModel = mViewModelProvider.get(FingerprintEnrollIntroViewModel.class);
        getLifecycle().addObserver(mViewModel);
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        getLifecycle().removeObserver(mViewModel);
        super.onDetach();
    }

    @NonNull
    private PorterDuffColorFilter getIconColorFilter(@NonNull Context context) {
        if (mIconColorFilter == null) {
            mIconColorFilter = new PorterDuffColorFilter(
                    DynamicColorPalette.getColor(context, ACCENT),
                    PorterDuff.Mode.SRC_IN);
        }
        return mIconColorFilter;
    }

    private GlifLayout getLayout() {
        return mView.findViewById(R.id.setup_wizard_layout);
    }

    @NonNull
    private FooterBarMixin getFooterBarMixin() {
        final GlifLayout layout = getLayout();
        return layout.getMixin(FooterBarMixin.class);
    }

    @NonNull
    private String getDescriptionDisabledByAdmin(@NonNull Context context) {
        final int defaultStrId =
                R.string.security_settings_fingerprint_enroll_introduction_message_unlock_disabled;
        if (mDevicePolicyMgrRes == null) {
            Log.w(TAG, "getDescriptionDisabledByAdmin, null device policy manager res");
            return "";
        }
        return mDevicePolicyMgrRes.getString(FINGERPRINT_UNLOCK_DISABLED,
                () -> context.getString(defaultStrId));
    }

    private void setHeaderText(@NonNull Activity activity, int resId) {
        TextView layoutTitle = getLayout().getHeaderTextView();
        CharSequence previousTitle = layoutTitle.getText();
        CharSequence title = activity.getText(resId);
        if (previousTitle != title) {
            if (!TextUtils.isEmpty(previousTitle)) {
                layoutTitle.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            }
            getLayout().setHeaderText(title);
            getLayout().getHeaderTextView().setContentDescription(title);
            activity.setTitle(title);
        }
        getLayout().getHeaderTextView().setContentDescription(activity.getText(resId));
    }

    void updateFooterButtons(@NonNull FingerprintEnrollIntroStatus status) {
        @StringRes final int scrollToBottomPrimaryResId =
                status.getEnrollableStatus() == FINGERPRINT_ENROLLABLE_OK
                        ? R.string.security_settings_fingerprint_enroll_introduction_agree
                        : R.string.done;

        mPrimaryFooterButton.setText(getContext(),
                status.hasScrollToBottom() ? scrollToBottomPrimaryResId : getMoreButtonTextRes());
        mSecondaryFooterButton.setVisibility(
                status.hasScrollToBottom() ? View.VISIBLE : View.INVISIBLE);

        final TextView errorTextView = mView.findViewById(R.id.error_text);
        switch (status.getEnrollableStatus()) {
            case FINGERPRINT_ENROLLABLE_OK:
                errorTextView.setText(null);
                errorTextView.setVisibility(View.GONE);
                break;
            case FINGERPRINT_ENROLLABLE_ERROR_REACH_MAX:
                errorTextView.setText(R.string.fingerprint_intro_error_max);
                errorTextView.setVisibility(View.VISIBLE);
                break;
            case FINGERPRINT_ENROLLABLE_UNKNOWN:
                // default case, do nothing.
        }
    }

    @StringRes
    private int getMoreButtonTextRes() {
        return R.string.security_settings_face_enroll_introduction_more;
    }
}
