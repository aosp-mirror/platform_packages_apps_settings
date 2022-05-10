/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.security;

import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import android.annotation.Nullable;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.security.Credentials;
import android.view.View;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SetupWizardUtils;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.util.ThemeHelper;

/**
 * Creates a warning dialog explaining the consequences of installing a CA certificate
 * This is displayed before a CA certificate can be installed from Settings.
 */
public class InstallCaCertificateWarning extends Activity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        ThemeHelper.trySetDynamicColor(this);
        setContentView(R.layout.ca_certificate_warning_dialog);
        getWindow().addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);

        final GlifLayout layout = findViewById(R.id.setup_wizard_layout);
        layout.setHeaderText(R.string.ca_certificate_warning_title);

        final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
        mixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.certificate_warning_install_anyway)
                        .setListener(installCaCertificate())
                        .setButtonType(FooterButton.ButtonType.OTHER)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );
        mixin.getSecondaryButtonView().setFilterTouchesWhenObscured(true);

        mixin.setPrimaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.certificate_warning_dont_install)
                        .setListener(returnToInstallCertificateFromStorage())
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(R.style.SudGlifButton_Primary)
                        .build()
        );
        mixin.getPrimaryButtonView().setFilterTouchesWhenObscured(true);
    }

    private View.OnClickListener installCaCertificate() {
        return v -> {
            final Intent intent = new Intent();
            intent.setAction(Credentials.INSTALL_ACTION);
            intent.putExtra(Credentials.EXTRA_CERTIFICATE_USAGE, Credentials.CERTIFICATE_USAGE_CA);
            startActivity(intent);
            finish();
        };
    }

    private View.OnClickListener returnToInstallCertificateFromStorage() {
        return v -> {
            Toast.makeText(this, R.string.cert_not_installed, Toast.LENGTH_SHORT).show();
            finish();
        };
    }

}
