/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

/**
 * Wrapper of {@link FaceEnrollIntroduction} to use with a pre-defined task affinity.
 *
 * <p>Trampolines over to FaceEnrollIntroduction - doing this as a trampoline rather than having
 * this activity extend FaceEnrollIntroduction works around b/331157120.
 */
public class FaceEnrollIntroductionInternal extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }

        // Copy our intent to grab all extras. Drop flags so we don't start new tasks twice.
        Intent trampoline = new Intent(getIntent());
        trampoline.setFlags(0);

        // Trampoline to the intended activity, and finish
        trampoline.setClassName(SETTINGS_PACKAGE_NAME, FaceEnrollIntroduction.class.getName());
        startActivity(trampoline);
        finish();
    }
}
