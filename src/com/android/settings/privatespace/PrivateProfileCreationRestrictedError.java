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

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

import java.util.regex.Pattern;

public class PrivateProfileCreationRestrictedError extends InstrumentedFragment {
    private static final String TAG = "PrivateSpaceCreationErr";

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        GlifLayout rootView =
                (GlifLayout)
                        inflater.inflate(R.layout.privatespace_creation_error, container, false);
        final FooterBarMixin mixin = rootView.getMixin(FooterBarMixin.class);
        mixin.setPrimaryButton(
                new FooterButton.Builder(getContext())
                        .setText(R.string.private_space_exit_label)
                        .setListener(onExit())
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
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
        rootView.setDescriptionText(R.string.private_space_error_description);
        TextView textView = rootView.getDescriptionTextView();
        Pattern pattern = Pattern.compile(getString(R.string.private_space_error_causes_text));
        Linkify.addLinks(
                textView,
                pattern,
                getString(R.string.private_space_learn_more_url));

        return rootView;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETUP_SPACE_CREATION_ERROR;
    }

    private View.OnClickListener onExit() {
        return v -> {
            Activity activity = getActivity();
            if (activity != null) {
                mMetricsFeatureProvider.action(
                        getContext(), SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_CANCEL_CREATE_SPACE);
                Log.i(TAG, "private space setup exited");
                activity.finish();
            }
        };
    }
}

