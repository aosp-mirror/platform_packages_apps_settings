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
 * limitations under the License
 */

package com.android.settings.password;

import static com.android.settings.password.TestUtils.NO_MORE_REMAINING_ATTEMPTS;
import static com.android.settings.password.TestUtils.PACKAGE_NAME;
import static com.android.settings.password.TestUtils.SERVICE_NAME;
import static com.android.settings.password.TestUtils.VALID_REMAINING_ATTEMPTS;
import static com.android.settings.password.TestUtils.buildConfirmDeviceCredentialBaseActivity;
import static com.android.settings.password.TestUtils.createPackageInfoWithService;
import static com.android.settings.password.TestUtils.createRemoteLockscreenValidationIntent;
import static com.android.settings.password.TestUtils.createRemoteLockscreenValidationSession;
import static com.android.settings.password.TestUtils.getConfirmDeviceCredentialBaseFragment;

import static com.google.common.truth.Truth.assertThat;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.admin.ManagedSubscriptionsPolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.FeatureFlagUtils;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.password.ConfirmDeviceCredentialBaseFragment.LastTryDialog;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplicationPackageManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowLockPatternUtils.class,
        ShadowUtils.class,
        ShadowDevicePolicyManager.class,
        ShadowUserManager.class,
        ShadowApplicationPackageManager.class
})
public class ConfirmCredentialTest {

    private Context mContext;
    private ShadowApplicationPackageManager mShadowApplicationPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = ApplicationProvider.getApplicationContext();

        mShadowApplicationPackageManager =
                (ShadowApplicationPackageManager) Shadows.shadowOf(mContext.getPackageManager());
        mShadowApplicationPackageManager.addPackageNoDefaults(
                TestUtils.createPackageInfoWithService(
                        PACKAGE_NAME, SERVICE_NAME,
                        Manifest.permission.BIND_REMOTE_LOCKSCREEN_VALIDATION_SERVICE));

        final ShadowDevicePolicyManager shadowDpm = ShadowDevicePolicyManager.getShadow();
        shadowDpm.setManagedSubscriptionsPolicy(
                new ManagedSubscriptionsPolicy(
                        ManagedSubscriptionsPolicy.TYPE_ALL_PERSONAL_SUBSCRIPTIONS));

        FeatureFlagUtils.setEnabled(mContext,
                FeatureFlagUtils.SETTINGS_REMOTE_DEVICE_CREDENTIAL_VALIDATION, true);
    }

    @Test
    public void onCreate_successfullyStart() {
        ConfirmDeviceCredentialBaseActivity activity =
                buildConfirmDeviceCredentialBaseActivity(ConfirmLockPassword.class, new Intent());
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);

        assertThat(activity.isFinishing()).isFalse();
        assertThat(fragment.mRemoteValidation).isFalse();
    }

    @Test
    public void onCreate_remoteValidation_successfullyStart() throws Exception {
        ConfirmDeviceCredentialBaseActivity activity = buildConfirmDeviceCredentialBaseActivity(
                ConfirmLockPassword.class, createRemoteLockscreenValidationIntent(
                        KeyguardManager.PASSWORD, VALID_REMAINING_ATTEMPTS));
        ConfirmDeviceCredentialBaseFragment fragment =
                getConfirmDeviceCredentialBaseFragment(activity);

        assertThat(activity.isFinishing()).isFalse();
        assertThat(fragment.mRemoteValidation).isTrue();
    }

    @Test
    public void onCreate_remoteValidation_flagDisabled_finishActivity() throws Exception {
        FeatureFlagUtils.setEnabled(mContext,
                FeatureFlagUtils.SETTINGS_REMOTE_DEVICE_CREDENTIAL_VALIDATION, false);

        ConfirmDeviceCredentialBaseActivity activity = buildConfirmDeviceCredentialBaseActivity(
                ConfirmLockPassword.class,
                createRemoteLockscreenValidationIntent(
                        KeyguardManager.PASSWORD, VALID_REMAINING_ATTEMPTS));

        assertThat(activity.isFinishing()).isTrue();
    }

    @Test
    @Ignore("b/295325503")
    public void onCreate_remoteValidation_invalidServiceComponentName_finishActivity()
            throws Exception {
        Intent intentWithInvalidComponentName = new Intent()
                .putExtra(ConfirmDeviceCredentialBaseFragment.IS_REMOTE_LOCKSCREEN_VALIDATION, true)
                .putExtra(KeyguardManager.EXTRA_REMOTE_LOCKSCREEN_VALIDATION_SESSION,
                        createRemoteLockscreenValidationSession(
                                KeyguardManager.PASSWORD, VALID_REMAINING_ATTEMPTS))
                .putExtra(Intent.EXTRA_COMPONENT_NAME, new ComponentName("pkg", "cls"));

        ConfirmDeviceCredentialBaseActivity activity = buildConfirmDeviceCredentialBaseActivity(
                ConfirmLockPassword.class, intentWithInvalidComponentName);

        assertThat(activity.isFinishing()).isTrue();
    }

    @Test
    public void onCreate_remoteValidation_serviceDoesNotRequestCorrectPermission_finishActivity()
            throws Exception {
        // Remove package with valid ServiceInfo
        mShadowApplicationPackageManager.removePackage(PACKAGE_NAME);
        // Add a service that does not request the BIND_REMOTE_LOCKSCREEN_SERVICE permission
        mShadowApplicationPackageManager.addPackageNoDefaults(
                createPackageInfoWithService(
                        PACKAGE_NAME,
                        SERVICE_NAME,
                        Manifest.permission.BIND_RUNTIME_PERMISSION_PRESENTER_SERVICE));

        ConfirmDeviceCredentialBaseActivity activity = buildConfirmDeviceCredentialBaseActivity(
                ConfirmLockPassword.class,
                createRemoteLockscreenValidationIntent(
                        KeyguardManager.PASSWORD, VALID_REMAINING_ATTEMPTS));

        assertThat(activity.isFinishing()).isTrue();
    }

    @Test
    public void onCreate_remoteValidation_noMoreAttempts_finishActivity() throws Exception {
        ConfirmDeviceCredentialBaseActivity activity = buildConfirmDeviceCredentialBaseActivity(
                ConfirmLockPassword.class,
                createRemoteLockscreenValidationIntent(
                        KeyguardManager.PASSWORD, NO_MORE_REMAINING_ATTEMPTS));

        assertThat(activity.isFinishing()).isTrue();
    }

    @Test
    public void testLastTryDialogShownExactlyOnce() {
        FragmentManager fm = Robolectric.buildActivity(FragmentActivity.class).
                setup().get().getSupportFragmentManager();

        // Launch only one instance at a time.
        assertThat(LastTryDialog.show(
                fm, "title", mContext.getString(android.R.string.ok),
                android.R.string.ok, false)).isTrue();
        assertThat(LastTryDialog.show(
                fm, "title", mContext.getString(android.R.string.ok),
                android.R.string.ok, false)).isFalse();

        // After cancelling, the dialog should be re-shown when asked for.
        LastTryDialog.hide(fm);
        assertThat(LastTryDialog.show(
                fm, "title", mContext.getString(android.R.string.ok),
                android.R.string.ok, false)).isTrue();
    }
}
