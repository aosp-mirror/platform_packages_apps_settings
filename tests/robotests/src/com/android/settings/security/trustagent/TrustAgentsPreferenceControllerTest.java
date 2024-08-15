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

package com.android.settings.security.trustagent;

import static com.google.common.truth.Truth.assertThat;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.service.trust.TrustAgentService;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplicationPackageManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowLockPatternUtils.class,
        ShadowRestrictedLockUtilsInternal.class,
        ShadowDevicePolicyManager.class, ShadowApplicationPackageManager.class
})
public class TrustAgentsPreferenceControllerTest {
    private static final ComponentName TRUST_AGENT_A = new ComponentName(
            "test.data.packageA", "clzAAA");
    private static final ComponentName TRUST_AGENT_B = new ComponentName(
            "test.data.packageB", "clzBBB");
    private static final ComponentName TRUST_AGENT_C = new ComponentName(
            "test.data.packageC", "clzCCC");
    private static final ComponentName TRUST_AGENT_D = new ComponentName(
            "test.data.packageD", "clzDDD");

    private Context mContext;
    private ShadowApplicationPackageManager mPackageManager;
    private TrustAgentsPreferenceController mController;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPackageManager = (ShadowApplicationPackageManager) Shadows.shadowOf(
                mContext.getPackageManager());
        mController = new TrustAgentsPreferenceController(mContext, "pref_key");
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreferenceScreen.setKey("pref_key");
    }

    @Test
    public void getAvailabilityStatus_byDefault_shouldBeShown() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void onStart_noTrustAgent_shouldNotAddPreference() {
        installFakeAvailableAgents(/* grantPermission= */ false);

        mController.displayPreference(mPreferenceScreen);
        mController.onStart();

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void onStart_uninstalledTrustAgent_shouldRemoveOnePreferenceAndLeaveTwoPreferences() {
        installFakeAvailableAgents(/* grantPermission= */ true);
        mController.displayPreference(mPreferenceScreen);
        mController.onStart();
        uninstallAgent(TRUST_AGENT_A);

        mController.onStart();

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void onStart_hasANewTrustAgent_shouldAddOnePreferenceAndHaveFourPreferences() {
        installFakeAvailableAgents(/* grantPermission= */ true);
        mController.displayPreference(mPreferenceScreen);
        mController.onStart();
        installFakeAvailableAgent(TRUST_AGENT_D, /* grantPermission= */ true);

        mController.onStart();

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(4);
    }

    @Test
    public void onStart_hasUnrestrictedTrustAgent_shouldAddThreeChangeablePreferences() {
        ShadowRestrictedLockUtilsInternal.setKeyguardDisabledFeatures(0);
        installFakeAvailableAgents(/* grantPermission= */ true);

        mController.displayPreference(mPreferenceScreen);
        mController.onStart();

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(3);
        for (int i = 0; i < mPreferenceScreen.getPreferenceCount(); i++) {
            RestrictedSwitchPreference preference =
                    (RestrictedSwitchPreference) mPreferenceScreen.getPreference(i);
            assertThat(preference.isDisabledByAdmin()).isFalse();
        }
    }

    @Test
    public void onStart_hasRestrictedTrustAgent_shouldAddThreeUnchangeablePreferences() {
        installFakeAvailableAgents(/* grantPermission= */ true);
        ShadowRestrictedLockUtilsInternal.setKeyguardDisabledFeatures(
                DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS);

        mController.displayPreference(mPreferenceScreen);
        mController.onStart();

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(3);
        for (int i = 0; i < mPreferenceScreen.getPreferenceCount(); i++) {
            RestrictedSwitchPreference preference =
                    (RestrictedSwitchPreference) mPreferenceScreen.getPreference(i);
            assertThat(preference.isDisabledByAdmin()).isTrue();
        }
    }

    private void installFakeAvailableAgents(boolean grantPermission) {
        installFakeAvailableAgent(TRUST_AGENT_A, grantPermission);
        installFakeAvailableAgent(TRUST_AGENT_B, grantPermission);
        installFakeAvailableAgent(TRUST_AGENT_C, grantPermission);
    }

    private void installFakeAvailableAgent(ComponentName name,
            boolean grantPermission) {
        mPackageManager.addServiceIfNotPresent(name);
        mPackageManager.addIntentFilterForService(name,
                new IntentFilter(TrustAgentService.SERVICE_INTERFACE));
        if (!grantPermission) {
            return;
        }
        PackageInfo pkgInfo = mPackageManager.getInternalMutablePackageInfo(
                name.getPackageName());
        pkgInfo.requestedPermissions =
                new String[]{android.Manifest.permission.PROVIDE_TRUST_AGENT};
        pkgInfo.requestedPermissionsFlags =
                new int[]{PackageInfo.REQUESTED_PERMISSION_GRANTED};
    }

    private void uninstallAgent(ComponentName name) {
        mPackageManager.removeService(name);
        mPackageManager.removePackage(name.getPackageName());
    }
}
