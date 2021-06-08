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
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.service.trust.TrustAgentService;

import android.text.TextUtils;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowApplicationPackageManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowLockPatternUtils.class,
        ShadowRestrictedLockUtilsInternal.class,
        ShadowDevicePolicyManager.class,
        ShadowApplicationPackageManager.class,
        TrustAgentsPreferenceControllerTest.ShadowTrustAgentManager.class
})
public class TrustAgentsPreferenceControllerTest {

    private static final Intent TEST_INTENT =
            new Intent(TrustAgentService.SERVICE_INTERFACE);

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

    @After
    public void tearDown() {
        ShadowTrustAgentManager.clearPermissionGrantedList();
    }

    @Test
    public void getAvailabilityStatus_byDefault_shouldBeShown() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void onStart_noTrustAgent_shouldNotAddPreference() {
        final List<ResolveInfo> availableAgents = createFakeAvailableAgents();
        mPackageManager.setResolveInfosForIntent(TEST_INTENT, availableAgents);

        mController.displayPreference(mPreferenceScreen);
        mController.onStart();

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void
    onStart_hasAUninstalledTrustAgent_shouldRemoveOnePreferenceAndLeaveTwoPreferences() {
        final List<ResolveInfo> availableAgents = createFakeAvailableAgents();
        final ResolveInfo uninstalledTrustAgent = availableAgents.get(0);

        for (ResolveInfo rInfo : availableAgents) {
            ShadowTrustAgentManager.grantPermissionToResolveInfo(rInfo);
        }
        mPackageManager.setResolveInfosForIntent(TEST_INTENT, availableAgents);
        mController.displayPreference(mPreferenceScreen);
        mController.onStart();
        availableAgents.remove(uninstalledTrustAgent);

        mPackageManager.setResolveInfosForIntent(TEST_INTENT, availableAgents);
        mController.onStart();

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void onStart_hasANewTrustAgent_shouldAddOnePreferenceAndHaveFourPreferences() {
        final List<ResolveInfo> availableAgents = createFakeAvailableAgents();
        final ComponentName newComponentName = new ComponentName("test.data.packageD", "clzDDD");
        final ResolveInfo newTrustAgent = createFakeResolveInfo(newComponentName);
        for (ResolveInfo rInfo : availableAgents) {
            ShadowTrustAgentManager.grantPermissionToResolveInfo(rInfo);
        }
        mPackageManager.setResolveInfosForIntent(TEST_INTENT, availableAgents);
        mController.displayPreference(mPreferenceScreen);
        mController.onStart();
        availableAgents.add(newTrustAgent);
        ShadowTrustAgentManager.grantPermissionToResolveInfo(newTrustAgent);

        mPackageManager.setResolveInfosForIntent(TEST_INTENT, availableAgents);
        mController.onStart();

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(4);
    }

    @Test
    public void onStart_hasUnrestrictedTrustAgent_shouldAddThreeChangeablePreferences() {
        ShadowRestrictedLockUtilsInternal.setKeyguardDisabledFeatures(0);
        final List<ResolveInfo> availableAgents = createFakeAvailableAgents();
        for (ResolveInfo rInfo : availableAgents) {
            ShadowTrustAgentManager.grantPermissionToResolveInfo(rInfo);
        }
        mPackageManager.setResolveInfosForIntent(TEST_INTENT, availableAgents);

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
    public void onStart_hasRestrictedTructAgent_shouldAddThreeUnchangeablePreferences() {
        final List<ResolveInfo> availableAgents = createFakeAvailableAgents();
        for (ResolveInfo rInfo : availableAgents) {
            ShadowTrustAgentManager.grantPermissionToResolveInfo(rInfo);
        }
        mPackageManager.setResolveInfosForIntent(TEST_INTENT, availableAgents);
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

    private List<ResolveInfo> createFakeAvailableAgents() {
        final List<ComponentName> componentNames = new ArrayList<>();
        componentNames.add(new ComponentName("test.data.packageA", "clzAAA"));
        componentNames.add(new ComponentName("test.data.packageB", "clzBBB"));
        componentNames.add(new ComponentName("test.data.packageC", "clzCCC"));
        final List<ResolveInfo> result = new ArrayList<>();
        for (ComponentName cn : componentNames) {
            final ResolveInfo ri = createFakeResolveInfo(cn);
            result.add(ri);
        }
        return result;
    }

    private ResolveInfo createFakeResolveInfo(ComponentName cn) {
        final ResolveInfo ri = new ResolveInfo();
        ri.serviceInfo = new ServiceInfo();
        ri.serviceInfo.packageName = cn.getPackageName();
        ri.serviceInfo.name = cn.getClassName();
        ri.serviceInfo.applicationInfo = new ApplicationInfo();
        ri.serviceInfo.applicationInfo.packageName = cn.getPackageName();
        ri.serviceInfo.applicationInfo.name = cn.getClassName();
        return ri;
    }

    @Implements(TrustAgentManager.class)
    public static class ShadowTrustAgentManager {
        private final static List<ResolveInfo> sPermissionGrantedList = new ArrayList<>();

        @Implementation
        protected boolean shouldProvideTrust(ResolveInfo resolveInfo, PackageManager pm) {
            for (ResolveInfo info : sPermissionGrantedList) {
                if (info.serviceInfo.equals(resolveInfo.serviceInfo)) {
                    return true;
                }
            }

            return false;
        }

        private static void grantPermissionToResolveInfo(ResolveInfo rInfo) {
            sPermissionGrantedList.add(rInfo);
        }

        private static void clearPermissionGrantedList() {
            sPermissionGrantedList.clear();
        }
    }
}
