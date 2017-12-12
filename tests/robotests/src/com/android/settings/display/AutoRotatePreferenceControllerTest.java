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
 * limitations under the License.
 */

package com.android.settings.display;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowSystemSettings;
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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
    manifest = TestConfig.MANIFEST_PATH,
    sdk = TestConfig.SDK_VERSION,
    shadows = ShadowSystemSettings.class
)
public class AutoRotatePreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    private Lifecycle mLifecycle;
    private SwitchPreference mPreference;
    private ContentResolver mContentResolver;
    private AutoRotatePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        mLifecycle = new Lifecycle();
        mPreference = new SwitchPreference(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getContentResolver()).thenReturn(mContentResolver);

        mController = new AutoRotatePreferenceController(mContext, mLifecycle);
    }

    @After
    public void tearDown() {
        ShadowSystemSettings.reset();
    }

    @Test
    public void isAvailableWhenPolicyAllows() {
        assertThat(mController.isAvailable()).isFalse();

        when(mPackageManager.hasSystemFeature(anyString())).thenReturn(true);
        when(mContext.getResources().getBoolean(anyInt())).thenReturn(true);
        Settings.System.putInt(mContentResolver,
                Settings.System.HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY, 0);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updatePreference_settingsIsOff_shouldTurnOffToggle() {
        Settings.System.putIntForUser(mContentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 0, UserHandle.USER_CURRENT);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updatePreference_settingsIsOn_shouldTurnOnToggle() {
        Settings.System.putIntForUser(mContentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 1, UserHandle.USER_CURRENT);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }
}
