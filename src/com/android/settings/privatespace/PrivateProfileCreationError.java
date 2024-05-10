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

package com.android.settings.privatespace;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

/** Fragment to display error screen if creation of private profile failed for any reason. */
public class PrivateProfileCreationError extends InstrumentedFragment {
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        GlifLayout rootView =
                (GlifLayout)
                        inflater.inflate(R.layout.privatespace_creation_error, container, false);
        final FooterBarMixin mixin = rootView.getMixin(FooterBarMixin.class);
        mixin.setPrimaryButton(
                new FooterButton.Builder(getContext())
                        .setText(R.string.private_space_tryagain_label)
                        .setListener(onTryAgain())
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build());
        mixin.setSecondaryButton(
                new FooterButton.Builder(getContext())
                        .setText(R.string.private_space_cancel_label)
                        .setListener(onCancel())
                        .setButtonType(FooterButton.ButtonType.CANCEL)
                        .setTheme(
                                androidx.appcompat.R.style
                                        .Base_TextAppearance_AppCompat_Widget_Button)
                        .build());
        OnBackPressedCallback callback =
                new OnBackPressedCallback(true /* enabled by default */) {
                    @Override
                    public void handleOnBackPressed() {
                        // Handle the back button event. We intentionally don't want to allow back
                        // button to work in this screen during the setup flow.
                    }
                };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);

        return rootView;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETUP_SPACE_CREATION_ERROR;
    }

    private View.OnClickListener onTryAgain() {
        return v -> {
            mMetricsFeatureProvider.action(
                    getContext(), SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_TRY_CREATE_SPACE_AGAIN);
            NavHostFragment.findNavController(PrivateProfileCreationError.this)
                    .navigate(R.id.action_retry_profile_creation);
        };
    }

    private View.OnClickListener onCancel() {
        return v -> {
            Activity activity = getActivity();
            if (activity != null) {
                mMetricsFeatureProvider.action(
                        getContext(), SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_CANCEL_CREATE_SPACE);
                activity.finish();
            }
        };
    }
}
