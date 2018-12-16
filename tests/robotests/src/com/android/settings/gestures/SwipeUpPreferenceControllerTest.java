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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

import com.android.internal.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class SwipeUpPreferenceControllerTest {

    private Context mContext;
    private ShadowPackageManager mPackageManager;
    private SwipeUpPreferenceController mController;

    private static final String ACTION_QUICKSTEP = "android.intent.action.QUICKSTEP_SERVICE";
    private static final String KEY_SWIPE_UP = "gesture_swipe_up";

    @Before
    public void setUp() {
        SettingsShadowResources.overrideResource(R.bool.config_swipe_up_gesture_setting_available,
                true);
        SettingsShadowResources.overrideResource(R.bool.config_swipe_up_gesture_default, true);

        mContext = RuntimeEnvironment.application;
        mPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        mController = new SwipeUpPreferenceController(mContext, KEY_SWIPE_UP);
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

        assertThat(SwipeUpPreferenceController.isGestureAvailable(mContext)).isTrue();
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

        assertThat(SwipeUpPreferenceController.isGestureAvailable(mContext)).isFalse();
    }

    @Test
    public void testIsGestureAvailable_noMatchingServiceExists_shouldReturnFalse() {
        assertThat(SwipeUpPreferenceController.isGestureAvailable(mContext)).isFalse();
    }

    @Test
    public void testIsChecked_defaultIsTrue_shouldReturnTrue() {
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void testIsChecked_defaultIsFalse_shouldReturnFalse() {
        SettingsShadowResources.overrideResource(R.bool.config_swipe_up_gesture_default, false);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void testIsChecked_setCheckedTrue_shouldReturnTrue() {
        // Set the setting to be enabled.
        mController.setChecked(true);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void testIsChecked_setCheckedFalse_shouldReturnFalse() {
        // Set the setting to be disabled.
        mController.setChecked(false);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final SwipeUpPreferenceController controller =
                new SwipeUpPreferenceController(mContext, "gesture_swipe_up");
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final SwipeUpPreferenceController controller =
                new SwipeUpPreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }
}
