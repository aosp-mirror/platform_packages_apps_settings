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
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settingslib.widget.LottieColorUtils;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

import java.util.regex.Pattern;

/** Fragment educating about the usage of Private Space. */
public class PrivateSpaceEducation extends InstrumentedFragment {
    private static final String TAG = "PrivateSpaceEducation";

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        if (!android.os.Flags.allowPrivateProfile()
                || !android.multiuser.Flags.enablePrivateSpaceFeatures()) {
            return null;
        }
        GlifLayout rootView =
                (GlifLayout)
                        inflater.inflate(R.layout.private_space_education_screen, container, false);
        final FooterBarMixin mixin = rootView.getMixin(FooterBarMixin.class);
        mixin.setPrimaryButton(
                new FooterButton.Builder(getContext())
                        .setText(R.string.private_space_setup_button_label)
                        .setListener(onSetup())
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build());
        mixin.setSecondaryButton(
                new FooterButton.Builder(getContext())
                        .setText(R.string.private_space_cancel_label)
                        .setListener(onCancel())
                        .setButtonType(FooterButton.ButtonType.CANCEL)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                        .build());
        LottieAnimationView lottieAnimationView = rootView.findViewById(R.id.lottie_animation);
        LottieColorUtils.applyDynamicColors(getContext(), lottieAnimationView);

        TextView infoTextView = rootView.findViewById(R.id.learn_more);
        Pattern pattern = Pattern.compile(infoTextView.getText().toString());
        Linkify.addLinks(
                infoTextView,
                pattern,
                getContext().getString(R.string.private_space_learn_more_url));

        return rootView;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETUP_EDUCATION;
    }

    private View.OnClickListener onSetup() {
        return v -> {
            mMetricsFeatureProvider.action(
                    getContext(), SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_START);
            Log.i(TAG, "Starting private space setup");
            NavHostFragment.findNavController(PrivateSpaceEducation.this)
                    .navigate(R.id.action_education_to_create);
        };
    }

    private View.OnClickListener onCancel() {
        return v -> {
            Activity activity = getActivity();
            if (activity != null) {
                mMetricsFeatureProvider.action(
                        getContext(), SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_CANCEL);
                Log.i(TAG, "private space setup cancelled");
                activity.finish();
            }
        };
    }
}
