/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.face.FaceManager;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class FaceSettingsLockscreenBypassPreferenceControllerTest {

    @Mock
    private FaceManager mFaceManager;
    private SwitchPreference mPreference;
    @Mock
    private UserManager mUserManager;

    private Context mContext;
    private FaceSettingsLockscreenBypassPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);

        mController = new FaceSettingsLockscreenBypassPreferenceController(mContext, "test_key");
        ReflectionHelpers.setField(mController, "mFaceManager", mFaceManager);
        ReflectionHelpers.setField(mController, "mUserManager", mUserManager);
    }

    @Test
    public void isAvailable_whenHardwareDetected() {
        assertThat(mController.isAvailable()).isFalse();
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_isManagedProfile_shouldReturnUnsupported() {
        when(mUserManager.isManagedProfile(anyInt())).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void onPreferenceChange_settingIsUpdated() {
        boolean defaultValue = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_faceAuthDismissesKeyguard);
        boolean state = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.FACE_UNLOCK_DISMISSES_KEYGUARD, defaultValue ? 1 : 0) != 0;

        assertThat(mController.onPreferenceChange(mPreference, !state)).isTrue();
        boolean newState = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.FACE_UNLOCK_DISMISSES_KEYGUARD, 0) != 0;
        assertThat(newState).isEqualTo(!state);
    }
}
