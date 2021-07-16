/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.biometrics.combination;

import android.content.Context;
import android.content.pm.PackageManager;

import com.android.settings.search.BaseSearchIndexProvider;

/**
 * Settings search index provider for the multi-biometric settings pages.
 */
public class CombinedBiometricSearchIndexProvider extends BaseSearchIndexProvider {
    public CombinedBiometricSearchIndexProvider(int xmlRes) {
        super(xmlRes);
    }

    @Override
    protected boolean isPageSearchEnabled(Context context) {
        final PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_FACE)
                && pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
    }
}
