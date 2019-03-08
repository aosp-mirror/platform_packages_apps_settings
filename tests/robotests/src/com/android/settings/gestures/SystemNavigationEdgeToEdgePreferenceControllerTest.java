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
 * limitations under the License.
 */

package com.android.settings.gestures;

import static com.android.settings.gestures.SystemNavigationEdgeToEdgePreferenceController.PREF_KEY_EDGE_TO_EDGE;
import static com.android.settings.gestures.SystemNavigationLegacyPreferenceController.PREF_KEY_LEGACY;
import static com.android.settings.gestures.SystemNavigationSwipeUpPreferenceController.PREF_KEY_SWIPE_UP;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.widget.RadioButtonPreference;

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
public class SystemNavigationEdgeToEdgePreferenceControllerTest {

    private Context mContext;
    private ShadowPackageManager mPackageManager;

    private SystemNavigationEdgeToEdgePreferenceController mController;

    private static final String ACTION_QUICKSTEP = "android.intent.action.QUICKSTEP_SERVICE";

    @Before
    public void setUp() {
        SettingsShadowResources.overrideResource(R.bool.config_swipe_up_gesture_setting_available,
                true);
        SettingsShadowResources.overrideResource(R.bool.config_swipe_up_gesture_default, true);

        mContext = RuntimeEnvironment.application;
        Settings.Global.putInt(mContext.getContentResolver(), "prototype_enabled", 1);

        mPackageManager = Shadows.shadowOf(mContext.getPackageManager());

        mController = new SystemNavigationEdgeToEdgePreferenceController(mContext,
                PREF_KEY_EDGE_TO_EDGE);
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

        assertThat(SystemNavigationEdgeToEdgePreferenceController.isGestureAvailable(mContext))
                .isTrue();
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

        assertThat(SystemNavigationEdgeToEdgePreferenceController.isGestureAvailable(mContext))
                .isFalse();
    }

    @Test
    public void testIsGestureAvailable_noMatchingServiceExists_shouldReturnFalse() {
        assertThat(SystemNavigationEdgeToEdgePreferenceController.isGestureAvailable(mContext))
                .isFalse();
    }

    @Test
    public void testIsChecked_defaultIsTrue_shouldReturnTrue() {
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void testIsChecked_defaultIsFalse_shouldReturnFalse() {
        Settings.Global.putInt(mContext.getContentResolver(), "prototype_enabled", 0);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void testIsChecked_radioButtonClicked_shouldReturnTrue() {
        // Set the setting to be enabled.
        mController.onRadioButtonClicked(null);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void testOnRadioButtonClicked_setsCorrectRadioButtonChecked() {
        RadioButtonPreference radioLegacy = mock(RadioButtonPreference.class);
        RadioButtonPreference radioSwipeUp = mock(RadioButtonPreference.class);
        RadioButtonPreference radioEdgeToEdge = mock(RadioButtonPreference.class);
        PreferenceScreen screen = mock(PreferenceScreen.class);

        when(screen.findPreference(PREF_KEY_LEGACY)).thenReturn(radioLegacy);
        when(screen.findPreference(PREF_KEY_SWIPE_UP)).thenReturn(radioSwipeUp);
        when(screen.findPreference(PREF_KEY_EDGE_TO_EDGE)).thenReturn(radioEdgeToEdge);

        mController.displayPreference(screen);
        mController.onRadioButtonClicked(radioEdgeToEdge);

        verify(radioLegacy, times(1)).setChecked(false);
        verify(radioSwipeUp, times(1)).setChecked(false);
        verify(radioEdgeToEdge, times(1)).setChecked(true);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        assertThat(mController.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final SystemNavigationEdgeToEdgePreferenceController controller =
                new SystemNavigationEdgeToEdgePreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }
}
