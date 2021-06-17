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

package com.android.settings.biometrics;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import static android.hardware.biometrics.BiometricAuthenticator.TYPE_FACE;
import static android.hardware.biometrics.BiometricAuthenticator.TYPE_FINGERPRINT;
import static android.hardware.biometrics.BiometricAuthenticator.TYPE_IRIS;

import android.hardware.biometrics.BiometricAuthenticator;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.RestrictedLockUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ParentalControlsUtilsTest {

    @Mock
    private Context mContext;
    @Mock
    private DevicePolicyManager mDpm;
    private ComponentName mSupervisionComponentName = new ComponentName("pkg", "cls");

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getContentResolver()).thenReturn(mock(ContentResolver.class));
    }

    /**
     * Helper that sets the appropriate mocks and testing behavior before returning the actual
     * EnforcedAdmin from ParentalControlsUtils.
     */
    @Nullable
    private RestrictedLockUtils.EnforcedAdmin getEnforcedAdminForCombination(
            @Nullable ComponentName supervisionComponentName,
            @BiometricAuthenticator.Modality int modality, int keyguardDisabledFlags) {
        when(mDpm.getProfileOwnerOrDeviceOwnerSupervisionComponent(any(UserHandle.class)))
                .thenReturn(supervisionComponentName);
        when(mDpm.getKeyguardDisabledFeatures(eq(supervisionComponentName)))
                .thenReturn(keyguardDisabledFlags);

        return ParentalControlsUtils.parentConsentRequiredInternal(mDpm, modality,
                new UserHandle(UserHandle.myUserId()));
    }

    @Test
    public void testEnforcedAdmin_whenDpmDisablesBiometricsAndSupervisionComponentExists() {
        int[][] tests = {
                {TYPE_FINGERPRINT, DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT},
                {TYPE_FACE, DevicePolicyManager.KEYGUARD_DISABLE_FACE},
                {TYPE_IRIS, DevicePolicyManager.KEYGUARD_DISABLE_IRIS},
        };

        for (int i = 0; i < tests.length; i++) {
            RestrictedLockUtils.EnforcedAdmin admin = getEnforcedAdminForCombination(
                    mSupervisionComponentName, tests[i][0] /* modality */,
                    tests[i][1] /* keyguardDisableFlags */);
            assertNotNull(admin);
            assertEquals(UserManager.DISALLOW_BIOMETRIC, admin.enforcedRestriction);
            assertEquals(mSupervisionComponentName, admin.component);
        }
    }

    @Test
    public void testNoEnforcedAdmin_whenNoSupervisionComponent() {
        // Even if DPM flag exists, returns null EnforcedAdmin when no supervision component exists
        RestrictedLockUtils.EnforcedAdmin admin = getEnforcedAdminForCombination(
                null /* supervisionComponentName */, TYPE_FINGERPRINT,
                DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);
        assertNull(admin);
    }
}
