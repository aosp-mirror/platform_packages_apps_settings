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

package com.android.settings.deviceinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class BuildNumberPreferenceControllerTest {

    private static final String KEY_BUILD_NUMBER = "build_number";
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InstrumentedPreferenceFragment mFragment;

    private Context mContext;
    private UserManager mUserManager;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private FakeFeatureFactory mFactory;
    private Preference mPreference;
    private BuildNumberPreferenceController mController;

    @Before
    @UiThreadTest
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        mUserManager = (UserManager) spy(mContext.getSystemService(Context.USER_SERVICE));
        doReturn(mUserManager).when(mContext).getSystemService(Context.USER_SERVICE);

        mFactory = FakeFeatureFactory.setupForTest();
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = spy(new BuildNumberPreferenceController(mContext, KEY_BUILD_NUMBER));
        mController.setHost(mFragment);

        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(mContext, false);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);
    }

    @Test
    public void handlePrefTreeClick_onlyHandleBuildNumberPref() {
        assertThat(mController.handlePreferenceTreeClick(mock(Preference.class))).isFalse();
    }

    @Test
    public void handlePrefTreeClick_notAdminUser_notDemoUser_doNothing() {
        when(mUserManager.isAdminUser()).thenReturn(false);
        when(mUserManager.isDemoUser()).thenReturn(false);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();
    }

    @Test
    public void handlePrefTreeClick_isAdminUser_notDemoUser_handleBuildNumberPref() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mUserManager.isDemoUser()).thenReturn(false);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isTrue();
    }

    @Test
    public void handlePrefTreeClick_notAdminUser_isDemoUser_handleBuildNumberPref() {
        when(mUserManager.isAdminUser()).thenReturn(false);
        when(mUserManager.isDemoUser()).thenReturn(true);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isTrue();
    }

    @Test
    public void handlePrefTreeClick_deviceNotProvisioned_doNothing() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mUserManager.isDemoUser()).thenReturn(false);

        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DEVICE_PROVISIONED,
                0);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();
        verify(mFactory.metricsFeatureProvider).action(
                any(Context.class),
                eq(MetricsProto.MetricsEvent.ACTION_SETTINGS_BUILD_NUMBER_PREF));
    }

    @Test
    public void handlePrefTreeClick_isMonkeyRun_doNothing() {
        when(mController.isUserAMonkey()).thenReturn(true);
        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();
    }

    @Test
    public void handlePrefTreeClick_userHasRestriction_doNothing() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mUserManager.isDemoUser()).thenReturn(false);
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES))
                .thenReturn(true);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();
        verify(mFactory.metricsFeatureProvider).action(
                any(Context.class),
                eq(MetricsProto.MetricsEvent.ACTION_SETTINGS_BUILD_NUMBER_PREF));
    }

    @Test
    public void onActivityResult_notConfirmPasswordRequest_doNothing() {
        final boolean activityResultHandled = mController.onActivityResult(
                BuildNumberPreferenceController.REQUEST_CONFIRM_PASSWORD_FOR_DEV_PREF + 1,
                Activity.RESULT_OK,
                null);

        assertThat(activityResultHandled).isFalse();
        assertThat(DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mContext)).isFalse();
    }

    @Test
    public void onActivityResult_confirmPasswordRequestFailed_doNotEnableDevPref() {
        final boolean activityResultHandled = mController.onActivityResult(
                BuildNumberPreferenceController.REQUEST_CONFIRM_PASSWORD_FOR_DEV_PREF,
                Activity.RESULT_CANCELED,
                null);

        assertThat(activityResultHandled).isTrue();
        assertThat(DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mContext)).isFalse();
    }

    @Test
    @UiThreadTest
    public void onActivityResult_confirmPasswordRequestCompleted_enableDevPref() {
        when(mUserManager.isAdminUser()).thenReturn(true);

        final boolean activityResultHandled = mController.onActivityResult(
                BuildNumberPreferenceController.REQUEST_CONFIRM_PASSWORD_FOR_DEV_PREF,
                Activity.RESULT_OK,
                null);

        assertThat(activityResultHandled).isTrue();
        assertThat(DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mContext)).isTrue();
    }
}
