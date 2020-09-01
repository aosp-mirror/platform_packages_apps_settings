/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.system;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUserManager.class)
public class ResetPreferenceControllerTest {

    private static final String KEY_RESET_DASHBOARD = "reset_dashboard";
    private ShadowUserManager mShadowUserManager;

    private Context mContext;
    private ResetPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new ResetPreferenceController(mContext, KEY_RESET_DASHBOARD);
        mShadowUserManager = ShadowUserManager.getShadow();
    }

    @Test
    public void isAvailable_byDefault_true() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_ifNotVisible_false() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getSummary_systemUser_shouldReturnFullSummary() {
        mShadowUserManager.setIsAdminUser(true);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.reset_dashboard_summary));
    }

    @Test
    public void getSummary_nonSystemUser_shouldReturnAppsSummary() {
        mShadowUserManager.setIsAdminUser(false);
        mShadowUserManager.setIsDemoUser(false);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.reset_dashboard_summary_onlyApps));
    }

    @Test
    public void getSummary_demoUser_shouldReturnFullSummary() {
        mShadowUserManager.setIsAdminUser(false);

        // Place the device in demo mode.
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DEVICE_DEMO_MODE, 1);

        // Indicate the user is a demo user.
        mShadowUserManager.addUser(UserHandle.myUserId(), "test", UserInfo.FLAG_DEMO);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.reset_dashboard_summary));
    }
}