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
package com.android.settings.core;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;
import static com.android.settings.core.BasePreferenceController.DISABLED_FOR_USER;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.slices.SliceData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BasePreferenceControllerTest {

    private final String KEY = "fake_key";

    private Context mContext;
    private FakeBasePreferenceController mPreferenceController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreferenceController = new FakeBasePreferenceController(mContext, KEY);
    }


    @Test(expected = IllegalArgumentException.class)
    public void newController_noKey_shouldCrash() {
        new BasePreferenceController(RuntimeEnvironment.application, null /* key */) {
            @Override
            public int getAvailabilityStatus() {
                return AVAILABLE;
            }
        };
    }

    @Test
    public void isAvailable_availableStatusAvailable_returnsTrue() {
        mPreferenceController.setAvailability(AVAILABLE);

        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_availableStatusUnSearchable_returnsTrue() {
        mPreferenceController.setAvailability(AVAILABLE_UNSEARCHABLE);

        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_availableStatusUnsupportedOnDevice_returnsFalse() {
        mPreferenceController.setAvailability(UNSUPPORTED_ON_DEVICE);

        assertThat(mPreferenceController.isAvailable()).isFalse();
    }


    @Test
    public void isAvailable_availableStatusConditionallyUnavailable_returnsFalse() {
        mPreferenceController.setAvailability(CONDITIONALLY_UNAVAILABLE);

        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_availableStatusDisabledForUser_returnsFalse() {
        mPreferenceController.setAvailability(DISABLED_FOR_USER);

        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_availableStatusBlockedDependent_returnsFalse() {
        mPreferenceController.setAvailability(DISABLED_DEPENDENT_SETTING);

        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    public void isSupported_availableStatusAvailable_returnsTrue() {
        mPreferenceController.setAvailability(AVAILABLE);

        assertThat(mPreferenceController.isSupported()).isTrue();
    }

    @Test
    public void isSupported_availableStatusUnsupported_returnsFalse() {
        mPreferenceController.setAvailability(UNSUPPORTED_ON_DEVICE);

        assertThat(mPreferenceController.isSupported()).isFalse();
    }

    @Test
    public void isSupported_availableStatusDisabledForUser_returnsTrue() {
        mPreferenceController.setAvailability(DISABLED_FOR_USER);

        assertThat(mPreferenceController.isSupported()).isTrue();
    }

    @Test
    public void isSupported_availableStatusDependentSetting_returnsTrue() {
        mPreferenceController.setAvailability(DISABLED_DEPENDENT_SETTING);

        assertThat(mPreferenceController.isSupported()).isTrue();
    }

    @Test
    public void getSliceType_shouldReturnIntent() {
        assertThat(mPreferenceController.getSliceType()).isEqualTo(SliceData.SliceType.INTENT);
    }

    @Test
    public void hasAsyncUpdate_shouldReturnFalse() {
        assertThat(mPreferenceController.hasAsyncUpdate()).isFalse();
    }

    @Test
    public void settingAvailable_disabledOnDisplayPreference_preferenceEnabled() {
        final PreferenceScreen screen = mock(PreferenceScreen.class);
        final Preference preference = new Preference(mContext);
        preference.setEnabled(true);
        when(screen.findPreference(anyString())).thenReturn(preference);

        mPreferenceController.displayPreference(screen);

        assertThat(preference.isEnabled()).isTrue();
    }

    @Test
    public void disabledDependentSetting_disabledOnDisplayPreference_preferenceDisabled() {
        final PreferenceScreen screen = mock(PreferenceScreen.class);
        final Preference preference = new Preference(mContext);
        preference.setEnabled(true);
        when(screen.findPreference(anyString())).thenReturn(preference);
        mPreferenceController.setAvailability(DISABLED_DEPENDENT_SETTING);

        mPreferenceController.displayPreference(screen);

        assertThat(preference.isEnabled()).isFalse();
    }

    @Test
    public void updateNonIndexableKeys_controllerUnavailable_shouldAddKey() {
        final List<String> keys = new ArrayList<>();
        mPreferenceController.setAvailability(UNSUPPORTED_ON_DEVICE);

        mPreferenceController.updateNonIndexableKeys(keys);

        assertThat(keys).containsExactly(mPreferenceController.getPreferenceKey());
    }

    @Test
    public void updateNonIndexableKeys_controllerUnsearchable_shouldAddKey() {
        final List<String> keys = new ArrayList<>();
        mPreferenceController.setAvailability(AVAILABLE_UNSEARCHABLE);

        mPreferenceController.updateNonIndexableKeys(keys);

        assertThat(keys).containsExactly(mPreferenceController.getPreferenceKey());
    }

    @Test
    public void updateNonIndexableKeys_controllerAvailable_shouldNotAddKey() {
        final List<String> keys = new ArrayList<>();
        mPreferenceController.setAvailability(AVAILABLE);

        mPreferenceController.updateNonIndexableKeys(keys);

        assertThat(keys).isEmpty();
    }

    @Test
    public void getMetricsCategory_metricsCategoryIsSet_shouldReturnTheSameCategory() {
        mPreferenceController.setMetricsCategory(SettingsEnums.DISPLAY);

        final int category = mPreferenceController.getMetricsCategory();

        assertThat(category).isEqualTo(SettingsEnums.DISPLAY);
    }

    private class FakeBasePreferenceController extends BasePreferenceController {

        private int mAvailable;

        private FakeBasePreferenceController(Context context, String preferenceKey) {
            super(context, preferenceKey);
            mAvailable = AVAILABLE;
        }

        @Override
        public int getAvailabilityStatus() {
            return mAvailable;
        }

        private void setAvailability(int availability) {
            mAvailable = availability;
        }
    }
}