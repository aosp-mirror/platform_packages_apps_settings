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

import static com.android.settings.privatespace.PrivateSpaceSetupActivity.ACCOUNT_LOGIN_ACTION;
import static com.android.settings.privatespace.PrivateSpaceSetupActivity.EXTRA_ACTION_TYPE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.settings.R;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

/** Fragment to display error screen if the profile is not signed in with a Google account. */
public class PrivateSpaceAccountLoginError extends Fragment {
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        GlifLayout rootView =
                (GlifLayout) inflater
                        .inflate(R.layout.privatespace_account_login_error, container, false);
        final FooterBarMixin mixin = rootView.getMixin(FooterBarMixin.class);
        mixin.setPrimaryButton(
                new FooterButton.Builder(getContext())
                        .setText(R.string.privatespace_tryagain_label)
                        .setListener(nextScreen())
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build());
        OnBackPressedCallback callback =
                new OnBackPressedCallback(true /* enabled by default */) {
                    @Override
                    public void handleOnBackPressed() {
                        // Handle the back button event
                    }
                };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);

        return rootView;
    }

    @SuppressLint("MissingPermission")
    private View.OnClickListener nextScreen() {
        return v -> {
            PrivateSpaceMaintainer privateSpaceMaintainer = PrivateSpaceMaintainer
                    .getInstance(getActivity());
            UserHandle userHandle;
            if (privateSpaceMaintainer.doesPrivateSpaceExist() && (userHandle =
                    privateSpaceMaintainer.getPrivateProfileHandle()) != null) {
                Intent intent = new Intent(getContext(), PrivateProfileContextHelperActivity.class);
                intent.putExtra(EXTRA_ACTION_TYPE, ACCOUNT_LOGIN_ACTION);
                getActivity().startActivityForResultAsUser(intent, ACCOUNT_LOGIN_ACTION,
                        userHandle);
            }
        };
    }
}
