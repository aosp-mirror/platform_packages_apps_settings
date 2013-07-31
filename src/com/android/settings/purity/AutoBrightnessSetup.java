/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.purity;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

public class AutoBrightnessSetup extends AlertActivity implements
        DialogInterface.OnClickListener {
    private static final String STATE_TWILIGHT = "AutoBrightnessSetup:TwilightAdjustment";
    private static final String STATE_SENSITIVITY = "AutoBrightnessSetup:SensitivitySelection";
    private static final String STATE_ADJUSTMENT = "AutoBrightnessSetup:AdjustmentValue";
    private static final String STATE_CUSTOMIZE_SHOWN = "AutoBrightnessSetup:CustomizeDialogShown";
    private static final String STATE_CUSTOMIZE_STATE = "AutoBrightnessSetup:CustomizeDialogState";

    private AutoBrightnessCustomizeDialog mCustomizeDialog;
    private CheckBox mTwilightAdjustment;
    private Spinner mSensitivity;
    private SeekBar mAdjustment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final View view = getLayoutInflater().inflate(R.layout.dialog_auto_brightness, null);
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = R.drawable.ic_appwidget_settings_brightness_auto_holo;
        p.mTitle = getString(R.string.auto_brightness_setup_title);
        p.mView = view;
        p.mPositiveButtonText = getString(android.R.string.ok);
        p.mPositiveButtonListener = this;
        p.mNeutralButtonText = getString(R.string.auto_brightness_adjust_button);
        p.mNeutralButtonListener = this;
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mNegativeButtonListener = this;

        setupAlert();

        mTwilightAdjustment = (CheckBox) view.findViewById(R.id.twilight_adjustment);
        mSensitivity = (Spinner) view.findViewById(R.id.sensitivity);
        mAdjustment = (SeekBar) view.findViewById(R.id.adjustment);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.auto_brightness_sensitivity_entries,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSensitivity.setAdapter(adapter);

        Button adjustButton = mAlert.getButton(BUTTON_NEUTRAL);
        adjustButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomizeDialog(null);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCustomizeDialog != null) {
            mCustomizeDialog.dismiss();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        final ContentResolver resolver = getContentResolver();
        float currentSensitivity = Settings.System.getFloat(resolver,
                Settings.System.AUTO_BRIGHTNESS_RESPONSIVENESS, 1.0f);
        int currentSensitivityInt = (int) (currentSensitivity * 100);
        int[] sensitivityValues = getResources().getIntArray(
                R.array.auto_brightness_sensitivity_values);

        for (int i = 0; i < sensitivityValues.length; i++) {
            if (sensitivityValues[i] == currentSensitivityInt) {
                mSensitivity.setSelection(i);
                break;
            }
        }

        float adjustmentValue = Settings.System.getFloat(resolver,
                Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, 0.0f);
        // valid range is -1.0..1.0, but we clamp the extreme values
        adjustmentValue = Math.min(Math.max(adjustmentValue, -0.5f), 0.5f);
        mAdjustment.setProgress((int) ((adjustmentValue + 0.5f) * mAdjustment.getMax()));

        mTwilightAdjustment.setChecked(Settings.System.getInt(resolver,
                Settings.System.AUTO_BRIGHTNESS_TWILIGHT_ADJUSTMENT, 0) != 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        boolean customizeDialogShown = mCustomizeDialog != null && mCustomizeDialog.isShowing();
        state.putBoolean(STATE_CUSTOMIZE_SHOWN, customizeDialogShown);
        if (customizeDialogShown) {
            state.putBundle(STATE_CUSTOMIZE_STATE, mCustomizeDialog.onSaveInstanceState());
        }
        state.putInt(STATE_SENSITIVITY, mSensitivity.getSelectedItemPosition());
        state.putInt(STATE_ADJUSTMENT, mAdjustment.getProgress());
        state.putBoolean(STATE_TWILIGHT, mTwilightAdjustment.isChecked());
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);

        if (state.getBoolean(STATE_CUSTOMIZE_SHOWN)) {
            Bundle dialogState = state.getBundle(STATE_CUSTOMIZE_STATE);
            showCustomizeDialog(dialogState);
        }
        mSensitivity.setSelection(state.getInt(STATE_SENSITIVITY));
        mAdjustment.setProgress(state.getInt(STATE_ADJUSTMENT));
        mTwilightAdjustment.setChecked(state.getBoolean(STATE_TWILIGHT));
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which != DialogInterface.BUTTON_POSITIVE) {
            return;
        }

        final ContentResolver resolver = getContentResolver();
        int selection = mSensitivity.getSelectedItemPosition();
        if (selection >= 0) {
            int[] sensitivityValues = getResources().getIntArray(
                    R.array.auto_brightness_sensitivity_values);
            float sensitivity = 0.01f * sensitivityValues[selection];

            Settings.System.putFloat(resolver,
                    Settings.System.AUTO_BRIGHTNESS_RESPONSIVENESS, sensitivity);
        }

        float adjustmentValue =
                ((float) mAdjustment.getProgress() / (float) mAdjustment.getMax()) - 0.5f;
        Settings.System.putFloat(resolver,
                Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, adjustmentValue);

        Settings.System.putInt(resolver,
                Settings.System.AUTO_BRIGHTNESS_TWILIGHT_ADJUSTMENT,
                mTwilightAdjustment.isChecked() ? 1 : 0);
    }

    private void showCustomizeDialog(Bundle state) {
        if (mCustomizeDialog != null && mCustomizeDialog.isShowing()) {
            return;
        }

        mCustomizeDialog = new AutoBrightnessCustomizeDialog(this);
        if (state != null) {
            mCustomizeDialog.onRestoreInstanceState(state);
        }
        mCustomizeDialog.show();
    }
}
