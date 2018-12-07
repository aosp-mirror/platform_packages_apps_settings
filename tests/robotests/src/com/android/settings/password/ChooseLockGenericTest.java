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

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.provider.Settings.Global;

import androidx.annotation.Nullable;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowStorageManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
                ShadowLockPatternUtils.class,
                ShadowStorageManager.class,
                ShadowUserManager.class,
                ShadowUtils.class
        })
public class ChooseLockGenericTest {

    private ChooseLockGenericFragment mFragment;
    private ChooseLockGeneric mActivity;

    @Before
    public void setUp() {
        Global.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 1);
        mFragment = new ChooseLockGenericFragment();
    }

    @After
    public void tearDown() {
        Global.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 1);
        ShadowStorageManager.reset();
    }

    @Test
    public void onCreate_deviceNotProvisioned_shouldFinishActivity() {
        Global.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 0);

        initActivity(null);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onActivityResult_nullIntentData_shouldNotCrash() {
        initActivity(null);
        mFragment.onActivityResult(
                ChooseLockGenericFragment.CONFIRM_EXISTING_REQUEST, Activity.RESULT_OK,
                null /* data */);
        // no crash
    }

    @Test
    public void updatePreferencesOrFinish_passwordTypeSetPin_shouldStartChooseLockPassword() {
        Intent intent = new Intent().putExtra(
                LockPatternUtils.PASSWORD_TYPE_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        initActivity(intent);

        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);

        assertThat(shadowOf(mActivity).getNextStartedActivity()).isNotNull();
    }

    @Test
    public void updatePreferencesOrFinish_passwordTypeSetPinNotFbe_shouldNotStartChooseLock() {
        ShadowStorageManager.setIsFileEncryptedNativeOrEmulated(false);
        Intent intent = new Intent().putExtra(
                LockPatternUtils.PASSWORD_TYPE_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        initActivity(intent);

        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);

        assertThat(shadowOf(mActivity).getNextStartedActivity()).isNull();
    }

    @Test
    public void onActivityResult_requestcode0_shouldNotFinish() {
        initActivity(null);

        mFragment.onActivityResult(
                SearchFeatureProvider.REQUEST_CODE, Activity.RESULT_OK, null /* data */);

        assertThat(mActivity.isFinishing()).isFalse();
    }

    @Test
    public void onActivityResult_requestcode101_shouldFinish() {
        initActivity(null);

        mFragment.onActivityResult(
                ChooseLockGenericFragment.ENABLE_ENCRYPTION_REQUEST, Activity.RESULT_OK,
                null /* data */);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onActivityResult_requestcode102_shouldFinish() {
        initActivity(null);

        mFragment.onActivityResult(
                ChooseLockGenericFragment.CHOOSE_LOCK_REQUEST, Activity.RESULT_OK, null /* data */);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onActivityResult_requestcode103_shouldFinish() {
        initActivity(null);

        mFragment.onActivityResult(
                ChooseLockGenericFragment.CHOOSE_LOCK_BEFORE_FINGERPRINT_REQUEST,
                BiometricEnrollBase.RESULT_FINISHED, null /* data */);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onActivityResult_requestcode104_shouldFinish() {
        initActivity(null);

        mFragment.onActivityResult(
                ChooseLockGenericFragment.SKIP_FINGERPRINT_REQUEST, Activity.RESULT_OK,
                null /* data */);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    private void initActivity(@Nullable Intent intent) {
        if (intent == null) {
            intent = new Intent();
        }
        intent.putExtra(ChooseLockGeneric.CONFIRM_CREDENTIALS, false);
        mActivity = Robolectric.buildActivity(ChooseLockGeneric.InternalActivity.class, intent)
                .setup().get();
        mActivity.getSupportFragmentManager().beginTransaction().add(mFragment, null).commitNow();
    }
}
