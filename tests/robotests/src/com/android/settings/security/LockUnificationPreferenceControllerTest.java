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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

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
public class LockUnificationPreferenceControllerTest {

    private static final int FAKE_PROFILE_USER_ID = 1234;

    @Mock
    private LockPatternUtils mLockPatternUtils;
    @Mock
    private UserManager mUm;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SecuritySettings mHost;

    private Context mContext;
    private LockUnificationPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        ShadowApplication.getInstance().setSystemService(Context.USER_SERVICE, mUm);
        when(mUm.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[] {FAKE_PROFILE_USER_ID});

        final FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.securityFeatureProvider.getLockPatternUtils(mContext))
                .thenReturn(mLockPatternUtils);
    }

    private void init() {
        mController = new LockUnificationPreferenceController(mContext, mHost);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mPreference = new Preference(mContext);
    }

    @Test
    public void isAvailable_noProfile_false() {
        when(mUm.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[0]);
        init();

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_separateChallengeNotAllowed_false() {
        when(mLockPatternUtils.isSeparateProfileChallengeAllowed(anyInt())).thenReturn(false);
        init();

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_separateChallengeAllowed_true() {
        when(mLockPatternUtils.isSeparateProfileChallengeAllowed(anyInt())).thenReturn(true);
        init();

        assertThat(mController.isAvailable()).isTrue();
    }
}
