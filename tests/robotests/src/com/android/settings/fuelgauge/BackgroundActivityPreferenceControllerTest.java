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

import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.UserManager;
import android.support.v14.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.enterprise.DevicePolicyManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BackgroundActivityPreferenceControllerTest {
    private static final int UID_NORMAL = 1234;
    private static final int UID_SPECIAL = 2345;
    private static final String HIGH_SDK_PACKAGE = "com.android.package.high";
    private static final String LOW_SDK_PACKAGE = "com.android.package.low";
    private static final String[] PACKAGES_NORMAL = {LOW_SDK_PACKAGE};
    private static final String[] PACKAGES_SPECIAL = {HIGH_SDK_PACKAGE, LOW_SDK_PACKAGE};

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
    private DevicePolicyManagerWrapper mDevicePolicyManagerWrapper;
    private BackgroundActivityPreferenceController mController;
    private SwitchPreference mPreference;
    private Context mShadowContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mShadowContext = RuntimeEnvironment.application;
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(Context.APP_OPS_SERVICE)).thenReturn(mAppOpsManager);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getSystemService(Context.DEVICE_POLICY_SERVICE)).thenReturn(
                mDevicePolicyManager);
        when(mPackageManager.getPackagesForUid(UID_NORMAL)).thenReturn(PACKAGES_NORMAL);
        when(mPackageManager.getPackagesForUid(UID_SPECIAL)).thenReturn(PACKAGES_SPECIAL);

        when(mPackageManager.getApplicationInfo(HIGH_SDK_PACKAGE, PackageManager.GET_META_DATA))
                .thenReturn(mHighApplicationInfo);
        when(mPackageManager.getApplicationInfo(LOW_SDK_PACKAGE, PackageManager.GET_META_DATA))
                .thenReturn(mLowApplicationInfo);
        mHighApplicationInfo.targetSdkVersion = Build.VERSION_CODES.O;
        mLowApplicationInfo.targetSdkVersion = Build.VERSION_CODES.L;

        mPreference = new SwitchPreference(mShadowContext);
        mController = spy(new BackgroundActivityPreferenceController(mContext, UID_NORMAL));
        mController.isAvailable();
        mController.mDpm = mDevicePolicyManagerWrapper;
    }

    @Test
    public void testOnPreferenceChange_TurnOnCheck_MethodInvoked() {
        mController.onPreferenceChange(mPreference, true);

        verify(mAppOpsManager).setMode(AppOpsManager.OP_RUN_IN_BACKGROUND, UID_NORMAL,
                mController.getTargetPackage(), AppOpsManager.MODE_ALLOWED);
        verify(mController).updateSummary(mPreference);
    }

    @Test
    public void testOnPreferenceChange_TurnOffCheck_MethodInvoked() {
        mController.onPreferenceChange(mPreference, false);

        verify(mAppOpsManager).setMode(AppOpsManager.OP_RUN_IN_BACKGROUND, UID_NORMAL,
                mController.getTargetPackage(), AppOpsManager.MODE_IGNORED);
        verify(mController).updateSummary(mPreference);
    }

    @Test
    public void testUpdateState_CheckOn_SetCheckedTrue() {
        when(mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_IN_BACKGROUND, UID_NORMAL, LOW_SDK_PACKAGE))
                .thenReturn(AppOpsManager.MODE_DEFAULT);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
        verify(mController).updateSummary(mPreference);
    }

    @Test
    public void testUpdateState_CheckOff_SetCheckedFalse() {
        when(mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_IN_BACKGROUND, UID_NORMAL, LOW_SDK_PACKAGE))
                .thenReturn(AppOpsManager.MODE_IGNORED);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
        verify(mController).updateSummary(mPreference);
    }

    @Test
    public void testUpdateSummary_modeError_showSummaryDisabled() {
        when(mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_IN_BACKGROUND, UID_NORMAL, LOW_SDK_PACKAGE))
                .thenReturn(AppOpsManager.MODE_ERRORED);
        final CharSequence expectedSummary = mShadowContext.getText(
                R.string.background_activity_summary_disabled);
        mController.updateSummary(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(expectedSummary);
    }

    @Test
    public void testUpdateSummary_modeDefault_showSummaryOn() {
        when(mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_IN_BACKGROUND, UID_NORMAL, LOW_SDK_PACKAGE))
                .thenReturn(AppOpsManager.MODE_DEFAULT);
        final CharSequence expectedSummary = mShadowContext.getText(
                R.string.background_activity_summary_on);

        mController.updateSummary(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(expectedSummary);
    }

    @Test
    public void testUpdateSummary_modeIgnored_showSummaryOff() {
        when(mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_IN_BACKGROUND, UID_NORMAL, LOW_SDK_PACKAGE))
                .thenReturn(AppOpsManager.MODE_IGNORED);
        final CharSequence expectedSummary = mShadowContext.getText(
                R.string.background_activity_summary_off);

        mController.updateSummary(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(expectedSummary);
    }

    @Test
    public void testIsPackageAvailable_SdkLowerThanO_ReturnTrue() {
        assertThat(mController.isLegacyApp(LOW_SDK_PACKAGE)).isTrue();
    }

    @Test
    public void testIsPackageAvailable_SdkLargerOrEqualThanO_ReturnFalse() {
        assertThat(mController.isLegacyApp(HIGH_SDK_PACKAGE)).isFalse();
    }

    @Test
    public void testMultiplePackages_ReturnStatusForTargetPackage() {
        mController = new BackgroundActivityPreferenceController(mContext, UID_SPECIAL);
        mController.mDpm = mDevicePolicyManagerWrapper;
        when(mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_IN_BACKGROUND, UID_SPECIAL, LOW_SDK_PACKAGE))
                .thenReturn(AppOpsManager.MODE_ALLOWED);
        when(mAppOpsManager
                .checkOpNoThrow(AppOpsManager.OP_RUN_IN_BACKGROUND, UID_SPECIAL, HIGH_SDK_PACKAGE))
                .thenReturn(AppOpsManager.MODE_IGNORED);

        final boolean available = mController.isAvailable();
        mController.updateState(mPreference);

        assertThat(available).isTrue();
        // Should get status from LOW_SDK_PACKAGE
        assertThat(mPreference.isChecked()).isTrue();
    }
}
