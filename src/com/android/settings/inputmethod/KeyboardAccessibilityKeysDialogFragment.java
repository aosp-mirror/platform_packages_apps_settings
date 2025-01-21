/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public abstract class KeyboardAccessibilityKeysDialogFragment extends DialogFragment {
    private static final int CUSTOM_PROGRESS_INTERVAL = 100;
    private static final long MILLISECOND_IN_SECONDS = TimeUnit.SECONDS.toMillis(1);
    protected static final String EXTRA_TITLE_RES = "extra_title_res";
    protected static final String EXTRA_SUBTITLE_RES = "extra_subtitle_res";
    protected static final String EXTRA_SEEKBAR_CONTENT_DESCRIPTION =
            "extra_seekbar_content_description_res";

    protected final MetricsFeatureProvider mMetricsFeatureProvider;

    public KeyboardAccessibilityKeysDialogFragment() {
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    protected void updateInputSettingKeysValue(int thresholdTimeMillis) {
    }

    protected void onCustomValueUpdated(int thresholdTimeMillis) {
    }

    protected int getInputSettingKeysValue() {
        return 0;
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        int titleRes = getArguments().getInt(EXTRA_TITLE_RES);
        int subtitleRes = getArguments().getInt(EXTRA_SUBTITLE_RES);
        int seekbarContentDescriptionRes = getArguments().getInt(EXTRA_SEEKBAR_CONTENT_DESCRIPTION);

        Activity activity = getActivity();
        View dialoglayout =
                LayoutInflater.from(activity).inflate(
                        R.layout.dialog_keyboard_a11y_input_setting_keys, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setView(dialoglayout);
        dialogBuilder.setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                            RadioGroup radioGroup =
                                    dialoglayout.findViewById(
                                            R.id.input_setting_keys_value_group);
                            SeekBar seekbar = dialoglayout.findViewById(
                                    R.id.input_setting_keys_value_custom_slider);
                            RadioButton customRadioButton = dialoglayout.findViewById(
                                    R.id.input_setting_keys_value_custom);
                            int threshold;
                            if (customRadioButton.isChecked()) {
                                threshold = seekbar.getProgress() * CUSTOM_PROGRESS_INTERVAL;
                            } else {
                                int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                                if (checkedRadioButtonId == R.id.input_setting_keys_value_600) {
                                    threshold = 600;
                                } else if (checkedRadioButtonId
                                        == R.id.input_setting_keys_value_400) {
                                    threshold = 400;
                                } else if (checkedRadioButtonId
                                        == R.id.input_setting_keys_value_200) {
                                    threshold = 200;
                                } else {
                                    threshold = 0;
                                }
                            }
                            updateInputSettingKeysValue(threshold);
                            onCustomValueUpdated(threshold);
                        })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
        AlertDialog accessibilityKeyDialog = dialogBuilder.create();
        accessibilityKeyDialog.setOnShowListener(dialog -> {
            RadioGroup cannedValueRadioGroup = accessibilityKeyDialog.findViewById(
                    R.id.input_setting_keys_value_group);
            RadioButton customRadioButton = accessibilityKeyDialog.findViewById(
                    R.id.input_setting_keys_value_custom);
            TextView customValueTextView = accessibilityKeyDialog.findViewById(
                    R.id.input_setting_keys_value_custom_value);
            SeekBar customProgressBar = accessibilityKeyDialog.findViewById(
                    R.id.input_setting_keys_value_custom_slider);
            TextView titleTextView = accessibilityKeyDialog.findViewById(
                    R.id.input_setting_keys_dialog_title);
            TextView subTitleTextView = accessibilityKeyDialog.findViewById(
                    R.id.input_setting_keys_dialog_subtitle);
            titleTextView.setText(titleRes);
            subTitleTextView.setText(subtitleRes);

            if (seekbarContentDescriptionRes != 0) {
                customProgressBar.setContentDescription(
                        getContext().getString(seekbarContentDescriptionRes));
            }
            customProgressBar.incrementProgressBy(CUSTOM_PROGRESS_INTERVAL);
            customProgressBar.setProgress(1);
            View customValueView = accessibilityKeyDialog.findViewById(
                    R.id.input_setting_keys_custom_value_option);
            customValueView.setOnClickListener(l -> customRadioButton.performClick());
            customRadioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    cannedValueRadioGroup.clearCheck();
                }
                customValueTextView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                customValueTextView.setText(
                        progressToThresholdInSecond(customProgressBar.getProgress()));
                customProgressBar.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                buttonView.setChecked(isChecked);
            });
            cannedValueRadioGroup.setOnCheckedChangeListener(
                    (group, checkedId) -> customRadioButton.setChecked(false));
            customProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    String threshold = progressToThresholdInSecond(progress);
                    customValueTextView.setText(threshold);
                    customProgressBar.setContentDescription(threshold);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            initStateBasedOnThreshold(cannedValueRadioGroup, customRadioButton, customValueTextView,
                    customProgressBar);
        });

        final Window window = accessibilityKeyDialog.getWindow();
        window.setType(TYPE_SYSTEM_DIALOG);

        return accessibilityKeyDialog;
    }

    private String progressToThresholdInSecond(int progress) {
        return (double) progress * CUSTOM_PROGRESS_INTERVAL
                / MILLISECOND_IN_SECONDS + " " + TimeUnit.SECONDS.name().toLowerCase(
                Locale.getDefault());
    }

    private void initStateBasedOnThreshold(RadioGroup cannedValueRadioGroup,
            RadioButton customRadioButton, TextView customValueTextView,
            SeekBar customProgressBar) {
        int inputSettingKeysThreshold = getInputSettingKeysValue();
        switch (inputSettingKeysThreshold) {
            case 600 -> cannedValueRadioGroup.check(R.id.input_setting_keys_value_600);
            case 400 -> cannedValueRadioGroup.check(R.id.input_setting_keys_value_400);
            case 0, 200 -> cannedValueRadioGroup.check(R.id.input_setting_keys_value_200);
            default -> {
                customValueTextView.setText(
                        String.valueOf(
                                (double) inputSettingKeysThreshold / MILLISECOND_IN_SECONDS));
                customProgressBar.setProgress(inputSettingKeysThreshold / CUSTOM_PROGRESS_INTERVAL);
                customRadioButton.setChecked(true);
            }
        }
    }
}
