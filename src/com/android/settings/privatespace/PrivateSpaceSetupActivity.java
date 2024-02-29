/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privatespace;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;

import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import com.google.android.setupdesign.util.ThemeHelper;

/** Activity class that helps in setting up of private space */
public class PrivateSpaceSetupActivity extends FragmentActivity {
    private static final String TAG = "PrivateSpaceSetupAct";
    public static final int SET_LOCK_ACTION = 1;
    public static final int ACCOUNT_LOGIN_ACTION = 2;
    public static final String EXTRA_ACTION_TYPE = "action_type";
    private NavHostFragment mNavHostFragment;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!android.os.Flags.allowPrivateProfile()) {
            return;
        }
        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        ThemeHelper.trySetDynamicColor(this);
        super.onCreate(savedInstanceState);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        setContentView(R.layout.privatespace_setup_root);
        mNavHostFragment =
                (NavHostFragment)
                        getSupportFragmentManager().findFragmentById(R.id.ps_nav_host_fragment);
        mNavHostFragment.getNavController().setGraph(R.navigation.privatespace_main_context_nav);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SET_LOCK_ACTION && resultCode == RESULT_OK) {
            mNavHostFragment.getNavController().navigate(R.id.action_success_fragment);
        } else if (requestCode == ACCOUNT_LOGIN_ACTION) {
            if (resultCode == RESULT_OK) {
                mMetricsFeatureProvider.action(
                        this, SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_ACCOUNT_LOGIN_SUCCESS, true);
                mNavHostFragment.getNavController().navigate(R.id.action_account_lock_fragment);
            } else {
                mMetricsFeatureProvider.action(
                        this,
                        SettingsEnums.ACTION_PRIVATE_SPACE_SETUP_ACCOUNT_LOGIN_SUCCESS,
                        false);
                mNavHostFragment.getNavController().navigate(R.id.action_advance_login_error);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
