/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.SettingsActivity;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SubSettingLauncherTest {

    @Mock
    private Fragment mFragment;
    @Mock
    private FragmentActivity mActivity;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
    }

    @Test(expected = IllegalStateException.class)
    public void cannotReuseLauncher() {
        final SubSettingLauncher launcher = spy(new SubSettingLauncher(mContext))
                .setDestination(SubSettingLauncherTest.class.getName())
                .setSourceMetricsCategory(123);
        doNothing().when(launcher).launch(any(Intent.class));
        launcher.launch();
        launcher.launch();
    }

    @Test(expected = IllegalArgumentException.class)
    public void launch_noSourceMetricsCategory_shouldCrash() {
        final SubSettingLauncher launcher = spy(new SubSettingLauncher(mContext))
                .setDestination(SubSettingLauncherTest.class.getName());
        launcher.launch();
    }

    @Test(expected = IllegalArgumentException.class)
    public void launch_noDestination_shouldCrash() {
        final SubSettingLauncher launcher = spy(new SubSettingLauncher(mContext))
                .setSourceMetricsCategory(123);
        launcher.launch();
    }

    @Test
    public void launch_shouldIncludeAllParams() {
        final ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        final SubSettingLauncher launcher = spy(new SubSettingLauncher(mContext));
        launcher.setTitleText("123")
                .setDestination(SubSettingLauncherTest.class.getName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setSourceMetricsCategory(123)
                .launch();
        doNothing().when(launcher).launch(any(Intent.class));
        verify(launcher).launch(intentArgumentCaptor.capture());
        final Intent intent = intentArgumentCaptor.getValue();

        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE))
                .isEqualTo("123");
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(SubSettingLauncherTest.class.getName());
        assertThat(intent.getFlags()).isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK);
        assertThat(intent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, -1))
                .isEqualTo(123);
    }

    @Test
    public void launch_hasRequestListener_shouldStartActivityForResult() {
        final int requestCode = 123123;
        when(mFragment.getActivity()).thenReturn(mActivity);

        final SubSettingLauncher launcher = spy(new SubSettingLauncher(mContext));
        launcher.setTitleText("123")
                .setDestination(SubSettingLauncherTest.class.getName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setSourceMetricsCategory(123)
                .setResultListener(mFragment, requestCode)
                .launch();

        verify(mFragment).startActivityForResult(any(Intent.class), eq(requestCode));
    }

    @Test
    public void launch_hasUserHandle_shouldStartActivityAsUser() {
        final UserHandle userHandle = new UserHandle(1234);

        final SubSettingLauncher launcher = spy(new SubSettingLauncher(mContext));
        doNothing().when(launcher).launchAsUser(any(Intent.class), any(UserHandle.class));

        launcher.setTitleText("123")
                .setDestination(SubSettingLauncherTest.class.getName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setSourceMetricsCategory(123)
                .setUserHandle(userHandle)
                .launch();

        verify(launcher).launchAsUser(any(Intent.class), eq(userHandle));
    }

    @Test
    public void launch_hasUserHandleAndRequestCode_shouldStartActivityForResultAsUser() {
        final int requestCode = 123123;
        final UserHandle userHandle = new UserHandle(1234);

        final SubSettingLauncher launcher = spy(new SubSettingLauncher(mContext));
        doNothing().when(launcher).launchForResultAsUser(
                any(Intent.class), any(UserHandle.class), any(Fragment.class), anyInt());

        launcher.setTitleText("123")
                .setDestination(SubSettingLauncherTest.class.getName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setSourceMetricsCategory(123)
                .setUserHandle(userHandle)
                .setResultListener(mFragment, requestCode)
                .launch();

        verify(launcher).launchForResultAsUser(any(Intent.class), eq(userHandle), eq(mFragment),
                eq(requestCode));
    }
}
