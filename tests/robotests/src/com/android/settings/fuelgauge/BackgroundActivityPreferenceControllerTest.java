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

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.fuelgauge.PowerWhitelistBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowFragment.class)
public class BackgroundActivityPreferenceControllerTest {

    private static final int UID_LOW_SDK = 1234;
    private static final String HIGH_SDK_PACKAGE = "com.android.package.high";
    private static final String LOW_SDK_PACKAGE = "com.android.package.low";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private AppOpsManager mAppOpsManager;
    @Mock
    private ApplicationInfo mHighApplicationInfo;
    @Mock
    private ApplicationInfo mLowApplicationInfo;
    @Mock
    private UserManager mUserManager;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    @Mock
    private DevicePolicyManager mDevicePolicyManagerWrapper;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AdvancedPowerUsageDetail mFragment;
    @Mock
    private PowerWhitelistBackend mPowerWhitelistBackend;
    private BackgroundActivityPreferenceController mController;
    private RestrictedPreference mPreference;
    private Context mShadowContext;
    private BatteryUtils mBatteryUtils;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mShadowContext = RuntimeEnvironment.application;
        FakeFeatureFactory.setupForTest();
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(Context.APP_OPS_SERVICE)).thenReturn(mAppOpsManager);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getSystemService(Context.DEVICE_POLICY_SERVICE)).thenReturn(
                mDevicePolicyManager);

        when(mPackageManager.getApplicationInfo(HIGH_SDK_PACKAGE, PackageManager.GET_META_DATA))
                .thenReturn(mHighApplicationInfo);
        when(mPackageManager.getApplicationInfo(LOW_SDK_PACKAGE, PackageManager.GET_META_DATA))
                .thenReturn(mLowApplicationInfo);

        when(mPowerWhitelistBackend.isWhitelisted(LOW_SDK_PACKAGE)).thenReturn(false);
        mHighApplicationInfo.targetSdkVersion = Build.VERSION_CODES.O;
        mLowApplicationInfo.targetSdkVersion = Build.VERSION_CODES.L;

        mBatteryUtils = spy(new BatteryUtils(mShadowContext));
        doNothing().when(mBatteryUtils).setForceAppStandby(anyInt(), anyString(), anyInt());

        mPreference = spy(new RestrictedPreference(mShadowContext, null /* attrs */));
        mPreference.setKey(BackgroundActivityPreferenceController.KEY_BACKGROUND_ACTIVITY);
        mController = spy(new BackgroundActivityPreferenceController(
                mContext, mFragment, UID_LOW_SDK, LOW_SDK_PACKAGE, mPowerWhitelistBackend));
        mController.mDpm = mDevicePolicyManagerWrapper;
        mController.mBatteryUtils = mBatteryUtils;
    }

    @Test
    public void handlePreferenceTreeClick_restrictApp_showDialog() {
        doReturn(AppOpsManager.MODE_ALLOWED).when(mAppOpsManager)
                .checkOpNoThrow(anyInt(), anyInt(), anyString());

        mController.handlePreferenceTreeClick(mPreference);

        verify(mController).showDialog(false /* restrict */);
    }

    @Test
    public void handlePreferenceTreeClick_unRestrictApp_showDialog() {
        doReturn(AppOpsManager.MODE_IGNORED).when(mAppOpsManager)
                .checkOpNoThrow(anyInt(), anyInt(), anyString());

        mController.handlePreferenceTreeClick(mPreference);

        verify(mController).showDialog(true /* restrict */);
    }

    @Test
    public void updateState_noError_setEnabled() {
        when(mAppOpsManager.checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, UID_LOW_SDK,
                LOW_SDK_PACKAGE)).thenReturn(AppOpsManager.MODE_ALLOWED);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        verify(mController).updateSummary(mPreference);
    }

    @Test
    public void updateState_whitelisted() {
        when(mPowerWhitelistBackend.isWhitelisted(LOW_SDK_PACKAGE)).thenReturn(true);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary()).isEqualTo(
                mShadowContext.getText(R.string.background_activity_summary_whitelisted));
    }

    @Test
    public void updateState_disabledByAdmin_doNothing() {
        doReturn(true).when(mPreference).isDisabledByAdmin();

        mController.updateState(mPreference);

        verify(mPreference, never()).setEnabled(anyBoolean());
    }

    @Test
    public void updateSummary_modeError_showSummaryDisabled() {
        when(mAppOpsManager.checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, UID_LOW_SDK,
                LOW_SDK_PACKAGE)).thenReturn(AppOpsManager.MODE_ERRORED);
        final CharSequence expectedSummary = mShadowContext.getText(
                R.string.background_activity_summary_disabled);
        mController.updateSummary(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(expectedSummary);
    }

    @Test
    public void updateSummary_modeDefault_showNotRestricted() {
        when(mAppOpsManager.checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, UID_LOW_SDK,
                LOW_SDK_PACKAGE)).thenReturn(AppOpsManager.MODE_DEFAULT);

        mController.updateSummary(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo("App can use battery in background");
    }

    @Test
    public void updateSummary_modeIgnored_showRestricted() {
        when(mAppOpsManager.checkOpNoThrow(AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, UID_LOW_SDK,
                LOW_SDK_PACKAGE)).thenReturn(AppOpsManager.MODE_IGNORED);

        mController.updateSummary(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo("Restricted");
    }

    @Test
    public void isAvailable_ReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }
}
