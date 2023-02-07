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
import androidx.fragment.app.FragmentActivity;
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

    private static final String TAG = FingerprintEnrollFinishFragment.class.getSimpleName();

    private FingerprintEnrollFinishViewModel mFingerprintEnrollFinishViewModel;
    private boolean mCanAssumeSfps;

    private View mView;
    private FooterBarMixin mFooterBarMixin;

    private final View.OnClickListener mAddButtonClickListener =
            (v) -> mFingerprintEnrollFinishViewModel.onAddButtonClick();
    private final View.OnClickListener mNextButtonClickListener =
            (v) -> mFingerprintEnrollFinishViewModel.onNextButtonClick();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        final FragmentActivity activity = getActivity();
        final ViewModelProvider provider = new ViewModelProvider(activity);
        mFingerprintEnrollFinishViewModel = provider.get(FingerprintEnrollFinishViewModel.class);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCanAssumeSfps = mFingerprintEnrollFinishViewModel.canAssumeSfps();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        if (mCanAssumeSfps) {
            mView = inflater.inflate(R.layout.sfps_enroll_finish, container, false);
        } else {
            mView = inflater.inflate(R.layout.fingerprint_enroll_finish, container, false);
        }

        final Activity activity = getActivity();
        final GlifLayoutHelper glifLayoutHelper = new GlifLayoutHelper(activity,
                (GlifLayout) mView);

        glifLayoutHelper.setHeaderText(R.string.security_settings_fingerprint_enroll_finish_title);
        glifLayoutHelper.setDescriptionText(getString(
                R.string.security_settings_fingerprint_enroll_finish_v2_message));

        final int maxEnrollments = mFingerprintEnrollFinishViewModel.getMaxFingerprints();
        final int enrolled = mFingerprintEnrollFinishViewModel.getNumOfEnrolledFingerprintsSize();
        if (mCanAssumeSfps) {
            if (enrolled < maxEnrollments) {
                glifLayoutHelper.setDescriptionText(getString(R.string
                        .security_settings_fingerprint_enroll_finish_v2_add_fingerprint_message));
            }
        }

        mFooterBarMixin = ((GlifLayout) mView).getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(getActivity())
                        .setText(R.string.fingerprint_enroll_button_add)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );

        mFooterBarMixin.setPrimaryButton(
                new FooterButton.Builder(getActivity())
                        .setText(R.string.security_settings_fingerprint_enroll_done)
                        .setListener(mNextButtonClickListener)
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(R.style.SudGlifButton_Primary)
                        .build()
        );

        FooterButton addButton = mFooterBarMixin.getSecondaryButton();
        if (enrolled >= maxEnrollments) {
            addButton.setVisibility(View.INVISIBLE);
        } else {
            addButton.setOnClickListener(mAddButtonClickListener);
        }

        return mView;
    }

}
