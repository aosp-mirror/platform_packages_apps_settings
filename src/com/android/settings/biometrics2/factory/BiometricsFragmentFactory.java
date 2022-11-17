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

package com.android.settings.biometrics2.factory;

import android.app.Application;
import android.app.admin.DevicePolicyManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.lifecycle.ViewModelProvider;

import com.android.settings.biometrics2.ui.view.FingerprintEnrollIntroFragment;

/**
 * Fragment factory for biometrics
 */
public class BiometricsFragmentFactory extends FragmentFactory {

    private final Application mApplication;
    private final ViewModelProvider mViewModelProvider;

    public BiometricsFragmentFactory(Application application,
            ViewModelProvider viewModelProvider) {
        mApplication = application;
        mViewModelProvider = viewModelProvider;
    }

    @NonNull
    @Override
    public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
        final Class<? extends Fragment> clazz = loadFragmentClass(classLoader, className);
        if (FingerprintEnrollIntroFragment.class.equals(clazz)) {
            final DevicePolicyManager devicePolicyManager =
                    mApplication.getSystemService(DevicePolicyManager.class);
            if (devicePolicyManager != null) {
                return new FingerprintEnrollIntroFragment(mViewModelProvider,
                        devicePolicyManager.getResources());
            }
        }
        return super.instantiate(classLoader, className);
    }
}
