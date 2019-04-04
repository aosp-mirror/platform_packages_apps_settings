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

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import static com.android.settings.gestures.SystemNavigationEdgeToEdgePreferenceController.PREF_KEY_EDGE_TO_EDGE;
import static com.android.settings.gestures.SystemNavigationLegacyPreferenceController.PREF_KEY_LEGACY;
import static com.android.settings.gestures.SystemNavigationSwipeUpPreferenceController.PREF_KEY_SWIPE_UP;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.om.IOverlayManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

import androidx.preference.PreferenceScreen;

import com.android.internal.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.widget.RadioButtonPreference;

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
public class SystemNavigationLegacyPreferenceControllerTest {

    private Context mContext;
    private ShadowPackageManager mPackageManager;

    @Mock
    private IOverlayManager mOverlayManager;

    private SystemNavigationLegacyPreferenceController mController;

    private static final String ACTION_QUICKSTEP = "android.intent.action.QUICKSTEP_SERVICE";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        SettingsShadowResources.overrideResource(R.bool.config_swipe_up_gesture_setting_available,
                true);

        mContext = RuntimeEnvironment.application;
        mPackageManager = Shadows.shadowOf(mContext.getPackageManager());

        mController = new SystemNavigationLegacyPreferenceController(mContext, mOverlayManager,
                PREF_KEY_LEGACY);
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

        assertThat(SystemNavigationLegacyPreferenceController.isGestureAvailable(mContext))
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

        assertThat(
                SystemNavigationLegacyPreferenceController.isGestureAvailable(mContext)).isFalse();
    }

    @Test
    public void testIsGestureAvailable_noMatchingServiceExists_shouldReturnFalse() {
        assertThat(
                SystemNavigationLegacyPreferenceController.isGestureAvailable(mContext)).isFalse();
    }

    @Test
    public void testIsChecked_defaultIsLegacy_shouldReturnTrue() {
        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_3BUTTON);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void testIsChecked_defaultIsSwipeUp_shouldReturnFalse() {
        // Turn on the Swipe Up mode (2-buttons)
        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_2BUTTON);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void testIsChecked_defaultIsEdgeToEdge_shouldReturnFalse() {
        // Turn on the Edge to Edge
        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_GESTURAL);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void testIsChecked_radioButtonClicked_shouldReturnTrue() {
        SettingsShadowResources.overrideResource(R.integer.config_navBarInteractionMode,
                NAV_BAR_MODE_3BUTTON);
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
        mController.onRadioButtonClicked(radioLegacy);

        verify(radioLegacy, times(1)).setChecked(true);
        verify(radioSwipeUp, times(1)).setChecked(false);
        verify(radioEdgeToEdge, times(1)).setChecked(false);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        assertThat(mController.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final SystemNavigationLegacyPreferenceController controller =
                new SystemNavigationLegacyPreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }
}
