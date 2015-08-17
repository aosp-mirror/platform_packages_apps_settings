/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.fingerprint;

import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import android.widget.Button;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.setupwizardlib.view.NavigationBar;

public class SetupFingerprintEnrollIntroduction extends FingerprintEnrollIntroduction
        implements NavigationBar.NavigationBarListener {

    @Override
    protected Intent getOnboardIntent() {
        final Intent intent = new Intent(this, SetupFingerprintEnrollOnboard.class);
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected Intent getFindSensorIntent() {
        final Intent intent = new Intent(this, SetupFingerprintEnrollFindSensor.class);
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    protected void initViews() {
        SetupWizardUtils.setImmersiveMode(this);

        final View buttonBar = findViewById(R.id.button_bar);
        if (buttonBar != null) {
            buttonBar.setVisibility(View.GONE);
        }

        getNavigationBar().setNavigationBarListener(this);
    }

    @Override
    protected Button getNextButton() {
        return getNavigationBar().getNextButton();
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
        onNextButtonClick();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.FINGERPRINT_ENROLL_INTRO_SETUP;
    }
}
