/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.android.settings.privatespace.PrivateSpaceSetupActivity.ACCOUNT_LOGIN_ACTION;
import static com.android.settings.privatespace.PrivateSpaceSetupActivity.EXTRA_ACTION_TYPE;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

/** Fragment for GAIA education screen */
public class PrivateSpaceGaiaEducationFragment extends InstrumentedFragment {
    private static final String TAG = "PrivateSpaceGaiaEduFrag";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (android.os.Flags.allowPrivateProfile()) {
            super.onCreate(savedInstanceState);
        }
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        GlifLayout rootView =
                (GlifLayout)
                        inflater.inflate(
                                R.layout.private_space_gaia_education_screen, container, false);
        final FooterBarMixin mixin = rootView.getMixin(FooterBarMixin.class);
        mixin.setPrimaryButton(
                new FooterButton.Builder(getContext())
                        .setText(R.string.private_space_gaia_education_got_it)
                        .setListener(onStartLogin())
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build());
        mixin.setSecondaryButton(
                new FooterButton.Builder(getContext())
                        .setText(R.string.skip_label)
                        .setListener(onSkip())
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
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
        return METRICS_CATEGORY_UNKNOWN;
    }

    private View.OnClickListener onSkip() {
        return v -> {
            NavHostFragment.findNavController(PrivateSpaceGaiaEducationFragment.this)
                    .navigate(R.id.action_account_lock_fragment);
        };
    }

    private View.OnClickListener onStartLogin() {
        return v -> {
            startAccountLogin();
        };
    }

    /** Start new activity in private profile to add an account to private profile */
    private void startAccountLogin() {
        UserHandle userHandle =
                PrivateSpaceMaintainer.getInstance(getActivity()).getPrivateProfileHandle();
        if (userHandle != null) {
            Intent intent = new Intent(getContext(), PrivateProfileContextHelperActivity.class);
            intent.putExtra(EXTRA_ACTION_TYPE, ACCOUNT_LOGIN_ACTION);
            mMetricsFeatureProvider.action(
                    getContext(), SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_ACCOUNT_LOGIN_START);
            getActivity().startActivityForResultAsUser(intent, ACCOUNT_LOGIN_ACTION, userHandle);
        } else {
            Log.w(TAG, "Private profile user handle is null");
        }
    }
}
