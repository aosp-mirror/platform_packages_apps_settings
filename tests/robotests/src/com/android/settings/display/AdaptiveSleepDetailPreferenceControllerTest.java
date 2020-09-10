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

package com.android.settings.display;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.internal.R;
import com.android.settings.bluetooth.RestrictionUtils;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {SettingsShadowResources.class})
public class AdaptiveSleepDetailPreferenceControllerTest {
    private static RestrictedLockUtils.EnforcedAdmin sFakeEnforcedAdmin;

    @BeforeClass
    public static void beforeClass() {
        sFakeEnforcedAdmin = new RestrictedLockUtils.EnforcedAdmin(
                new ComponentName("test.package", "test.Class"),
                UserHandle.of(10));
    }

    private AdaptiveSleepDetailPreferenceController mController;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private RestrictionUtils mRestrictionUtils;
    @Mock
    private RestrictedSwitchPreference mPreference;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = Mockito.spy(RuntimeEnvironment.application);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        mController = new AdaptiveSleepDetailPreferenceController(mContext, "test_key",
                mRestrictionUtils);
    }

    @Test
    public void isSliceable_returnTrue() {
        mController.onPreferenceChange(null, true);
        assertThat(mController.isSliceable()).isTrue();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_configTrueSet_shouldReturnAvailable() {
        SettingsShadowResources.overrideResource(R.bool.config_adaptive_sleep_available, true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_configFalseSet_shouldReturnUnsupportedOnDevice() {
        SettingsShadowResources.overrideResource(R.bool.config_adaptive_sleep_available, false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void updateState_noRestriction_allowScreenAttentionSet() {
        when(mRestrictionUtils.checkIfRestrictionEnforced(mContext,
                UserManager.DISALLOW_CONFIG_SCREEN_TIMEOUT)).thenReturn(null);

        mController.updateState(mPreference);

        verify(mPreference, never()).setDisabledByAdmin(
                any(RestrictedLockUtils.EnforcedAdmin.class));
    }

    @Test
    public void updateState_enforceRestrictions_disallowScreenAttentionSet() {
        when(mRestrictionUtils.checkIfRestrictionEnforced(mContext,
                UserManager.DISALLOW_CONFIG_SCREEN_TIMEOUT)).thenReturn(sFakeEnforcedAdmin);

        mController.updateState(mPreference);

        verify(mPreference).setDisabledByAdmin(sFakeEnforcedAdmin);
    }
}
