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
import android.os.UserHandle;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SetupChooseLockGeneric;
import com.android.settings.SetupWizardUtils;

public class SetupFingerprintEnrollIntroduction extends FingerprintEnrollIntroduction {

    @Override
    protected Intent getChooseLockIntent() {
        Intent intent = new Intent(this, SetupChooseLockGeneric.class);
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
        super.initViews();

        TextView description = (TextView) findViewById(R.id.description_text);
        description.setText(
                R.string.security_settings_fingerprint_enroll_introduction_message_setup);

        Button nextButton = getNextButton();
        nextButton.setText(
                R.string.security_settings_fingerprint_enroll_introduction_continue_setup);

        final Button cancelButton = (Button) findViewById(R.id.fingerprint_cancel_button);
        cancelButton.setText(
                R.string.security_settings_fingerprint_enroll_introduction_cancel_setup);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FINGERPRINT_FIND_SENSOR_REQUEST) {
            if (data == null) {
                data = new Intent();
            }
            LockPatternUtils lockPatternUtils = new LockPatternUtils(this);
            data.putExtra(SetupChooseLockGeneric.
                    SetupChooseLockGenericFragment.EXTRA_PASSWORD_QUALITY,
                    lockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId()));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCancelButtonClick() {
        SetupSkipDialog dialog = SetupSkipDialog.newInstance(
                getIntent().getBooleanExtra(SetupSkipDialog.EXTRA_FRP_SUPPORTED, false));
        dialog.show(getFragmentManager());
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.FINGERPRINT_ENROLL_INTRO_SETUP;
    }
}
