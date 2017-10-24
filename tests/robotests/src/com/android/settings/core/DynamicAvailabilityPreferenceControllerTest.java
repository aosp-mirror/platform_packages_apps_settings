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

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DynamicAvailabilityPreferenceController}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class DynamicAvailabilityPreferenceControllerTest {

    private final String PREFERENCE_KEY = "preference_key";

    private @Mock Context mContext;
    private @Mock Preference mPreference;
    private @Mock PreferenceScreen mScreen;
    private @Mock Lifecycle mLifecycle;
    private @Mock PreferenceAvailabilityObserver mObserver;

    private boolean mIsAvailable;
    private Preference mUpdatedPreference = null;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mPreference.getKey()).thenReturn(PREFERENCE_KEY);
        when(mScreen.findPreference(PREFERENCE_KEY)).thenReturn(mPreference);
        when(mScreen.getPreferenceCount()).thenReturn(1);
        when(mScreen.getPreference(0)).thenReturn(mPreference);
    }

    @Test
    public void testAvailableToUnavailable() {
        mIsAvailable = true;

        final DynamicAvailabilityPreferenceController controller
                = new DynamicAvailabilityPreferenceControllerTestable(mLifecycle);
        verify(mLifecycle).addObserver(controller);

        controller.displayPreference(mScreen);
        verify(mScreen, never()).removePreference(mPreference);
        verify(mScreen, never()).addPreference(mPreference);
        assertThat(mUpdatedPreference).isNull();

        controller.onResume();
        verify(mScreen, never()).removePreference(mPreference);
        verify(mScreen, never()).addPreference(mPreference);
        assertThat(mUpdatedPreference).isEqualTo(mPreference);

        mUpdatedPreference = null;
        mIsAvailable = false;
        controller.onResume();
        verify(mScreen).removePreference(mPreference);
        verify(mScreen, never()).addPreference(mPreference);
        assertThat(mUpdatedPreference).isNull();
    }

    @Test
    public void testUnavailableToAvailable() {
        mIsAvailable = false;

        final DynamicAvailabilityPreferenceController controller
                = new DynamicAvailabilityPreferenceControllerTestable(mLifecycle);
        verify(mLifecycle).addObserver(controller);

        controller.displayPreference(mScreen);
        verify(mScreen).removePreference(mPreference);
        verify(mScreen, never()).addPreference(mPreference);
        assertThat(mUpdatedPreference).isNull();

        reset(mScreen);
        controller.onResume();
        verify(mScreen, never()).removePreference(mPreference);
        verify(mScreen, never()).addPreference(mPreference);
        assertThat(mUpdatedPreference).isNull();

        mIsAvailable = true;
        controller.onResume();
        verify(mScreen, never()).removePreference(mPreference);
        verify(mScreen).addPreference(mPreference);
        assertThat(mUpdatedPreference).isEqualTo(mPreference);
    }

    @Test
    public void testNotifyOnAvailabilityUpdate() {
        final DynamicAvailabilityPreferenceController controller
                = new DynamicAvailabilityPreferenceControllerTestable(mLifecycle);
        controller.setAvailabilityObserver(mObserver);
        assertThat(controller.getAvailabilityObserver()).isEqualTo(mObserver);

        mIsAvailable = false;
        controller.isAvailable();
        verify(mObserver).onPreferenceAvailabilityUpdated(PREFERENCE_KEY, false);

        mIsAvailable = true;
        controller.isAvailable();
        verify(mObserver).onPreferenceAvailabilityUpdated(PREFERENCE_KEY, true);
    }

    private class DynamicAvailabilityPreferenceControllerTestable
            extends DynamicAvailabilityPreferenceController {
        public DynamicAvailabilityPreferenceControllerTestable(Lifecycle lifecycle) {
            super(DynamicAvailabilityPreferenceControllerTest.this.mContext, lifecycle);
        }

        @Override
        public boolean isAvailable() {
            notifyOnAvailabilityUpdate(mIsAvailable);
            return mIsAvailable;
        }

        @Override
        public void updateState(Preference preference) {
            mUpdatedPreference = preference;
        }

        @Override
        public String getPreferenceKey() {
            return PREFERENCE_KEY;
        }
    }
}
