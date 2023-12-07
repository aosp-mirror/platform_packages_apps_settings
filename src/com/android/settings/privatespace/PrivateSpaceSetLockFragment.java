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
import static com.android.settings.privatespace.PrivateSpaceSetupActivity.SET_LOCK_ACTION;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

/**
 * Fragment that provides an option to user to choose between the existing screen lock or set a
 * separate private profile lock.
 */
public class PrivateSpaceSetLockFragment extends InstrumentedFragment {
    private static final String TAG = "PrivateSpaceSetLockFrag";

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        if (!android.os.Flags.allowPrivateProfile()) {
            return null;
        }
        GlifLayout rootView =
                (GlifLayout) inflater.inflate(
                        R.layout.privatespace_setlock_screen, container, false);
        final FooterBarMixin mixin = rootView.getMixin(FooterBarMixin.class);
        mixin.setPrimaryButton(
                new FooterButton.Builder(getContext())
                        .setText(R.string.private_space_use_screenlock_label)
                        .setListener(onClickUse())
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build());
        mixin.setSecondaryButton(
                new FooterButton.Builder(getContext())
                        .setText(R.string.private_space_set_lock_label)
                        .setListener(onClickNewLock())
                        .setButtonType(FooterButton.ButtonType.NEXT)
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
        return SettingsEnums.PRIVATE_SPACE_SETUP_LOCK;
    }

    private View.OnClickListener onClickUse() {
        return v -> {
            mMetricsFeatureProvider.action(
                    getContext(), SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_USE_SCREEN_LOCK);
            // Simply Use default screen lock. No need to handle
            launchActivityForAction(ACCOUNT_LOGIN_ACTION);
        };
    }

    private View.OnClickListener onClickNewLock() {
        return v -> {
            mMetricsFeatureProvider.action(
                    getContext(), SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_NEW_LOCK);
            launchActivityForAction(SET_LOCK_ACTION);
        };
    }

    private void launchActivityForAction(int action) {
        UserHandle userHandle =
                PrivateSpaceMaintainer.getInstance(getActivity()).getPrivateProfileHandle();
        if (userHandle != null) {
            Intent intent = new Intent(getContext(), PrivateProfileContextHelperActivity.class);
            intent.putExtra(EXTRA_ACTION_TYPE, action);
            getActivity().startActivityForResultAsUser(intent, action, userHandle);
        } else {
            Log.w(TAG, "Private profile user handle is null");
        }
    }
}
