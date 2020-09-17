/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Arrays;
import java.util.Collection;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class PowerMenuPrivacyPreferenceControllerAvailabilityTest {

    private static final String CONTROLS_ENABLED = Settings.Secure.CONTROLS_ENABLED;
    private static final String CONTROLS_FEATURE = PackageManager.FEATURE_CONTROLS;
    private static final String CARDS_ENABLED = Settings.Secure.GLOBAL_ACTIONS_PANEL_ENABLED;
    private static final String CARDS_AVAILABLE = Settings.Secure.GLOBAL_ACTIONS_PANEL_AVAILABLE;

    @ParameterizedRobolectricTestRunner.Parameters(
            name = "ctrls available={0}, ctrls enabled={1}, cards available={2}, cards enabled={3}")
    public static Collection data() {
        return Arrays.asList(new Object[][]{
                // controls available, controls enabled, cards available, cards enabled, available
                {false, false, false, false, BasePreferenceController.DISABLED_DEPENDENT_SETTING},
                {false, false, false, true, BasePreferenceController.DISABLED_DEPENDENT_SETTING},
                {false, false, true, false, BasePreferenceController.DISABLED_DEPENDENT_SETTING},
                {false, false, true, true, BasePreferenceController.AVAILABLE},
                {false, true, false, false, BasePreferenceController.DISABLED_DEPENDENT_SETTING},
                {false, true, false, true, BasePreferenceController.DISABLED_DEPENDENT_SETTING},
                {false, true, true, false, BasePreferenceController.DISABLED_DEPENDENT_SETTING},
                {false, true, true, true, BasePreferenceController.AVAILABLE},
                {true, false, false, false, BasePreferenceController.DISABLED_DEPENDENT_SETTING},
                {true, false, false, true, BasePreferenceController.DISABLED_DEPENDENT_SETTING},
                {true, false, true, false, BasePreferenceController.DISABLED_DEPENDENT_SETTING},
                {true, false, true, true, BasePreferenceController.AVAILABLE},
                {true, true, false, false, BasePreferenceController.AVAILABLE},
                {true, true, false, true, BasePreferenceController.AVAILABLE},
                {true, true, true, false, BasePreferenceController.AVAILABLE},
                {true, true, true, true, BasePreferenceController.AVAILABLE}
        });
    }

    private Context mContext;
    private PowerMenuPrivacyPreferenceController mController;
    private ShadowPackageManager mShadowPackageManager;

    @Mock
    private LockPatternUtils mLockPatternUtils;

    private boolean mControlsAvailable;
    private boolean mControlsEnabled;
    private boolean mCardsAvailable;
    private boolean mCardsEnabled;
    private int mAvailable;

    public PowerMenuPrivacyPreferenceControllerAvailabilityTest(
            boolean controlsAvailable,
            boolean controlsEnabled,
            boolean cardsAvailable,
            boolean cardsEnabled,
            int available) {
        mControlsAvailable = controlsAvailable;
        mControlsEnabled = controlsEnabled;
        mCardsAvailable = cardsAvailable;
        mCardsEnabled = cardsEnabled;
        mAvailable = available;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mShadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();

        // For these tests we assume the device has a secure lock.
        when(featureFactory.securityFeatureProvider.getLockPatternUtils(mContext))
                .thenReturn(mLockPatternUtils);
        when(mLockPatternUtils.isSecure(anyInt())).thenReturn(true);

        mController = new PowerMenuPrivacyPreferenceController(mContext, "TEST_KEY");
    }

    @Test
    public void getAvailabilityStatus_possiblyAvailableAndEnabled() {
        mShadowPackageManager.setSystemFeature(CONTROLS_FEATURE, mControlsAvailable);
        ContentResolver cr = mContext.getContentResolver();
        Settings.Secure.putInt(cr, CONTROLS_ENABLED, mControlsEnabled ? 1 : 0);
        Settings.Secure.putInt(cr, CARDS_AVAILABLE, mCardsAvailable ? 1 : 0);
        Settings.Secure.putInt(cr, CARDS_ENABLED, mCardsEnabled ? 1 : 0);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(mAvailable);
    }
}
