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

package com.android.settings.gestures;

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import static com.android.settings.gestures.SystemNavigationPreferenceController.PREF_KEY_SYSTEM_NAVIGATION;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.text.TextUtils;

import com.android.internal.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class SystemNavigationPreferenceControllerTest {

    private Context mContext;
    private ShadowPackageManager mPackageManager;

    @Mock
    private Context mMockContext;
    @Mock
    private PackageManager mMockPackageManager;

    private SystemNavigationPreferenceController mController;

    private static final String ACTION_QUICKSTEP = "android.intent.action.QUICKSTEP_SERVICE";
    private static final String TEST_RECENTS_COMPONENT_NAME = "test.component.name/.testActivity";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        SettingsShadowResources.overrideResource(R.bool.config_swipe_up_gesture_setting_available,
                true);

        mContext = RuntimeEnvironment.application;
        mPackageManager = Shadows.shadowOf(mContext.getPackageManager());

        mController = new SystemNavigationPreferenceController(mContext,
                PREF_KEY_SYSTEM_NAVIGATION);

        when(mMockContext.getPackageManager()).thenReturn(mMockPackageManager);
        when(mMockContext.getString(com.android.internal.R.string.config_recentsComponentName))
                .thenReturn(TEST_RECENTS_COMPONENT_NAME);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void testIsGestureAvailable_matchingServiceExists_shouldReturnTrue() {
        final ComponentName recentsComponentName = ComponentName.unflattenFromString(
                mContext.getString(com.android.internal.R.string.config_recentsComponentName));
        final Intent quickStepIntent = new Intent(ACTION_QUICKSTEP)
                .setPackage(recentsComponentName.getPackageName());
        final ResolveInfo info = new ResolveInfo();
        info.serviceInfo = new ServiceInfo();
        info.resolvePackageName = recentsComponentName.getPackageName();
        info.serviceInfo.packageName = info.resolvePackageName;
        info.serviceInfo.name = recentsComponentName.getClassName();
        info.serviceInfo.applicationInfo = new ApplicationInfo();
        info.serviceInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        mPackageManager.addResolveInfoForIntent(quickStepIntent, info);

        assertThat(SystemNavigationPreferenceController.isGestureAvailable(mContext)).isTrue();
    }

    @Test
    public void testIsGestureAvailable_overlayDisabled_matchingServiceExists_shouldReturnFalse() {
        SettingsShadowResources.overrideResource(R.bool.config_swipe_up_gesture_setting_available,
                false);

        final ComponentName recentsComponentName = ComponentName.unflattenFromString(
                mContext.getString(com.android.internal.R.string.config_recentsComponentName));
        final Intent quickStepIntent = new Intent(ACTION_QUICKSTEP)
                .setPackage(recentsComponentName.getPackageName());
        mPackageManager.addResolveInfoForIntent(quickStepIntent, new ResolveInfo());

        assertThat(SystemNavigationPreferenceController.isGestureAvailable(mContext)).isFalse();
    }

    @Test
    public void testIsGestureAvailable_noMatchingServiceExists_shouldReturnFalse() {
        assertThat(SystemNavigationPreferenceController.isGestureAvailable(mContext)).isFalse();
    }

    @Test
    public void testIsOverlayPackageAvailable_noOverlayPackage_shouldReturnFalse() {
        assertThat(SystemNavigationPreferenceController.isOverlayPackageAvailable(mContext,
                "com.package.fake")).isFalse();
    }

    @Test
    public void testIsSwipeUpEnabled() {
        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_2BUTTON);
        assertThat(SystemNavigationPreferenceController.is2ButtonNavigationEnabled(
                mContext)).isTrue();

        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_3BUTTON);
        assertThat(SystemNavigationPreferenceController.is2ButtonNavigationEnabled(
                mContext)).isFalse();

        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_GESTURAL);
        assertThat(SystemNavigationPreferenceController.is2ButtonNavigationEnabled(
                mContext)).isFalse();
    }

    @Test
    public void testIsEdgeToEdgeEnabled() {
        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_GESTURAL);
        assertThat(SystemNavigationPreferenceController.isGestureNavigationEnabled(
                mContext)).isTrue();

        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_3BUTTON);
        assertThat(SystemNavigationPreferenceController.isGestureNavigationEnabled(
                mContext)).isFalse();

        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_2BUTTON);
        assertThat(SystemNavigationPreferenceController.isGestureNavigationEnabled(
                mContext)).isFalse();
    }

    @Test
    public void testGetSummary() {
        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_GESTURAL);
        assertThat(TextUtils.equals(mController.getSummary(), mContext.getText(
                com.android.settings.R.string.edge_to_edge_navigation_title))).isTrue();

        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_3BUTTON);
        assertThat(TextUtils.equals(mController.getSummary(),
                mContext.getText(com.android.settings.R.string.legacy_navigation_title))).isTrue();

        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_2BUTTON);
        assertThat(TextUtils.equals(mController.getSummary(), mContext.getText(
                com.android.settings.R.string.swipe_up_to_switch_apps_title))).isTrue();
    }
}