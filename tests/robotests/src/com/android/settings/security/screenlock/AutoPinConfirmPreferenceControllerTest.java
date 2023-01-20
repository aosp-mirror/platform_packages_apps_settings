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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.widget.LockPatternUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AutoPinConfirmPreferenceControllerTest {
    private static final Integer TEST_USER_ID = 1;
    @Mock
    private LockPatternUtils mLockPatternUtils;
    private AutoPinConfirmPreferenceController mController;
    private SwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = ApplicationProvider.getApplicationContext();
        mController =
                new AutoPinConfirmPreferenceController(context, TEST_USER_ID, mLockPatternUtils);
        mPreference = new SwitchPreference(context);
    }

    @Test
    public void isAvailable_featureEnabledAndLockSetToNone_shouldReturnFalse() {
        when(mLockPatternUtils.isSecure(TEST_USER_ID)).thenReturn(true);
        when(mLockPatternUtils.isAutoPinConfirmFeatureAvailable()).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_featureEnabledAndLockSetToPassword_shouldReturnFalse() {
        when(mLockPatternUtils.isSecure(TEST_USER_ID)).thenReturn(true);
        when(mLockPatternUtils.isAutoPinConfirmFeatureAvailable()).thenReturn(true);
        when(mLockPatternUtils.getCredentialTypeForUser(TEST_USER_ID))
                .thenReturn(LockPatternUtils.CREDENTIAL_TYPE_PASSWORD);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_featureEnabledAndLockSetToPIN_lengthLessThanSix_shouldReturnFalse() {
        when(mLockPatternUtils.isAutoPinConfirmFeatureAvailable()).thenReturn(true);
        when(mLockPatternUtils.getCredentialTypeForUser(TEST_USER_ID))
                .thenReturn(LockPatternUtils.CREDENTIAL_TYPE_PIN);
        when(mLockPatternUtils.getPinLength(TEST_USER_ID)).thenReturn(5L);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_featureEnabledAndLockSetToPIN_lengthMoreThanEqSix_shouldReturnTrue() {
        when(mLockPatternUtils.isSecure(TEST_USER_ID)).thenReturn(true);
        when(mLockPatternUtils.isAutoPinConfirmFeatureAvailable()).thenReturn(true);
        when(mLockPatternUtils.getCredentialTypeForUser(TEST_USER_ID))
                .thenReturn(LockPatternUtils.CREDENTIAL_TYPE_PIN);
        when(mLockPatternUtils.getPinLength(TEST_USER_ID)).thenReturn(6L);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_featureDisabledAndLockSetToPIN_shouldReturnFalse() {
        when(mLockPatternUtils.isAutoPinConfirmFeatureAvailable()).thenReturn(false);
        when(mLockPatternUtils.isSecure(TEST_USER_ID)).thenReturn(true);
        when(mLockPatternUtils.getCredentialTypeForUser(TEST_USER_ID))
                .thenReturn(LockPatternUtils.CREDENTIAL_TYPE_PIN);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_ChangingSettingState_shouldSetPreferenceToAppropriateCheckedState() {
        when(mLockPatternUtils.isAutoPinConfirmFeatureAvailable()).thenReturn(true);
        // When auto_pin_confirm setting is disabled, switchPreference is unchecked
        when(mLockPatternUtils.isAutoPinConfirmEnabled(TEST_USER_ID)).thenReturn(false);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();

        // When auto_pin_confirm setting is enabled, switchPreference is checked
        when(mLockPatternUtils.isAutoPinConfirmEnabled(TEST_USER_ID)).thenReturn(true);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void onPreferenceChange_shouldUpdatePinAutoConfirmSetting() {
        when(mLockPatternUtils.isAutoPinConfirmFeatureAvailable()).thenReturn(true);
        mController.onPreferenceChange(mPreference, /* newValue= */ true);
        verify(mLockPatternUtils).setAutoPinConfirm(true, TEST_USER_ID);
    }
}
