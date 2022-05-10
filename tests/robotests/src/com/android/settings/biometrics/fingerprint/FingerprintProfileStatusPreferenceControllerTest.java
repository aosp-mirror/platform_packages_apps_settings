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

package com.android.settings.biometrics.fingerprint;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserManager;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class FingerprintProfileStatusPreferenceControllerTest {

    private static final String TEST_PREF_KEY = "bar";
    private static final int FAKE_PROFILE_USER_ID = 1234;

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private LockPatternUtils mLockPatternUtils;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private UserManager mUm;

    private FakeFeatureFactory mFeatureFactory;
    private Context mContext;
    private FingerprintProfileStatusPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)).thenReturn(true);
        ShadowApplication.getInstance().setSystemService(Context.FINGERPRINT_SERVICE,
                mFingerprintManager);
        ShadowApplication.getInstance().setSystemService(Context.USER_SERVICE, mUm);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFeatureFactory.securityFeatureProvider.getLockPatternUtils(mContext))
                .thenReturn(mLockPatternUtils);
        when(mUm.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[] {1234});
        mController = new FingerprintProfileStatusPreferenceController(mContext, TEST_PREF_KEY);
    }

    @Test
    public void getUserId_shouldReturnProfileId() {
        assertThat(mController.getUserId()).isEqualTo(FAKE_PROFILE_USER_ID);
    }

    @Test
    public void getPreferenceKey_byDefault_returnsDefaultValue() {
        mController = new FingerprintProfileStatusPreferenceController(mContext);

        assertThat(mController.getPreferenceKey()).isEqualTo("fingerprint_settings_profile");
    }

    @Test
    public void getPreferenceKey_whenGivenValue_returnsGivenValue() {
        mController = new FingerprintProfileStatusPreferenceController(mContext, "key");

        assertThat(mController.getPreferenceKey()).isEqualTo("key");
    }
}
