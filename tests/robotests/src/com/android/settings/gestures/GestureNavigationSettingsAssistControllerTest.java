/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.internal.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class GestureNavigationSettingsAssistControllerTest {

    private static final String KEY_SWIPE_FOR_ASSIST = "assistant_gesture_corner_swipe";
    private static final String ACTION_QUICKSTEP = "android.intent.action.QUICKSTEP_SERVICE";
    private GestureNavigationSettingsAssistController mController;

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();

        // This sets up SystemNavigationPreferenceController.isGestureAvailable() so it returns true
        SettingsShadowResources.overrideResource(R.bool.config_swipe_up_gesture_setting_available,
                true);
        final String recentsComponentPackageName = "recents.component";
        SettingsShadowResources.overrideResource(R.string.config_recentsComponentName,
                recentsComponentPackageName + "/.ComponentName");
        final Intent quickStepIntent = new Intent(ACTION_QUICKSTEP)
                .setPackage(recentsComponentPackageName);
        final ResolveInfo info = new ResolveInfo();
        info.serviceInfo = new ServiceInfo();
        info.resolvePackageName = recentsComponentPackageName;
        info.serviceInfo.packageName = info.resolvePackageName;
        info.serviceInfo.name = recentsComponentPackageName;
        info.serviceInfo.applicationInfo = new ApplicationInfo();
        info.serviceInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        ShadowPackageManager shadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        shadowPackageManager.addResolveInfoForIntent(quickStepIntent, info);

        mController = new GestureNavigationSettingsAssistController(mContext, KEY_SWIPE_FOR_ASSIST);
    }

    @Test
    public void isAvailable_systemNavigationControllerReturnsTrue_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_systemNavigationControllerReturnsFalse_shouldReturnFalse() {
        SettingsShadowResources.overrideResource(R.bool.config_swipe_up_gesture_setting_available,
                false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isChecked_noDefault_true() {
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_valueFalse_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_TOUCH_GESTURE_ENABLED, 0);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_valueTrue_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_TOUCH_GESTURE_ENABLED, 1);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void onPreferenceChange_preferenceChecked_valueTrue() {
        mController.onPreferenceChange(null, true);
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_TOUCH_GESTURE_ENABLED, -1)).isEqualTo(1);
    }

    @Test
    public void onPreferenceChange_preferenceUnchecked_valueFalse() {
        mController.onPreferenceChange(null, false);
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_TOUCH_GESTURE_ENABLED, -1)).isEqualTo(0);
    }
}
