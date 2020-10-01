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

package com.android.settings.security.screenlock;

import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;

import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

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
public class LockScreenPreferenceControllerTest {

    private static final int FAKE_PROFILE_USER_ID = 1234;

    @Mock
    private LockPatternUtils mLockPatternUtils;
    @Mock
    private UserManager mUm;
    @Mock
    private PreferenceScreen mScreen;

    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private FakeFeatureFactory mFeatureFactory;
    private Context mContext;
    private LockScreenPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        ShadowApplication.getInstance().setSystemService(Context.USER_SERVICE, mUm);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFeatureFactory.securityFeatureProvider.getLockPatternUtils(mContext))
                .thenReturn(mLockPatternUtils);
        when(mUm.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[] {FAKE_PROFILE_USER_ID});
        mPreference = new Preference(mContext);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = new LockScreenPreferenceController(mContext, "Test_key");
    }

    @Test
    public void getAvailabilityStatus_notSecure_lockscreenDisabled_AVAILABLE() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt())).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_notSecure_lockscreenEnabled_AVAILABLE() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(false);
        when(mLockPatternUtils.isLockScreenDisabled(anyInt())).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_secure_hasLockScreen_AVAILABLE() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(anyInt()))
                .thenReturn(PASSWORD_QUALITY_ALPHABETIC);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_secure_noLockScreen_AVAILABLE() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(anyInt()))
                .thenReturn(PASSWORD_QUALITY_UNSPECIFIED);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void onResume_available_shouldShow() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(anyInt()))
                .thenReturn(PASSWORD_QUALITY_ALPHABETIC);

        mController.displayPreference(mScreen);
        mLifecycle.handleLifecycleEvent(ON_RESUME);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void onResume_unavailable_shouldShow() {
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);
        when(mLockPatternUtils.getKeyguardStoredPasswordQuality(anyInt()))
                .thenReturn(PASSWORD_QUALITY_UNSPECIFIED);

        mController.displayPreference(mScreen);
        mLifecycle.handleLifecycleEvent(ON_RESUME);

        assertThat(mPreference.isVisible()).isTrue();
    }
}
