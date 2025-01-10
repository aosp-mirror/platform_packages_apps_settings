/*
 * Copyright 2024 The Android Open Source Project
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

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.keyboard.Flags;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

/**
 * Abstract class for toggle controllers of Keyboard input setting related function.
 */
public abstract class InputSettingPreferenceController extends TogglePreferenceController implements
        LifecycleObserver {
    private static final int CUSTOM_PROGRESS_INTERVAL = 100;
    private static final long MILLISECOND_IN_SECONDS = TimeUnit.SECONDS.toMillis(1);
    private final ContentResolver mContentResolver;
    protected final MetricsFeatureProvider mMetricsFeatureProvider;
    private final ContentObserver mContentObserver = new ContentObserver(new Handler(true)) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (getSettingUri().equals(uri)) {
                onInputSettingUpdated();
            }
        }
    };
    protected AlertDialog mAlertDialog;

    protected abstract void onInputSettingUpdated();

    protected abstract Uri getSettingUri();

    protected void updateInputSettingKeysValue(int thresholdTimeMillis) {
    }

    protected int getInputSettingKeysValue() {
        return 0;
    }

    protected void onCustomValueUpdated(int thresholdTimeMillis) {
    }

    public InputSettingPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);
        refreshSummary(preference);
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.keyboardAndTouchpadA11yNewPageEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }

    /** Invoked when the panel is resumed. */
    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        registerSettingsObserver();
    }

    /** Invoked when the panel is paused. */
    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        unregisterSettingsObserver();
    }

    private void registerSettingsObserver() {
        unregisterSettingsObserver();
        mContentResolver.registerContentObserver(
                getSettingUri(),
                false,
                mContentObserver,
                UserHandle.myUserId());
        onInputSettingUpdated();
    }

    private void unregisterSettingsObserver() {
        mContentResolver.unregisterContentObserver(mContentObserver);
    }

    protected void constructDialog(Context context, int titleRes, int subtitleRes) {
        mAlertDialog = new AlertDialog.Builder(context)
                .setView(R.layout.dialog_keyboard_a11y_input_setting_keys)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                            RadioGroup radioGroup =
                                    mAlertDialog.findViewById(
                                            R.id.input_setting_keys_value_group);
                            SeekBar seekbar = mAlertDialog.findViewById(
                                    R.id.input_setting_keys_value_custom_slider);
                            RadioButton customRadioButton = mAlertDialog.findViewById(
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
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        mAlertDialog.setOnShowListener(dialog -> {
            RadioGroup cannedValueRadioGroup = mAlertDialog.findViewById(
                    R.id.input_setting_keys_value_group);
            RadioButton customRadioButton = mAlertDialog.findViewById(
                    R.id.input_setting_keys_value_custom);
            TextView customValueTextView = mAlertDialog.findViewById(
                    R.id.input_setting_keys_value_custom_value);
            SeekBar customProgressBar = mAlertDialog.findViewById(
                    R.id.input_setting_keys_value_custom_slider);
            TextView titleTextView = mAlertDialog.findViewById(
                    R.id.input_setting_keys_dialog_title);
            TextView subTitleTextView = mAlertDialog.findViewById(
                    R.id.input_setting_keys_dialog_subtitle);
            titleTextView.setText(titleRes);
            subTitleTextView.setText(subtitleRes);

            customProgressBar.incrementProgressBy(CUSTOM_PROGRESS_INTERVAL);
            customProgressBar.setProgress(1);
            View customValueView = mAlertDialog.findViewById(
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
                    customValueTextView.setText(progressToThresholdInSecond(progress));
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
    }

    private static String progressToThresholdInSecond(int progress) {
        return NumberFormat.getInstance().format((float) progress * CUSTOM_PROGRESS_INTERVAL
                / MILLISECOND_IN_SECONDS);
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
