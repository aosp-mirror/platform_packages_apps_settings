/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.provider.Settings.Secure.SYSTEM_NAVIGATION_KEYS_ENABLED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.provider.Settings;

import com.android.settings.dashboard.suggestions.SuggestionFeatureProviderImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SwipeToNotificationPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private FingerprintManager mFingerprintManager;

    private SwipeToNotificationPreferenceController mController;
    private static final String KEY_SWIPE_DOWN = "gesture_swipe_down_fingerprint";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new SwipeToNotificationPreferenceController(mContext, KEY_SWIPE_DOWN);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(Context.FINGERPRINT_SERVICE))
                .thenReturn(mFingerprintManager);
    }

    @Test
    public void isAvailable_hardwareNotAvailable_shouldReturnFalse() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_supportSystemNavigationKeys))
                .thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_configIsTrue_shouldReturnTrue() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_supportSystemNavigationKeys))
                .thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_configIsFalse_shouldReturnFalse() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_supportSystemNavigationKeys))
                .thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsChecked_configIsSet_shouldReturnTrue() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        // Set the setting to be enabled.
        final Context context = RuntimeEnvironment.application;
        Settings.Secure.putInt(context.getContentResolver(), SYSTEM_NAVIGATION_KEYS_ENABLED, 1);
        mController = new SwipeToNotificationPreferenceController(context, KEY_SWIPE_DOWN);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void testIsChecked_configIsNotSet_shouldReturnFalse() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        // Set the setting to be disabled.
        final Context context = RuntimeEnvironment.application;
        Settings.Secure.putInt(context.getContentResolver(), SYSTEM_NAVIGATION_KEYS_ENABLED, 0);
        mController = new SwipeToNotificationPreferenceController(context, KEY_SWIPE_DOWN);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isSuggestionCompleted_configDisabled_shouldReturnTrue() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_supportSystemNavigationKeys))
                .thenReturn(false);

        assertThat(SwipeToNotificationPreferenceController.isSuggestionComplete(
                mContext, null /* prefs */))
                .isTrue();
    }

    @Test
    public void isSuggestionCompleted_notVisited_shouldReturnFalse() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_supportSystemNavigationKeys))
                .thenReturn(true);
        // No stored value in shared preferences if not visited yet.
        final Context context = RuntimeEnvironment.application;
        final SharedPreferences prefs = new SuggestionFeatureProviderImpl(context)
                .getSharedPrefs(context);

        assertThat(SwipeToNotificationPreferenceController.isSuggestionComplete(mContext, prefs))
                .isFalse();
    }

    @Test
    public void isSuggestionCompleted_visited_shouldReturnTrue() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_supportSystemNavigationKeys))
                .thenReturn(true);
        // No stored value in shared preferences if not visited yet.
        final Context context = RuntimeEnvironment.application;
        final SharedPreferences prefs = new SuggestionFeatureProviderImpl(context)
                .getSharedPrefs(context);
        prefs.edit()
                .putBoolean(SwipeToNotificationSettings.PREF_KEY_SUGGESTION_COMPLETE, true)
                .commit();

        assertThat(SwipeToNotificationPreferenceController.isSuggestionComplete(mContext, prefs))
                .isTrue();
    }

    private void stubFingerprintSupported(boolean enabled) {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(enabled);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final SwipeToNotificationPreferenceController controller = new
                SwipeToNotificationPreferenceController(mContext,"gesture_swipe_down_fingerprint");
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final SwipeToNotificationPreferenceController controller =
                new SwipeToNotificationPreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }
}
