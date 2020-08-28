/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import static com.android.settings.development.DevelopmentOptionsActivityRequestCodes
        .REQUEST_CODE_ENABLE_OEM_UNLOCK;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.os.UserManager;
import android.service.oemlock.OemLockManager;
import android.telephony.TelephonyManager;

import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class OemUnlockPreferenceControllerTest {

    private static final String OEM_UNLOCK_SUPPORTED_KEY = "ro.oem_unlock_supported";
    private static final String UNSUPPORTED = "-9999";
    private static final String SUPPORTED = "1";

    @Mock
    private Context mContext;
    @Mock
    private Activity mActivity;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;
    @Mock
    private RestrictedSwitchPreference mPreference;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private OemLockManager mOemLockManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private Resources mResources;
    private OemUnlockPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        SystemProperties.set(OEM_UNLOCK_SUPPORTED_KEY, SUPPORTED);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getPackageManager().hasSystemFeature(PackageManager
                    .FEATURE_TELEPHONY_CARRIERLOCK)).thenReturn(true);
        when(mContext.getSystemService(Context.OEM_LOCK_SERVICE)).thenReturn(mOemLockManager);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getResources()).thenReturn(mResources);
        mController = new OemUnlockPreferenceController(mContext, mActivity, mFragment);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        when(mFragment.getChildFragmentManager())
            .thenReturn(mock(FragmentManager.class, Answers.RETURNS_DEEP_STUBS));
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void OemUnlockPreferenceController_oemUnlockUnsupported_shouldNotCrash() {
        SystemProperties.set(OEM_UNLOCK_SUPPORTED_KEY, UNSUPPORTED);

        new OemUnlockPreferenceController(mContext, mActivity, mFragment);
    }

    @Test
    public void OemUnlockPreferenceController_oemUnlockSupported_shouldNotCrash() {
        new OemUnlockPreferenceController(mContext, mActivity, mFragment);
    }

    @Test
    public void isAvailable_shouldReturnTrueWhenOemLockManagerIsNotNull() {
        boolean returnValue = mController.isAvailable();

        assertThat(returnValue).isTrue();
    }

    @Test
    public void isAvailable_shouldReturnFalseWhenOemLockManagerIsNull() {
        when(mContext.getSystemService(Context.OEM_LOCK_SERVICE)).thenReturn(null);
        mController = new OemUnlockPreferenceController(mContext, mActivity, mFragment);
        boolean returnValue = mController.isAvailable();

        assertThat(returnValue).isFalse();
    }

    @Test
    public void onPreferenceChanged_turnOnUnlock() {
        mController = spy(mController);
        doReturn(false).when(mController).showKeyguardConfirmation(mResources,
                REQUEST_CODE_ENABLE_OEM_UNLOCK);
        doNothing().when(mController).confirmEnableOemUnlock();

        mController.onPreferenceChange(null, true);

        verify(mController).confirmEnableOemUnlock();
    }

    @Test
    public void onPreferenceChanged_turnOffUnlock() {
        mController = spy(mController);
        mController.onPreferenceChange(null, false);
        doReturn(false).when(mController).isBootloaderUnlocked();

        verify(mFragment).getChildFragmentManager();
    }

    @Test
    public void updateState_preferenceShouldBeCheckedAndShouldBeDisabled() {
        mController = spy(mController);
        doReturn(true).when(mController).isOemUnlockedAllowed();
        doReturn(true).when(mController).isOemUnlockAllowedByUserAndCarrier();
        doReturn(true).when(mController).isBootloaderUnlocked();

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
        verify(mPreference).setEnabled(false);
    }

    @Test
    public void updateState_preferenceShouldBeUncheckedAndShouldBeDisabled() {
        mController = spy(mController);
        doReturn(false).when(mController).isOemUnlockedAllowed();
        doReturn(true).when(mController).isOemUnlockAllowedByUserAndCarrier();
        doReturn(true).when(mController).isBootloaderUnlocked();

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);
    }

    @Test
    public void updateState_preferenceShouldBeCheckedAndShouldBeEnabled() {
        mController = spy(mController);
        doReturn(true).when(mController).isOemUnlockedAllowed();
        doReturn(true).when(mController).isOemUnlockAllowedByUserAndCarrier();
        doReturn(false).when(mController).isBootloaderUnlocked();

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
        verify(mPreference).setEnabled(true);
    }

    @Test
    public void onActivityResult_shouldReturnTrue() {
        final boolean result = mController.onActivityResult(REQUEST_CODE_ENABLE_OEM_UNLOCK,
                Activity.RESULT_OK, null);

        assertThat(result).isTrue();
    }

    @Test
    public void onActivityResult_shouldReturnFalse() {
        final boolean result = mController.onActivityResult(123454, 1434, null);

        assertThat(result).isFalse();
    }

    @Test
    public void onDeveloperOptionsEnabled_preferenceShouldCheckRestriction() {
        mController = spy(mController);
        doReturn(false).when(mController).isOemUnlockAllowedByUserAndCarrier();
        doReturn(false).when(mController).isBootloaderUnlocked();
        when(mPreference.isEnabled()).thenReturn(true);

        mController.onDeveloperOptionsEnabled();

        verify(mPreference).checkRestrictionAndSetDisabled(UserManager.DISALLOW_FACTORY_RESET);
    }

    @Test
    public void onOemUnlockConfirmed_oemManagerShouldSetUnlockAllowedByUser() {
        mController.onOemUnlockConfirmed();

        verify(mOemLockManager).setOemUnlockAllowedByUser(true);
    }
}
