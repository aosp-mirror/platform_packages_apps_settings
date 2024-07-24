/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.password;

import static android.app.admin.DevicePolicyResources.Strings.Settings.FORGOT_PASSWORD_TEXT;
import static android.app.admin.DevicePolicyResources.Strings.Settings.FORGOT_PASSWORD_TITLE;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.widget.TextView;

import com.android.settings.R;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.util.ContentStyler;
import com.google.android.setupdesign.util.ThemeHelper;

/**
 * An activity that asks the user to contact their admin to get assistance with forgotten password.
 */
public class ForgotPasswordActivity extends Activity {
    public static final String TAG = ForgotPasswordActivity.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int userId = getIntent().getIntExtra(Intent.EXTRA_USER_ID, -1);
        if (userId < 0) {
            Log.e(TAG, "No valid userId supplied, exiting");
            finish();
            return;
        }
        ThemeHelper.trySetDynamicColor(this);
        setContentView(R.layout.forgot_password_activity);

        DevicePolicyManager devicePolicyManager = getSystemService(DevicePolicyManager.class);
        TextView forgotPasswordText = (TextView) findViewById(R.id.forgot_password_text);
        forgotPasswordText.setText(devicePolicyManager.getResources().getString(
                FORGOT_PASSWORD_TEXT, () -> getString(R.string.forgot_password_text)));

        final GlifLayout layout = findViewById(R.id.setup_wizard_layout);
        layout.getMixin(FooterBarMixin.class).setPrimaryButton(
                new FooterButton.Builder(this)
                        .setText(android.R.string.ok)
                        .setListener(v -> finish())
                        .setButtonType(FooterButton.ButtonType.DONE)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build()
        );

        if (ThemeHelper.shouldApplyMaterialYouStyle(this)) {
            ContentStyler.applyBodyPartnerCustomizationStyle(
                    layout.findViewById(R.id.forgot_password_text));
        }

        layout.setHeaderText(devicePolicyManager.getResources().getString(
                FORGOT_PASSWORD_TITLE, () -> getString(R.string.forgot_password_title)));

        UserManager.get(this).requestQuietModeEnabled(
                false, UserHandle.of(userId), UserManager.QUIET_MODE_DISABLE_DONT_ASK_CREDENTIAL);
    }
}
