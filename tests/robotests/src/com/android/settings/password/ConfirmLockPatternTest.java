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

package com.android.settings.password;

import static com.android.settings.password.TestUtils.GUESS_INVALID_RESULT;
import static com.android.settings.password.TestUtils.GUESS_VALID_RESULT;
import static com.android.settings.password.TestUtils.LOCKOUT_RESULT;
import static com.android.settings.password.TestUtils.NO_REMAINING_ATTEMPTS_RESULT;
import static com.android.settings.password.TestUtils.PACKAGE_NAME;
import static com.android.settings.password.TestUtils.SERVICE_NAME;
import static com.android.settings.password.TestUtils.TIMEOUT_MS;
import static com.android.settings.password.TestUtils.VALID_REMAINING_ATTEMPTS;
import static com.android.settings.password.TestUtils.buildConfirmDeviceCredentialBaseActivity;
import static com.android.settings.password.TestUtils.createPackageInfoWithService;
import static com.android.settings.password.TestUtils.createRemoteLockscreenValidationIntent;
import static com.android.settings.password.TestUtils.getConfirmDeviceCredentialBaseFragment;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.admin.ManagedSubscriptionsPolicy;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.service.remotelockscreenvalidation.IRemoteLockscreenValidationCallback;
import android.service.remotelockscreenvalidation.RemoteLockscreenValidationClient;
import android.util.FeatureFlagUtils;

import androidx.test.core.app.ApplicationProvider;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplicationPackageManager;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowLockPatternUtils.class,
        ShadowUtils.class,
        ShadowDevicePolicyManager.class,
        ShadowUserManager.class,
        ShadowApplicationPackageManager.class
})
public class ConfirmLockPatternTest {

    @Mock
    CredentialCheckResultTracker mCredentialCheckResultTracker;
    @Mock
    RemoteLockscreenValidationClient mRemoteLockscreenValidationClient;
    @Captor
    ArgumentCaptor<IRemoteLockscreenValidationCallback> mCallbackCaptor;

    private Context mContext;
    private LockPatternUtils mLockPatternUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = ApplicationProvider.getApplicationContext();
        mLockPatternUtils = new LockPatternUtils(mContext);

        ShadowApplicationPackageManager shadowApplicationPackageManager =
                (ShadowApplicationPackageManager) Shadows.shadowOf(mContext.getPackageManager());
        shadowApplicationPackageManager.addPackageNoDefaults(
                createPackageInfoWithService(
                        PACKAGE_NAME,
                        SERVICE_NAME,
                        Manifest.permission.BIND_REMOTE_LOCKSCREEN_VALIDATION_SERVICE));

        final ShadowDevicePolicyManager shadowDpm = ShadowDevicePolicyManager.getShadow();
        shadowDpm.setManagedSubscriptionsPolicy(
                new ManagedSubscriptionsPolicy(
                        ManagedSubscriptionsPolicy.TYPE_ALL_PERSONAL_SUBSCRIPTIONS));

        // Set false by default so we can check if lock was set when remote validation succeeds.
        ShadowLockPatternUtils.setIsSecure(UserHandle.myUserId(), false);

        FeatureFlagUtils.setEnabled(mContext,
                FeatureFlagUtils.SETTINGS_REMOTE_DEVICE_CREDENTIAL_VALIDATION, true);
    }

    @After
    public void tearDown() {
        ShadowLockPatternUtils.reset();
    }

    @Test
    public void onCreate_remoteValidation_successfullyStart() throws Exception {
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);

        assertThat(activity.isFinishing()).isFalse();
        assertThat(fragment.mRemoteValidation).isTrue();
        LockPatternView lockPatternView = (LockPatternView) activity.findViewById(R.id.lockPattern);
        assertThat(lockPatternView.isInStealthMode()).isFalse();
    }

    @Test
    public void onPatternDetected_normalFlow_doesNotAttemptRemoteLockscreenValidation() {
        ConfirmLockPattern activity = Robolectric.buildActivity(
                ConfirmLockPattern.class, new Intent()).setup().get();
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);
        LockPatternView lockPatternView = activity.findViewById(R.id.lockPattern);
        fragment.mRemoteLockscreenValidationClient = mRemoteLockscreenValidationClient;

        triggerOnPatternDetected(lockPatternView);

        verifyNoInteractions(mRemoteLockscreenValidationClient);
    }

    @Test
    @Ignore("b/295325503")
    public void onPatternDetected_remoteValidation_guessValid_checkboxChecked() throws Exception {
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);
        LockPatternView lockPatternView = activity.findViewById(R.id.lockPattern);
        ReflectionHelpers.setField(fragment,
                "mCredentialCheckResultTracker", mCredentialCheckResultTracker);
        fragment.mRemoteLockscreenValidationClient = mRemoteLockscreenValidationClient;

        triggerOnPatternDetected(lockPatternView);
        verify(mRemoteLockscreenValidationClient)
                .validateLockscreenGuess(any(), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onSuccess(GUESS_VALID_RESULT);

        verify(mCredentialCheckResultTracker).setResult(
                eq(true), any(), eq(0), eq(fragment.mEffectiveUserId));
        assertThat(mLockPatternUtils.isSecure(fragment.mEffectiveUserId)).isTrue();
        assertThat(fragment.mRemoteLockscreenValidationFragment.getLockscreenCredential()).isNull();
    }

    @Test
    @Ignore("b/295325503")
    public void onPatternDetected_remoteValidation_guessValid_checkboxUnchecked() throws Exception {
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);
        LockPatternView lockPatternView = activity.findViewById(R.id.lockPattern);
        ReflectionHelpers.setField(fragment,
                "mCredentialCheckResultTracker", mCredentialCheckResultTracker);
        fragment.mCheckBox.setChecked(false);
        fragment.mRemoteLockscreenValidationClient = mRemoteLockscreenValidationClient;

        triggerOnPatternDetected(lockPatternView);
        verify(mRemoteLockscreenValidationClient)
                .validateLockscreenGuess(any(), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onSuccess(GUESS_VALID_RESULT);

        verify(mCredentialCheckResultTracker).setResult(
                eq(true), any(), eq(0), eq(fragment.mEffectiveUserId));
        assertThat(mLockPatternUtils.isSecure(fragment.mEffectiveUserId)).isFalse();
        assertThat(fragment.mRemoteLockscreenValidationFragment.getLockscreenCredential()).isNull();
    }

    @Test
    @Ignore("b/295325503")
    public void onPatternDetected_remoteValidation_guessInvalid() throws Exception {
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);
        LockPatternView lockPatternView = activity.findViewById(R.id.lockPattern);
        ReflectionHelpers.setField(fragment,
                "mCredentialCheckResultTracker", mCredentialCheckResultTracker);
        fragment.mRemoteLockscreenValidationClient = mRemoteLockscreenValidationClient;

        triggerOnPatternDetected(lockPatternView);
        verify(mRemoteLockscreenValidationClient)
                .validateLockscreenGuess(any(), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onSuccess(GUESS_INVALID_RESULT);

        verify(mCredentialCheckResultTracker).setResult(
                eq(false), any(), eq(0), eq(fragment.mEffectiveUserId));
        assertThat(mLockPatternUtils.isSecure(fragment.mEffectiveUserId)).isFalse();
    }

    @Test
    @Ignore("b/295325503")
    public void onPatternDetected_remoteValidation_lockout() throws Exception {
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);
        LockPatternView lockPatternView = activity.findViewById(R.id.lockPattern);
        ReflectionHelpers.setField(fragment,
                "mCredentialCheckResultTracker", mCredentialCheckResultTracker);
        fragment.mRemoteLockscreenValidationClient = mRemoteLockscreenValidationClient;

        triggerOnPatternDetected(lockPatternView);
        verify(mRemoteLockscreenValidationClient)
                .validateLockscreenGuess(any(), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onSuccess(LOCKOUT_RESULT);

        verify(mCredentialCheckResultTracker).setResult(
                eq(false), any(), eq(TIMEOUT_MS), eq(fragment.mEffectiveUserId));
        assertThat(mLockPatternUtils.isSecure(fragment.mEffectiveUserId)).isFalse();
    }

    @Test
    @Ignore("b/295325503")
    public void onPatternDetected_noRemainingAttempts_finishActivity() throws Exception {
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(
                        ConfirmLockPattern.class,
                        createRemoteLockscreenValidationIntent(
                                KeyguardManager.PATTERN, VALID_REMAINING_ATTEMPTS));
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);
        LockPatternView lockPatternView = activity.findViewById(R.id.lockPattern);
        ReflectionHelpers.setField(fragment,
                "mCredentialCheckResultTracker", mCredentialCheckResultTracker);
        fragment.mRemoteLockscreenValidationClient = mRemoteLockscreenValidationClient;

        triggerOnPatternDetected(lockPatternView);
        verify(mRemoteLockscreenValidationClient)
                .validateLockscreenGuess(any(), mCallbackCaptor.capture());
        mCallbackCaptor.getValue().onSuccess(NO_REMAINING_ATTEMPTS_RESULT);

        assertThat(activity.isFinishing()).isTrue();
        verifyNoInteractions(mCredentialCheckResultTracker);
        assertThat(mLockPatternUtils.isSecure(fragment.mEffectiveUserId)).isFalse();
    }

    private void triggerOnPatternDetected(LockPatternView lockPatternView) {
        List<LockPatternView.Cell> pattern = List.of(LockPatternView.Cell.of(0, 0));
        lockPatternView.setPattern(LockPatternView.DisplayMode.Correct, pattern);
        ReflectionHelpers.callInstanceMethod(lockPatternView, "notifyPatternDetected");
    }
}
