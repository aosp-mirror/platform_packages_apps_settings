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
 * limitations under the License
 */

package com.android.settings.password;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Global;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
public class ChooseLockGenericTest {

    private Context mContext;
    private ChooseLockGenericFragment mFragment;
    private FragmentActivity mActivity;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFragment = spy(new ChooseLockGenericFragment());
        mActivity = mock(FragmentActivity.class);
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mFragment.getFragmentManager()).thenReturn(mock(FragmentManager.class));
        doNothing().when(mFragment).startActivity(any(Intent.class));
    }

    @After
    public void tearDown() {
        Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Global.DEVICE_PROVISIONED, 1);
    }

    @Test
    @Config(shadows = SettingsShadowResources.SettingsShadowTheme.class)
    public void onCreate_deviceNotProvisioned_shouldFinishActivity() {
        Global.putInt(mContext.getContentResolver(), Global.DEVICE_PROVISIONED, 0);
        when(mActivity.getContentResolver()).thenReturn(mContext.getContentResolver());
        when(mActivity.getTheme()).thenReturn(mContext.getTheme());
        when(mFragment.getArguments()).thenReturn(Bundle.EMPTY);

        mFragment.onCreate(Bundle.EMPTY);
        verify(mActivity).finish();
    }

    @Test
    public void onActivityResult_nullIntentData_shouldNotCrash() {
        doNothing().when(mFragment).updatePreferencesOrFinish(anyBoolean());

        mFragment.onActivityResult(
                ChooseLockGenericFragment.CONFIRM_EXISTING_REQUEST, Activity.RESULT_OK,
                null /* data */);
        // no crash
    }

    @Test
    public void onActivityResult_requestcode0_shouldNotFinish() {
        mFragment.onActivityResult(
                SearchFeatureProvider.REQUEST_CODE, Activity.RESULT_OK, null /* data */);

        verify(mFragment, never()).finish();
    }

    @Test
    public void onActivityResult_requestcode101_shouldFinish() {
        mFragment.onActivityResult(
                ChooseLockGenericFragment.ENABLE_ENCRYPTION_REQUEST, Activity.RESULT_OK,
                null /* data */);

        verify(mFragment).finish();
    }

    @Test
    public void onActivityResult_requestcode102_shouldFinish() {
        mFragment.onActivityResult(
                ChooseLockGenericFragment.CHOOSE_LOCK_REQUEST, Activity.RESULT_OK, null /* data */);

        verify(mFragment).finish();
    }

    @Test
    public void onActivityResult_requestcode103_shouldFinish() {
        mFragment.onActivityResult(
                ChooseLockGenericFragment.CHOOSE_LOCK_BEFORE_FINGERPRINT_REQUEST,
                BiometricEnrollBase.RESULT_FINISHED, null /* data */);

        verify(mFragment).finish();
    }

    @Test
    public void onActivityResult_requestcode104_shouldFinish() {
        mFragment.onActivityResult(
                ChooseLockGenericFragment.SKIP_FINGERPRINT_REQUEST, Activity.RESULT_OK,
                null /* data */);

        verify(mFragment).finish();
    }
}
