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

package com.android.settings.security.screenlock;

import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PASSWORD_OR_PIN;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PATTERN;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PIN;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.SwitchPreference;

import com.android.internal.widget.LockPatternUtils;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PinPrivacyPreferenceControllerTest {

    private static final int TEST_USER_ID = 0;

    @Mock
    private LockPatternUtils mLockPatternUtils;
    private Context mContext;
    private PinPrivacyPreferenceController mController;
    private SwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController =
                new PinPrivacyPreferenceController(mContext, TEST_USER_ID, mLockPatternUtils);
        mPreference = new SwitchPreference(mContext);
    }

    @Test
    public void isAvailable_lockSetToPin_shouldReturnTrue() {
        when(mLockPatternUtils.getCredentialTypeForUser(TEST_USER_ID)).thenReturn(
                CREDENTIAL_TYPE_PIN);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_lockSetToOther_shouldReturnFalse() {
        when(mLockPatternUtils.getCredentialTypeForUser(TEST_USER_ID)).thenReturn(
                CREDENTIAL_TYPE_PATTERN);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_shouldSetPref() {
        when(mLockPatternUtils.isPinEnhancedPrivacyEnabled(TEST_USER_ID)).thenReturn(true);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_shouldSetPref_false() {
        when(mLockPatternUtils.isPinEnhancedPrivacyEnabled(TEST_USER_ID)).thenReturn(false);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void onPreferenceChange_shouldUpdateLockPatternUtils() {
        mController.onPreferenceChange(mPreference, true);
        verify(mLockPatternUtils).setPinEnhancedPrivacyEnabled(true, TEST_USER_ID);
    }

    @Test
    public void getPreferenceKey_returnsConst() {
        assertThat(mController.getPreferenceKey().equals("enhancedPinPrivacy")).isTrue();
    }
}
