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

package com.android.settings.biometrics2.ui.view;

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
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFinishViewModel;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

/**
 * Fragment which concludes fingerprint enrollment.
 */
public class FingerprintEnrollFinishFragment extends Fragment {

    private FingerprintEnrollFinishViewModel mViewModel;

    private final View.OnClickListener mAddButtonClickListener =
            (v) -> mViewModel.onAddButtonClick();
    private final View.OnClickListener mNextButtonClickListener =
            (v) -> mViewModel.onNextButtonClick();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        final ViewModelProvider provider = new ViewModelProvider(getActivity());
        mViewModel = provider.get(FingerprintEnrollFinishViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        GlifLayout view = (GlifLayout) inflater.inflate(
                mViewModel.canAssumeSfps()
                        ? R.layout.sfps_enroll_finish
                        : R.layout.fingerprint_enroll_finish,
                container,
                false);

        final Activity activity = getActivity();
        final GlifLayoutHelper glifLayoutHelper = new GlifLayoutHelper(activity, view);

        glifLayoutHelper.setHeaderText(R.string.security_settings_fingerprint_enroll_finish_title);
        if (mViewModel.canAssumeSfps() && mViewModel.isAnotherFingerprintEnrollable()) {
            glifLayoutHelper.setDescriptionText(getString(R.string
                    .security_settings_fingerprint_enroll_finish_v2_add_fingerprint_message));
        } else {
            glifLayoutHelper.setDescriptionText(getString(
                    R.string.security_settings_fingerprint_enroll_finish_v2_message));
        }

        final FooterBarMixin footerBarMixin = view.getMixin(FooterBarMixin.class);
        footerBarMixin.setPrimaryButton(
                new FooterButton.Builder(activity)
                        .setText(mViewModel.getRequest().isSuw()
                                ? R.string.next_label
                                : R.string.security_settings_fingerprint_enroll_done)
                        .setListener(mNextButtonClickListener)
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(R.style.SudGlifButton_Primary)
                        .build()
        );
        if (mViewModel.isAnotherFingerprintEnrollable()) {
            footerBarMixin.setSecondaryButton(new FooterButton.Builder(activity)
                    .setText(R.string.fingerprint_enroll_button_add)
                    .setListener(mAddButtonClickListener)
                    .setButtonType(FooterButton.ButtonType.SKIP)
                    .setTheme(R.style.SudGlifButton_Secondary)
                    .build());
        }

        return view;
    }
}
