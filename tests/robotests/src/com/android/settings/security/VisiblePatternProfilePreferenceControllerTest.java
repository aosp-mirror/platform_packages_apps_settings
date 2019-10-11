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

package com.android.settings.security;

import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_FOR_USER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserManager;

import androidx.lifecycle.LifecycleOwner;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class VisiblePatternProfilePreferenceControllerTest {

    private static final int FAKE_PROFILE_USER_ID = 1234;

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private LockPatternUtils mLockPatternUtils;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private UserManager mUm;

    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private FakeFeatureFactory mFeatureFactory;
    private Context mContext;
    private VisiblePatternProfilePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)).thenReturn(true);
        final ShadowApplication application = ShadowApplication.getInstance();
        application.setSystemService(Context.FINGERPRINT_SERVICE, mFingerprintManager);
        application.setSystemService(Context.USER_SERVICE, mUm);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFeatureFactory.securityFeatureProvider.getLockPatternUtils(mContext))
                .thenReturn(mLockPatternUtils);
        when(mUm.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[] {FAKE_PROFILE_USER_ID});

        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = new VisiblePatternProfilePreferenceController(mContext, mLifecycle);
    }

    @Test
    public void getAvailabilityStatus_notSecure_DISABLED() {
        when(mLockPatternUtils.isSecure(FAKE_PROFILE_USER_ID)).thenReturn(false);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(FAKE_PROFILE_USER_ID))
                .thenReturn(PASSWORD_QUALITY_UNSPECIFIED);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_secureWithPassword_DISABLED() {
        when(mLockPatternUtils.isSecure(FAKE_PROFILE_USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(FAKE_PROFILE_USER_ID))
                .thenReturn(PASSWORD_QUALITY_ALPHABETIC);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_secureWithPattern_AVAILABLE() {
        when(mLockPatternUtils.isSecure(FAKE_PROFILE_USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(FAKE_PROFILE_USER_ID))
                .thenReturn(PASSWORD_QUALITY_SOMETHING);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }
}
