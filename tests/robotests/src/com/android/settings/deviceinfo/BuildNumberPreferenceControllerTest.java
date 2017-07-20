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
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.BidiFormatter;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.TestConfig;
import com.android.settings.development.DevelopmentSettings;
import com.android.settings.search.DatabaseIndexingManager;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {
                ShadowUtils.class
        })
public class BuildNumberPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Fragment mFragment;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock
    private UserManager mUserManager;

    private Lifecycle mLifecycle;
    private FakeFeatureFactory mFactory;
    private Preference mPreference;
    private BuildNumberPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFactory = FakeFeatureFactory.setupForTest(mContext);
        mLifecycle = new Lifecycle();
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mController = new BuildNumberPreferenceController(
                mContext, mActivity, mFragment, mLifecycle);

        mPreference = new Preference(RuntimeEnvironment.application);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void displayPref_shouldAlwaysDisplay() {
        mController.displayPreference(mScreen);

        verify(mScreen.findPreference(mController.getPreferenceKey()))
                .setSummary(BidiFormatter.getInstance().unicodeWrap(Build.DISPLAY));
        verify(mScreen, never()).removePreference(any(Preference.class));
    }

    @Test
    public void handlePrefTreeClick_onlyHandleBuildNumberPref() {
        assertThat(mController.handlePreferenceTreeClick(mock(Preference.class))).isFalse();
    }

    @Test
    public void handlePrefTreeClick_notAdminUser_doNothing() {
        when(mUserManager.isAdminUser()).thenReturn(false);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();
    }

    @Test
    public void handlePrefTreeClick_deviceNotProvisioned_doNothing() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        final Context context = RuntimeEnvironment.application;
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);

        mController = new BuildNumberPreferenceController(
                context, mActivity, mFragment, mLifecycle);
        ReflectionHelpers.setField(mController, "mContext", context);
        ReflectionHelpers.setField(mController, "mUm", mUserManager);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();
        verify(mFactory.metricsFeatureProvider).action(
                any(Context.class),
                eq(MetricsProto.MetricsEvent.ACTION_SETTINGS_BUILD_NUMBER_PREF));
    }

    @Test
    public void handlePrefTreeClick_isMonkeyRun_doNothing() {
        final Context context = spy(RuntimeEnvironment.application);
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);
        ShadowUtils.setIsUserAMonkey(true);
        mController = new BuildNumberPreferenceController(
                context, mActivity, mFragment, mLifecycle);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();
    }

    @Test
    public void handlePrefTreeClick_userHasRestriction_doNothing() {
        final Context context = spy(RuntimeEnvironment.application);
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);

        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES))
                .thenReturn(true);

        mController = new BuildNumberPreferenceController(
                mContext, mActivity, mFragment, mLifecycle);
        ReflectionHelpers.setField(mController, "mContext", context);

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
        verify(mContext, never())
                .getSharedPreferences(DevelopmentSettings.PREF_FILE, Context.MODE_PRIVATE);
    }

    @Test
    public void onActivityResult_confirmPasswordRequestFailed_doNotEnableDevPref() {
        final boolean activityResultHandled = mController.onActivityResult(
                BuildNumberPreferenceController.REQUEST_CONFIRM_PASSWORD_FOR_DEV_PREF,
                Activity.RESULT_CANCELED,
                null);

        assertThat(activityResultHandled).isTrue();
        verify(mContext, never())
                .getSharedPreferences(DevelopmentSettings.PREF_FILE, Context.MODE_PRIVATE);
    }

    @Test
    public void onActivityResult_confirmPasswordRequestCompleted_enableDevPref() {
        final Context context = RuntimeEnvironment.application;

        when(mFactory.searchFeatureProvider.getIndexingManager(any(Context.class)))
                .thenReturn(mock(DatabaseIndexingManager.class));

        mController = new BuildNumberPreferenceController(
                context, mActivity, mFragment, mLifecycle);

        final boolean activityResultHandled = mController.onActivityResult(
                BuildNumberPreferenceController.REQUEST_CONFIRM_PASSWORD_FOR_DEV_PREF,
                Activity.RESULT_OK,
                null);

        assertThat(activityResultHandled).isTrue();
        assertThat(context.getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE).getBoolean(DevelopmentSettings.PREF_SHOW, false))
                .isTrue();
    }

}
