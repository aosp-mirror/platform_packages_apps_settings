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

import static android.view.View.OnClickListener;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.settings.R;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

/**
 * Fragment explaining the under-display fingerprint sensor location for fingerprint enrollment.
 * It interacts with Primary button, and LottieAnimationView.
 * <pre>
 | Has                 | UDFPS | SFPS | Other (Rear FPS) |
 |---------------------|-------|------|------------------|
 | Primary button      | Yes   | No   | No               |
 | Illustration Lottie | Yes   | Yes  | No               |
 | Animation           | No    | No   | Depend on layout |
 | Progress ViewModel  | No    | Yes  | Yes              |
 | Orientation detect  | No    | Yes  | No               |
 | Foldable detect     | No    | Yes  | No               |
 </pre>
 */
public class FingerprintEnrollFindUdfpsFragment extends Fragment {

    private FingerprintEnrollFindSensorViewModel mViewModel;

    private View mView;
    private GlifLayout mGlifLayout;
    private FooterBarMixin mFooterBarMixin;
    private final OnClickListener mOnSkipClickListener = (v) -> mViewModel.onSkipButtonClick();
    private final OnClickListener mOnStartClickListener = (v) -> mViewModel.onStartButtonClick();
    private LottieAnimationView mIllustrationLottie;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        final Context context = inflater.getContext();
        mView = inflater.inflate(R.layout.udfps_enroll_find_sensor_layout, container, false);
        mGlifLayout = mView.findViewById(R.id.setup_wizard_layout);
        mIllustrationLottie = mView.findViewById(R.id.illustration_lottie);
        mFooterBarMixin = mGlifLayout.getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(context)
                        .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );
        mFooterBarMixin.setPrimaryButton(
                new FooterButton.Builder(context)
                        .setText(R.string.security_settings_udfps_enroll_find_sensor_start_button)
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(R.style.SudGlifButton_Primary)
                        .build()
        );
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Activity activity = getActivity();
        final GlifLayoutHelper glifLayoutHelper = new GlifLayoutHelper(activity, mGlifLayout);
        glifLayoutHelper.setHeaderText(R.string.security_settings_udfps_enroll_find_sensor_title);
        glifLayoutHelper.setDescriptionText(
                getText(R.string.security_settings_udfps_enroll_find_sensor_message));
        mFooterBarMixin.getSecondaryButton().setOnClickListener(mOnSkipClickListener);
        mFooterBarMixin.getPrimaryButton().setOnClickListener(mOnStartClickListener);
        mIllustrationLottie.setOnClickListener(mOnStartClickListener);

        if (mViewModel.isAccessibilityEnabled()) {
            mIllustrationLottie.setAnimation(R.raw.udfps_edu_a11y_lottie);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        mViewModel = new ViewModelProvider(getActivity()).get(
                FingerprintEnrollFindSensorViewModel.class);
        super.onAttach(context);
    }
}
