/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;

import com.android.settings.accounts.AccountRestrictionHelper;
import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class EmergencyBroadcastPreferenceControllerTest {

    private static final String PREF_TEST_KEY = "test_key";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private AccountRestrictionHelper mAccountHelper;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private RestrictedPreference mPreference;

    private EmergencyBroadcastPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mController =
            new EmergencyBroadcastPreferenceController(mContext, mAccountHelper, PREF_TEST_KEY);
    }

    @Test
    public void updateState_shouldCheckRestriction() {
        mController.updateState(mPreference);

        verify(mPreference).checkRestrictionAndSetDisabled(anyString());
    }

    @Test
    public void getPreferenceKey_shouldReturnKeyDefinedInConstructor() {
        assertThat(mController.getPreferenceKey()).isEqualTo(PREF_TEST_KEY);
    }

    @Test
    public void isAvailable_notAdminUser_shouldReturnFalse() {
        when(mUserManager.isAdminUser()).thenReturn(false);
        when(mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks)).thenReturn(true);
        when(mPackageManager.getApplicationEnabledSetting(anyString()))
                .thenReturn(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        when(mAccountHelper.hasBaseUserRestriction(anyString(), anyInt())).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_hasConfigCellBroadcastRestriction_shouldReturnFalse() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks)).thenReturn(true);
        when(mPackageManager.getApplicationEnabledSetting(anyString()))
                .thenReturn(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        when(mAccountHelper.hasBaseUserRestriction(
                eq(UserManager.DISALLOW_CONFIG_CELL_BROADCASTS), anyInt())).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_cellBroadcastAppLinkDisabled_shouldReturnFalse() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks)).thenReturn(false);
        when(mPackageManager.getApplicationEnabledSetting(anyString()))
                .thenReturn(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        when(mAccountHelper.hasBaseUserRestriction(anyString(), anyInt())).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_cellBroadcastReceiverDisabled_shouldReturnFalse() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks)).thenReturn(true);
        when(mPackageManager.getApplicationEnabledSetting(anyString()))
                .thenReturn(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        when(mAccountHelper.hasBaseUserRestriction(anyString(), anyInt())).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }
}
