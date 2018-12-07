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
package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.robolectric.RuntimeEnvironment.application;

import android.util.AttributeSet;

import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.RestrictedLockUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUserManager.class)
public class TimeoutListPreferenceTest {
    private static final CharSequence[] VALUES =
            {"15000", "30000", "60000", "120000", "300000", "600000"};
    private static final CharSequence[] ENTRIES = {"15s", "30s", "1m", "2m", "5m", "10m"};

    @Mock
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;

    private TimeoutListPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        AttributeSet attributeSet = Robolectric.buildAttributeSet().build();

        mPreference = new TimeoutListPreference(application, attributeSet);
        ReflectionHelpers.setField(mPreference, "mInitialValues", VALUES);
        ReflectionHelpers.setField(mPreference, "mInitialEntries", ENTRIES);
        ReflectionHelpers.setField(mPreference, "mEntries", ENTRIES);
        ReflectionHelpers.setField(mPreference, "mEntryValues", VALUES);
        ReflectionHelpers.setField(mPreference, "mAdmin", mEnforcedAdmin);
    }

    @Test
    public void removeUnusableTimeouts_selectedValueRemoved_shouldSetValueToLargestAllowedValue() {
        mPreference.setValue("600000"); // set to 10 minutes
        mPreference.removeUnusableTimeouts(480000L, mEnforcedAdmin); // max allowed is 8 minutes

        // should set to largest allowed value, which is 5 minute
        assertThat(mPreference.getValue()).isEqualTo("300000");
    }
}
