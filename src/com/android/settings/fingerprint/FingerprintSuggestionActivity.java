/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.widget.Button;

import com.android.settings.R;

public class FingerprintSuggestionActivity extends SetupFingerprintEnrollIntroduction {

    @Override
    protected void initViews() {
        super.initViews();

        final Button cancelButton = findViewById(R.id.fingerprint_cancel_button);
        cancelButton.setText(R.string.security_settings_fingerprint_enroll_introduction_cancel);
    }

    @Override
    public void finish() {
        // Always use RESULT_CANCELED because this action can be done multiple times
        setResult(RESULT_CANCELED);
        super.finish();
    }
}
