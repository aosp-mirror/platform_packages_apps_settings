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

import android.content.Context;
import android.hardware.input.InputSettings;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleObserver;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.PrimarySwitchPreference;

public class KeyboardAccessibilityBounceKeysController extends
        InputSettingPreferenceController implements
        LifecycleObserver {
    public static final int BOUNCE_KEYS_THRESHOLD = 500;

    private AlertDialog mAlertDialog;
    @Nullable
    private PrimarySwitchPreference mPrimaryPreference;

    public KeyboardAccessibilityBounceKeysController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
        constructDialog(context);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPrimaryPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        return (super.getAvailabilityStatus() == AVAILABLE)
                && InputSettings.isAccessibilityBounceKeysFeatureEnabled() ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        if (mAlertDialog != null) {
            mAlertDialog.show();
        }
        return true;
    }

    @Override
    public boolean isChecked() {
        return InputSettings.isAccessibilityBounceKeysEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        InputSettings.setAccessibilityBounceKeysThreshold(mContext,
                isChecked ? BOUNCE_KEYS_THRESHOLD : 0);
        return true;
    }

    @Override
    protected void onInputSettingUpdated() {
        if (mPrimaryPreference != null) {
            mPrimaryPreference.setChecked(
                    InputSettings.isAccessibilityBounceKeysEnabled(mContext));
        }
    }

    @Override
    protected Uri getSettingUri() {
        return Settings.Secure.getUriFor(
                Settings.Secure.ACCESSIBILITY_BOUNCE_KEYS);
    }

    private void constructDialog(Context context) {
        mAlertDialog = new AlertDialog.Builder(context)
                .setView(R.layout.dialog_a11y_bounce_key)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                            RadioGroup radioGroup =
                                    mAlertDialog.findViewById(R.id.bounce_key_value_group);
                            int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                            int threshold = checkedRadioButtonId == R.id.bounce_key_value_600 ? 600
                                    : checkedRadioButtonId == R.id.bounce_key_value_400 ? 400
                                            : checkedRadioButtonId == R.id.bounce_key_value_200
                                                    ? 200 : 0;
                            InputSettings.setAccessibilityBounceKeysThreshold(context, threshold);
                        })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        mAlertDialog.setOnShowListener(dialog -> {
            RadioGroup radioGroup = mAlertDialog.findViewById(R.id.bounce_key_value_group);
            int bounceKeysThreshold = InputSettings.getAccessibilityBounceKeysThreshold(context);
            switch (bounceKeysThreshold) {
                case 600 -> radioGroup.check(R.id.bounce_key_value_600);
                case 400 -> radioGroup.check(R.id.bounce_key_value_400);
                default -> radioGroup.check(R.id.bounce_key_value_200);
            }
        });
    }
}
