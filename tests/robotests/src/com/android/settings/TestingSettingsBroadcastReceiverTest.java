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

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.telephony.TelephonyManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowBuild;

@RunWith(RobolectricTestRunner.class)
public class TestingSettingsBroadcastReceiverTest {

    private Context mContext;
    private Application mApplication;
    private TestingSettingsBroadcastReceiver mReceiver;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mApplication = RuntimeEnvironment.application;
        mReceiver = new TestingSettingsBroadcastReceiver();
    }

    @Test
    public void onReceive_nullIntent_shouldNotCrash() {
        final Intent intent = new Intent();

        mReceiver.onReceive(mContext, null);
        mReceiver.onReceive(mContext, intent);

        final Intent next = Shadows.shadowOf(mApplication).getNextStartedActivity();
        assertThat(next).isNull();
    }

    @Test
    public void onReceive_wrongIntent_shouldNotStartActivity() {
        final Intent intent = new Intent();
        intent.setAction("");

        mReceiver.onReceive(mContext, intent);

        final Intent next = Shadows.shadowOf(mApplication).getNextStartedActivity();
        assertThat(next).isNull();
    }

    @Test
    public void onReceive_correctIntent_shouldStartActivity() {
        final Intent intent = new Intent();
        intent.setAction(TelephonyManager.ACTION_SECRET_CODE);

        mReceiver.onReceive(mContext, intent);

        final Intent next = Shadows.shadowOf(mApplication).getNextStartedActivity();
        assertThat(next).isNotNull();
        final String dest = next.getComponent().getClassName();
        assertThat(dest).isEqualTo(Settings.TestingSettingsActivity.class.getName());
    }

    @Test
    public void onReceive_disabledForUserBuild_BuildType_User_shouldNotStartActivity() {
        // TestingSettingsMenu should be disabled if current Build.TYPE is "user" and
        // 'config_hide_testing_settings_menu_for_user_builds' is true
        ShadowBuild.setType("user");

        mContext = spy(RuntimeEnvironment.application);
        setUpConfig(mContext, true /*disable for user build*/);

        final Intent intent = new Intent();
        intent.setAction(TelephonyManager.ACTION_SECRET_CODE);

        mReceiver.onReceive(mContext, intent);

        final Intent next = Shadows.shadowOf(mApplication).getNextStartedActivity();
        assertThat(next).isNull();
    }

    @Test
    public void onReceive_disabledForUserBuild_BuildType_Userdebug_shouldStartActivity() {
        // TestingSettingsMenu should not be disabled if current Build.TYPE is "userdebug" and
        // 'config_hide_testing_settings_menu_for_user_builds' is true
        ShadowBuild.setType("userdebug");

        mContext = spy(RuntimeEnvironment.application);
        setUpConfig(mContext, true /*disable for user build*/);

        final Intent intent = new Intent();
        intent.setAction(TelephonyManager.ACTION_SECRET_CODE);

        mReceiver.onReceive(mContext, intent);

        final Intent next = Shadows.shadowOf(mApplication).getNextStartedActivity();
        assertThat(next).isNotNull();
        final String dest = next.getComponent().getClassName();
        assertThat(dest).isEqualTo(Settings.TestingSettingsActivity.class.getName());
    }

    @Test
    public void onReceive_notDisabledForUserBuildType_shouldStartActivity() {
        // TestingSettingsMenu should not be disabled if
        // 'config_hide_testing_settings_menu_for_user_builds' is false, regardless of Build.TYPE
        mContext = spy(RuntimeEnvironment.application);
        setUpConfig(mContext, false /*disable for user build*/);

        final Intent intent = new Intent();
        intent.setAction(TelephonyManager.ACTION_SECRET_CODE);

        mReceiver.onReceive(mContext, intent);

        final Intent next = Shadows.shadowOf(mApplication).getNextStartedActivity();
        assertThat(next).isNotNull();
        final String dest = next.getComponent().getClassName();
        assertThat(dest).isEqualTo(Settings.TestingSettingsActivity.class.getName());
    }

    private static void setUpConfig(Context context, boolean disabledForUserBuild) {
        when(context.getApplicationContext()).thenReturn(context);
        Resources spiedResources = spy(context.getResources());
        when(context.getResources()).thenReturn(spiedResources);
        when(spiedResources.getBoolean(R.bool.config_hide_testing_settings_menu_for_user_builds))
                .thenReturn(disabledForUserBuild);
    }
}
