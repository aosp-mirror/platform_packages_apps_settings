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

package com.android.settings.biometrics;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class BiometricNavigationUtilsTest {

    private static final String SETTINGS_CLASS_NAME = "SettingsClassName";
    private static final String EXTRA_KEY = "EXTRA_KEY";

    @Mock
    private UserManager mUserManager;
    private Context mContext;
    private BiometricNavigationUtils mBiometricNavigationUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        doNothing().when(mContext).startActivity(any());
        mBiometricNavigationUtils = new BiometricNavigationUtils();
    }

    @Test
    public void openBiometricSettings_quietMode_launchesQuiteModeDialog() {
        when(mUserManager.isQuietModeEnabled(any())).thenReturn(true);

        mBiometricNavigationUtils.launchBiometricSettings(mContext, SETTINGS_CLASS_NAME,
                Bundle.EMPTY);

        assertQuietModeDialogLaunchRequested();
    }

    @Test
    public void openBiometricSettings_quietMode_returnsFalse() {
        when(mUserManager.isQuietModeEnabled(any())).thenReturn(true);

        assertThat(mBiometricNavigationUtils.launchBiometricSettings(
                mContext, SETTINGS_CLASS_NAME, Bundle.EMPTY)).isFalse();
    }

    @Test
    public void openBiometricSettings_noQuietMode_emptyExtras_launchesFragmentWithoutExtras() {
        when(mUserManager.isQuietModeEnabled(any())).thenReturn(false);

        mBiometricNavigationUtils.launchBiometricSettings(
                mContext, SETTINGS_CLASS_NAME, Bundle.EMPTY);

        assertSettingsPageLaunchRequested(false /* shouldContainExtras */);
    }

    @Test
    public void openBiometricSettings_noQuietMode_emptyExtras_returnsTrue() {
        when(mUserManager.isQuietModeEnabled(any())).thenReturn(false);

        assertThat(mBiometricNavigationUtils.launchBiometricSettings(
                mContext, SETTINGS_CLASS_NAME, Bundle.EMPTY)).isTrue();
    }

    @Test
    public void openBiometricSettings_noQuietMode_withExtras_launchesFragmentWithExtras() {
        when(mUserManager.isQuietModeEnabled(any())).thenReturn(false);

        final Bundle extras = createNotEmptyExtras();
        mBiometricNavigationUtils.launchBiometricSettings(mContext, SETTINGS_CLASS_NAME, extras);

        assertSettingsPageLaunchRequested(true /* shouldContainExtras */);
    }

    @Test
    public void openBiometricSettings_noQuietMode_withExtras_returnsTrue() {
        when(mUserManager.isQuietModeEnabled(any())).thenReturn(false);

        assertThat(mBiometricNavigationUtils.launchBiometricSettings(
                mContext, SETTINGS_CLASS_NAME, createNotEmptyExtras())).isTrue();
    }

    private Bundle createNotEmptyExtras() {
        final Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_KEY, 0);
        return bundle;
    }

    private void assertQuietModeDialogLaunchRequested() {
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());

        Intent intent = intentCaptor.getValue();
        assertThat(intent.getComponent().getPackageName())
                .isEqualTo("android");
        assertThat(intent.getComponent().getClassName())
                .isEqualTo("com.android.internal.app.UnlaunchableAppActivity");
    }

    private void assertSettingsPageLaunchRequested(boolean shouldContainExtras) {
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());

        Intent intent = intentCaptor.getValue();
        assertThat(intent.getComponent().getPackageName())
                .isEqualTo("com.android.settings");
        assertThat(intent.getComponent().getClassName())
                .isEqualTo(SETTINGS_CLASS_NAME);
        assertThat(intent.getExtras().containsKey(EXTRA_KEY)).isEqualTo(shouldContainExtras);
    }

}
